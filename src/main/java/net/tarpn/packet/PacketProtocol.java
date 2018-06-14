package net.tarpn.packet;

import net.tarpn.packet.impl.SimplePacketProtocol;

public interface PacketProtocol {
  PacketProtocol SIMPLE = new SimplePacketProtocol();

  Packet fromBytes(byte[] packetBytes);
  byte[] toBytes(Packet packet);
}
