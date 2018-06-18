package net.tarpn.packet;


import net.tarpn.packet.impl.SimplePacketReader;

public interface PacketProtocol {
  PacketProtocol SIMPLE = new PacketProtocol() {
    @Override
    public PacketReader getReader() {
      return new SimplePacketReader();
    }
  };

  PacketReader getReader();
}
