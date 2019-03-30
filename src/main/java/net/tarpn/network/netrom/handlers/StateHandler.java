package net.tarpn.network.netrom.handlers;

import java.util.function.Consumer;
import java.util.function.Function;
import net.tarpn.datalink.LinkPrimitive;
import net.tarpn.network.netrom.NetRomCircuitEvent;
import net.tarpn.network.netrom.NetRomPacket;
import net.tarpn.network.netrom.NetRomCircuit;
import net.tarpn.network.netrom.NetRomRouter;

public interface StateHandler {
  NetRomCircuit.State handle(
      NetRomCircuit circuit,
      NetRomCircuitEvent event,
      Consumer<LinkPrimitive> networkEvents,
      NetRomRouter outgoing);
}
