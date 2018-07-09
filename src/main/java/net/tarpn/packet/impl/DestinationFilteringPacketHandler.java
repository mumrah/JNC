package net.tarpn.packet.impl;

import net.tarpn.Configuration;
import net.tarpn.packet.PacketHandler;
import net.tarpn.packet.PacketRequest;
import net.tarpn.packet.impl.ax25.AX25Packet;

public class DestinationFilteringPacketHandler implements PacketHandler {

  @Override
  public void onPacket(PacketRequest packetRequest) {
    if(!((AX25Packet)packetRequest.getPacket()).getDestCall().equals(Configuration.getOwnNodeCallsign())){
      System.err.println("Got packet for someone else, discarding");
      packetRequest.abort();
    }
  }
}
