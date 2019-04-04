package net.tarpn.network.netrom.handlers;

import java.util.function.Consumer;

import net.tarpn.network.netrom.NetRomCircuitEvent;
import net.tarpn.network.netrom.NetRomCircuit;
import net.tarpn.network.netrom.NetRomRouter;
import net.tarpn.network.netrom.NetworkPrimitive;

public interface StateHandler {
  NetRomCircuit.State handle(
      NetRomCircuit circuit,
      NetRomCircuitEvent event,
      Consumer<NetworkPrimitive> networkEvents,
      NetRomRouter outgoing);
}
