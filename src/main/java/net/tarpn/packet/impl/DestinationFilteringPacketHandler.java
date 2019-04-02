package net.tarpn.packet.impl;

import net.tarpn.config.AppConfig;
import net.tarpn.packet.PacketHandler;
import net.tarpn.packet.PacketRequest;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class DestinationFilteringPacketHandler implements PacketHandler {

  private final static Logger LOG = LoggerFactory.getLogger(DestinationFilteringPacketHandler.class);

  private final AX25Call destination;

  public DestinationFilteringPacketHandler(AX25Call destination) {
    this.destination = destination;
  }

  @Override
  public void onPacket(PacketRequest packetRequest) {
    if(!destination.equals(((AX25Packet)packetRequest.getPacket()).getDestCall())) {
      LOG.warn("Got packet for someone else, discarding: " + packetRequest.getPacket());
      packetRequest.abort();
    }
  }
}
