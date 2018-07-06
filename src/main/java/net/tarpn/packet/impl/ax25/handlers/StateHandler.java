package net.tarpn.packet.impl.ax25.handlers;

import java.util.function.Consumer;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.State;
import net.tarpn.packet.impl.ax25.StateEvent;
import net.tarpn.packet.impl.ax25.StateType;

public interface StateHandler {
  StateType onEvent(
      State state,
      StateEvent event,
      Consumer<AX25Packet> outgoingPackets,
      Consumer<AX25Packet> L3Packets);
}
