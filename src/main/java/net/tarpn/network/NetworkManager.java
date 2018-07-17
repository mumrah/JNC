package net.tarpn.network;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.tarpn.util.Util;
import net.tarpn.config.NetRomConfig;
import net.tarpn.config.PortConfig;
import net.tarpn.datalink.DataLinkManager;
import net.tarpn.datalink.LinkPrimitive;
import net.tarpn.datalink.LinkPrimitive.Type;
import net.tarpn.io.DataPort;
import net.tarpn.io.impl.PortFactory;
import net.tarpn.network.netrom.NetRomCircuitEvent;
import net.tarpn.network.netrom.NetRomCircuitManager;
import net.tarpn.network.netrom.NetRomNodes;
import net.tarpn.network.netrom.NetRomPacket;
import net.tarpn.network.netrom.NetRomRouter;
import net.tarpn.network.netrom.NetRomRouter.Neighbor;
import net.tarpn.network.netrom.NetRomSession;
import net.tarpn.packet.PacketHandler;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.FrameType;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;
import net.tarpn.packet.impl.ax25.AX25State;
import net.tarpn.packet.impl.ax25.AX25State.State;
import net.tarpn.packet.impl.ax25.AX25StateEvent;
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

  private final NetRomConfig netromConfig;
  private final Queue<LinkPrimitive> level2Events;
  private final Map<Integer, DataLinkManager> dataPorts;
  private final NetRomRouter router;
  private final NetRomCircuitManager circuitManager;

  private NetworkManager(NetRomConfig netromConfig, Consumer<NetRomCircuitEvent> networkEvents) {
    this.netromConfig = netromConfig;
    this.level2Events = new ConcurrentLinkedQueue<>();
    this.dataPorts = new HashMap<>();
    this.router = new NetRomRouter(netromConfig, portNum -> dataPorts.get(portNum).getPortConfig());

    Consumer<NetRomPacket> packetRouter = netRomPacket ->
        route(LinkPrimitive.newDataRequest(netRomPacket.getDestNode(), Protocol.NETROM, netRomPacket.getPayload()));

    this.circuitManager = new NetRomCircuitManager(netromConfig, packetRouter, networkEvents);
  }

  public static NetworkManager create(NetRomConfig config, Consumer<NetRomCircuitEvent> networkEvents) {
    return new NetworkManager(config, networkEvents);
  }

  /**
   * A special {@link PacketHandler} which handles routing table packets sent to the NODES destination
   * @return
   */
  public PacketHandler getNetRomNodesHandler() {
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

  public void initialize(PortConfig portConfig) {
    PacketHandler netRomNodesHandler = getNetRomNodesHandler();
    if(portConfig.isEnabled()) {
      DataPort dataPort = PortFactory.createPortFromConfig(portConfig);
      DataLinkManager portManager = DataLinkManager.create(
          portConfig, dataPort, level2Events::add, netRomNodesHandler, executorService);

      dataPorts.put(dataPort.getPortNumber(), portManager);
    }
  }

  /**
   * Accept a level 2 primitive, find the appropriate port to route it to, and send it
   */
  private void route(LinkPrimitive level2Primitive) {
    List<AX25Call> potentialRoutes = router.routePacket(level2Primitive.getRemoteCall());
    boolean routed = false;

    for(AX25Call route : potentialRoutes) {
      Neighbor neighbor = router.getNeighbors().get(route);
      int routePort = neighbor.getPort();
      DataLinkManager portManager = dataPorts.get(routePort);
      AX25State state = portManager.getAx25StateHandler().getState(route);
      if(state.getState().equals(State.DISCONNECTED)) {
        portManager.acceptDataLinkPrimitive(LinkPrimitive.newConnectRequest(neighbor.getNodeCall()));
      }
      // Change the dest address to the neighbor and send it
      LinkPrimitive readdressed = level2Primitive.copyOf(neighbor.getNodeCall());
      portManager.acceptDataLinkPrimitive(readdressed);
      routed = true;
      break;
    }

    if(!routed) {
      LOG.warn("No route to destination " + level2Primitive.getRemoteCall());
    }
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

      // Start event processing
      Util.queueProcessingLoop(
          level2Events::poll,
          dataLinkPrimitive -> {
            LOG.info("Got DL event " + dataLinkPrimitive);
            // Only pass data and unit data up to L3, everything else is ignored
            if(dataLinkPrimitive.getType().equals(Type.DL_DATA) || dataLinkPrimitive.getType().equals(Type.DL_UNIT_DATA)) {
              circuitManager.onPacket(dataLinkPrimitive.getPacket());
            }
          },
          (failedEvent, t) -> LOG.error("Error processing event " + failedEvent, t));
    });

    // Send automatic NODES packets
    executorService.scheduleAtFixedRate(() -> {
      NetRomNodes nodes = router.getNodes();
      byte[] nodesData = NetRomNodes.write(nodes);
      for(DataLinkManager portManager : dataPorts.values()) {
        LOG.info("Sending automatic NODES message on " + portManager.getDataPort() + ": " + nodes);
        portManager.getAx25StateHandler().getEventQueue().add(
            AX25StateEvent.createUnitDataEvent(AX25Call.create("NODES"), Protocol.NETROM, nodesData));
        //Thread.sleep(2000); // Slight delay between broadcasts
      }
    }, 15, netromConfig.getNodesInterval(), TimeUnit.SECONDS);

    // Prune routing table
    executorService.scheduleAtFixedRate(router::pruneRoutes,
        30, netromConfig.getNodesInterval(), TimeUnit.SECONDS);
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

  public NetRomSession open(AX25Call address) {
    int circuitId = circuitManager.open(address);
    if(circuitId == -1) {
      throw new RuntimeException("All circuits busy.");
    } else {
      return new NetRomSession(circuitId, address, circuitManager::onCircuitEvent);
    }
  }
}
