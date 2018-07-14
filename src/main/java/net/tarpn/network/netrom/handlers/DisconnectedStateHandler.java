package net.tarpn.network.netrom.handlers;

import java.util.function.Consumer;
import net.tarpn.network.netrom.BaseNetRomPacket;
import net.tarpn.network.netrom.NetRomCircuit;
import net.tarpn.network.netrom.NetRomCircuit.State;
import net.tarpn.network.netrom.NetRomCircuitEvent;
import net.tarpn.network.netrom.NetRomCircuitEvent.DataLinkEvent;
import net.tarpn.network.netrom.NetRomCircuitEvent.Type;
import net.tarpn.network.netrom.NetRomConnectAck;
import net.tarpn.network.netrom.NetRomConnectRequest;
import net.tarpn.network.netrom.NetRomPacket;
import net.tarpn.network.netrom.NetRomPacket.OpType;

public class DisconnectedStateHandler implements StateHandler {

  @Override
  public State handle(
      NetRomCircuit circuit,
      NetRomCircuitEvent event,
      Consumer<byte[]> datagramConsumer,
      Consumer<NetRomPacket> outgoing) {

    final State newState;
    switch (event.getType()) {
      case NETROM_CONNECT: {
        NetRomConnectRequest connReq = (NetRomConnectRequest) ((DataLinkEvent)event).getNetRomPacket();
        NetRomConnectAck connAck = NetRomConnectAck.create(
            connReq.getDestNode(),
            connReq.getOriginNode(),
            (byte) 0x07,
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
        newState = State.CONNECTED;
        break;
      }
      case NL_CONNECT: {
        NetRomConnectRequest connReq = NetRomConnectRequest.create(
            circuit.getLocalNodeCall(),
            circuit.getRemoteNodeCall(),
            (byte) 0x07,
            (byte) circuit.getCircuitId(),
            (byte) circuit.getCircuitId(),
            (byte) 0,
            (byte) 0,
            (byte) 0x02, // TODO make window size configurable
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
            (byte) 0x07,
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
