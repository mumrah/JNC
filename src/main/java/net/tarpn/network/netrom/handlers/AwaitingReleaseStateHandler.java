package net.tarpn.network.netrom.handlers;

import java.util.function.Consumer;

import net.tarpn.network.netrom.NetRomCircuit;
import net.tarpn.network.netrom.NetRomCircuitEvent;
import net.tarpn.network.netrom.NetRomCircuitEvent.DataLinkEvent;
import net.tarpn.network.netrom.NetworkPrimitive;
import net.tarpn.network.netrom.packet.NetRomPacket;
import net.tarpn.network.netrom.NetRomCircuit.State;
import net.tarpn.network.netrom.NetRomRouter;
import net.tarpn.util.ByteUtil;

public class AwaitingReleaseStateHandler implements StateHandler {

  @Override
  public State handle(
      NetRomCircuit circuit,
      NetRomCircuitEvent event,
      Consumer<NetworkPrimitive> networkEvents,
      NetRomRouter outgoing) {
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
      case NETROM_DISCONNECT_ACK: {
        NetRomPacket discAck = ((DataLinkEvent) event).getNetRomPacket();
        if (ByteUtil.equals(discAck.getCircuitIndex(), circuit.getCircuitIdByte()) &&
            ByteUtil.equals(discAck.getCircuitId(), circuit.getCircuitIdByte())) {
          networkEvents.accept(NetworkPrimitive.newDisconnectAck(discAck.getOriginNode(), discAck.getDestNode(), circuit.getCircuitId()));
          newState = State.DISCONNECTED;
        } else {
          // Error!
          newState = State.DISCONNECTED;
        }
        break;
      }
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
