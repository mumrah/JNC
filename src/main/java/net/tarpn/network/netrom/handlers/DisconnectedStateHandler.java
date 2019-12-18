package net.tarpn.network.netrom.handlers;

import net.tarpn.network.netrom.NetRomCircuit;
import net.tarpn.network.netrom.NetRomCircuit.State;
import net.tarpn.network.netrom.NetRomCircuitEvent;
import net.tarpn.network.netrom.NetRomCircuitEvent.DataLinkEvent;
import net.tarpn.network.netrom.NetRomRouter;
import net.tarpn.network.netrom.NetworkPrimitive;
import net.tarpn.network.netrom.packet.BaseNetRomPacket;
import net.tarpn.network.netrom.packet.NetRomConnectAck;
import net.tarpn.network.netrom.packet.NetRomConnectRequest;
import net.tarpn.network.netrom.packet.NetRomPacket;
import net.tarpn.network.netrom.packet.NetRomPacket.OpType;

import java.util.function.Consumer;

public class DisconnectedStateHandler implements StateHandler {

  @Override
  public State handle(
      NetRomCircuit circuit,
      NetRomCircuitEvent event,
      Consumer<NetworkPrimitive> networkEvents,
      NetRomRouter outgoing) {

    final State newState;
    switch (event.getType()) {
      case NETROM_CONNECT: {
        NetRomConnectRequest connReq = (NetRomConnectRequest) ((DataLinkEvent)event).getNetRomPacket();
        NetRomConnectAck connAck = NetRomConnectAck.create(
            connReq.getDestNode(),
            connReq.getOriginNode(),
            circuit.getConfig().getTTL(),
            connReq.getCircuitIndex(),
            connReq.getCircuitId(),
            (byte) circuit.getCircuitId(),
            (byte) circuit.getCircuitId(),
            connReq.getProposedWindowSize(),
            OpType.ConnectAcknowledge.asByte(false, false,false)
        );
        circuit.setRemoteCircuitId(connReq.getCircuitId());
        circuit.setRemoteCircuitIdx(connReq.getCircuitIndex());
        outgoing.route(connAck);
        networkEvents.accept(NetworkPrimitive.newConnectIndication(connAck.getOriginNode(), connAck.getDestNode(), circuit.getCircuitId()));
        newState = State.CONNECTED;
        break;
      }
      case NL_CONNECT: {
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
        outgoing.route(connReq);
        newState = State.AWAITING_CONNECTION;
        break;
      }
      case NETROM_CONNECT_ACK:
      case NETROM_DISCONNECT:
      case NETROM_DISCONNECT_ACK:
      case NETROM_INFO:
      case NETROM_INFO_ACK: {
        // If we get an unexpected packet, send a Disconnect Request
        NetRomPacket packet = ((DataLinkEvent)event).getNetRomPacket();
        NetRomPacket disc = BaseNetRomPacket.createDisconnectRequest(
            packet.getDestNode(),
            packet.getOriginNode(),
            circuit.getConfig().getTTL(),
            packet.getCircuitIndex(),
            packet.getCircuitId()
        );
        outgoing.route(disc);
        newState = State.DISCONNECTED;
        break;
      }
      case NL_DISCONNECT: {
        // no-op
        newState = State.DISCONNECTED;
        break;
      }
      case NL_DATA: {
        // error
        newState = State.DISCONNECTED;
        break;
      }
      default: {
        newState = State.DISCONNECTED;
        break;
      }

    }

    return newState;
  }
}
