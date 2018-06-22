package net.tarpn.packet.impl.ax25;

import net.tarpn.packet.PacketHandler;
import net.tarpn.packet.PacketRequest;

public class AX25StateMachine {

  public enum State {
    /**
     * An initial and idle state, no traffic is being handled or passed
     */
    DISCONNECTED,

    /**
     * Starting the connection process
     */
    AWAITING_CONNECTION,

    /**
     * Starting the disconnect process
     */
    AWAITING_RESPONSE,

    /**
     * Ready to send and receive data
     */
    CONNECTED,

    /**
     *
     */
    TIMER_RECOVERY;
  }
}
