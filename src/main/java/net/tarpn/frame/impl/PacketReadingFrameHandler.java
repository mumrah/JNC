package net.tarpn.frame.impl;

import java.util.function.Consumer;
import net.tarpn.frame.FrameHandler;
import net.tarpn.frame.FrameRequest;
import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketReader;

public class PacketReadingFrameHandler implements FrameHandler {

  private final PacketReader packetReader;
  private final Consumer<Packet> packetConsumer;

  public PacketReadingFrameHandler(
      PacketReader packetReader,
      Consumer<Packet> packetConsumer) {
    this.packetReader = packetReader;
    this.packetConsumer = packetConsumer;
  }

  @Override
  public void onFrame(FrameRequest frameRequest) {
    packetReader.accept(frameRequest.getFrame(), packetConsumer);
  }
}
