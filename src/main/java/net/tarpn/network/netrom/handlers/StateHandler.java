package net.tarpn.network.netrom.handlers;

import java.util.function.Consumer;
import net.tarpn.network.netrom.NetRomPacket;
import net.tarpn.network.netrom.NetRomCircuit;

public interface StateHandler {
  NetRomCircuit.State handle(
      NetRomCircuit circuit,
      NetRomPacket packet,
      Consumer<byte[]> datagramConsumer,
      Consumer<NetRomPacket> outgoing);

  enum StateEvent {
    NETROM_CONNECT,
    NETROM_CONNECT_ACK,
    NETROM_DISCONNECT,
    NETROM_DISCONNECT_ACK,
    NETROM_INFO,
    NETROM_INFO_ACK,
    USER_DATA
  }
}
