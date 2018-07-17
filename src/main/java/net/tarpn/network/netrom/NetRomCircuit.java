package net.tarpn.network.netrom;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.tarpn.config.NetRomConfig;
import net.tarpn.network.netrom.NetRomPacket.OpType;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.util.Timer;

public class NetRomCircuit {

  private final AX25Call localNodeCall;

  private final AX25Call remoteNodeCall;

  private final int circuitId;

  private final NetRomConfig config;

  private final AtomicInteger vs = new AtomicInteger(0);

  private final AtomicInteger vr = new AtomicInteger(0);

  private State state;

  private byte remoteCircuitId;

  private byte remoteCircuitIdx;

  private int windowSize;

  private Timer ackTimer;

  private Timer t1Timer;

  private boolean ackPending;

  public NetRomCircuit(
      int circuitId,
      AX25Call remoteNodeCall,
      AX25Call localNodeCall,
      NetRomConfig config) {
    this.circuitId = circuitId;
    this.remoteNodeCall = remoteNodeCall;
    this.localNodeCall = localNodeCall;
    this.config = config;
    this.state = State.DISCONNECTED;
  }

  public NetRomConfig getConfig() {
    return config;
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

  public void enqueueInfoAck(Consumer<NetRomPacket> outgoing) {
    if(ackTimer == null) {
      ackTimer = Timer.create(100, () -> {
        if(ackPending) {
          NetRomPacket infoAck = BaseNetRomPacket.createInfoAck(
              getRemoteNodeCall(),
              getLocalNodeCall(),
              getConfig().getTTL(),
              getRemoteCircuitIdx(),
              getRemoteCircuitId(),
              getRecvStateSeqByte(),
              OpType.InformationAcknowledge.asByte(false, true, false)
          );
          outgoing.accept(infoAck);
          ackPending = false;
        }
      });
    }
    ackTimer.start();
    ackPending = true;
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

  public AX25Call getLocalNodeCall() {
    return localNodeCall;
  }

  public AX25Call getRemoteNodeCall() {
    return remoteNodeCall;
  }

  public enum State {
    DISCONNECTED,
    AWAITING_CONNECTION,
    CONNECTED,
    AWAITING_RELEASE;
  }
}
