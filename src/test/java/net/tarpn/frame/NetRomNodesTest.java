package net.tarpn.frame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.tarpn.Configuration;
import net.tarpn.network.netrom.NetRomRouter;
import net.tarpn.network.netrom.NetRomRouter.Destination;
import net.tarpn.network.netrom.NetRomRouter.Destination.DestinationRoute;
import net.tarpn.network.netrom.NetRomRouter.Neighbor;
import net.tarpn.packet.impl.AX25PacketReader;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.UIFrame;
import net.tarpn.network.netrom.NetRomNodes;
import org.junit.Test;

public class NetRomNodesTest {
  @Test
  public void testTarpnData() throws IOException {
    String[] nodesFiles = new String[] {
        "data/tadd-nodes-part-1.bin",
        "data/tadd-nodes-part-2.bin",
        "data/doug-nodes.bin"
    };

    NetRomRouter router = new NetRomRouter();

    for(String nodeFile : nodesFiles) {
      byte[] packetBytes = Files.readAllBytes(Paths.get(nodeFile));
      AX25Packet packet = AX25PacketReader.parse(packetBytes);
      byte[] info = ((UIFrame)packet).getInfo();
      NetRomNodes nodes = NetRomNodes.read(info);
      System.err.println("Routing table for " + nodes.getSendingAlias());
      for(NetRomNodes.NodeDestination dest : nodes.getDestinationList()) {
        System.err.println(dest.getDestNode() + "\t" + dest.getDestAlias() + "\t" + dest.getBestNeighborNode() + "\t" + Integer.toString((int)dest.getQuality() & 0xff));
      }

      Neighbor neighbor = router.getNeighbors().computeIfAbsent(packet.getSourceCall(),
          call -> new Neighbor(call, 0, 255));

      Destination destination = router.getDestinations().computeIfAbsent(packet.getSourceCall(),
          call -> new Destination(call, nodes.getSendingAlias())
      );

      // Add direct route to whoever send the NODES
      destination.getNeighbors().add(new DestinationRoute(packet.getSourceCall(), 255));

      nodes.getDestinationList().forEach(nodeDestination -> {
        final int routeQuality;
        if(nodeDestination.getBestNeighborNode().equals(Configuration.getOwnNodeCallsign())) {
          // Best neighbor is us, this is a "trivial loop", quality is zero
          routeQuality = 0;
        } else {
          int qualityProduct = nodeDestination.getQuality() * neighbor.getQuality();
          routeQuality = (qualityProduct + 128) / 256;
        }
        Destination neighborDest = router.getDestinations().computeIfAbsent(nodeDestination.getDestNode(),
            call -> new Destination(call, nodeDestination.getDestAlias())
        );
        neighborDest.getNeighbors().add(new DestinationRoute(neighbor.getNodeCall(), routeQuality));
      });
    }

    router.getDestinations().forEach((call, dest) -> {
      System.err.println(dest.getNodeCall() + ":" + dest.getNodeAlias());
      dest.getNeighbors().forEach(System.err::println);
    });

    // find route for call
    AX25Call targetCall = AX25Call.fromString("N3LTV-2");
    Destination destination = router.getDestinations().get(targetCall);
    if(destination != null) {
      System.err.println("Found routes to " + targetCall);
      System.err.println(destination.getNeighbors());
      // loop through routes for this dest in order of quality
      // if there's an existing link to this neighbor, use it
      // if not, open a link and send the frame
    } else {
      System.err.println("No route to " + targetCall);
    }

  }
}
