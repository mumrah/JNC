package net.tarpn.packet.impl.ax25.fsm;

import java.util.concurrent.atomic.AtomicInteger;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.DXE.Timer;

public class State {
  public static final int T1_TIMEOUT_MS = 100;

  private final String stateId;
  private final AX25Call sourceCall;
  private final AX25Call destCall;

  private StateType currentState;

  private final AtomicInteger vs = new AtomicInteger(0);
  private final AtomicInteger vr = new AtomicInteger(0);
  private final AtomicInteger va = new AtomicInteger(0);

  private int nr;
  private int ns;

  private Timer t1Timer = Timer.create(T1_TIMEOUT_MS);

  public State(String stateId, AX25Call sourceCall, AX25Call destCall) {
    this.stateId = stateId;
    this.sourceCall = sourceCall;
    this.destCall = destCall;
    this.currentState = StateType.DISCONNECTED;
  }

  public String getStateId() {
    return stateId;
  }

  public void setState(StateType newState) {
    currentState = newState;
  }

  public StateType getState() {
    return currentState;
  }

  public int getSendState() {
    return vs.get() % 8;
  }

  public int getNextSendState() {
    return vs.getAndIncrement() % 8;
  }

  public void setLastSendHeard(int ns) {
    this.ns = ns;
  }

  public int getReceiveState() {
    return vr.get() % 8;
  }

  public void incrementReceiveState() {
    this.vr.incrementAndGet();
  }

  public void setLastReceiveHeard(int nr) {
    this.nr = nr;
  }

  public void reset() {
    vs.set(0);
    vr.set(0);
    t1Timer.reset();
  }

  @Override
  public String toString() {
    return "State{" +
        "state=" + currentState +
        ", V(s)=" + vs.get() +
        ", V(r)=" + vr.get() +
        ", V(a)=" + va.get() +
        '}';
  }
}
