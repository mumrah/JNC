package net.tarpn.packet.impl.ax25;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.tarpn.datalink.LinkPrimitive;
import net.tarpn.packet.impl.ax25.AX25Packet.HasInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AX25State {

  private static final Logger LOG = LoggerFactory.getLogger(AX25State.class);

  public static final AX25State NO_STATE = new AX25State(
      "N0CALL-0",
      AX25Call.create("N0CALL", 0),
      AX25Call.create("N0CALL", 0),
      event -> {},
      event -> {});

  public static final int T1_TIMEOUT_MS = 4000;

  public static final int T3_TIMEOUT_MS = 180000; // 3 minutes

  public static final int SRT_MS = 1000;

  private final String sessionId;

  private final AX25Call remoteNodeCall;

  private final AX25Call localNodeCall;

  private State currentState;

  private final Queue<HasInfo> pendingInfoFrames;

  private final Consumer<AX25StateEvent> internalEvents;

  private final Consumer<LinkPrimitive> outgoingEvents;

  /**
   * Send state variable
   */
  private final AtomicInteger vs = new AtomicInteger(0);

  /**
   * Receive state variable
   */
  private final AtomicInteger vr = new AtomicInteger(0);

  /**
   * Acknowledge state variable
   */
  private final AtomicInteger va = new AtomicInteger(0);

  private final Timer t1Timer;

  private final Timer t3Timer;

  /**
   * Retry count
   */
  private int RC = 0;

  /**
   * Smoothed round trip time
   */
  private int SRT = SRT_MS;

  private boolean ackPending = false;

  private boolean rejectException = false;

  public AX25State(
      String sessionId,
      AX25Call remoteNodeCall,
      AX25Call localNodeCall,
      Consumer<AX25StateEvent> stateEventConsumer,
      Consumer<LinkPrimitive> outgoingEvents) {
    this.sessionId = sessionId;
    this.remoteNodeCall = remoteNodeCall;
    this.localNodeCall = localNodeCall;
    this.internalEvents = stateEventConsumer;
    this.outgoingEvents = outgoingEvents;
    this.currentState = State.DISCONNECTED;
    this.pendingInfoFrames = new LinkedList<>();
    this.t1Timer = Timer.create(T1_TIMEOUT_MS, () -> {
      LOG.debug("T1 expired for " + this);
      this.internalEvents.accept(AX25StateEvent.createT1ExpireEvent(remoteNodeCall));
    });

    this.t3Timer = Timer.create(T3_TIMEOUT_MS, () -> {
      LOG.debug("T3 expired for " + this);
      this.internalEvents.accept(AX25StateEvent.createT3ExpireEvent(remoteNodeCall));
    });
  }

  public void pushIFrame(HasInfo iFrameData) {
    pendingInfoFrames.add(iFrameData);
    internalEvents.accept(AX25StateEvent.createIFrameQueueEvent(remoteNodeCall));
  }

  public HasInfo popIFrame() {
    return pendingInfoFrames.poll();
  }

  public void clearIFrames() {
    pendingInfoFrames.clear();
  }

  public void sendDataLinkPrimitive(LinkPrimitive event) {
    outgoingEvents.accept(event);
  }

  public void internalDisconnectRequest() {
    AX25StateEvent stateEvent = AX25StateEvent.createDisconnectEvent(remoteNodeCall);
    internalEvents.accept(stateEvent);
  }

  public void clearAckPending() {
    ackPending = false;
  }

  public boolean isAckPending() {
    return ackPending;
  }

  public AX25Call getRemoteNodeCall() {
    return remoteNodeCall;
  }

  public AX25Call getLocalNodeCall() {
    return localNodeCall;
  }

  public Timer getT1Timer() {
    return t1Timer;
  }

  public Timer getT3Timer() {
    return t3Timer;
  }


  public void resetRC() {
    RC = 0;
  }

  public void incrementRC() {
    RC++;
  }

  public int getRC() {
    return RC;
  }

  public int getSRT() {
    return SRT;
  }

  public void setSRT(int srt) {
    this.SRT = srt;
  }

  public String getSessionId() {
    return sessionId;
  }

  public void setState(State newState) {
    currentState = newState;
  }

  public State getState() {
    return currentState;
  }



  public int getSendState() {
    return vs.get();
  }

  public Byte getSendStateByte() {
    return (byte)(vs.get() % 8);
  }

  public void incrementSendState() {
    vs.getAndIncrement();
  }



  public int getReceiveState() {
    return vr.get();
  }

  public Byte getReceiveStateByte() {
    return (byte)(vr.get() % 8);
  }

  public void incrementReceiveState() {
    this.vr.incrementAndGet();
  }



  public int getAcknowledgeState() {
    return va.get();
  }

  public byte getAcknowledgeStateByte() {
    return (byte)(va.get() & 0xff);
  }

  public void setAcknowledgeState(byte va) {
    this.va.set(va & 0xff);
  }

  public boolean windowExceeded() {
    return (vs.get() % 8) == ((va.get() + 7) % 8);
  }

  public boolean checkSendEqAckSeq() {
    return (vs.get() % 8) == va.get();
  }


  public boolean isRejectException() {
    return rejectException;
  }

  public void setRejectException() {
    rejectException = true;
  }

  public void clearRejectException() {
    rejectException = false;
  }



  public void clearExceptions() {
    rejectException = false;
    ackPending = false;
  }

  public void reset() {
    // TODO XXX do we always reset these counters _and_ the timers at the same time?
    vs.set(0);
    vr.set(0);
    va.set(0);
    RC = 0;
    t1Timer.cancel();
    t3Timer.cancel();
  }


  @Override
  public String toString() {
    return "AX25State(" + sessionId + "){" +
        "state=" + currentState +
        ", V(s)=" + getSendState() +
        ", N(s)=" + (getSendStateByte() & 0xff) +
        ", V(r)=" + getReceiveState() +
        ", N(r)=" + (getReceiveStateByte() & 0xff) +
        ", V(a)=" + getAcknowledgeState() +
        ", SRT=" + getSRT() +
        ", T1=" + getT1Timer().timeRemaining() + "/" + getT1Timer().getTimeout() +
        ", T3=" + getT3Timer().timeRemaining() + "/" + getT3Timer().getTimeout() +
        '}';
  }

  public static class Timer {
    private static final ScheduledExecutorService TIMER_EXECUTOR = Executors.newSingleThreadScheduledExecutor();

    private final Runnable callback;
    private long timeout;
    private ScheduledFuture<?> future;
    private long startedAt;

    private Timer(long timeout, Runnable callback) {
      this.timeout = timeout;
      this.callback = callback;
    }

    public static Timer create(long timeout, Runnable callback) {
      return new Timer(timeout, callback);
    }

    public void start() {
      if(future != null) {
        future.cancel(false);
      }
      future = TIMER_EXECUTOR.schedule(callback, timeout, TimeUnit.MILLISECONDS);
      startedAt = System.currentTimeMillis();
    }

    public long getTimeout() {
      return timeout;
    }

    public void setTimeout(long newTimeout) {
      this.timeout = newTimeout;
    }

    public void cancel() {
      if(future != null) {
        future.cancel(false);
      }
    }

    public boolean isRunning() {
      return future != null && !(future.isDone() || future.isCancelled());
    }

    public long timeRemaining() {
      if(isRunning()) {
        return timeout - (System.currentTimeMillis() - startedAt);
      } else {
        return -1;
      }
    }

    @Override
    public String toString() {
      return "Timer{" +
          "timeout=" + timeout +
          ", running=" + isRunning() +
          ", started=" + startedAt +
          ", remaining=" + timeRemaining() +
          '}';
    }
  }

  public enum State {
    DISCONNECTED,
    AWAITING_CONNECTION,
    CONNECTED,
    TIMER_RECOVERY,
    AWAITING_RELEASE;
  }
}
