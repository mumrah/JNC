package net.tarpn.packet.impl.ax25.handlers;

import java.util.function.Consumer;

import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.util.ByteUtil;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Command;
import net.tarpn.packet.impl.ax25.AX25Packet.HasInfo;
import net.tarpn.packet.impl.ax25.AX25Packet.SupervisoryFrame;
import net.tarpn.packet.impl.ax25.AX25Packet.UnnumberedFrame.ControlType;
import net.tarpn.packet.impl.ax25.AX25State;
import net.tarpn.packet.impl.ax25.AX25State.State;
import net.tarpn.util.Timer;
import net.tarpn.packet.impl.ax25.AX25StateEvent;
import net.tarpn.packet.impl.ax25.AX25StateEvent.InternalInfo;
import net.tarpn.packet.impl.ax25.AX25StateEvent.Type;
import net.tarpn.datalink.DataLinkPrimitive.ErrorType;
import net.tarpn.packet.impl.ax25.IFrame;
import net.tarpn.packet.impl.ax25.SFrame;
import net.tarpn.packet.impl.ax25.UFrame;
import net.tarpn.packet.impl.ax25.UIFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnectedStateHandler implements StateHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ConnectedStateHandler.class);

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
        InternalInfo internalInfo = (InternalInfo)packet;
        state.pushIFrame(internalInfo);
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
          LOG.warn("Cannot send " + pendingIFrame + ". The window is, waiting a bit and retrying. Pending IFrames: " + state.peekIFrames() + ", State: " + state);
          // one-shot timer to delay the re-sending a bit
          Timer.create(200, () ->
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
          state.storeSentIFrame(iFrame);
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
      case T1_EXPIRE:
      case T3_EXPIRE: {
        state.resetRC();
        state.resetRC();
        if(event.getType().equals(Type.T1_EXPIRE)) {
          state.incrementRC();
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
        state.sendDataLinkPrimitive(DataLinkPrimitive
            .newErrorResponse(state.getRemoteNodeCall(), state.getLocalNodeCall(), ErrorType.F));
        if(state.getSendState() == state.getAcknowledgeState()) {
          state.clearIFrames();
          state.sendDataLinkPrimitive(DataLinkPrimitive.newConnectIndication(state.getRemoteNodeCall(), state.getLocalNodeCall()));
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
        state.sendDataLinkPrimitive(DataLinkPrimitive.newDisconnectIndication(state.getRemoteNodeCall(), state.getLocalNodeCall()));
        state.getT1Timer().cancel();
        state.getT3Timer().cancel();
        newState = State.DISCONNECTED;
        break;
      }
      case AX25_UA: {
        state.sendDataLinkPrimitive(DataLinkPrimitive
            .newErrorResponse(state.getRemoteNodeCall(), state.getLocalNodeCall(), ErrorType.C));
        StateHelper.establishDataLink(state, outgoingPackets);
        // Clear layer 3
        newState = State.AWAITING_CONNECTION;
        break;
      }
      case AX25_DM: {
        state.sendDataLinkPrimitive(DataLinkPrimitive
            .newErrorResponse(state.getRemoteNodeCall(), state.getLocalNodeCall(), ErrorType.E));
        state.sendDataLinkPrimitive(DataLinkPrimitive.newDisconnectIndication(state.getRemoteNodeCall(), state.getLocalNodeCall()));
        state.clearIFrames();
        state.getT1Timer().cancel();
        state.getT3Timer().cancel();
        newState = State.DISCONNECTED;
        break;
      }
      case DL_UNIT_DATA: {
        InternalInfo internalInfo = (InternalInfo)packet;
        UIFrame uiFrame = UIFrame.create(state.getRemoteNodeCall(), state.getLocalNodeCall(),
            internalInfo.getProtocol(), internalInfo.getInfo());
        outgoingPackets.accept(uiFrame);
        newState = State.CONNECTED;
        break;
      }
      case AX25_UI: {
        UIFrame uiFrame = (UIFrame)packet;
        state.sendDataLinkPrimitive(DataLinkPrimitive.newUnitDataIndication(uiFrame));
        if(uiFrame.isPollFinalSet()) {
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
          LOG.warn("N(R) error recovery, re-establishing connectiong");
          StateHelper.nrErrorRecovery(state, outgoingPackets);
          newState = State.AWAITING_CONNECTION;
        }
        break;
      }

      case AX25_INFO: {
        // Got info frame, need to ack it
        IFrame iFrame = (IFrame) packet;
        if(iFrame.getCommand().equals(Command.COMMAND)) {
          if(ByteUtil.lessThanEq(state.getAcknowledgeStateByte(), iFrame.getReceiveSequenceNumber()) &&
              ByteUtil.lessThanEq(iFrame.getReceiveSequenceNumber(), state.getSendStateByte())) {
            StateHelper.checkIFrameAck(state, iFrame.getReceiveSequenceNumber());
            if(ByteUtil.equals(iFrame.getSendSequenceNumber(), state.getReceiveStateByte())) {
              state.incrementReceiveState();
              state.clearRejectException();
              state.sendDataLinkPrimitive(DataLinkPrimitive.newDataIndication(iFrame));
              if(iFrame.isPollBitSet()) {
                // Set N(R) = V(R)
                state.enqueueInfoAck(outgoingPackets);
              }
            } else {
              if(state.isRejectException()) {
                // Discard IFrame
                if(iFrame.isPollBitSet()) {
                  SFrame rr = SFrame.create(
                      iFrame.getSourceCall(),
                      iFrame.getDestCall(),
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
                    iFrame.getSourceCall(),
                    iFrame.getDestCall(),
                    Command.RESPONSE,
                    SupervisoryFrame.ControlType.REJ,
                    state.getReceiveState(),
                    iFrame.isPollBitSet());
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
          state.sendDataLinkPrimitive(DataLinkPrimitive
              .newErrorResponse(state.getRemoteNodeCall(), state.getLocalNodeCall(), ErrorType.S));
          // Discard this frame
          newState = State.CONNECTED;
        }
        break;
      }

      case AX25_FRMR: {
        state.sendDataLinkPrimitive(DataLinkPrimitive
                .newErrorResponse(state.getRemoteNodeCall(), state.getLocalNodeCall(), ErrorType.K));
        StateHelper.establishDataLink(state, outgoingPackets);
        newState = State.AWAITING_CONNECTION;
        // TODO set layer 3
        break;
      }

      case AX25_SREJ:
      case AX25_REJ:
      case AX25_UNKNOWN:
      default:
        newState = State.CONNECTED;
        break;
    }
    return newState;
  }
}
