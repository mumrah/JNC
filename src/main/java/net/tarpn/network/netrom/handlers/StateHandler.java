package net.tarpn.network.netrom.handlers;

import java.util.function.Consumer;
import net.tarpn.network.netrom.NetRomPacket;
import net.tarpn.network.netrom.NetRomCircuit;

public interface StateHandler {
  NetRomCircuit.State handle(
      NetRomCircuit circuit, NetRomPacket packet, Consumer<NetRomPacket> outgoing);
}
