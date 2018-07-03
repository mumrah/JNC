package net.tarpn.packet.impl.ax25.fsm;

import java.util.function.Consumer;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Command;
import net.tarpn.packet.impl.ax25.AX25Packet.FrameType;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;
import net.tarpn.packet.impl.ax25.AX25Packet.UnnumberedFrame.ControlType;
import net.tarpn.packet.impl.ax25.IFrame;
import net.tarpn.packet.impl.ax25.SFrame;
import net.tarpn.packet.impl.ax25.UFrame;
import net.tarpn.packet.impl.ax25.UIFrame;
import net.tarpn.packet.impl.netrom.NetRomNodes;

public class DisconnectedStateHandler implements StateHandler {

  @Override
  public void onEvent(
      State state,
      StateEvent event,
      Consumer<AX25Packet> outgoingPackets,
      Consumer<AX25Packet> L3Packets) {
    AX25Packet packet = event.getPacket();
    AX25Call source = packet.getSourceCall();
    AX25Call dest = packet.getDestCall();

    final StateType newState;
    switch (event.getType()) {
      case AX25_UI: {
        if (((UIFrame) event.getPacket()).isPollFinalSet()) {
          // Send DM F=1
          UFrame ua = UFrame.create(source, dest, Command.RESPONSE, ControlType.DM, true);
          outgoingPackets.accept(ua);
        } else {
          // Pass UI to layer 3
          L3Packets.accept(event.getPacket());
        }
        newState = StateType.DISCONNECTED;
        break;
      }
      case AX25_SABM: {
        // Check if we can connect (are we busy?)
        // Send UA
        UFrame ua = UFrame.create(source, dest, Command.RESPONSE, ControlType.UA, true);
        outgoingPackets.accept(ua);
        // Reset exceptions, state values, and timers
        state.reset();
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
        boolean finalFlag = ((UFrame) event.getPacket()).isPollFinalSet();
        UFrame dm = UFrame.create(source, dest, Command.RESPONSE, ControlType.DM, finalFlag);
        outgoingPackets.accept(dm);
        newState = StateType.DISCONNECTED;
        break;
      }
      case AX25_RR:
      case AX25_RNR:
      case AX25_SREJ:
      case AX25_REJ: {
        boolean pollBitSet = ((SFrame) event.getPacket()).isPollOrFinalSet();
        UFrame dm = UFrame.create(source, dest, Command.RESPONSE, ControlType.DM, pollBitSet);
        outgoingPackets.accept(dm);
        newState = StateType.DISCONNECTED;
        break;
      }
      case AX25_INFO: {
        boolean pollBitSet = ((IFrame) event.getPacket()).isPollBitSet();
        UFrame dm = UFrame.create(source, dest, Command.RESPONSE, ControlType.DM, pollBitSet);
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
      case T1_EXPIRE:
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
