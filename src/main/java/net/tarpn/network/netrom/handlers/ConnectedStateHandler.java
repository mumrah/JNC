package net.tarpn.network.netrom.handlers;

import java.util.function.Consumer;
import net.tarpn.ByteUtil;
import net.tarpn.network.netrom.BaseNetRomPacket;
import net.tarpn.network.netrom.NetRomCircuit;
import net.tarpn.network.netrom.NetRomConnectAck;
import net.tarpn.network.netrom.NetRomConnectRequest;
import net.tarpn.network.netrom.NetRomInfo;
import net.tarpn.network.netrom.NetRomPacket;
import net.tarpn.network.netrom.NetRomCircuit.State;
import net.tarpn.network.netrom.NetRomPacket.OpType;

public class ConnectedStateHandler implements StateHandler {

  @Override
  public State handle(
      NetRomCircuit circuit,
      NetRomPacket packet,
      Consumer<byte[]> datagramConsumer,
      Consumer<NetRomPacket> outgoing) {
    final State newState;
    switch(packet.getOpType()) {
      case ConnectRequest:
        NetRomConnectRequest connReq = (NetRomConnectRequest) packet;
        if(ByteUtil.equals(connReq.getCircuitIndex(), circuit.getRemoteCircuitIdx()) &&
              ByteUtil.equals(connReq.getCircuitId(), circuit.getRemoteCircuitId())) {
          // Treat as a reconnect and ack it
          NetRomConnectAck connAck = NetRomConnectAck.create(
              connReq.getDestNode(),
              connReq.getOriginNode(),
              (byte) 0x07,
              connReq.getCircuitIndex(),
              connReq.getCircuitId(),
              (byte) circuit.getCircuitId(),
              (byte) circuit.getCircuitId(),
              connReq.getProposedWindowSize(),
              OpType.ConnectAcknowledge.asByte(false, false, false)
          );
          outgoing.accept(connAck);
          newState = State.CONNECTED;
        } else {
          // Reject the connection
          NetRomConnectAck connRej = NetRomConnectAck.create(
              connReq.getDestNode(),
              connReq.getOriginNode(),
              (byte) 0x07,
              connReq.getCircuitIndex(),
              connReq.getCircuitId(),
              (byte) circuit.getCircuitId(),
              (byte) circuit.getCircuitId(),
              connReq.getProposedWindowSize(),
              OpType.ConnectAcknowledge.asByte(true, false, false)
          );
          outgoing.accept(connRej);
          newState = State.DISCONNECTED;
        }
        break;
      case ConnectAcknowledge:
        NetRomConnectAck connAck = (NetRomConnectAck) packet;
        // Unexpected - maybe they resent their ack? If it matches current circuit, ignore it
        if(ByteUtil.equals(connAck.getTxSeqNumber(), circuit.getRemoteCircuitIdx()) &&
            ByteUtil.equals(connAck.getRxSeqNumber(), circuit.getRemoteCircuitId()) &&
            ByteUtil.equals(connAck.getCircuitIndex(), circuit.getCircuitIdByte()) &&
            ByteUtil.equals(connAck.getCircuitId(), circuit.getCircuitIdByte())) {
          newState = State.CONNECTED;
        } else {
          // disconnect?
          newState = State.CONNECTED;
        }
        break;
      case DisconnectRequest:
        // Ack and transition
        BaseNetRomPacket discReq = (BaseNetRomPacket) packet;
        BaseNetRomPacket discAck = BaseNetRomPacket.createDisconnectAck(
            discReq.getDestNode(),
            discReq.getOriginNode(),
            (byte) 0x07,
            discReq.getCircuitIndex(),
            discReq.getCircuitId()
        );
        outgoing.accept(discAck);
        newState = State.DISCONNECTED;
        break;
      case DisconnectAcknowledge:
        // Unexpected.. what to do?
        newState = State.CONNECTED;
        break;
      case Information:
        if(packet.getTxSeqNumber() == circuit.getRecvStateSeqByte()) {
          circuit.incrementRecvState();
          datagramConsumer.accept(((NetRomInfo)packet).getInfo());
          NetRomPacket infoAck = BaseNetRomPacket.createInfoAck(
              packet.getDestNode(),
              packet.getOriginNode(),
              (byte)0x07,
              circuit.getRemoteCircuitIdx(),
              circuit.getRemoteCircuitId(),
              circuit.getRecvStateSeqByte(),
              OpType.InformationAcknowledge.asByte(false, false, false)
          );
          outgoing.accept(infoAck);
        } else {
          NetRomPacket infoAck = BaseNetRomPacket.createInfoAck(
              packet.getDestNode(),
              packet.getOriginNode(),
              (byte)0x07,
              circuit.getRemoteCircuitIdx(),
              circuit.getRemoteCircuitId(),
              circuit.getRecvStateSeqByte(),
              OpType.InformationAcknowledge.asByte(false, true, false)
          );
          outgoing.accept(infoAck);
        }
        newState = State.CONNECTED;
        break;
      case InformationAcknowledge:
        // Check RX seq against circuit N(S)
        // Check NAK and Choke flags
        newState = State.CONNECTED;
        break;
      default:
        newState = State.CONNECTED;
        break;
    }
    return newState;
  }
}
