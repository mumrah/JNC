package net.tarpn.packet.impl.ax25.fsm;

import java.util.function.Consumer;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Command;
import net.tarpn.packet.impl.ax25.AX25Packet.UnnumberedFrame.ControlType;
import net.tarpn.packet.impl.ax25.UFrame;
import net.tarpn.packet.impl.ax25.UIFrame;

public class DisconnectedStateHandler implements StateHandler {

  @Override
  public void onEvent(State state, StateEvent event, Consumer<AX25Packet> outgoingPackets) {
    AX25Packet packet = event.getPacket();
    AX25Call source = packet.getSourceCall();
    AX25Call dest = packet.getDestCall();

    final StateType newState;
    switch (event.getType()) {
      case AX25_UA: {
        // print error N
        newState = StateType.DISCONNECTED;
        break;
      } case AX25_DM: {
        // no action
        newState = StateType.DISCONNECTED;
        break;
      } case AX25_UI: {
        // pass UI frame to layer 3
        if (((UIFrame) event.getPacket()).isPollFinalSet()) {
          // Send DM F=1
          UFrame ua = UFrame.create(source, dest, Command.RESPONSE, ControlType.DM, true);
          outgoingPackets.accept(ua);
        }
        newState = StateType.DISCONNECTED;
        break;
      } case AX25_DISC: {
        // Send DM F=P
        boolean finalFlag = ((UFrame) event.getPacket()).isPollFinalSet();
        UFrame dm = UFrame.create(source, dest, Command.RESPONSE, ControlType.DM, finalFlag);
        outgoingPackets.accept(dm);
        newState = StateType.DISCONNECTED;
        break;
      } case AX25_SABM: {
        // Check if we can connect (are we busy?)
        // Send UA
        UFrame ua = UFrame.create(dest, source, Command.RESPONSE, ControlType.UA, true);
        outgoingPackets.accept(ua);
        // Reset exceptions, state values, and timers
        state.reset();
        newState = StateType.CONNECTED;
        break;
      } case AX25_SABME: {
        // Send DM F=P (not going to support 2.2 yet)
        boolean finalFlag = ((UFrame) event.getPacket()).isPollFinalSet();
        UFrame dm = UFrame.create(source, dest, Command.RESPONSE, ControlType.DM, finalFlag);
        outgoingPackets.accept(dm);
        newState = StateType.DISCONNECTED;
        break;
      } default: {
        // Log error
        newState = StateType.DISCONNECTED;
        break;
      }
    }
    state.setState(newState);
  }
}
