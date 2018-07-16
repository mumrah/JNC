package net.tarpn.packet.impl.ax25.handlers;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Command;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;
import net.tarpn.packet.impl.ax25.AX25Packet.UnnumberedFrame.ControlType;
import net.tarpn.packet.impl.ax25.AX25StateEvent.InternalInfo;
import net.tarpn.datalink.LinkPrimitive;
import net.tarpn.datalink.LinkPrimitive.ErrorType;
import net.tarpn.packet.impl.ax25.IFrame;
import net.tarpn.packet.impl.ax25.SFrame;
import net.tarpn.packet.impl.ax25.UFrame;
import net.tarpn.packet.impl.ax25.UIFrame;
import net.tarpn.packet.impl.ax25.AX25State;
import net.tarpn.packet.impl.ax25.AX25StateEvent;
import net.tarpn.packet.impl.ax25.AX25State.State;

public class DisconnectedStateHandler implements StateHandler {

  @Override
  public State onEvent(
      AX25State state,
      AX25StateEvent event,
      Consumer<AX25Packet> outgoingPackets) {
    final AX25Packet packet = event.getPacket();
    final State newState;
    switch (event.getType()) {
      case AX25_UA: {
        state.sendDataLinkPrimitive(
            LinkPrimitive.newErrorResponse(state.getRemoteNodeCall(), ErrorType.C));
        state.sendDataLinkPrimitive(
            LinkPrimitive.newErrorResponse(state.getRemoteNodeCall(), ErrorType.D));
        newState = State.DISCONNECTED;
        break;
      }
      case AX25_DM: {
        newState = State.DISCONNECTED;
        break;
      }
      case AX25_UI: {
        StateHelper.UICheck(state, (UIFrame)packet);
        if (((UIFrame) packet).isPollFinalSet()) {
          UFrame ua = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.DM, true);
          outgoingPackets.accept(ua);
        }
        newState = State.DISCONNECTED;
        break;
      }
      case DL_DISCONNECT: {
        state.sendDataLinkPrimitive(LinkPrimitive.newDisconnectConfirmation(state.getRemoteNodeCall()));
        newState = State.DISCONNECTED;
        break;
      }
      case AX25_DISC: {
        boolean finalFlag = ((UFrame) packet).isPollFinalSet();
        UFrame dm = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.DM, finalFlag);
        outgoingPackets.accept(dm);
        newState = State.DISCONNECTED;
        break;
      }
      case DL_UNIT_DATA: {
        InternalInfo internalInfo = (InternalInfo)packet;
        UIFrame uiFrame = UIFrame.create(state.getRemoteNodeCall(), state.getLocalNodeCall(),
            internalInfo.getProtocol(), internalInfo.getInfo());
        outgoingPackets.accept(uiFrame);
        newState = State.DISCONNECTED;
        break;
      }
      case DL_DATA: {
        newState = State.DISCONNECTED;
        break;
      }
      case DL_CONNECT: {
        StateHelper.establishDataLink(state, outgoingPackets);
        newState = State.AWAITING_CONNECTION;
        break;
      }
      case AX25_SABM: {
        // Check if we can connect (are we busy?)
        // Send UA
        UFrame ua = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.UA, true);
        outgoingPackets.accept(ua);
        // Reset exceptions, state values, and timers
        StateHelper.clearExceptionConditions(state);
        state.reset();
        state.sendDataLinkPrimitive(LinkPrimitive.newConnectIndication(state.getRemoteNodeCall()));
        // Set TIV (T initial value?)
        state.getT3Timer().start();
        if(!state.getWelcomeMessage().isEmpty()) {
          state.pushIFrame(
              IFrame.create(
                  packet.getSourceCall(),
                  packet.getDestCall(),
                  Command.COMMAND,
                  (byte) 0,
                  (byte) 0,
                  true,
                  Protocol.NO_LAYER3,
                  (state.getWelcomeMessage() + '\r').getBytes(StandardCharsets.US_ASCII)));
        } else {
          // TODO warn, no welcome message defined
        }
        newState = State.CONNECTED;
        break;
      }
      case AX25_SABME: {
        // AX25 2.2 not supported
        UFrame ua = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.DM, false);
        outgoingPackets.accept(ua);
        newState = State.DISCONNECTED;
        break;
      }

      // Respond to all other commands with DM
      case AX25_FRMR:
      case AX25_RR:
      case AX25_RNR:
      case AX25_SREJ:
      case AX25_REJ: {
        boolean pollBitSet = ((SFrame) packet).isPollOrFinalSet();
        UFrame dm = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.DM, pollBitSet);
        outgoingPackets.accept(dm);
        newState = State.DISCONNECTED;
        break;
      }
      case AX25_INFO: {
        boolean pollBitSet = ((IFrame) packet).isPollBitSet();
        UFrame dm = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.DM, pollBitSet);
        outgoingPackets.accept(dm);
        newState = State.DISCONNECTED;
        break;
      }

      // Ignore other primitives
      case IFRAME_READY:
      case T1_EXPIRE:
      case T3_EXPIRE:
      case AX25_UNKNOWN:
      default: {
        // Log error?
        newState = State.DISCONNECTED;
        break;
      }
    }
    return newState;
  }
}
