package net.tarpn.packet;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A Data Packet (Layer 3)
 *
 * The source address is one of our neighbors (who we heard this packet from) and the destination
 * address _should_ be this node (though, technically it could be anything).
 */
public class Packet {
  private final int port;
  private final String source;
  private final String destination;
  private final byte[] data;

  public Packet(int port, String source, String destination, byte[] data) {
    this.port = port;
    this.source = source;
    this.destination = destination;
    this.data = data;
  }

  public int getPort() {
    return port;
  }

  public byte[] getMessage() {
    return data;
  }

  public String getSource() {
    return source;
  }

  public String getDestination() {
    return destination;
  }

  public static byte[] toFrame(Packet packet) {
    String frameString = Stream.of(
        packet.getSource(),
        packet.getDestination(),
        new String(packet.getMessage(), StandardCharsets.UTF_8)
    ).collect(Collectors.joining(";"));
    return frameString.getBytes(StandardCharsets.UTF_8);
  }

  @Override
  public String toString() {
    return "Packet{" +
        "port=" + port +
        ", source='" + source + '\'' +
        ", destination='" + destination + '\'' +
        '}';
  }
}
