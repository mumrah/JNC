package net.tarpn.packet.impl;

import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketProtocol;

/**
 * A Simple text oriented protocol
 *
 * <pre>SRC;DEST;MESSAGE</pre>
 */
public class SimplePacketProtocol implements PacketProtocol {

  @Override
  public Packet fromBytes(byte[] packetBytes) {
    String frameAsString = new String(packetBytes, StandardCharsets.UTF_8);
    String[] tokens = frameAsString.split(";", 3);
    return new SimplePacket(tokens[0], tokens[1], tokens[2]);
  }

  @Override
  public byte[] toBytes(Packet packet) {
    String frameString = Stream.of(
        packet.getSource(),
        packet.getDestination(),
        new String(packet.getMessage(), StandardCharsets.UTF_8)
    ).collect(Collectors.joining(";"));
    return frameString.getBytes(StandardCharsets.UTF_8);
  }
}
