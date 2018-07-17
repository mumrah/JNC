package net.tarpn.util;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Generic timer that runs a callback when the timer expires. Supports cancelling and restarting.
 */
public class Timer {
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
