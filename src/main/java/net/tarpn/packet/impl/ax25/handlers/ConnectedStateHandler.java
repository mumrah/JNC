package net.tarpn.packet.impl.ax25.handlers;

import java.util.function.Consumer;
import net.tarpn.ByteUtil;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Command;
import net.tarpn.packet.impl.ax25.AX25Packet.FrameType;
import net.tarpn.packet.impl.ax25.AX25Packet.HasInfo;
import net.tarpn.packet.impl.ax25.AX25Packet.SupervisoryFrame;
import net.tarpn.packet.impl.ax25.AX25Packet.UnnumberedFrame.ControlType;
import net.tarpn.packet.impl.ax25.AX25State;
import net.tarpn.packet.impl.ax25.AX25State.State;
import net.tarpn.packet.impl.ax25.AX25State.Timer;
import net.tarpn.packet.impl.ax25.AX25StateEvent;
import net.tarpn.packet.impl.ax25.AX25StateEvent.Type;
import net.tarpn.packet.impl.ax25.DataLinkEvent;
import net.tarpn.packet.impl.ax25.DataLinkEvent.BaseDataLinkEvent;
import net.tarpn.packet.impl.ax25.DataLinkEvent.DataIndicationDataLinkEvent;
import net.tarpn.packet.impl.ax25.DataLinkEvent.ErrorIndicationDataLinkEvent;
import net.tarpn.packet.impl.ax25.DataLinkEvent.ErrorIndicationDataLinkEvent.ErrorType;
import net.tarpn.packet.impl.ax25.IFrame;
import net.tarpn.packet.impl.ax25.SFrame;
import net.tarpn.packet.impl.ax25.UFrame;
import net.tarpn.packet.impl.ax25.UIFrame;

public class ConnectedStateHandler implements StateHandler {

  @Override
  public State onEvent(
      AX25State state,
      AX25StateEvent event,
      Consumer<AX25Packet> outgoingPackets) {
    final AX25Packet packet = event.getPacket();
    final State newState;
    switch(event.getType()) {
      case DL_CONNECT: {
        state.clearIFrames();
        StateHelper.establishDataLink(state, outgoingPackets);
        // set layer 3
        newState = State.AWAITING_CONNECTION;
        break;
      }
      case DL_DISCONNECT: {
        state.clearIFrames();
        state.resetRC();
        UFrame disc = UFrame.create(
            state.getRemoteNodeCall(),
            state.getLocalNodeCall(),
            Command.COMMAND, ControlType.DISC, true);
        outgoingPackets.accept(disc);
        state.getT3Timer().cancel();
        state.getT1Timer().start();
        newState = State.AWAITING_RELEASE;
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
        if(pendingIFrame == null) {
          newState = State.CONNECTED;
          break;
        }
        if(state.windowExceeded()) {
          System.err.println("Window Full!!");
          // one-shot timer to delay the re-sending a bit
          Timer.create(10, () ->
              state.pushIFrame(pendingIFrame)
          ).start();
        } else {
          IFrame iFrame = IFrame.create(
              state.getRemoteNodeCall(),
              state.getLocalNodeCall(),
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
      case T3_EXPIRE:
      case T1_EXPIRE: {
        state.resetRC();
        if(event.getType().equals(Type.T1_EXPIRE)) {
          state.checkAndIncrementRC();
        }
        StateHelper.transmitEnquiry(state, outgoingPackets);
        newState = State.TIMER_RECOVERY;
        break;
      }
      case AX25_SABM:
      case AX25_SABME: {
        // Send UA with F=P
        boolean finalFlag = ((UFrame) packet).isPollFinalSet();
        UFrame ua = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.UA, finalFlag);
        outgoingPackets.accept(ua);
        StateHelper.clearExceptionConditions(state);
        state.sendDataLinkEvent(new ErrorIndicationDataLinkEvent(ErrorType.F, state.getSessionId()));
        if(state.getSendState() == state.getAcknowledgeState()) {
          state.clearIFrames();
          state.sendDataLinkEvent(new BaseDataLinkEvent(state.getSessionId(), DataLinkEvent.Type.DL_CONNECT));
        }
        state.reset();
        newState = State.CONNECTED;
        break;
      }
      case AX25_DISC: {
        state.clearIFrames();
        boolean finalFlag = ((UFrame) packet).isPollFinalSet();
        UFrame ua = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.UA, finalFlag);
        outgoingPackets.accept(ua);
        state.sendDataLinkEvent(new BaseDataLinkEvent(state.getSessionId(), DataLinkEvent.Type.DL_DISCONNECT));
        state.getT1Timer().cancel();
        state.getT3Timer().cancel();
        newState = State.DISCONNECTED;
        break;
      }
      case AX25_UA: {
        state.sendDataLinkEvent(new ErrorIndicationDataLinkEvent(ErrorType.C, state.getSessionId()));
        StateHelper.establishDataLink(state, outgoingPackets);
        // Clear layer 3
        newState = State.AWAITING_CONNECTION;
        break;
      }
      case AX25_DM: {
        state.sendDataLinkEvent(new ErrorIndicationDataLinkEvent(ErrorType.E, state.getSessionId()));
        state.sendDataLinkEvent(new BaseDataLinkEvent(state.getSessionId(), DataLinkEvent.Type.DL_DISCONNECT));
        state.clearIFrames();
        state.getT1Timer().cancel();
        state.getT3Timer().cancel();
        newState = State.DISCONNECTED;
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
      case AX25_UI: {
        state.sendDataLinkEvent(new DataIndicationDataLinkEvent(packet, state.getSessionId(), DataLinkEvent.Type.DL_UNIT_DATA));
        if(((UIFrame)packet).isPollFinalSet()) {
          StateHelper.enquiryResponse(state, packet, outgoingPackets);
        }
        newState = State.CONNECTED;
        break;
      }
      case AX25_RNR:
      case AX25_RR: {
        if(event.getType().equals(Type.AX25_RNR)) {
          // set peer busy
        } else {
          // clear peer busy
        }
        SFrame frame = (SFrame) packet;
        StateHelper.checkNeedForResponse(state, frame, outgoingPackets);
        if(ByteUtil.lessThanEq(frame.getReceiveSequenceNumber(), state.getSendStateByte())) {
          StateHelper.checkIFrameAck(state, frame.getReceiveSequenceNumber());
          newState = State.CONNECTED;
        } else {
          StateHelper.nrErrorRecovery(state, outgoingPackets);
          newState = State.AWAITING_CONNECTION;
        }
        break;
      }

      case AX25_INFO: {
        // Got info frame, need to ack it
        IFrame frame = (IFrame) packet;
        if(frame.getCommand().equals(Command.COMMAND)) {
          if(ByteUtil.lessThanEq(frame.getReceiveSequenceNumber(), state.getSendStateByte())) {
            StateHelper.checkIFrameAck(state, frame.getReceiveSequenceNumber());
            if(ByteUtil.equals(frame.getSendSequenceNumber(), state.getReceiveStateByte())) {
              state.incrementReceiveState();
              state.clearRejectException();
              state.sendDataLinkEvent(new DataIndicationDataLinkEvent(packet, state.getSessionId(), DataLinkEvent.Type.DL_DATA));
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
            } else {
              if(state.isRejectException()) {
                // Discard IFrame
                if(frame.isPollBitSet()) {
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
              } else {
                // Discard IFrame
                state.setRejectException();
                SFrame rej = SFrame.create(
                    frame.getSourceCall(),
                    frame.getDestCall(),
                    Command.RESPONSE,
                    SupervisoryFrame.ControlType.REJ,
                    state.getReceiveState(),
                    frame.isPollBitSet());
                outgoingPackets.accept(rej);
                state.clearAckPending();
              }
            }
            newState = State.CONNECTED;
          } else {
            StateHelper.nrErrorRecovery(state, outgoingPackets);
            newState = State.AWAITING_CONNECTION;
          }
        } else {
          state.sendDataLinkEvent(new ErrorIndicationDataLinkEvent(ErrorType.S, state.getSessionId()));
          // Discard this frame
          newState = State.CONNECTED;
        }
        break;
      }

      // TODO ???
      case AX25_UNKNOWN:
      case AX25_FRMR:
      case AX25_SREJ:
      case AX25_REJ:

      default:
        newState = State.CONNECTED;
        break;
    }
    return newState;
  }
}
