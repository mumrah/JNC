package net.tarpn.packet.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.tarpn.packet.PacketHandler;
import net.tarpn.packet.PacketRequest;

public class CompositePacketHandler implements PacketHandler {

  private final List<PacketHandler> handlers = new ArrayList<>();

  private CompositePacketHandler(Collection<PacketHandler> handlers) {
    this.handlers.addAll(handlers);
  }

  @Override
  public void onPacket(PacketRequest packetRequest) {
    handlers.forEach(packetHandler -> {
      if(packetRequest.shouldContinue()) {
        packetHandler.onPacket(packetRequest);
      }
    });
  }

  public static PacketHandler wrap(PacketHandler... handlers) {
    return new CompositePacketHandler(Arrays.asList(handlers));
  }
}

