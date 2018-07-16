package net.tarpn.network.netrom.handlers;

import java.util.function.Consumer;
import net.tarpn.datalink.LinkPrimitive;
import net.tarpn.network.netrom.NetRomCircuit;
import net.tarpn.network.netrom.NetRomCircuitEvent;
import net.tarpn.network.netrom.NetRomPacket;
import net.tarpn.network.netrom.NetRomCircuit.State;

public class AwaitingReleaseStateHandler implements StateHandler {

  @Override
  public State handle(
      NetRomCircuit circuit,
      NetRomCircuitEvent event,
      Consumer<NetRomCircuitEvent> networkEvents,
      Consumer<NetRomPacket> outgoing) {
    final State newState;
    switch(event.getType()) {
      case NETROM_CONNECT:
        newState = State.AWAITING_RELEASE;
        break;
      case NETROM_CONNECT_ACK:
        newState = State.AWAITING_RELEASE;
        break;
      case NETROM_DISCONNECT:
        newState = State.AWAITING_RELEASE;
        break;
      case NETROM_DISCONNECT_ACK:
        newState = State.DISCONNECTED;
        break;
      case NETROM_INFO:
        newState = State.AWAITING_RELEASE;
        break;
      case NETROM_INFO_ACK:
        newState = State.AWAITING_RELEASE;
        break;
      case NL_CONNECT:
        newState = State.AWAITING_RELEASE;
        break;
      case NL_DISCONNECT:
        newState = State.AWAITING_RELEASE;
        break;
      case NL_DATA:
        newState = State.AWAITING_RELEASE;
        break;
      default:
        newState = State.AWAITING_RELEASE;
        break;
    }
    return newState;
  }
}
