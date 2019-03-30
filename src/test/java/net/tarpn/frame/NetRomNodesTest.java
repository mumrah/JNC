package net.tarpn.frame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.tarpn.config.Configs;
import net.tarpn.network.netrom.NetRomNodes;
import net.tarpn.network.netrom.NetRomRoutingTable;
import net.tarpn.packet.impl.AX25PacketReader;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.UIFrame;
import org.junit.Test;

public class NetRomNodesTest {
  @Test
  public void testTarpnData() throws IOException {
    String[] nodesFiles = new String[] {
        "data/tadd-nodes-part-1.bin",
        "data/tadd-nodes-part-1.bin",
        //"data/doug-nodes.bin"
    };

    Configs configs = Configs.read("conf/sample.ini");
    NetRomRoutingTable router = new NetRomRoutingTable(configs.getNetRomConfig(), portNum -> configs.getPortConfigs().get(portNum));

    for(String nodeFile : nodesFiles) {
      byte[] packetBytes = Files.readAllBytes(Paths.get(nodeFile));
      AX25Packet packet = AX25PacketReader.parse(packetBytes);
      byte[] info = ((UIFrame)packet).getInfo();
      NetRomNodes nodes = NetRomNodes.read(info);
      router.updateNodes(packet.getSourceCall(), 1, nodes);
    }

    router.pruneRoutes();
    router.pruneRoutes();
    router.pruneRoutes();


    // find route for call
    /*
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
    }*/

    System.err.println(router.toPrettyString());

  }
}
