package net.tarpn.packet.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import net.tarpn.DataPort;
import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketRequest;
import net.tarpn.packet.PacketRequestHandler;
import net.tarpn.packet.PacketRouter;

public class LastHeardPacketRouter implements PacketRouter, PacketRequestHandler {

  private final Map<String, String> lastHeard = new ConcurrentHashMap<>();
  private final Function<String, DataPort> portFinder;

  public LastHeardPacketRouter(Function<String, DataPort> portFinder) {
    this.portFinder = portFinder;
  }

  @Override
  public DataPort routePacket(Packet packet) {
    String dest = packet.getDestination();
    String portName = lastHeard.get(dest);
    if(portName == null) {
      return portFinder.apply("?");
    } else {
      return portFinder.apply(portName);
    }
  }

  @Override
  public void onRequest(PacketRequest packetRequest) {
    lastHeard.put(packetRequest.getPacket().getSource(), packetRequest.getDataPort());
  }
}
