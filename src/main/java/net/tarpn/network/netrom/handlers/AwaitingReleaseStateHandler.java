package net.tarpn.network.netrom.handlers;

import java.util.function.Consumer;
import net.tarpn.network.netrom.NetRomCircuit;
import net.tarpn.network.netrom.NetRomPacket;
import net.tarpn.network.netrom.NetRomCircuit.State;

public class AwaitingReleaseStateHandler implements StateHandler {

  @Override
  public State handle(
      NetRomCircuit circuit,
      NetRomPacket packet,
      Consumer<byte[]> datagramConsumer,
      Consumer<NetRomPacket> outgoing) {
    final State newState;
    switch (packet.getOpType()) {
      case ConnectRequest:
        newState = State.AWAITING_RELEASE;
        break;
      case ConnectAcknowledge:
        newState = State.AWAITING_RELEASE;
        break;
      case DisconnectRequest:
        newState = State.AWAITING_RELEASE;
        break;
      case DisconnectAcknowledge:
        newState = State.AWAITING_RELEASE;
        break;
      case Information:
        newState = State.AWAITING_RELEASE;
        break;
      case InformationAcknowledge:
        newState = State.AWAITING_RELEASE;
        break;
      default:
        newState = State.AWAITING_RELEASE;
        break;
    }
    return newState;
  }
}
