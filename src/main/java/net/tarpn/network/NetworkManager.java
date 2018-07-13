package net.tarpn.network;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.tarpn.Util;
import net.tarpn.config.Configuration;
import net.tarpn.io.DataPort;
import net.tarpn.io.impl.DataLinkManager;
import net.tarpn.network.netrom.NetRomCircuitManager;
import net.tarpn.network.netrom.NetRomNodes;
import net.tarpn.network.netrom.NetRomPacket;
import net.tarpn.network.netrom.NetRomRouter;
import net.tarpn.packet.PacketHandler;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.FrameType;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;
import net.tarpn.packet.impl.ax25.AX25State;
import net.tarpn.packet.impl.ax25.AX25State.State;
import net.tarpn.packet.impl.ax25.AX25StateEvent;
import net.tarpn.packet.impl.ax25.DataLinkEvent;
import net.tarpn.packet.impl.ax25.DataLinkEvent.DataIndicationDataLinkEvent;
import net.tarpn.packet.impl.ax25.DataLinkEvent.Type;
import net.tarpn.packet.impl.ax25.UIFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Networking layer (level 3)
 *
 * Handle incoming NET/ROM packets from the Data Link layer (level 2) and decide what to do with
 * them.
 *
 */
public class NetworkManager {

  private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(128);

  private static final Logger LOG = LoggerFactory.getLogger(NetworkManager.class);

  private final Configuration config;
  private final Queue<DataLinkEvent> dataLinkEvents;
  private final Map<Integer, DataLinkManager> dataPorts;
  private final NetRomRouter router;

  private NetworkManager(Configuration config) {
    this.config = config;
    this.dataLinkEvents = new ConcurrentLinkedQueue<>();
    this.dataPorts = new HashMap<>();
    this.router = new NetRomRouter(config);
  }

  public static NetworkManager create(Configuration config) {
    return new NetworkManager(config);
  }

  /**
   * A special {@link PacketHandler} which handles routing table packets sent to the NODES destination
   * @return
   */
  public PacketHandler getNetromNodesHandler() {
    return packetRequest -> {
      if(packetRequest.getPacket() instanceof AX25Packet) {
        AX25Packet ax25Packet = (AX25Packet)packetRequest.getPacket();
        if(ax25Packet.getFrameType().equals(FrameType.UI)) {
          UIFrame uiFrame = (UIFrame)ax25Packet;
          if(uiFrame.getDestCall().equals(AX25Call.create("NODES", 0))) {
            NetRomNodes nodes = NetRomNodes.read(uiFrame.getInfo());
            router.updateNodes(uiFrame.getSourceCall(), packetRequest.getPort(), nodes);
            packetRequest.abort();
          }
        }
      }
    };
  }

  public void acceptDataLinkEvent(DataLinkEvent event) {
    dataLinkEvents.add(event);
  }

  public void initialize(DataPort dataPort) {
    PacketHandler netRomNodesHandler = getNetromNodesHandler();

    DataLinkManager portManager = DataLinkManager.create(
        config, dataPort, dataLinkEvents::add, netRomNodesHandler, executorService);

    dataPorts.put(dataPort.getPortNumber(), portManager);
  }

  public DataLinkManager getPortManager(int port) {
    return dataPorts.get(port);
  }

  public Map<Integer, DataLinkManager> getPorts() {
    return dataPorts;
  }

  public void start() {
    // Start up ports
    dataPorts.values().forEach(DataLinkManager::start);

    // Start up router and event processor
    executorService.submit(() -> {
      NetRomCircuitManager circuitManager = new NetRomCircuitManager(config);
      Consumer<NetRomPacket> packetRouter = netRomPacket -> {
        List<AX25Call> potentialRoutes = router.routePacket(netRomPacket.getDestNode());
        boolean routed = false;
        for(AX25Call route : potentialRoutes) {
          int routePort = router.getNeighbors().get(route).getPort();
          DataLinkManager portManager = dataPorts.get(routePort);
          // Check if we have a connection
          AX25State state = portManager.getAx25StateHandler().getState(route);
          if(state.getState().equals(State.CONNECTED)) {
            portManager.getAx25StateHandler().getEventQueue().add(
                AX25StateEvent.createDataEvent(route, Protocol.NETROM, netRomPacket.getPayload())
            );
            routed = true;
          }
        }
        if(!routed) {
          LOG.warn("No route to destination " + netRomPacket.getDestNode());
        }
      };

      // Start event processing
      Util.queueProcessingLoop(
          dataLinkEvents::poll,
          dataLinkEvent -> {
            LOG.info("Got DL event " + dataLinkEvent.getType());
            if(dataLinkEvent.getType().equals(Type.DL_DATA) || dataLinkEvent.getType().equals(Type.DL_UNIT_DATA)) {
              circuitManager.onPacket(((DataIndicationDataLinkEvent)dataLinkEvent).getPacket(), packetRouter);
            }
          },
          (failedEvent, t) -> LOG.error("Error processing event " + failedEvent, t));
    });

    // Send automatic NODES packets
    executorService.scheduleAtFixedRate(() -> {
      NetRomNodes nodes = router.getNodes();
      AX25Packet nodesPacket = UIFrame.create(
          AX25Call.create("NODES"),
          config.getNodeCall(),
          Protocol.NETROM,
          NetRomNodes.write(nodes)
      );

      for(DataLinkManager portManager : dataPorts.values()) {
        LOG.info("Sending automatic NODES message on " + portManager.getDataPort());
        portManager.getAx25StateHandler().getEventQueue().add(
            AX25StateEvent.createUIEvent(nodesPacket));
        //Thread.sleep(2000); // Slight delay between broadcasts
      }
    }, 15, 300, TimeUnit.SECONDS);
  }

  public void stop() {
    try {
      executorService.shutdownNow();
      executorService.awaitTermination(10, TimeUnit.SECONDS);
      dataPorts.values().forEach(dataPort -> {
        try {
          dataPort.getDataPort().close();
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
      });
    } catch (InterruptedException | UncheckedIOException e) {
      throw new RuntimeException("Clean shutdown failed", e);
    }
  }
}
