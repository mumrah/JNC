package net.tarpn.packet.impl;

import net.tarpn.packet.PacketHandler;
import net.tarpn.packet.PacketRequest;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;

public class DestinationFilteringPacketHandler implements PacketHandler {

  private final AX25Call destination;

  public DestinationFilteringPacketHandler(AX25Call destination) {
    this.destination = destination;
  }

  @Override
  public void onPacket(PacketRequest packetRequest) {
    if(!((AX25Packet)packetRequest.getPacket()).getDestCall().equals(destination)){
      System.err.println("Got packet for someone else, discarding");
      packetRequest.abort();
    }
  }
}
