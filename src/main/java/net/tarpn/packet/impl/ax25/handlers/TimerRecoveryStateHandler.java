package net.tarpn.packet.impl.ax25.handlers;

import java.util.function.Consumer;
import net.tarpn.Configuration;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Command;
import net.tarpn.packet.impl.ax25.AX25Packet.SupervisoryFrame;
import net.tarpn.packet.impl.ax25.AX25Packet.UnnumberedFrame;
import net.tarpn.packet.impl.ax25.AX25Packet.UnnumberedFrame.ControlType;
import net.tarpn.packet.impl.ax25.SFrame;
import net.tarpn.packet.impl.ax25.UFrame;
import net.tarpn.packet.impl.ax25.AX25State;
import net.tarpn.packet.impl.ax25.AX25StateEvent;
import net.tarpn.packet.impl.ax25.AX25State.State;

public class TimerRecoveryStateHandler implements StateHandler {
  @Override
  public State onEvent(
      AX25State state,
      AX25StateEvent event,
      Consumer<AX25Packet> outgoingPackets,
      Consumer<AX25Packet> L3Packets) {
    final AX25Packet packet = event.getPacket();
    final State newState;
    switch(event.getType()) {
      case AX25_RNR:
      case AX25_RR: {
        SFrame sFrame = (SFrame)packet;
        if(sFrame.getCommand().equals(Command.RESPONSE) && sFrame.isPollOrFinalSet()) {
          state.getT1Timer().cancel();
          if(state.getAcknowledgeState() <= sFrame.getReceiveSequenceNumber() &&
              sFrame.getReceiveSequenceNumber() <= state.getSendState()) {
            state.setAcknowledgeState(sFrame.getReceiveSequenceNumber());
            if(state.getSendState() == state.getAcknowledgeState()) {
              state.getT3Timer().start();
              newState = State.CONNECTED;
            } else {
              // retransmission
              newState = State.TIMER_RECOVERY;
            }
          } else {
            // N(R) error recovery
            newState = State.AWAITING_CONNECTION;
          }
        } else {
          if (sFrame.getCommand().equals(Command.COMMAND) && sFrame.isPollOrFinalSet()) {
            // enquiry response (RR F=1)
            SFrame resp = SFrame.create(
                state.getRemoteNodeCall(),
                Configuration.getOwnNodeCallsign(),
                Command.RESPONSE,
                SupervisoryFrame.ControlType.RR,
                state.getReceiveState(),
                true);
            outgoingPackets.accept(resp);
            state.clearAckPending();
          }
          if (state.getAcknowledgeState() <= sFrame.getReceiveSequenceNumber() &&
              sFrame.getReceiveSequenceNumber() <= state.getSendState()) {
            state.setAcknowledgeState(sFrame.getReceiveSequenceNumber());
            newState = State.TIMER_RECOVERY;
          } else {
            // N(R) error recovery
            newState = State.AWAITING_CONNECTION;
          }
        }
        break;
      }
      case AX25_SABME:
      case AX25_SABM: {
        boolean isFinalSet = ((UnnumberedFrame) packet).isPollFinalSet();
        UFrame ua = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.UA, isFinalSet);
        outgoingPackets.accept(ua);
        // DL-ERROR F
        if(state.getSendState() != state.getAcknowledgeState()) {
          // discard i frame queue
          // DL-CONNECT
        }
        state.reset();
        state.getT3Timer().start();
        newState = State.CONNECTED;
        break;
      }
      case T1_EXPIRE: {
        if(state.checkAndIncrementRC()) {
          // transmit enquiry
          SFrame rr = SFrame.create(
              state.getRemoteNodeCall(),
              Configuration.getOwnNodeCallsign(),
              Command.RESPONSE,
              SupervisoryFrame.ControlType.RR,
              state.getReceiveState(),
              true);
          outgoingPackets.accept(rr);
          state.clearAckPending();
          state.getT1Timer().start();
        } else {
          if(state.getAcknowledgeState() == state.getSendState()) {
            // DL-ERROR U
          } else {
            // DL-ERROR I
          }
          // DL-DISCONNECT
          UFrame dm = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.COMMAND, ControlType.DM, true);
          outgoingPackets.accept(dm);
          newState = State.DISCONNECTED;
          break;
        }
      }
      default:
        newState = State.TIMER_RECOVERY;
        break;
    }
    return newState;
  }
}
