package net.tarpn.network.netrom;

import java.util.concurrent.atomic.AtomicInteger;

public class NetRomCircuit {

  private final int circuitId;

  private State state;

  private byte remoteCircuitId;

  private byte remoteCircuitIdx;

  private int windowSize;

  private final AtomicInteger vs = new AtomicInteger(0);

  private final AtomicInteger vr = new AtomicInteger(0);

  public NetRomCircuit(int circuitId) {
    this.circuitId = circuitId;
    this.state = State.DISCONNECTED;
  }

  public int getCircuitId() {
    return circuitId;
  }

  public byte getCircuitIdByte() {
    return (byte)(circuitId & 0xff);
  }

  public State getState() {
    return state;
  }

  public void setState(State state) {
    this.state = state;
  }


  public int getSendStateSeq() {
    return vs.get();
  }

  public byte getSendStateSeqByte() {
    return (byte)((vs.get() % 128) & 0xff);
  }

  public void incrementSendState() {
    vs.getAndIncrement();
  }


  public int getRecvStateSeq() {
    return vr.get();
  }

  public byte getRecvStateSeqByte() {
    return (byte)((vr.get() % 128) & 0xff);
  }

  public void incrementRecvState() {
    vr.getAndIncrement();
  }

  public byte getRemoteCircuitId() {
    return remoteCircuitId;
  }

  public void setRemoteCircuitId(byte remoteCircuitId) {
    this.remoteCircuitId = remoteCircuitId;
  }

  public byte getRemoteCircuitIdx() {
    return remoteCircuitIdx;
  }

  public void setRemoteCircuitIdx(byte remoteCircuitIdx) {
    this.remoteCircuitIdx = remoteCircuitIdx;
  }

  public int getWindowSize() {
    return windowSize;
  }

  public void setWindowSize(int windowSize) {
    this.windowSize = windowSize;
  }

  @Override
  public String toString() {
    return "NetRomState(" + circuitId + "){" +
        "state=" + state +
        ", V(s)=" + vs.get() +
        ", V(r)=" + vr.get() +
        ", cId=" + (remoteCircuitId & 0xff) +
        ", cIdx=" + (remoteCircuitIdx & 0xff) +
        ", k=" + windowSize +
        '}';
  }

  public enum State {
    DISCONNECTED,
    AWAITING_CONNECTION,
    CONNECTED,
    AWAITING_RELEASE;
  }
}
