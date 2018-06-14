package net.tarpn.packet.impl;

import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketHandler;

public class ConsolePacketHandler implements PacketHandler {

  @Override
  public void onPacket(Packet packet) {
    System.err.println("Got Packet: " + packet);
  }
}
