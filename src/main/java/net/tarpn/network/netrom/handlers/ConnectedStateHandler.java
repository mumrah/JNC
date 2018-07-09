package net.tarpn.network.netrom.handlers;

import java.util.function.Consumer;
import net.tarpn.network.netrom.NetRomCircuit;
import net.tarpn.network.netrom.NetRomPacket;
import net.tarpn.network.netrom.NetRomCircuit.State;

public class ConnectedStateHandler implements StateHandler {

  @Override
  public State handle(NetRomCircuit circuit, NetRomPacket packet, Consumer<NetRomPacket> outgoing) {
    final State newState;
    switch(packet.getOpType()) {
      case ConnectRequest:
        newState = State.CONNECTED;
        break;
      case ConnectAcknowledge:
        newState = State.CONNECTED;
        break;
      case DisconnectRequest:
        newState = State.CONNECTED;
        break;
      case DisconnectAcknowledge:
        newState = State.CONNECTED;
        break;
      case Information:
        newState = State.CONNECTED;
        break;
      case InformationAcknowledge:
        newState = State.CONNECTED;
        break;
      default:
        newState = State.CONNECTED;
        break;
    }
    return newState;
  }
}
