package net.tarpn.packet.impl.ax25.fsm;

import java.util.function.Consumer;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Command;
import net.tarpn.packet.impl.ax25.AX25Packet.SupervisoryFrame;
import net.tarpn.packet.impl.ax25.AX25Packet.UnnumberedFrame.ControlType;
import net.tarpn.packet.impl.ax25.IFrame;
import net.tarpn.packet.impl.ax25.SFrame;
import net.tarpn.packet.impl.ax25.UFrame;
import net.tarpn.packet.impl.ax25.UIFrame;

public class ConnectedStateHandler implements StateHandler {

  @Override
  public void onEvent(State state, StateEvent event, Consumer<AX25Packet> outgoingPackets) {
    AX25Packet packet = event.getPacket();
    AX25Call source = packet.getSourceCall();
    AX25Call dest = packet.getDestCall();

    final StateType newState;
    switch(event.getType()) {
      case AX25_UA: {
        // Emit DL-ERROR C
        // Clear layer 3
        newState = StateType.AWAITING_CONNECTION;
        break;
      }
      case AX25_DM: {
        // Emit DL-ERROR E
        // Emit DL-DISCONNECT
        // Clear I Queue
        // Stop timers
        newState = StateType.DISCONNECTED;
        break;
      }
      case AX25_UI: {
        UIFrame frame = (UIFrame) event.getPacket();
        // ack the incoming UI frame
        SFrame rr = SFrame.create(source, dest, Command.COMMAND, SupervisoryFrame.ControlType.RR,
            state.getReceiveState(), true);
        newState = StateType.CONNECTED;
        outgoingPackets.accept(rr);
        break;
      }
      case AX25_DISC: {
        // Clear I Queue
        boolean finalFlag = ((UFrame) event.getPacket()).isPollFinalSet();
        UFrame ua = UFrame.create(source, dest, Command.RESPONSE, ControlType.UA, finalFlag);
        outgoingPackets.accept(ua);
        // Emit DL-DISCONNECT
        // Stop timers
        newState = StateType.DISCONNECTED;
        break;
      }
      case AX25_SABM:
      case AX25_SABME: {
        // Send UA with F=P
        boolean finalFlag = ((UFrame) event.getPacket()).isPollFinalSet();
        UFrame ua = UFrame.create(source, dest, Command.RESPONSE, ControlType.UA, finalFlag);
        //outgoingPackets.accept(ua);
        // Emit DL-ERROR F
        // IF V(s) != V(a)
        //   Clear I Queue
        //   Emit DL-CONNECT
        // ENDIF
        state.reset();
        newState = StateType.CONNECTED;
        break;
      }
      case AX25_INFO: {
        // Got info frame, need to ack it
        IFrame frame = (IFrame) packet;
        if(frame.getCommand().equals(Command.COMMAND)) {
          if (frame.getSendSequenceNumber() == state.getReceiveState()) {
            state.incrementReceiveState();
            // Emit DL-DATA
            boolean pollFlag = ((IFrame) event.getPacket()).isPollBitSet();
            if(pollFlag) { // Asking for an Ack
              SFrame ack = SFrame.create(
                  frame.getSourceCall(),
                  frame.getDestCall(),
                  Command.RESPONSE,
                  SupervisoryFrame.ControlType.RR,
                  state.getReceiveState(),
                  true);
              outgoingPackets.accept(ack);
            }
            newState = StateType.CONNECTED;
          } else {
            // N(R) error recovery
            newState = StateType.AWAITING_CONNECTION;
          }
        } else {
          // Emit DL-ERROR S
          // Discard this frame
          newState = StateType.CONNECTED;
        }
        break;
      }
      case AX25_RR: {
        // Sender is asking how we're doing
        SFrame frame = (SFrame) packet;
        SFrame resp = SFrame.create(
            frame.getSourceCall(),
            frame.getDestCall(),
            Command.RESPONSE,
            SupervisoryFrame.ControlType.RR,
            state.getReceiveState(),
            true);
        outgoingPackets.accept(resp);
        newState = StateType.CONNECTED;
        break;
      }
      case AX25_UNKNOWN:
      case AX25_FRMR:
      case AX25_RNR:
      case AX25_SREJ:
      case AX25_REJ:
      case T1_EXPIRE:
      case T3_EXPIRE:
      default:
        newState = StateType.CONNECTED;
        break;
    }
    state.setState(newState);
  }
}
