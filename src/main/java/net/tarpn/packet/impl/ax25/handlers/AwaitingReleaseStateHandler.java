package net.tarpn.packet.impl.ax25.handlers;

import java.util.function.Consumer;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Command;
import net.tarpn.packet.impl.ax25.AX25Packet.UnnumberedFrame.ControlType;
import net.tarpn.packet.impl.ax25.AX25State;
import net.tarpn.packet.impl.ax25.AX25State.State;
import net.tarpn.packet.impl.ax25.AX25StateEvent;
import net.tarpn.packet.impl.ax25.AX25StateEvent.InternalInfo;
import net.tarpn.datalink.LinkPrimitive;
import net.tarpn.datalink.LinkPrimitive.ErrorType;
import net.tarpn.packet.impl.ax25.UFrame;
import net.tarpn.packet.impl.ax25.UIFrame;

public class AwaitingReleaseStateHandler implements StateHandler {

  @Override
  public State onEvent(
      AX25State state,
      AX25StateEvent event,
      Consumer<AX25Packet> outgoingPackets) {
    final AX25Packet packet = event.getPacket();
    final State newState;
    switch (event.getType()) {
      case DL_DISCONNECT: {
        UFrame ua = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.COMMAND,
            ControlType.DM, false);
        outgoingPackets.accept(ua);
        state.getT1Timer().cancel();
        newState = State.DISCONNECTED;
        break;
      }
      case AX25_SABM: {
        UFrame ua = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.COMMAND,
            ControlType.DM, ((UFrame) packet).isPollFinalSet());
        outgoingPackets.accept(ua);
        newState = State.AWAITING_RELEASE;
        break;
      }
      case AX25_DISC: {
        UFrame ua = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.COMMAND,
            ControlType.UA, ((UFrame) packet).isPollFinalSet());
        outgoingPackets.accept(ua);
        newState = State.AWAITING_RELEASE;
        break;
      }
      case DL_UNIT_DATA: {
        InternalInfo internalInfo = (InternalInfo)packet;
        UIFrame uiFrame = UIFrame.create(state.getRemoteNodeCall(), state.getLocalNodeCall(),
            internalInfo.getProtocol(), internalInfo.getInfo());
        outgoingPackets.accept(uiFrame);
        newState = State.AWAITING_RELEASE;
        break;
      }
      case AX25_INFO:
      case AX25_RR:
      case AX25_RNR:
      case AX25_REJ:
      case AX25_SREJ: {
        UFrame uFrame = (UFrame)packet;
        if(uFrame.isPollFinalSet()) {
          UFrame ua = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.COMMAND,
              ControlType.DM, true);
          outgoingPackets.accept(ua);
        }
        newState = State.AWAITING_RELEASE;
        break;
      }
      case AX25_UI: {
        UIFrame uiFrame = (UIFrame)packet;
        StateHelper.UICheck(state, uiFrame);
        if(uiFrame.isPollFinalSet()) {
          UFrame ua = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.COMMAND,
              ControlType.DM, true);
          outgoingPackets.accept(ua);
        }
        newState = State.AWAITING_RELEASE;
        break;
      }
      case AX25_UA: {
        UFrame uFrame = (UFrame)packet;
        if(uFrame.isPollFinalSet()) {
          state.sendDataLinkPrimitive(LinkPrimitive.newDisconnectConfirmation(state.getRemoteNodeCall()));
          state.getT1Timer().cancel();
          newState = State.DISCONNECTED;
        } else {
          state.sendDataLinkPrimitive(LinkPrimitive
              .newErrorResponse(state.getRemoteNodeCall(), ErrorType.D));
          newState = State.AWAITING_RELEASE;
        }
        break;
      }
      case AX25_DM: {
        UFrame uFrame = (UFrame)packet;
        if(uFrame.isPollFinalSet()) {
          state.sendDataLinkPrimitive(LinkPrimitive.newDisconnectConfirmation(state.getRemoteNodeCall()));
          state.getT1Timer().cancel();
          newState = State.DISCONNECTED;
        } else {
          newState = State.AWAITING_RELEASE;
        }
        break;
      }
      case T1_EXPIRE: {
        if(state.checkRC()) {
          state.incrementRC();
          UFrame disc = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.COMMAND,
              ControlType.DISC, true);
          outgoingPackets.accept(disc);
          StateHelper.selectT1Value(state);
          state.getT1Timer().start();
          newState = State.AWAITING_RELEASE;
        } else {
          state.sendDataLinkPrimitive(LinkPrimitive
              .newErrorResponse(state.getRemoteNodeCall(), ErrorType.H));
          state.sendDataLinkPrimitive(LinkPrimitive.newDisconnectConfirmation(state.getRemoteNodeCall()));
          newState = State.DISCONNECTED;
        }
        break;
      }
      // Other primitives, do nothing, other commands are not specified in the spec
      case AX25_SABME:
      case AX25_UNKNOWN:
      case AX25_FRMR:
      case T3_EXPIRE:
      case DL_CONNECT:
      case DL_DATA:
      case IFRAME_READY:
      default:
        newState = State.DISCONNECTED;
        break;
    }
    return newState;
  }

}
