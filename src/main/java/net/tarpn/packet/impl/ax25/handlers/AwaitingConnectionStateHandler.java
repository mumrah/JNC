package net.tarpn.packet.impl.ax25.handlers;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import net.tarpn.ByteUtil;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Command;
import net.tarpn.packet.impl.ax25.AX25Packet.HasInfo;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;
import net.tarpn.packet.impl.ax25.AX25Packet.UnnumberedFrame;
import net.tarpn.packet.impl.ax25.AX25Packet.UnnumberedFrame.ControlType;
import net.tarpn.packet.impl.ax25.AX25State.Timer;
import net.tarpn.packet.impl.ax25.AX25StateEvent.InternalInfo;
import net.tarpn.datalink.LinkPrimitive;
import net.tarpn.datalink.LinkPrimitive.ErrorType;
import net.tarpn.packet.impl.ax25.IFrame;
import net.tarpn.packet.impl.ax25.UFrame;
import net.tarpn.packet.impl.ax25.UIFrame;
import net.tarpn.packet.impl.ax25.AX25State;
import net.tarpn.packet.impl.ax25.AX25StateEvent;
import net.tarpn.packet.impl.ax25.AX25State.State;

public class AwaitingConnectionStateHandler implements StateHandler {

  @Override
  public State onEvent(
      AX25State state,
      AX25StateEvent event,
      Consumer<AX25Packet> outgoingPackets) {
    final AX25Packet packet = event.getPacket();
    final State newState;
    switch(event.getType()) {
      case DL_CONNECT: {
        // Discard queue
        // Set layer 3 init
        newState = State.AWAITING_CONNECTION;
        break;
      }
      case AX25_SABM: {
        boolean isFinalSet = ((UnnumberedFrame) packet).isPollFinalSet();
        UFrame ua = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.UA, isFinalSet);
        outgoingPackets.accept(ua);
        newState = State.AWAITING_CONNECTION;
        break;
      }
      case AX25_DISC: {
        boolean isFinalSet = ((UnnumberedFrame) packet).isPollFinalSet();
        UFrame ua = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.DM, isFinalSet);
        outgoingPackets.accept(ua);
        newState = State.AWAITING_CONNECTION;
        break;
      }
      case DL_DATA: {
        // If layer 3
        InternalInfo internalInfo = (InternalInfo)packet;
        state.pushIFrame(internalInfo);
        newState = State.AWAITING_CONNECTION;
        break;
      }
      case IFRAME_READY: {
        HasInfo pendingIFrame = state.popIFrame();
        if(pendingIFrame != null) {
          // one-shot timer to delay the re-sending a bit
          Timer.create(10, () ->
              state.pushIFrame(pendingIFrame)
          ).start();
        }
        newState = State.AWAITING_CONNECTION;
        break;
      }
      case AX25_UI: {
        StateHelper.UICheck(state, (UIFrame)packet);
        if (((UIFrame) packet).isPollFinalSet()) {
          // Send DM F=1
          UFrame ua = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.DM, true);
          outgoingPackets.accept(ua);
        }
        newState = State.AWAITING_CONNECTION;
        break;
      }
      case DL_UNIT_DATA: {
        InternalInfo internalInfo = (InternalInfo)packet;
        UIFrame uiFrame = UIFrame.create(state.getRemoteNodeCall(), state.getLocalNodeCall(),
            internalInfo.getProtocol(), internalInfo.getInfo());
        outgoingPackets.accept(uiFrame);
        newState = State.AWAITING_CONNECTION;
        break;
      }
      case AX25_DM: {
        boolean isFinalSet = ((UnnumberedFrame) packet).isPollFinalSet();
        if (isFinalSet) {
          state.clearIFrames();
          state.sendDataLinkPrimitive(LinkPrimitive.newDisconnectIndication(state.getRemoteNodeCall()));
          state.getT1Timer().cancel();
          newState = State.DISCONNECTED;
        } else {
          newState = State.AWAITING_CONNECTION;
        }
        break;
      }
      case AX25_UA: {
        boolean isFinalSet = ((UnnumberedFrame) packet).isPollFinalSet();
        if (isFinalSet) {
          if(true) {
            // TODO if layer 3
            state.sendDataLinkPrimitive(LinkPrimitive.newConnectConfirmation(state.getRemoteNodeCall()));
            } else {
            if(!ByteUtil.equals(state.getSendStateByte(), state.getAcknowledgeStateByte())) {
              state.clearIFrames();
              state.sendDataLinkPrimitive(LinkPrimitive.newConnectIndication(state.getRemoteNodeCall()));
            }
          }
          state.reset();
          StateHelper.selectT1Value(state);
          newState = State.CONNECTED;
        } else {
          state.sendDataLinkPrimitive(LinkPrimitive
              .newErrorResponse(state.getRemoteNodeCall(), ErrorType.D));
          newState = State.AWAITING_CONNECTION;
        }
        break;
      }
      case AX25_SABME: {
        UFrame ua = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.DM, true);
        outgoingPackets.accept(ua);
        // I guess we should disconnect here since we don't support 2.2
        state.sendDataLinkPrimitive(LinkPrimitive.newDisconnectIndication(state.getRemoteNodeCall()));
        newState = State.DISCONNECTED;
        break;
      }
      case T1_EXPIRE: {
        if(state.checkRC()) {
          state.incrementRC();
          UFrame sabm = UFrame.create(
              state.getRemoteNodeCall(),
              state.getLocalNodeCall(),
              Command.COMMAND, ControlType.SABM, true);
          outgoingPackets.accept(sabm);
          StateHelper.selectT1Value(state);
          state.getT1Timer().start();
          newState = State.AWAITING_CONNECTION;
        } else {
          state.sendDataLinkPrimitive(LinkPrimitive
              .newErrorResponse(state.getRemoteNodeCall(), ErrorType.G));
          state.sendDataLinkPrimitive(LinkPrimitive.newDisconnectIndication(state.getRemoteNodeCall()));
          newState = State.DISCONNECTED;
        }
        break;
      }
      // The spec does not really say what to do if we get commands in this state, so let's ignore them
      case AX25_UNKNOWN:
      case AX25_INFO:
      case AX25_FRMR:
      case AX25_RR:
      case AX25_RNR:
      case AX25_SREJ:
      case AX25_REJ:
      case T3_EXPIRE:
      case DL_DISCONNECT:
      default: {
        newState = State.AWAITING_CONNECTION;
        break;
      }
    }
    return newState;
  }
}
