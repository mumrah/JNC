package net.tarpn.network.netrom.handlers;

import java.util.function.Consumer;
import net.tarpn.ByteUtil;
import net.tarpn.network.netrom.BaseNetRomPacket;
import net.tarpn.network.netrom.NetRomCircuit;
import net.tarpn.network.netrom.NetRomCircuitEvent;
import net.tarpn.network.netrom.NetRomCircuitEvent.DataLinkEvent;
import net.tarpn.network.netrom.NetRomCircuitEvent.UserDataEvent;
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
      NetRomCircuitEvent event,
      Consumer<byte[]> datagramConsumer,
      Consumer<NetRomPacket> outgoing) {
    final State newState;
    switch(event.getType()) {

      case NETROM_CONNECT: {
        NetRomConnectRequest connReq = (NetRomConnectRequest) ((DataLinkEvent)event).getNetRomPacket();
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
          outgoing.accept(connAck); // Need to check the result of the router before transitioning to CONNECTED
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
      }
      case NETROM_CONNECT_ACK: {
        NetRomConnectAck connAck = (NetRomConnectAck) ((DataLinkEvent)event).getNetRomPacket();
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
      }
      case NETROM_DISCONNECT: {
        // Ack and transition
        NetRomPacket discReq = ((DataLinkEvent)event).getNetRomPacket();
        BaseNetRomPacket discAck = BaseNetRomPacket.createDisconnectAck(
            discReq.getDestNode(),
            discReq.getOriginNode(),
            (byte) 0x07,
            discReq.getCircuitIndex(),
            discReq.getCircuitId()
        );
        outgoing.accept(discAck);
        newState = State.AWAITING_RELEASE;
        break;
      }
      case NETROM_INFO: {
        NetRomPacket info = ((DataLinkEvent)event).getNetRomPacket();
        if(ByteUtil.equals(info.getTxSeqNumber(), circuit.getRecvStateSeqByte())) {
          circuit.incrementRecvState();
          datagramConsumer.accept(((NetRomInfo)info).getInfo());
          NetRomPacket infoAck = BaseNetRomPacket.createInfoAck(
              info.getDestNode(),
              info.getOriginNode(),
              (byte)0x07,
              circuit.getRemoteCircuitIdx(),
              circuit.getRemoteCircuitId(),
              circuit.getRecvStateSeqByte(),
              OpType.InformationAcknowledge.asByte(false, false, false)
          );
          outgoing.accept(infoAck);
        } else {
          NetRomPacket infoAck = BaseNetRomPacket.createInfoAck(
              info.getDestNode(),
              info.getOriginNode(),
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
      }
      case NETROM_INFO_ACK: {
        // Check RX seq against circuit N(S)
        // Check NAK and Choke flags
        newState = State.CONNECTED;
        break;
      }
      case NL_DATA: {
        NetRomPacket info = NetRomInfo.create(
            circuit.getLocalNodeCall(),
            circuit.getRemoteNodeCall(),
            (byte)0x07,
            circuit.getRemoteCircuitIdx(),
            circuit.getRemoteCircuitId(),
            circuit.getSendStateSeqByte(),
            circuit.getRecvStateSeqByte(),
            ((UserDataEvent)event).getData()
        );
        outgoing.accept(info);
        newState = State.CONNECTED;
        break;
      }
      case NL_CONNECT:
      case NL_DISCONNECT:
      case NETROM_DISCONNECT_ACK:
      default: {
        newState = State.CONNECTED;
        break;
      }
    }

    return newState;
  }
}
