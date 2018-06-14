package net.tarpn.frame.impl;

import java.util.Date;
import java.util.function.Function;
import net.tarpn.frame.FrameHandler;
import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketRequestHandler;
import net.tarpn.packet.PacketRequest;
import net.tarpn.packet.PacketRouter;

public class PacketReadingFrameHandler implements FrameHandler {

  private final PacketRequestHandler packetHandler;
  private final Function<byte[], Packet> packetReader;

  public PacketReadingFrameHandler(
      PacketRequestHandler packetHandler,
      Function<byte[], Packet> packetReader) {
    this.packetHandler = packetHandler;
    this.packetReader = packetReader;
  }

  @Override
  public void onFrame(String portName, byte[] frame) {
    Packet packet = packetReader.apply(frame);
    PacketRequest packetRequest = new PacketRequest(packet, portName, new Date());
    packetHandler.onRequest(packetRequest);
  }
}
