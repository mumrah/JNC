package net.tarpn.packet.impl.netrom;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.tarpn.packet.impl.ax25.AX25Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Networking layer (level 3)
 *
 * Handle incoming NET/ROM packets from the Data Link layer (level 2) and decide what to do with
 * them.
 *
 * TODO routing
 * TODO circuits (sessions)
 */
public class NetworkManager {

  private static final Logger LOG = LoggerFactory.getLogger(NetworkManager.class);

  private final Queue<AX25Packet> inboundPackets;
  private final Queue<AX25Packet> outboundPackets;

  private NetworkManager(
      Queue<AX25Packet> inboundPackets,
      Queue<AX25Packet> outboundPackets) {
    this.inboundPackets = inboundPackets;
    this.outboundPackets = outboundPackets;
  }

  public static NetworkManager create() {
    return new NetworkManager(new ConcurrentLinkedQueue<>(), new ConcurrentLinkedQueue<>());
  }


  public Queue<AX25Packet> getInboundPackets() {
    return inboundPackets;
  }

  public Queue<AX25Packet> getOutboundPackets() {
    return outboundPackets;
  }

  public Runnable getRunnable() {
    return () -> {
      NetRomHandler handler = new NetRomHandler();
      while(true) {
        AX25Packet inboundPacket = inboundPackets.poll();
        try {
          if (inboundPacket != null) {
            handler.onPacket(inboundPacket, outboundPackets::add);
          } else {
            Thread.sleep(50);
          }
        } catch (Throwable t) {
          LOG.error("Error processing packet " + inboundPacket, t);
        }
      }
    };
  }
}
