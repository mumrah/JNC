package net.tarpn.packet.impl;

import net.tarpn.packet.PacketHandler;
import net.tarpn.packet.PacketRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsolePacketHandler implements PacketHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ConsolePacketHandler.class);

  @Override
  public void onPacket(PacketRequest packet) {
    LOG.info("Got Packet: " + packet.getPacket());
  }
}
