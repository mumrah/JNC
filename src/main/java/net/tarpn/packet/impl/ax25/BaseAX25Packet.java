package net.tarpn.packet.impl.ax25;

import java.util.List;

abstract class BaseAX25Packet implements AX25Packet {
  private final byte[] packet;
  private final AX25Call destination;
  private final AX25Call source;
  private final List<AX25Call> paths;
  private final byte control;

  BaseAX25Packet(byte[] packet, AX25Call destination, AX25Call source, List<AX25Call> paths, byte control) {
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
  public AX25Call getDestCall() {
    return destination;
  }

  @Override
  public AX25Call getSourceCall() {
    return source;
  }

  @Override
  public List<AX25Call> getRepeaterPaths() {
    return paths;
  }

  @Override
  public byte getControlByte() {
    return control;
  }
}
