package net.tarpn.packet.impl.ax25;

public class DXE {
  private final AX25Call sourceCall;
  private final AX25Call destCall;
  private volatile int vs = 0;
  private volatile int vr = 0;
  private volatile boolean connected = false;

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

  public boolean isConnected() {
    return connected;
  }

  public void reset() {
    vs = 0;
    vr = 0;
    connected = false;
  }

  public void startTimer(int delay, Runnable runnable) {
    // keep sending SABM until we hear a UA or we time out
  }
}
