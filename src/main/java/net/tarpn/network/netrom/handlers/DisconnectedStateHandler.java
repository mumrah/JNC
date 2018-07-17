package net.tarpn.network.netrom.handlers;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import net.tarpn.datalink.LinkPrimitive;
import net.tarpn.network.netrom.BaseNetRomPacket;
import net.tarpn.network.netrom.NetRomCircuit;
import net.tarpn.network.netrom.NetRomCircuit.State;
import net.tarpn.network.netrom.NetRomCircuitEvent;
import net.tarpn.network.netrom.NetRomCircuitEvent.DataLinkEvent;
import net.tarpn.network.netrom.NetRomCircuitEvent.Type;
import net.tarpn.network.netrom.NetRomCircuitEvent.UserDataEvent;
import net.tarpn.network.netrom.NetRomConnectAck;
import net.tarpn.network.netrom.NetRomConnectRequest;
import net.tarpn.network.netrom.NetRomInfo;
import net.tarpn.network.netrom.NetRomPacket;
import net.tarpn.network.netrom.NetRomPacket.OpType;

public class DisconnectedStateHandler implements StateHandler {

  @Override
  public State handle(
      NetRomCircuit circuit,
      NetRomCircuitEvent event,
      Consumer<NetRomCircuitEvent> networkEvents,
      Consumer<NetRomPacket> outgoing) {

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
        outgoing.accept(connAck);
        circuit.setRemoteCircuitId(connReq.getCircuitId());
        circuit.setRemoteCircuitIdx(connReq.getCircuitIndex());
        // TODO move this out
        NetRomPacket welcome = NetRomInfo.create(
            circuit.getLocalNodeCall(),
            circuit.getRemoteNodeCall(),
            circuit.getConfig().getTTL(),
            circuit.getRemoteCircuitIdx(),
            circuit.getRemoteCircuitId(),
            circuit.getSendStateSeqByte(),
            circuit.getRecvStateSeqByte(),
            "Welcome to David's packet node! L3!".getBytes(StandardCharsets.US_ASCII)
        );
        outgoing.accept(welcome);
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
        outgoing.accept(connReq);
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
        outgoing.accept(disc);
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
