package net.tarpn.packet.impl.ax25.handlers;

import java.util.function.Consumer;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Command;
import net.tarpn.packet.impl.ax25.AX25Packet.SupervisoryFrame;
import net.tarpn.packet.impl.ax25.AX25State;
import net.tarpn.packet.impl.ax25.SFrame;

public class StateHelper {
  public static void checkIFrameAck(AX25State state, int nr) {
    if(nr == state.getSendState()) {
      state.setAcknowledgeState((byte)(nr & 0xff));
      state.getT1Timer().cancel();
      state.getT3Timer().start();
      selectT1Value(state);
    } else {
      if(nr != state.getAcknowledgeState()) {
        state.setAcknowledgeState((byte)(nr & 0xff));
        state.getT1Timer().start();
      }
    }
  }

  public static void selectT1Value(AX25State state) {
    if(state.getRC() == 0) {

    } else {

    }
  }

  public static void checkNeedForResponse(AX25State state, SFrame sFrame, Consumer<AX25Packet> packetConsumer) {
    if(sFrame.getCommand().equals(Command.COMMAND) && sFrame.isPollOrFinalSet()) {
      enquiryResponse(state, sFrame, packetConsumer);
    } else {
      if(sFrame.getCommand().equals(Command.RESPONSE) && sFrame.isPollOrFinalSet()) {
        // DL-ERROR A
      }
    }
  }

  public static void enquiryResponse(AX25State state, AX25Packet packet, Consumer<AX25Packet> packetConsumer) {
    // TODO ever send RNR?
    SFrame rr = SFrame.create(
        packet.getSourceCall(),
        packet.getDestCall(),
        Command.RESPONSE,
        SupervisoryFrame.ControlType.RR,
        state.getReceiveState(),
        true);
    packetConsumer.accept(rr);
    state.clearAckPending();
  }
}
