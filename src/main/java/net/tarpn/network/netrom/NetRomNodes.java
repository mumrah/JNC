package net.tarpn.network.netrom;

import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.tarpn.util.Util;
import net.tarpn.packet.impl.ax25.AX25Call;

/**
 * Helper class to parse NODES broadcast for updating the {@link NetRomRoutingTable}
 */
public class NetRomNodes {
  private final String sendingAlias;
  private final List<NodeDestination> destinationList;

  public NetRomNodes(
      String sendingAlias,
      List<NodeDestination> destinationList) {
    this.sendingAlias = sendingAlias;
    this.destinationList = destinationList;
  }

  public static NetRomNodes read(byte[] nodeInfo) {
    ByteBuffer buffer = ByteBuffer.wrap(nodeInfo);
    buffer.get(); // 0xff
    byte[] alias = new byte[6];
    buffer.get(alias, 0, 6);
    String sendingAlias = new String(alias, StandardCharsets.US_ASCII);
    List<NodeDestination> destinations = new ArrayList<>();
    while(buffer.remaining() > 0) {
      try {
        AX25Call destNode = AX25Call.read(buffer);
        buffer.get(alias, 0, 6);
        String destAlias = new String(alias, StandardCharsets.US_ASCII);
        AX25Call neighbor = AX25Call.read(buffer);
        byte quality = buffer.get();
        destinations.add(new NodeDestination(destNode, destAlias.trim(), neighbor, (quality & 0xff)));
      } catch (BufferUnderflowException e) {
        System.err.println("Problem parsing NODES");
      }
    }
    return new NetRomNodes(sendingAlias.trim(), destinations);
  }

  public static byte[] write(NetRomNodes nodes) {
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    buffer.put((byte)0xff);
    byte[] alias = String.format("%1$-6s", nodes.getSendingAlias()).getBytes(StandardCharsets.US_ASCII);
    buffer.put(alias);
    for(NodeDestination dest : nodes.getDestinationList()) {
      dest.getDestNode().write(buffer::put);
      byte[] destAlias = String.format("%1$-6s", dest.getDestAlias()).getBytes(StandardCharsets.US_ASCII);
      buffer.put(destAlias);
      dest.getBestNeighborNode().write(buffer::put);
      buffer.put((byte)(dest.getQuality() & 0xff));
    }
    return Util.copyFromBuffer(buffer);
  }

  public String getSendingAlias() {
    return sendingAlias;
  }

  public List<NodeDestination> getDestinationList() {
    return destinationList;
  }

  @Override
  public String toString() {
    return "NetRomNodes{" +
        "sendingAlias='" + sendingAlias + '\'' +
        ", destinations=" + destinationList +
        '}';
  }

  public static class NodeDestination {
    private final AX25Call destNode;
    private final String destAlias;
    private final AX25Call bestNeighborNode;
    private final int quality;

    public NodeDestination(
        AX25Call destNode, String destAlias,
        AX25Call bestNeighborNode, int quality) {
      this.destNode = destNode;
      this.destAlias = destAlias;
      this.bestNeighborNode = bestNeighborNode;
      this.quality = quality;
    }

    public AX25Call getDestNode() {
      return destNode;
    }

    public String getDestAlias() {
      return destAlias;
    }

    public AX25Call getBestNeighborNode() {
      return bestNeighborNode;
    }

    public int getQuality() {
      return quality;
    }

    @Override
    public String toString() {
      return "Node{" +
          "dest=" + destNode +
          ", alias='" + destAlias + '\'' +
          ", neighbor=" + bestNeighborNode +
          ", quality=" + Integer.toString(quality) +
          '}';
    }
  }
}
