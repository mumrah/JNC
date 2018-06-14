package net.tarpn.packet.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import net.tarpn.packet.PacketRequest;
import net.tarpn.packet.PacketRequestHandler;

public class CompositePacketHandler implements PacketRequestHandler {

  private final Set<PacketRequestHandler> handlerSet = new HashSet<>();

  private CompositePacketHandler(Collection<PacketRequestHandler> handlers) {
    handlerSet.addAll(handlers);
  }

  @Override
  public void onRequest(PacketRequest packetRequest) {
    handlerSet.forEach(handler -> handler.onRequest(packetRequest));
  }

  public static PacketRequestHandler wrap(PacketRequestHandler... handlers) {
    return new CompositePacketHandler(Arrays.asList(handlers));
  }
}

