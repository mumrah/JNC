package net.tarpn.packet.impl.ax25.handlers;

import java.util.function.Consumer;
import net.tarpn.Configuration;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Command;
import net.tarpn.packet.impl.ax25.AX25Packet.FrameType;
import net.tarpn.packet.impl.ax25.AX25Packet.HasInfo;
import net.tarpn.packet.impl.ax25.AX25Packet.SupervisoryFrame;
import net.tarpn.packet.impl.ax25.AX25Packet.UnnumberedFrame.ControlType;
import net.tarpn.packet.impl.ax25.AX25State.Timer;
import net.tarpn.packet.impl.ax25.IFrame;
import net.tarpn.packet.impl.ax25.SFrame;
import net.tarpn.packet.impl.ax25.UFrame;
import net.tarpn.packet.impl.ax25.UIFrame;
import net.tarpn.packet.impl.ax25.AX25State;
import net.tarpn.packet.impl.ax25.AX25StateEvent;
import net.tarpn.packet.impl.ax25.AX25StateEvent.Type;
import net.tarpn.packet.impl.ax25.AX25State.State;

public class ConnectedStateHandler implements StateHandler {

  @Override
  public State onEvent(
      AX25State state,
      AX25StateEvent event,
      Consumer<AX25Packet> outgoingPackets,
      Consumer<AX25Packet> L3Packets) {
    final AX25Packet packet = event.getPacket();
    final State newState;
    switch(event.getType()) {
      case AX25_UA: {
        // Emit DL-ERROR C
        // Clear layer 3
        newState = State.AWAITING_CONNECTION;
        break;
      }
      case AX25_DM: {
        // Emit DL-ERROR E
        // Emit DL-DISCONNECT
        state.clearIFrames();
        state.getT1Timer().cancel();
        state.getT3Timer().cancel();
        newState = State.DISCONNECTED;
        break;
      }
      case AX25_UI: {
        // Emit DL-DATA
        L3Packets.accept(packet);

        // If P=1, send RR
        if(((UIFrame)packet).isPollFinalSet()) {
          SFrame rr = SFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.COMMAND, SupervisoryFrame.ControlType.RR,
              state.getReceiveState(), true);
          outgoingPackets.accept(rr);
        }
        newState = State.CONNECTED;
        break;
      }
      case AX25_DISC: {
        // Clear I Queue
        state.clearIFrames();
        boolean finalFlag = ((UFrame) packet).isPollFinalSet();
        UFrame ua = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.UA, finalFlag);
        outgoingPackets.accept(ua);
        // Emit DL-DISCONNECT
        state.getT1Timer().cancel();
        state.getT3Timer().cancel();
        newState = State.DISCONNECTED;
        break;
      }
      case AX25_SABM:
      case AX25_SABME: {
        // Send UA with F=P
        boolean finalFlag = ((UFrame) packet).isPollFinalSet();
        UFrame ua = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.UA, finalFlag);
        outgoingPackets.accept(ua);
        // Clear exceptions?
        // Emit DL-ERROR F
        if(state.getSendState() == state.getAcknowledgeState()) {
          state.clearIFrames();
          //   Emit DL-CONNECT
        }
        state.reset();
        newState = State.CONNECTED;
        break;
      }
      case AX25_INFO: {
        // Got info frame, need to ack it
        IFrame frame = (IFrame) packet;
        if(frame.getCommand().equals(Command.COMMAND)) {
          if (frame.getReceiveSequenceNumber() <= state.getSendState()) {
            if(frame.getSendSequenceNumber() == state.getReceiveState()) {
              state.incrementReceiveState();
              // Emit DL-DATA
              L3Packets.accept(frame);
              if(frame.isPollBitSet()) {
                // Set N(R) = V(R)
                SFrame rr = SFrame.create(
                    frame.getSourceCall(),
                    frame.getDestCall(),
                    Command.RESPONSE,
                    SupervisoryFrame.ControlType.RR,
                    state.getReceiveState(),
                    true);
                outgoingPackets.accept(rr);
                state.clearAckPending();
              }
            }
            newState = State.CONNECTED;
          } else {
            // N(R) error recovery
            newState = State.AWAITING_CONNECTION;
          }
        } else {
          // Emit DL-ERROR S
          // Discard this frame
          newState = State.CONNECTED;
        }
        break;
      }
      case AX25_RR: {
        SFrame frame = (SFrame) packet;
        if(frame.getCommand().equals(Command.COMMAND) && frame.isPollOrFinalSet()) {
          // Sender is asking how we're doing
          SFrame rr = SFrame.create(
              frame.getSourceCall(),
              frame.getDestCall(),
              Command.RESPONSE,
              SupervisoryFrame.ControlType.RR,
              state.getReceiveState(),
              true);
          outgoingPackets.accept(rr);
        } else {
          if(frame.getCommand().equals(Command.RESPONSE) && frame.isPollOrFinalSet()) {
            // Error A
          }
        }

        if(frame.getReceiveSequenceNumber() <= state.getSendState()) {
          // Check I Frames ack'd
          if(frame.getReceiveSequenceNumber() == state.getSendState()) {

            state.setAcknowledgeState(frame.getReceiveSequenceNumber());
            state.getT1Timer().cancel();
            state.getT3Timer().start();
          } else {
            // All frames are ack'd
            if(frame.getReceiveSequenceNumber() != state.getAcknowledgeState()) {
              state.setAcknowledgeState(frame.getReceiveSequenceNumber());
              state.getT1Timer().start();
            }
          }
          newState = State.CONNECTED;
        } else {
          // N(R) error recovery
          newState = State.AWAITING_CONNECTION;
        }
        break;
      }
      case DL_UNIT_DATA: {
        if(packet.getFrameType().equals(FrameType.UI)) {
          outgoingPackets.accept(packet);
        } else {
          // warning
        }
        newState = State.CONNECTED;
        break;
      }
      case DL_DATA: {
        if(packet instanceof HasInfo) {
          state.pushIFrame((HasInfo)packet);
        } else {
          // warning
        }
        newState = State.CONNECTED;
        break;
      }
      case IFRAME_READY: {
        HasInfo pendingIFrame = state.popIFrame();
        if(state.windowExceeded()) {
          System.err.println("Window Full!!");
          // one-shot timer to delay the re-sending a bit
          Timer.create(10, () ->
              state.pushIFrame(pendingIFrame)
          ).start();
        } else {
          IFrame iFrame = IFrame.create(
              state.getRemoteNodeCall(),
              Configuration.getOwnNodeCallsign(),
              Command.COMMAND,
              state.getSendStateByte(),
              state.getReceiveStateByte(),
              false,
              pendingIFrame.getProtocol(),
              pendingIFrame.getInfo());
          outgoingPackets.accept(iFrame);
          state.incrementSendState();
          state.clearAckPending();
          if(!state.getT1Timer().isRunning()) {
            state.getT3Timer().cancel();
            state.getT1Timer().start();
          }
        }
        newState = State.CONNECTED;
        break;
      }
      case DL_DISCONNECT: {
        state.resetRC();
        UFrame disc = UFrame.create(state.getRemoteNodeCall(), Configuration.getOwnNodeCallsign(),
            Command.COMMAND, ControlType.DISC, true);
        outgoingPackets.accept(disc);
        state.getT3Timer().cancel();
        state.getT1Timer().cancel(); // TODO start (waiting on UA or DM)
        newState = State.DISCONNECTED; // TODO AWAITING_RELEASE
        break;
      }
      case AX25_UNKNOWN:
      case AX25_FRMR:
      case AX25_RNR:
      case AX25_SREJ:
      case AX25_REJ:
      case T3_EXPIRE:
      case T1_EXPIRE: {
        state.resetRC();
        if(event.getType().equals(Type.T1_EXPIRE)) {
          state.checkAndIncrementRC();
        }
        // Send RR to check on the other side
        SFrame resp = SFrame.create(
            state.getRemoteNodeCall(),
            Configuration.getOwnNodeCallsign(),
            Command.COMMAND,
            SupervisoryFrame.ControlType.RR,
            state.getReceiveState(),
            true);
        outgoingPackets.accept(resp);
        state.getT1Timer().start();
        newState = State.TIMER_RECOVERY;
        break;
      }
      default:
        newState = State.CONNECTED;
        break;
    }
    return newState;
  }
}
