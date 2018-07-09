package net.tarpn.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import net.tarpn.Configuration;
import net.tarpn.io.DataPort;
import net.tarpn.io.impl.DataPortManager;
import net.tarpn.network.netrom.NetRomCircuitManager;
import net.tarpn.network.netrom.NetRomPacket;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Command;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;
import net.tarpn.packet.impl.ax25.IFrame;
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

  private static final ExecutorService executorService = Executors.newCachedThreadPool();
  private static final Logger LOG = LoggerFactory.getLogger(NetworkManager.class);

  private final Queue<AX25Packet> inboundPackets;
  private final Map<Integer, DataPortManager> dataPorts;
  private final Map<AX25Call, Integer> routes;


  private NetworkManager() {
    this.inboundPackets = new ConcurrentLinkedQueue<>();
    this.routes = new HashMap<>();
    this.dataPorts = new HashMap<>();
  }

  public static NetworkManager create() {
    return new NetworkManager();
  }

  public void addPort(DataPort dataPort) {
    DataPortManager portManager = DataPortManager.initialize(dataPort, inboundPackets::add, routes::put);
    executorService.submit(portManager.getReaderRunnable()); // read data off the incoming port
    executorService.submit(portManager.getWriterRunnable()); // write outbound packets to the port
    executorService.submit(portManager.getAx25StateHandler().getRunnable()); // process ax.25 packets on this port
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
      NetRomCircuitManager handler = new NetRomCircuitManager();
      Consumer<NetRomPacket> packetRouter = netRomPacket -> {
        int portNumber = routes.getOrDefault(netRomPacket.getDestNode(), -1);
        if(portNumber == -1) {
          // discard
          System.err.println("No route found for " + netRomPacket.getDestNode() + ", discarding.");
        } else {
          DataPortManager portManager = dataPorts.get(portNumber);
          //IFrame resp = IFrame.create(infoFrame.getSourceCall(), Configuration.getOwnNodeCallsign(),
          //    Command.COMMAND, (byte) 0, (byte) 0, true, Protocol.NETROM, netRomPacket.getPayload());
          //portManager.getOutboundPackets().add(resp);
        }
      };

      while(true) {
        AX25Packet inboundPacket = inboundPackets.poll();
        try {
          if (inboundPacket != null) {
            handler.onPacket(inboundPacket, packetRouter);
          } else {
            Thread.sleep(50);
          }
        } catch (Throwable t) {
          LOG.error("Error processing packet " + inboundPacket, t);
        }
      }
    });
  }

  /**
   * A neighboring node directly linked to this node
   */
  public static class Neighbor {
    AX25Call nodeCall;
    // 2 Digitpeaters
    int port;
    int quality; // 0-255
    // Port
    // Quality
    // How many routes point to this neighbor?
  }

  /**
   * Represents a "known" node in the network. These are reported by NODES
   */
  public static class Destination {
    String nodeAlias;
    AX25Call nodeCall;
    List<Route> neighbors; // up to 3

    public static class Route {
      int quality; // 0-255, 255 best, 0 worst (never used)
      int obsolescence; // 6
      AX25Call neighbor;
    }
  }

  List<Neighbor> neighborList = new ArrayList<>();

  public void updateNeighbor(AX25Call neighborCall, int heardOnPort) {
    // See if it's in the list
    boolean inNeighborList = neighborList.stream()
        .anyMatch(neighbor -> neighbor.nodeCall.equals(neighborCall));
    if(!inNeighborList) {
      Neighbor neighbor = new Neighbor();
      neighbor.nodeCall = neighborCall;
      neighbor.port = heardOnPort;
      neighbor.quality = 255;
      neighborList.add(neighbor);
    }
  }
}
