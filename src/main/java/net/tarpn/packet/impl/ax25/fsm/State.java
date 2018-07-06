package net.tarpn.packet.impl.ax25.fsm;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.tarpn.packet.impl.ax25.AX25Call;

public class State {
  public static final int T1_TIMEOUT_MS = 4000;
  public static final int T3_TIMEOUT_MS = 180000; // 3 minutes


  private final String sessionId;
  private final AX25Call remoteNodeCall;

  private StateType currentState;

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

  private boolean ackPending = false;

  public State(String sessionId, AX25Call remoteNodeCall, Consumer<StateEvent> stateEventConsumer) {
    this.sessionId = sessionId;
    this.remoteNodeCall = remoteNodeCall;
    this.currentState = StateType.DISCONNECTED;
    this.t1Timer = Timer.create(T1_TIMEOUT_MS, () -> {
      System.err.println("T1 expired");
      stateEventConsumer.accept(StateEvent.createT1ExpireEvent(remoteNodeCall));
    });

    this.t3Timer = Timer.create(T3_TIMEOUT_MS, () -> {
      System.err.println("T3 expired");
      stateEventConsumer.accept(StateEvent.createT3ExpireEvent(remoteNodeCall));
    });
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

  public Timer getT1Timer() {
    return t1Timer;
  }

  public Timer getT3Timer() {
    return t3Timer;
  }

  public void resetRC() {
    RC = 0;
  }

  public boolean checkAndIncrementRC() {
    return RC++ < 4;
  }

  public String getSessionId() {
    return sessionId;
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


  public int getReceiveState() {
    return vr.get() % 8;
  }

  public void incrementReceiveState() {
    this.vr.incrementAndGet();
  }


  public int getAcknowledgeState() {
    return va.get();
  }

  public void setAcknowledgeState(int value) {
    va.set(value);
  }

  public void reset() {
    vs.set(0);
    vr.set(0);
    va.set(0);
    RC = 0;
    t1Timer.cancel();
    t3Timer.cancel();
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
}
