package net.tarpn.network.netrom;

public interface NetRomRouter {
  boolean route(NetRomPacket packet);
}
