package net.tarpn.packet;

public interface Packet {
  String getSource();
  String getDestination();
  byte[] getMessage();
}
