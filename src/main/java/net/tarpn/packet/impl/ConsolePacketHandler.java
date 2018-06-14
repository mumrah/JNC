package net.tarpn.packet.impl;

import net.tarpn.packet.PacketRequest;
import net.tarpn.packet.PacketRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsolePacketHandler implements PacketRequestHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ConsolePacketHandler.class);

  @Override
  public void onRequest(PacketRequest request) {
    LOG.info("Got Request: " + request);
  }
}
