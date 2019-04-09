package net.tarpn.packet.impl.ax25.handlers;

import java.util.function.Consumer;

import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.util.ByteUtil;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Command;
import net.tarpn.packet.impl.ax25.AX25Packet.HasInfo;
import net.tarpn.packet.impl.ax25.AX25Packet.SupervisoryFrame;
import net.tarpn.packet.impl.ax25.AX25Packet.UnnumberedFrame;
import net.tarpn.packet.impl.ax25.AX25Packet.UnnumberedFrame.ControlType;
import net.tarpn.packet.impl.ax25.AX25State;
import net.tarpn.packet.impl.ax25.AX25State.State;
import net.tarpn.util.Timer;
import net.tarpn.packet.impl.ax25.AX25StateEvent;
import net.tarpn.packet.impl.ax25.AX25StateEvent.InternalInfo;
import net.tarpn.datalink.DataLinkPrimitive.ErrorType;
import net.tarpn.packet.impl.ax25.IFrame;
import net.tarpn.packet.impl.ax25.SFrame;
import net.tarpn.packet.impl.ax25.UFrame;
import net.tarpn.packet.impl.ax25.UIFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimerRecoveryStateHandler implements StateHandler {

  private static final Logger LOG = LoggerFactory.getLogger(TimerRecoveryStateHandler.class);

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
        // set layer 3 init
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
        newState = State.TIMER_RECOVERY;
        break;
      }
      case IFRAME_READY: {
        HasInfo pendingIFrame = state.popIFrame();
        if(pendingIFrame == null) {
          newState = State.TIMER_RECOVERY;
          break;
        }
        if(state.windowExceeded()) {
          LOG.warn("IFrame window is full, waiting a bit and retrying");
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
        newState = State.TIMER_RECOVERY;
        break;
      }
      case T1_EXPIRE: {
        if(state.checkRC()) {
          state.incrementRC();
          StateHelper.transmitEnquiry(state, outgoingPackets);
          newState = State.TIMER_RECOVERY;
        } else {
          if(state.getAcknowledgeState() == state.getSendState()) {
            state.sendDataLinkPrimitive(DataLinkPrimitive
                .newErrorResponse(state.getRemoteNodeCall(), ErrorType.U));
          } else {
            state.sendDataLinkPrimitive(DataLinkPrimitive
                .newErrorResponse(state.getRemoteNodeCall(), ErrorType.I));
          }
          state.internalDisconnectRequest();
          UFrame dm = UFrame.create(state.getRemoteNodeCall(), state.getLocalNodeCall(),
              Command.COMMAND, ControlType.DM, true);
          outgoingPackets.accept(dm);
          newState = State.DISCONNECTED;
        }
        break;
      }
      case AX25_SABME: // TODO we shouldn't accept this since we don't support it
      case AX25_SABM: {
        boolean isFinalSet = ((UnnumberedFrame) packet).isPollFinalSet();
        UFrame ua = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.UA, isFinalSet);
        outgoingPackets.accept(ua);
        state.sendDataLinkPrimitive(DataLinkPrimitive
            .newErrorResponse(state.getRemoteNodeCall(), ErrorType.F));
        if(!ByteUtil.equals(state.getSendStateByte(), state.getAcknowledgeStateByte())) {
          state.clearIFrames();
          state.sendDataLinkPrimitive(DataLinkPrimitive.newConnectIndication(state.getRemoteNodeCall()));
        }
        state.reset();
        state.getT3Timer().start();
        newState = State.CONNECTED;
        break;
      }
      case AX25_RNR:
        // TODO Set peer busy
      case AX25_RR: {
        // TODO Set peer clear
        SFrame sFrame = (SFrame)packet;
        if(sFrame.getCommand().equals(Command.RESPONSE) && sFrame.isPollOrFinalSet()) {
          state.getT1Timer().cancel();
          StateHelper.selectT1Value(state);
          if(ByteUtil.lessThanEq(sFrame.getReceiveSequenceNumber(), state.getSendStateByte()) &&
              ByteUtil.lessThanEq(state.getAcknowledgeStateByte(), sFrame.getReceiveSequenceNumber())) {
            state.setAcknowledgeState(sFrame.getReceiveSequenceNumber());
            if(state.checkSendEqAckSeq()) {
              state.getT3Timer().start();
              newState = State.CONNECTED;
            } else {
              LOG.debug("Invoke Retransmission, N(r)=" + sFrame.getReceiveSequenceNumber() + ", state=" + state);
              StateHelper.invokeRetransmission(sFrame.getReceiveSequenceNumber(), state);
              newState = State.TIMER_RECOVERY;
            }
          } else {
            LOG.debug("N(r) Error Recovery");
            StateHelper.nrErrorRecovery(state, outgoingPackets);
            newState = State.AWAITING_CONNECTION;
          }
        } else {
          if (sFrame.getCommand().equals(Command.COMMAND) && sFrame.isPollOrFinalSet()) {
            StateHelper.enquiryResponse(state, sFrame, outgoingPackets);
          }
          if (ByteUtil.lessThanEq(sFrame.getReceiveSequenceNumber(), state.getSendStateByte()) &&
              ByteUtil.lessThanEq(state.getAcknowledgeStateByte(), sFrame.getReceiveSequenceNumber())) {
            state.setAcknowledgeState(sFrame.getReceiveSequenceNumber());
            newState = State.TIMER_RECOVERY;
          } else {
            // N(R) error recovery
            LOG.debug("N(r) Error Recovery");
            StateHelper.nrErrorRecovery(state, outgoingPackets);
            newState = State.AWAITING_CONNECTION;
          }
        }
        break;
      }
      case AX25_DISC: {
        state.clearIFrames();
        boolean isFinalSet = ((UnnumberedFrame) packet).isPollFinalSet();
        UFrame ua = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.UA, isFinalSet);
        outgoingPackets.accept(ua);
        state.sendDataLinkPrimitive(DataLinkPrimitive.newDisconnectIndication(state.getRemoteNodeCall()));
        state.getT1Timer().cancel();
        state.getT3Timer().cancel();
        newState = State.DISCONNECTED;
        break;
      }
      case AX25_UA: {
        state.sendDataLinkPrimitive(DataLinkPrimitive
            .newErrorResponse(state.getRemoteNodeCall(), ErrorType.C));
        StateHelper.establishDataLink(state, outgoingPackets);
        // clear layer 3
        newState = State.AWAITING_CONNECTION;
        break;
      }
      case AX25_UI: {
        StateHelper.UICheck(state, (UIFrame)packet);
        if(((UIFrame)packet).isPollFinalSet()) {
          StateHelper.enquiryResponse(state, packet, outgoingPackets);
        }
        newState = State.TIMER_RECOVERY;
        break;
      }
      case DL_UNIT_DATA: {
        InternalInfo internalInfo = (InternalInfo)packet;
        UIFrame uiFrame = UIFrame.create(state.getRemoteNodeCall(), state.getLocalNodeCall(),
            internalInfo.getProtocol(), internalInfo.getInfo());
        outgoingPackets.accept(uiFrame);
        newState = State.TIMER_RECOVERY;
        break;
      }
      case AX25_DM: {
        state.sendDataLinkPrimitive(DataLinkPrimitive
            .newErrorResponse(state.getRemoteNodeCall(), ErrorType.E));
        state.sendDataLinkPrimitive(DataLinkPrimitive.newDisconnectIndication(state.getRemoteNodeCall()));
        state.clearIFrames();
        state.getT1Timer().cancel();
        state.getT3Timer().cancel();
        newState = State.DISCONNECTED;
        break;
      }
      case AX25_INFO: {
        // Got info frame, need to ack it
        IFrame iFrame = (IFrame) packet;
        if(iFrame.getCommand().equals(Command.COMMAND)) {
          if(ByteUtil.lessThanEq(iFrame.getReceiveSequenceNumber(), state.getSendStateByte())) {
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
            newState = State.TIMER_RECOVERY;
          } else {
            StateHelper.nrErrorRecovery(state, outgoingPackets);
            newState = State.AWAITING_CONNECTION;
          }
        } else {
          state.sendDataLinkPrimitive(DataLinkPrimitive
              .newErrorResponse(state.getRemoteNodeCall(), ErrorType.S));
          // Discard this frame
          newState = State.TIMER_RECOVERY;
        }
        break;
      }
      // TODO implement these (?)
      case AX25_FRMR:
      case AX25_SREJ:
      case AX25_REJ:
      case T3_EXPIRE:
      case AX25_UNKNOWN:
      default:
        newState = State.TIMER_RECOVERY;
        break;
    }
    return newState;
  }
}
