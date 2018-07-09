package net.tarpn.packet.impl.ax25.handlers;

import java.util.function.Consumer;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Command;
import net.tarpn.packet.impl.ax25.AX25Packet.FrameType;
import net.tarpn.packet.impl.ax25.AX25Packet.UnnumberedFrame;
import net.tarpn.packet.impl.ax25.AX25Packet.UnnumberedFrame.ControlType;
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
      Consumer<AX25Packet> outgoingPackets,
      Consumer<AX25Packet> L3Packets) {
    final AX25Packet packet = event.getPacket();
    final State newState;
    switch(event.getType()) {
      case T1_EXPIRE: {
        System.err.println("T1 expired!");
        // RC == N2? (retry count == max retries?)
        if(state.checkAndIncrementRC()) {
          UFrame sabm = UFrame.create(
              state.getRemoteNodeCall(),
              state.getLocalNodeCall(),
              Command.COMMAND, ControlType.SABM, true);
          outgoingPackets.accept(sabm);
          // Increase T1 and restart it
          state.getT1Timer().setTimeout(state.getT1Timer().getTimeout() * 2); // TODO fix this increase
          state.getT1Timer().start();
          newState = State.AWAITING_CONNECTION;
        } else {
          // Error G, DL_DISCONNECT,
          state.getT1Timer().setTimeout(AX25State.T1_TIMEOUT_MS);
          newState = State.DISCONNECTED;
        }
        break;
      }
      case AX25_UA: {
        boolean isFinalSet = ((UnnumberedFrame) packet).isPollFinalSet();
        if (isFinalSet) {
          // DL_CONNECT confirm
          state.reset();
          newState = State.CONNECTED;
        } else {
          // Error D
          newState = State.AWAITING_CONNECTION;
        }
        break;
      }
      case AX25_DM: {
        boolean isFinalSet = ((UnnumberedFrame) packet).isPollFinalSet();
        if (isFinalSet) {
          // DL_DISCONNECT
          state.getT1Timer().cancel();
          newState = State.DISCONNECTED;
        } else {
          newState = State.AWAITING_CONNECTION;
        }
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
      case AX25_UI: {
        if (((UIFrame) packet).isPollFinalSet()) {
          // Send DM F=1
          UFrame ua = UFrame.create(packet.getSourceCall(), packet.getDestCall(), Command.RESPONSE, ControlType.DM, true);
          outgoingPackets.accept(ua);
        } else {
          // Pass UI to layer 3
          L3Packets.accept(packet);
        }
        newState = State.AWAITING_CONNECTION;
        break;
      }
      case DL_UNIT_DATA: {
        if(packet.getFrameType().equals(FrameType.UI)) {
          outgoingPackets.accept(packet);
        } else {
          // warning
        }
        newState = State.AWAITING_CONNECTION;
        break;
      }
      default: {
        newState = State.AWAITING_CONNECTION;
        break;
      }
    }
    return newState;
  }
}
