package net.tarpn.packet.impl.ax25.fsm;

public enum StateType {
  DISCONNECTED,
  AWAITING_CONNECTION,
  CONNECTED,
  TIMER_RECOVERY;
}
