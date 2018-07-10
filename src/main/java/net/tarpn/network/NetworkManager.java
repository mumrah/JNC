package net.tarpn.network;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.tarpn.config.Configuration;
import net.tarpn.io.DataPort;
import net.tarpn.io.impl.DataPortManager;
import net.tarpn.network.netrom.NetRomCircuitManager;
import net.tarpn.network.netrom.NetRomNodes;
import net.tarpn.network.netrom.NetRomPacket;
import net.tarpn.network.netrom.NetRomRouter;
import net.tarpn.packet.PacketHandler;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Command;
import net.tarpn.packet.impl.ax25.AX25Packet.FrameType;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;
import net.tarpn.packet.impl.ax25.AX25State;
import net.tarpn.packet.impl.ax25.AX25State.State;
import net.tarpn.packet.impl.ax25.AX25StateEvent;
import net.tarpn.packet.impl.ax25.IFrame;
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
  private final Queue<AX25Packet> inboundPackets;
  private final Map<Integer, DataPortManager> dataPorts;
  private final NetRomRouter router;


  private NetworkManager(Configuration config) {
    this.config = config;
    this.inboundPackets = new ConcurrentLinkedQueue<>();
    this.dataPorts = new HashMap<>();
    this.router = new NetRomRouter(config);
  }

  public static NetworkManager create(Configuration config) {
    return new NetworkManager(config);
  }

  public void addPort(DataPort dataPort) {
    PacketHandler netRomNodesHandler = packetRequest -> {
      if(packetRequest.getPacket() instanceof AX25Packet) {
        AX25Packet ax25Packet = (AX25Packet)packetRequest.getPacket();
        if(ax25Packet.getFrameType().equals(FrameType.UI)) {
          UIFrame uiFrame = (UIFrame)ax25Packet;
          if(uiFrame.getDestCall().equals(AX25Call.create("NODES", 0))) {
            NetRomNodes nodes = NetRomNodes.read(uiFrame.getInfo());
            router.updateNodes(uiFrame.getSourceCall(), dataPort.getPortNumber(), nodes);
            packetRequest.abort();
          }
        }
      }
    };

    try {
      dataPort.open();
    } catch (IOException e) {
      throw new RuntimeException("Could not open port " + dataPort, e);
    }

    DataPortManager portManager = DataPortManager.initialize(config, dataPort, inboundPackets::add, netRomNodesHandler);
    executorService.submit(portManager.getReaderRunnable()); // read data off the incoming port
    executorService.submit(portManager.getWriterRunnable()); // write outbound packets to the port
    executorService.submit(portManager.getAx25StateHandler().getRunnable()); // process ax.25 packets on this port
    executorService.scheduleWithFixedDelay(() -> {
      LOG.info("Sending automatic ID message on " + portManager.getDataPort());
      AX25Packet idPacket = UIFrame.create(
          AX25Call.create("ID"),
          config.getNodeCall(),
          Protocol.NO_LAYER3,
          String.format("Terrestrial Amateur Radio Packet Network node %s op is %s\r", config.getAlias(), config.getNodeCall().toString())
              .getBytes(StandardCharsets.US_ASCII));
      portManager.getAx25StateHandler().getEventQueue().add(
          AX25StateEvent.createUIEvent(idPacket));
    }, 5, 300, TimeUnit.SECONDS);
    dataPorts.put(dataPort.getPortNumber(), portManager);
  }

  public DataPortManager getPortManager(int port) {
    return dataPorts.get(port);
  }

  public Queue<AX25Packet> getInboundPackets() {
    return inboundPackets;
  }

  public void start() {
    executorService.submit(() -> {
      NetRomCircuitManager circuitManager = new NetRomCircuitManager(config);
      Consumer<NetRomPacket> packetRouter = netRomPacket -> {
        List<AX25Call> potentialRoutes = router.routePacket(netRomPacket.getDestNode());
        boolean routed = false;
        for(AX25Call route : potentialRoutes) {
          int routePort = router.getNeighbors().get(route).getPort();
          DataPortManager portManager = dataPorts.get(routePort);
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
          System.err.println("No route to destination " + netRomPacket.getDestNode());
        }
      };

      while(true) {
        AX25Packet inboundPacket = inboundPackets.poll();
        try {
          if (inboundPacket != null) {
            circuitManager.onPacket(inboundPacket, packetRouter);
          } else {
            Thread.sleep(50);
          }
        } catch (Throwable t) {
          LOG.error("Error processing packet " + inboundPacket, t);
        }
      }
    });
    executorService.scheduleAtFixedRate(() -> {
      NetRomNodes nodes = router.getNodes();
      AX25Packet nodesPacket = UIFrame.create(
          AX25Call.create("NODES"),
          config.getNodeCall(),
          Protocol.NETROM,
          NetRomNodes.write(nodes)
      );

      for(DataPortManager portManager : dataPorts.values()) {
        LOG.info("Sending automatic NODES message on " + portManager.getDataPort());
        portManager.getAx25StateHandler().getEventQueue().add(
            AX25StateEvent.createUIEvent(nodesPacket));
        //Thread.sleep(2000); // Slight delay between broadcasts
      }
    }, 15, 300, TimeUnit.SECONDS);
  }

  public void stop() {
    try {
      executorService.shutdown();
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
