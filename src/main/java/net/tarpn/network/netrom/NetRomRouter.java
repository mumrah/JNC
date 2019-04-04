package net.tarpn.network.netrom;

import net.tarpn.network.netrom.packet.NetRomPacket;

public interface NetRomRouter {
  boolean route(NetRomPacket packet);
}
