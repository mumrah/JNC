package net.tarpn.packet;

import net.tarpn.io.DataPort;
import net.tarpn.PortProvider;

public interface PacketRouter {
  DataPort routePacket(PortProvider portProvider, Packet packetRequest);
}
