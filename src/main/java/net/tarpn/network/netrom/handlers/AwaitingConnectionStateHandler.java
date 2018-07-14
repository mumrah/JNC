package net.tarpn.network.netrom.handlers;

import java.util.function.Consumer;
import net.tarpn.network.netrom.NetRomCircuit;
import net.tarpn.network.netrom.NetRomCircuitEvent;
import net.tarpn.network.netrom.NetRomPacket;
import net.tarpn.network.netrom.NetRomCircuit.State;

public class AwaitingConnectionStateHandler implements StateHandler {

  @Override
  public State handle(
      NetRomCircuit circuit,
      NetRomCircuitEvent event,
      Consumer<byte[]> datagramConsumer,
      Consumer<NetRomPacket> outgoing) {
    final State newState;
    switch(event.getType()) {
      case NETROM_CONNECT:
        newState = State.AWAITING_CONNECTION;
        break;
      case NETROM_CONNECT_ACK:
        newState = State.CONNECTED;
        break;
      case NETROM_DISCONNECT:
        newState = State.AWAITING_CONNECTION;
        break;
      case NETROM_DISCONNECT_ACK:
        newState = State.AWAITING_CONNECTION;
        break;
      case NETROM_INFO:
        newState = State.AWAITING_CONNECTION;
        break;
      case NETROM_INFO_ACK:
        newState = State.AWAITING_CONNECTION;
        break;
      case NL_CONNECT:
        newState = State.AWAITING_CONNECTION;
        break;
      case NL_DISCONNECT:
        newState = State.AWAITING_CONNECTION;
        break;
      case NL_DATA:
        newState = State.AWAITING_CONNECTION;
        break;
      default:
        newState = State.AWAITING_CONNECTION;
        break;
    }
    return newState;
  }
}
