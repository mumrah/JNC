package net.tarpn.packet.impl.ax25.fsm;

import java.util.function.Consumer;
import net.tarpn.Configuration;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Command;
import net.tarpn.packet.impl.ax25.AX25Packet.FrameType;
import net.tarpn.packet.impl.ax25.AX25Packet.UnnumberedFrame.ControlType;
import net.tarpn.packet.impl.ax25.IFrame;
import net.tarpn.packet.impl.ax25.SFrame;
import net.tarpn.packet.impl.ax25.UFrame;
import net.tarpn.packet.impl.ax25.UIFrame;

public class DisconnectedStateHandler implements StateHandler {

  @Override
  public void onEvent(
      State state,
      StateEvent event,
      Consumer<AX25Packet> outgoingPackets,
      Consumer<AX25Packet> L3Packets) {
    final AX25Packet packet = event.getPacket();
    final StateType newState;
    switch (event.getType()) {
      case AX25_UI: {
        L3Packets.accept(packet);
        if (((UIFrame) packet).isPollFinalSet()) {
          // Send DM F=1
          UFrame ua = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.DM, true);
          outgoingPackets.accept(ua);
        }
        newState = StateType.DISCONNECTED;
        break;
      }
      case AX25_SABM: {
        // Check if we can connect (are we busy?)
        // Send UA
        UFrame ua = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.UA, true);
        outgoingPackets.accept(ua);
        // Reset exceptions, state values, and timers
        state.reset();
        // DL-CONNECT indication
        // Set TIV (T initial value?)
        state.getT3Timer().start();
        newState = StateType.CONNECTED;
        break;
      }
      case AX25_UA:
        // print error N
      case AX25_DM:
      case AX25_SABME: // not going to support this yet
      case AX25_FRMR:
      case AX25_DISC: {
        // Send DM F=P
        boolean finalFlag = ((UFrame) packet).isPollFinalSet();
        UFrame dm = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.DM, finalFlag);
        outgoingPackets.accept(dm);
        newState = StateType.DISCONNECTED;
        break;
      }
      case AX25_RR:
      case AX25_RNR:
      case AX25_SREJ:
      case AX25_REJ: {
        boolean pollBitSet = ((SFrame) packet).isPollOrFinalSet();
        UFrame dm = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.DM, pollBitSet);
        outgoingPackets.accept(dm);
        newState = StateType.DISCONNECTED;
        break;
      }
      case AX25_INFO: {
        boolean pollBitSet = ((IFrame) packet).isPollBitSet();
        UFrame dm = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.DM, pollBitSet);
        outgoingPackets.accept(dm);
        newState = StateType.DISCONNECTED;
        break;
      }
      case DL_UNIT_DATA: {
        if(packet.getFrameType().equals(FrameType.UI)) {
          outgoingPackets.accept(packet);
        } else {
          // warning
        }
        newState = StateType.DISCONNECTED;
        break;
      }
      case DL_DATA: {
        // error
        newState = StateType.DISCONNECTED;
        break;
      }
      case DL_CONNECT: {
        state.resetRC();
        UFrame sabm = UFrame.create(state.getRemoteNodeCall(), Configuration.getOwnNodeCallsign(), Command.COMMAND, ControlType.SABM, true);
        outgoingPackets.accept(sabm);
        state.getT3Timer().cancel();
        state.getT1Timer().start();
        newState = StateType.AWAITING_CONNECTION;
        break;
      }
      case T1_EXPIRE:
        System.err.println("T1 Expired!!");
      case T3_EXPIRE:
      case AX25_UNKNOWN:
      default: {
        // Log error
        newState = StateType.DISCONNECTED;
        break;
      }
    }
    state.setState(newState);
  }
}
