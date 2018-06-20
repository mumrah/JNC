package net.tarpn.packet;

public interface PacketHandler {
  void onPacket(PacketRequest packet);
}
