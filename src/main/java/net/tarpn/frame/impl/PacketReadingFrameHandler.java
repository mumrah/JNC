package net.tarpn.frame.impl;

import java.util.function.Function;
import net.tarpn.frame.FrameHandler;
import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketHandler;

public class PacketReadingFrameHandler implements FrameHandler {

  private final PacketHandler packetHandler;
  private final Function<byte[], Packet> packetReader;

  public PacketReadingFrameHandler(PacketHandler packetHandler, Function<byte[], Packet> packetReader) {
    this.packetHandler = packetHandler;
    this.packetReader = packetReader;
  }

  @Override
  public void onFrame(byte[] frame) {
    packetHandler.onPacket(packetReader.apply(frame));
  }
}
