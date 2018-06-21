package net.tarpn.packet.impl.ax25;

import java.util.concurrent.atomic.AtomicInteger;

public class DXE {
  private final AX25Call sourceCall;
  private final AX25Call destCall;

  private final AtomicInteger vs = new AtomicInteger(0);
  private final AtomicInteger vr = new AtomicInteger(0);

  private int nr;
  private int ns;

  private volatile long lastHeard;

  private volatile State state = State.CLOSED;

  public enum State {
    /**
     * An initial and idle state, no traffic is being handled or passed
     */
    CLOSED,

    /**
     * Starting the connection process
     */
    CONNECTING,

    /**
     * Starting the disconnect process
     */
    CLOSING,

    /**
     * Ready to send and receive data
     */
    CONNECTED
  };


  public DXE(AX25Call sourceCall, AX25Call destCall) {
    this.sourceCall = sourceCall;
    this.destCall = destCall;
  }

  public AX25Call getSourceCall() {
    return sourceCall;
  }

  public AX25Call getDestCall() {
    return destCall;
  }

  // State management

  public State getState() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
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

  /**
   * Test if the current V(S) is equal to the last heard N(S) + 7. If that's true, flow control
   * can get corrupted.
   *
   * See https://www.tapr.org/pub_ax25.html#2.4.4.1
   * @return
   */
  public boolean shouldSendIFrame() {
    return vs.get() != ((nr + 7) % 8);
  }

  public void reset() {
    vs.set(0);
    vr.set(0);
    state = State.CLOSED;
  }
}
