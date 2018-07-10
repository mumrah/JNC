package net.tarpn.network.netrom.handlers;

import java.util.function.Consumer;
import net.tarpn.network.netrom.BaseNetRomPacket;
import net.tarpn.network.netrom.NetRomCircuit;
import net.tarpn.network.netrom.NetRomPacket;
import net.tarpn.network.netrom.NetRomCircuit.State;

public class ConnectedStateHandler implements StateHandler {

  @Override
  public State handle(NetRomCircuit circuit, NetRomPacket packet, Consumer<NetRomPacket> outgoing) {
    final State newState;
    switch(packet.getOpType()) {
      case ConnectRequest:
        // Like a "reconnect" - maybe they restarted, respond with connect ack
        newState = State.CONNECTED;
        break;
      case ConnectAcknowledge:
        // Unexpected - maybe they resent their ack? If it matches current circuit, ignore it
        // If not, send disconnect
        newState = State.CONNECTED;
        break;
      case DisconnectRequest:
        // Ack and transition
        newState = State.CONNECTED;
        break;
      case DisconnectAcknowledge:
        // Unexpected.. what to do
        newState = State.CONNECTED;
        break;
      case Information:
        if(packet.getTxSeqNumber() == circuit.getRecvStateSeqByte()) {
          circuit.incrementRecvState();
          NetRomPacket infoAck = BaseNetRomPacket.createInfoAck(
              packet.getDestNode(),
              packet.getOriginNode(),
              (byte)0x07,
              circuit.getRemoteCircuitIdx(),
              circuit.getRemoteCircuitId(),
              circuit.getRecvStateSeqByte()
          );
          outgoing.accept(infoAck);
        } else {
          // NAK it
          System.err.println("Need to NAK " + packet);
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
