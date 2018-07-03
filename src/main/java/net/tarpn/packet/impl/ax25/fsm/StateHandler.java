package net.tarpn.packet.impl.ax25.fsm;

import java.util.function.Consumer;
import net.tarpn.packet.impl.ax25.AX25Packet;

public interface StateHandler {
  void onEvent(
      State state,
      StateEvent event,
      Consumer<AX25Packet> outgoingPackets,
      Consumer<AX25Packet> L3Packets);
}
