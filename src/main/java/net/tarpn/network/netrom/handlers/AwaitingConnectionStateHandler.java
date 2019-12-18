package net.tarpn.network.netrom.handlers;

import java.util.function.Consumer;
import net.tarpn.network.netrom.NetRomCircuit;
import net.tarpn.network.netrom.NetRomCircuitEvent;
import net.tarpn.network.netrom.NetRomCircuitEvent.DataLinkEvent;
import net.tarpn.network.netrom.NetworkPrimitive;
import net.tarpn.network.netrom.packet.NetRomConnectAck;
import net.tarpn.network.netrom.packet.NetRomConnectRequest;
import net.tarpn.network.netrom.NetRomCircuit.State;
import net.tarpn.network.netrom.NetRomRouter;
import net.tarpn.util.ByteUtil;

public class AwaitingConnectionStateHandler implements StateHandler {

  @Override
  public State handle(
      NetRomCircuit circuit,
      NetRomCircuitEvent event,
      Consumer<NetworkPrimitive> networkEvents,
      NetRomRouter outgoing) {
    final State newState;
    switch(event.getType()) {
      case NETROM_CONNECT:
        newState = State.AWAITING_CONNECTION;
        break;
      case NETROM_CONNECT_ACK: {
        NetRomConnectAck connAck = (NetRomConnectAck) ((DataLinkEvent) event).getNetRomPacket();
        if (ByteUtil.equals(connAck.getCircuitIndex(), circuit.getCircuitIdByte()) &&
            ByteUtil.equals(connAck.getCircuitId(), circuit.getCircuitIdByte())) {
          circuit.setRemoteCircuitId(connAck.getRxSeqNumber());
          circuit.setRemoteCircuitIdx(connAck.getTxSeqNumber());
          circuit.setWindowSize(connAck.getAcceptWindowSize());
          networkEvents.accept(NetworkPrimitive.newConnectAck(connAck.getOriginNode(), connAck.getDestNode(), circuit.getCircuitId()));
          newState = State.CONNECTED;
        } else {
          // Error!
          newState = State.AWAITING_CONNECTION;
        }
        break;
      }
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
        NetRomConnectRequest connReq = NetRomConnectRequest.create(
            circuit.getLocalNodeCall(),
            circuit.getRemoteNodeCall(),
            circuit.getConfig().getTTL(),
            (byte) circuit.getCircuitId(),
            (byte) circuit.getCircuitId(),
            circuit.getConfig().getWindowSize(),
            circuit.getLocalNodeCall(),  // TODO make user configurable
            circuit.getLocalNodeCall()
        );
        boolean routed = outgoing.route(connReq);
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
