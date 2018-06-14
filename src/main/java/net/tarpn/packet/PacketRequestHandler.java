package net.tarpn.packet;

public interface PacketRequestHandler {
  void onRequest(PacketRequest packetRequest);
}
