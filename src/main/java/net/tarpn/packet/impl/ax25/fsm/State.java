package net.tarpn.packet.impl.ax25.fsm;

import java.util.concurrent.atomic.AtomicInteger;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.DXE.Timer;

public class State {
  public static final int T1_TIMEOUT_MS = 100;

  private final AX25Call sourceCall;
  private final AX25Call destCall;

  private StateType currentState;

  private final AtomicInteger vs = new AtomicInteger(0);
  private final AtomicInteger vr = new AtomicInteger(0);

  private Timer t1Timer = Timer.create(T1_TIMEOUT_MS);

  public State(AX25Call sourceCall, AX25Call destCall) {
    this.sourceCall = sourceCall;
    this.destCall = destCall;
    this.currentState = StateType.DISCONNECTED;
  }


  public void setState(StateType newState) {
    currentState = newState;
  }

  public StateType getState() {
    return currentState;
  }

  public void reset() {
    vs.set(0);
    vr.set(0);
    t1Timer.reset();
  }

}
