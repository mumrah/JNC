package net.tarpn.packet;

import net.tarpn.DataPort;

public interface PacketRouter {
  DataPort routePacket(Packet packet);
}
