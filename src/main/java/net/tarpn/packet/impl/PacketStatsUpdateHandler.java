package net.tarpn.packet.impl;

import java.util.HashMap;
import java.util.Map;
import net.tarpn.packet.PacketRequest;
import net.tarpn.packet.PacketRequestHandler;

public class PacketStatsUpdateHandler implements PacketRequestHandler {

  Map<String, String> lastHeard = new HashMap<>();

  @Override
  public void onRequest(PacketRequest packetRequest) {
    lastHeard.put(packetRequest.getPacket().getSource(), packetRequest.getDataPort());
  }

  public String getLastHeardPort(String address) {
    return lastHeard.getOrDefault(address, "default");
  }

}
