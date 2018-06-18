package net.tarpn.packet.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketHandler;

public class CompositePacketHandler implements PacketHandler {

  private final List<PacketHandler> handlerSet = new ArrayList<>();

  private CompositePacketHandler(Collection<PacketHandler> handlers) {
    handlerSet.addAll(handlers);
  }

  @Override
  public void onPacket(Packet packet) {
    handlerSet.forEach(handler -> handler.onPacket(packet));
  }

  public static PacketHandler wrap(PacketHandler... handlers) {
    return new CompositePacketHandler(Arrays.asList(handlers));
  }
}

