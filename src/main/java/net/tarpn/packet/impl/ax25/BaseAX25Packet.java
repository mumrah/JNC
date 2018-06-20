package net.tarpn.packet.impl.ax25;

import java.util.List;

abstract class BaseAX25Packet implements AX25Packet {
  private final byte[] packet;
  private final String destination;
  private final String source;
  private final List<String> paths;
  private final byte control;

  BaseAX25Packet(byte[] packet, String destination, String source, List<String> paths, byte control) {
    this.packet = packet;
    this.destination = destination;
    this.source = source;
    this.paths = paths;
    this.control = control;
  }

  @Override
  public byte[] getPayload() {
    return packet;
  }

  @Override
  public String getDestination() {
    return destination;
  }

  @Override
  public String getSource() {
    return source;
  }

  @Override
  public List<String> getRepeaterPaths() {
    return paths;
  }

  @Override
  public byte getControlByte() {
    return control;
  }
}
