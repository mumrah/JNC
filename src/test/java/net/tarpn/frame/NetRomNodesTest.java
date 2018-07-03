package net.tarpn.frame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import net.tarpn.packet.impl.AX25PacketReader;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.UIFrame;
import net.tarpn.packet.impl.netrom.NetRomNodes;
import org.junit.Test;

public class NetRomNodesTest {
  @Test
  public void testTarpnData() throws IOException {
    byte[] packetBytes = Files.readAllBytes(Paths.get("data/tadd-nodes-part-1.bin"));
    AX25Packet packet = AX25PacketReader.parse(packetBytes);
    byte[] info = ((UIFrame)packet).getInfo();
    NetRomNodes nodes = NetRomNodes.read(info);
    System.err.println("Routing table for " + nodes.getSendingAlias());
    for(NetRomNodes.NodeDestination dest : nodes.getDestinationList()) {
      System.err.println(dest.getDestNode() + "\t" + dest.getDestAlias() + "\t" + dest.getBestNeighborNode() + "\t" + Integer.toString((int)dest.getQuality() & 0xff));
    }
  }
}
