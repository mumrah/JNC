package net.tarpn.network.netrom.handlers;

import java.util.function.Consumer;
import net.tarpn.datalink.LinkPrimitive;
import net.tarpn.datalink.LinkPrimitive.ErrorType;
import net.tarpn.datalink.LinkPrimitive.LinkInfo;
import net.tarpn.datalink.LinkPrimitive.Type;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;
import net.tarpn.util.ByteUtil;
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
      Consumer<LinkPrimitive> networkEvents,
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
              circuit.getConfig().getTTL(),
              connReq.getCircuitIndex(),
              connReq.getCircuitId(),
              (byte) circuit.getCircuitId(),
              (byte) circuit.getCircuitId(),
              connReq.getProposedWindowSize(),
              OpType.ConnectAcknowledge.asByte(false, false, false)
          );
          // TODO Need to check the result of the router before transitioning to CONNECTED
          outgoing.accept(connAck);
          networkEvents.accept(LinkPrimitive.newConnectIndication(circuit.getRemoteNodeCall()));
          newState = State.CONNECTED;
        } else {
          // Reject the connection
          NetRomConnectAck connRej = NetRomConnectAck.create(
              connReq.getDestNode(),
              connReq.getOriginNode(),
              circuit.getConfig().getTTL(),
              connReq.getCircuitIndex(),
              connReq.getCircuitId(),
              (byte) circuit.getCircuitId(),
              (byte) circuit.getCircuitId(),
              connReq.getProposedWindowSize(),
              OpType.ConnectAcknowledge.asByte(true, false, false)
          );
          outgoing.accept(connRej);
          networkEvents.accept(LinkPrimitive.newDisconnectIndication(circuit.getRemoteNodeCall()));
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
            circuit.getConfig().getTTL(),
            discReq.getCircuitIndex(),
            discReq.getCircuitId()
        );
        outgoing.accept(discAck);
        LinkPrimitive.newDisconnectIndication(circuit.getRemoteNodeCall());
        newState = State.DISCONNECTED;
        break;
      }
      case NETROM_INFO: {
        NetRomPacket info = ((DataLinkEvent)event).getNetRomPacket();
        if(ByteUtil.equals(info.getTxSeqNumber(), circuit.getRecvStateSeqByte())) {
          circuit.incrementRecvState();

          /*networkEvents.accept(new UserDataEvent(
              circuit.getCircuitId(),
              circuit.getRemoteNodeCall(),
              ((NetRomInfo)info).getInfo()
          ));*/
          circuit.enqueueInfoAck(outgoing);
          networkEvents.accept(LinkPrimitive.newDataIndication(
              circuit.getRemoteNodeCall(), Protocol.NETROM, ((NetRomInfo)info).getInfo()));
        } else {
          NetRomPacket infoNak = BaseNetRomPacket.createInfoAck(
              circuit.getRemoteNodeCall(),
              circuit.getLocalNodeCall(),
              circuit.getConfig().getTTL(),
              circuit.getRemoteCircuitIdx(),
              circuit.getRemoteCircuitId(),
              circuit.getRecvStateSeqByte(),
              OpType.InformationAcknowledge.asByte(false, true, false)
          );
          outgoing.accept(infoNak);
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
            circuit.getConfig().getTTL(),
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
      case NL_DISCONNECT: {
        NetRomPacket disc = BaseNetRomPacket.createDisconnectRequest(
            circuit.getLocalNodeCall(),
            circuit.getRemoteNodeCall(),
            circuit.getConfig().getTTL(),
            circuit.getRemoteCircuitIdx(),
            circuit.getRemoteCircuitId()
        );
        outgoing.accept(disc);
        newState = State.AWAITING_RELEASE;
        break;
      }
      case NL_CONNECT:
        // TODO ??? send connect request, transition to awaiting connection
      case NETROM_DISCONNECT_ACK:
        // TODO error!!
      default: {
        newState = State.CONNECTED;
        break;
      }
    }

    return newState;
  }
}
