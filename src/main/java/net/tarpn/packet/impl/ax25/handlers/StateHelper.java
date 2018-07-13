package net.tarpn.packet.impl.ax25.handlers;

import java.util.function.Consumer;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Command;
import net.tarpn.packet.impl.ax25.AX25Packet.SupervisoryFrame;
import net.tarpn.packet.impl.ax25.AX25Packet.UnnumberedFrame.ControlType;
import net.tarpn.packet.impl.ax25.AX25State;
import net.tarpn.packet.impl.ax25.DataLinkEvent.DataIndicationDataLinkEvent;
import net.tarpn.packet.impl.ax25.DataLinkEvent.ErrorIndicationDataLinkEvent;
import net.tarpn.packet.impl.ax25.DataLinkEvent.ErrorIndicationDataLinkEvent.ErrorType;
import net.tarpn.packet.impl.ax25.DataLinkEvent.Type;
import net.tarpn.packet.impl.ax25.SFrame;
import net.tarpn.packet.impl.ax25.UFrame;
import net.tarpn.packet.impl.ax25.UIFrame;

public class StateHelper {

  public static void nrErrorRecovery(AX25State state, Consumer<AX25Packet> packetConsumer) {
    state.sendDataLinkEvent(
        new ErrorIndicationDataLinkEvent(ErrorType.J, state.getSessionId()));
    establishDataLink(state, packetConsumer);
    // clear layer 3 init
  }

  public static void establishDataLink(AX25State state, Consumer<AX25Packet> packetConsumer) {
    clearExceptionConditions(state);
    state.resetRC();
    UFrame sabm = UFrame.create(
        state.getRemoteNodeCall(),
        state.getLocalNodeCall(),
        Command.COMMAND, ControlType.SABM, true);
    packetConsumer.accept(sabm);
    state.getT3Timer().cancel();
    state.getT1Timer().start();
  }

  public static void clearExceptionConditions(AX25State state) {
    // Clear peer busy
    // Clear reject exception
    // Clear own busy
    state.clearAckPending();
  }

  public static void transmitEnquiry(AX25State state, Consumer<AX25Packet> packetConsumer) {
    SFrame resp = SFrame.create(
        state.getRemoteNodeCall(),
        state.getLocalNodeCall(),
        Command.COMMAND,
        SupervisoryFrame.ControlType.RR,
        state.getReceiveState(),
        true);
    packetConsumer.accept(resp);
    state.clearAckPending();
    state.getT1Timer().start();
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

  public static void checkNeedForResponse(AX25State state, SFrame sFrame, Consumer<AX25Packet> packetConsumer) {
    if(sFrame.getCommand().equals(Command.COMMAND) && sFrame.isPollOrFinalSet()) {
      enquiryResponse(state, sFrame, packetConsumer);
    } else {
      if(sFrame.getCommand().equals(Command.RESPONSE) && sFrame.isPollOrFinalSet()) {
        state.sendDataLinkEvent(
            new ErrorIndicationDataLinkEvent(ErrorType.A, state.getSessionId()));
      }
    }
  }

  public static void UICheck(AX25State state, UIFrame uiFrame) {
    if(uiFrame.getCommand().equals(Command.COMMAND)) {
      // TODO check length, error K
      state.sendDataLinkEvent(
          new DataIndicationDataLinkEvent(uiFrame, state.getSessionId(), Type.DL_UNIT_DATA));
    } else {
      state.sendDataLinkEvent(
          new ErrorIndicationDataLinkEvent(ErrorType.Q, state.getSessionId()));
    }
  }


  public static void selectT1Value(AX25State state) {
    if(state.getRC() == 0) {

    } else {

    }
  }




}
