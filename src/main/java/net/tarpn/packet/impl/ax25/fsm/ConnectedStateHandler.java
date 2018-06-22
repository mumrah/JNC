package net.tarpn.packet.impl.ax25.fsm;

import java.util.function.Consumer;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;

public class ConnectedStateHandler implements StateHandler {

  @Override
  public void onEvent(State state, StateEvent event, Consumer<AX25Packet> outgoingPackets) {
    AX25Packet packet = event.getPacket();
    AX25Call source = packet.getSourceCall();
    AX25Call dest = packet.getDestCall();

    final StateType newState;
    switch(event.getType()) {
      case AX25_UA:
        break;
      case AX25_DM:
        break;
      case AX25_UI:
        break;
      case AX25_DISC:
        break;
      case AX25_SABM:
        break;
      case AX25_SABME:
        break;
      case AX25_UNKNOWN:
        break;
    }
  }
}
