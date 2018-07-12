package net.tarpn.network.netrom.handlers;

import java.util.function.Consumer;
import net.tarpn.network.netrom.NetRomCircuit;
import net.tarpn.network.netrom.NetRomCircuit.State;
import net.tarpn.network.netrom.NetRomConnectAck;
import net.tarpn.network.netrom.NetRomConnectRequest;
import net.tarpn.network.netrom.NetRomPacket;
import net.tarpn.network.netrom.NetRomPacket.OpType;

public class DisconnectedStateHandler implements StateHandler {

  @Override
  public State handle(
      NetRomCircuit circuit,
      NetRomPacket packet,
      Consumer<byte[]> datagramConsumer,
      Consumer<NetRomPacket> outgoing) {
    final State newState;
    switch (packet.getOpType()) {
      case ConnectRequest:
        NetRomConnectRequest connReq = (NetRomConnectRequest) packet;
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
        outgoing.accept(connAck); // TODO what if this fails?
        circuit.setRemoteCircuitId(connReq.getCircuitId());
        circuit.setRemoteCircuitIdx(connReq.getCircuitIndex());
        newState = State.CONNECTED;
        break;
      case ConnectAcknowledge:
      case DisconnectRequest:
      case DisconnectAcknowledge:
      case Information:
      case InformationAcknowledge:
      default:
        // If we get an unexpected packet, maybe send a Disconnect Request?
        newState = State.DISCONNECTED;
        break;
    }
    return newState;
  }
}
