package net.tarpn.packet;

public interface Packet {
  byte[] getPayload();

  String getSource();

  String getDestination();
}
