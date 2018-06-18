package net.tarpn.packet.impl;

import java.util.HashMap;
import java.util.Map;
import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketHandler;

public class PacketStatsUpdateHandler implements PacketHandler {

  Map<String, Integer> lastHeard = new HashMap<>();

  public int getLastHeardPort(String address) {
    return lastHeard.getOrDefault(address, -1);
  }

  @Override
  public void onPacket(Packet packet) {
    lastHeard.put(packet.getSource(), packet.getPort());

  }
}
