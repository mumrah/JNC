package net.tarpn.packet.impl;

import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.tarpn.packet.Packet;

public class SimplePacket implements Packet {

  private final String source;
  private final String destination;
  private final String message;

  public SimplePacket(String source, String destination, String message) {
    this.source = source;
    this.destination = destination;
    this.message = message;
  }

  @Override
  public String getSource() {
    return source;
  }

  @Override
  public String getDestination() {
    return destination;
  }

  @Override
  public byte[] getMessage() {
    return message.getBytes(StandardCharsets.UTF_8);
  }

  public static Packet fromFrame(byte[] frame) {
    String frameAsString = new String(frame, StandardCharsets.UTF_8);
    // SRC;DEST;MESSAGE
    String[] tokens = frameAsString.split(";", 3);
    return new SimplePacket(tokens[0], tokens[1], tokens[2]);
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
    return "SimplePacket{" +
        "source='" + source + '\'' +
        ", destination='" + destination + '\'' +
        ", message='" + message + '\'' +
        '}';
  }
}
