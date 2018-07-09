package net.tarpn.network.netrom.handlers;

import java.util.function.Consumer;
import net.tarpn.network.netrom.NetRomCircuit;
import net.tarpn.network.netrom.NetRomConnectAck;
import net.tarpn.network.netrom.NetRomConnectRequest;
import net.tarpn.network.netrom.NetRomPacket;
import net.tarpn.network.netrom.NetRomCircuit.State;
import net.tarpn.packet.impl.ax25.AX25Packet.Command;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;
import net.tarpn.packet.impl.ax25.IFrame;

public class DisconnectedStateHandler implements StateHandler {

  @Override
  public State handle(NetRomCircuit circuit, NetRomPacket packet, Consumer<NetRomPacket> outgoing) {
    final State newState;
    switch (packet.getOpType()) {
      case ConnectRequest:
        NetRomConnectRequest netromReq = (NetRomConnectRequest) packet;
        NetRomConnectAck netromResp = NetRomConnectAck.create(
            netromReq.getDestNode(),
            netromReq.getOriginNode(),
            (byte) 0x07,
            netromReq.getCircuitIndex(),
            netromReq.getCircuitId(),
            (byte) circuit.getCircuitId(),
            (byte) circuit.getCircuitId(),
            netromReq.getProposedWindowSize()
        );
        outgoing.accept(netromResp);
        newState = State.CONNECTED;
        break;
      case ConnectAcknowledge:
        newState = State.DISCONNECTED;
        break;
      case DisconnectRequest:
        newState = State.DISCONNECTED;
        break;
      case DisconnectAcknowledge:
        newState = State.DISCONNECTED;
        break;
      case Information:
        newState = State.DISCONNECTED;
        break;
      case InformationAcknowledge:
        newState = State.DISCONNECTED;
        break;
      default:
        newState = State.DISCONNECTED;
        break;
    }
    return newState;
  }
}
