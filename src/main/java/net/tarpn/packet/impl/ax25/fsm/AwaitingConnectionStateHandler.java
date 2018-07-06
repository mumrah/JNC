package net.tarpn.packet.impl.ax25.fsm;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import net.tarpn.Configuration;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Command;
import net.tarpn.packet.impl.ax25.AX25Packet.FrameType;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;
import net.tarpn.packet.impl.ax25.AX25Packet.UnnumberedFrame;
import net.tarpn.packet.impl.ax25.AX25Packet.UnnumberedFrame.ControlType;
import net.tarpn.packet.impl.ax25.IFrame;
import net.tarpn.packet.impl.ax25.UFrame;
import net.tarpn.packet.impl.ax25.UIFrame;

public class AwaitingConnectionStateHandler implements StateHandler {

  @Override
  public void onEvent(
      State state,
      StateEvent event,
      Consumer<AX25Packet> outgoingPackets,
      Consumer<AX25Packet> L3Packets) {
    final AX25Packet packet = event.getPacket();
    final StateType newState;
    switch(event.getType()) {
      case T1_EXPIRE: {
        System.err.println("T1 expired!");
        // RC == N2? (retry count == max retries?)
        if(state.checkAndIncrementRC()) {
          UFrame sabm = UFrame.create(state.getRemoteNodeCall(), Configuration.getOwnNodeCallsign(), Command.COMMAND, ControlType.SABM, true);
          outgoingPackets.accept(sabm);
          // Increase T1 and restart it
          state.getT1Timer().setTimeout(state.getT1Timer().getTimeout() * 2); // TODO fix this increase
          state.getT1Timer().start();
          newState = StateType.AWAITING_CONNECTION;
        } else {
          // Error G, DL_DISCONNECT,
          state.getT1Timer().setTimeout(State.T1_TIMEOUT_MS);
          newState = StateType.DISCONNECTED;
        }
        break;
      }
      case AX25_UA: {
        boolean isFinalSet = ((UnnumberedFrame) packet).isPollFinalSet();
        if (isFinalSet) {
          // DL_CONNECT confirm
          state.reset();
          newState = StateType.CONNECTED;
        } else {
          // Error D
          newState = StateType.AWAITING_CONNECTION;
        }
        break;
      }
      case AX25_DM: {
        boolean isFinalSet = ((UnnumberedFrame) packet).isPollFinalSet();
        if (isFinalSet) {
          // DL_DISCONNECT
          state.getT1Timer().cancel();
          newState = StateType.DISCONNECTED;
        } else {
          newState = StateType.AWAITING_CONNECTION;
        }
        break;
      }
      case AX25_SABM: {
        boolean isFinalSet = ((UnnumberedFrame) packet).isPollFinalSet();
        UFrame ua = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.UA, isFinalSet);
        outgoingPackets.accept(ua);
        newState = StateType.AWAITING_CONNECTION;
        break;
      }
      case AX25_DISC: {
        boolean isFinalSet = ((UnnumberedFrame) packet).isPollFinalSet();
        UFrame ua = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.DM, isFinalSet);
        outgoingPackets.accept(ua);
        newState = StateType.AWAITING_CONNECTION;
        break;
      }
      case AX25_UI: {
        if (((UIFrame) packet).isPollFinalSet()) {
          // Send DM F=1
          UFrame ua = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.DM, true);
          outgoingPackets.accept(ua);
        } else {
          // Pass UI to layer 3
          L3Packets.accept(packet);
        }
        newState = StateType.AWAITING_CONNECTION;
        break;
      }
      case DL_UNIT_DATA: {
        if(packet.getFrameType().equals(FrameType.UI)) {
          outgoingPackets.accept(packet);
        } else {
          // warning
        }
        newState = StateType.AWAITING_CONNECTION;
        break;
      }
      default: {
        newState = StateType.AWAITING_CONNECTION;
        break;
      }
    }
    state.setState(newState);
  }
}
