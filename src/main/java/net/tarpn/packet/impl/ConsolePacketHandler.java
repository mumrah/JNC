package net.tarpn.packet.impl;

import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsolePacketHandler implements PacketHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ConsolePacketHandler.class);

  @Override
  public void onPacket(Packet packet) {
    LOG.info("Got Packet: " + packet);
  }
}
