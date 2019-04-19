package net.tarpn.packet.impl.ax25;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import net.tarpn.config.PortConfig;
import net.tarpn.config.impl.PortConfigImpl;
import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.packet.impl.ax25.AX25Packet.Command;
import net.tarpn.packet.impl.ax25.AX25Packet.HasInfo;
import net.tarpn.packet.impl.ax25.AX25Packet.SupervisoryFrame;
import net.tarpn.util.Timer;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO document the hell out of this...
public class AX25State {

  private static final Logger LOG = LoggerFactory.getLogger(AX25State.class);

  public static final AX25State NO_STATE = new AX25State(
      "N0CALL-0",
      AX25Call.create("N0CALL", 0),
      AX25Call.create("N0CALL", 0),
      new PortConfigImpl(-1, new PropertiesConfiguration()),
      event -> {},
      event -> {});

  public static final int DEFAULT_T1_TIMEOUT_MS = 4000;

  public static final int DEFAULT_T3_TIMEOUT_MS = 180000; // 3 minutes

  public static final int DEFAULT_RTT_MS = 1000;

  private final String sessionId;

  private final AX25Call remoteNodeCall;

  private final AX25Call localNodeCall;

  private final PortConfig portConfig;

  private State currentState;

  private final Queue<HasInfo> pendingInfoFrames;

  private final Consumer<AX25StateEvent> internalEvents;

  private final Consumer<DataLinkPrimitive> outgoingEvents;

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
   * Retry counter
   */
  private int RC = 0;

  /**
   * Smoothed round trip time
   */
  private int SRT;

  private boolean ackPending = false;

  private boolean rejectException = false;

  public AX25State(
      String sessionId,
      AX25Call remoteNodeCall,
      AX25Call localNodeCall,
      PortConfig portConfig,
      Consumer<AX25StateEvent> stateEventConsumer,
      Consumer<DataLinkPrimitive> outgoingEvents) {
    this.sessionId = sessionId;
    this.remoteNodeCall = remoteNodeCall;
    this.localNodeCall = localNodeCall;
    this.portConfig = portConfig;
    this.internalEvents = stateEventConsumer;
    this.outgoingEvents = outgoingEvents;
    this.currentState = State.DISCONNECTED;
    this.pendingInfoFrames = new LinkedList<>();

    this.t1Timer = Timer.create(portConfig.getInt("l2.retry.timeout", DEFAULT_T1_TIMEOUT_MS), () -> {
      LOG.debug("T1 expired for " + this);
      this.internalEvents.accept(AX25StateEvent.createT1ExpireEvent(remoteNodeCall));
    });

    this.t3Timer = Timer.create(portConfig.getInt("l2.idle.timeout", DEFAULT_T3_TIMEOUT_MS), () -> {
      LOG.debug("T3 expired for " + this);
      this.internalEvents.accept(AX25StateEvent.createT3ExpireEvent(remoteNodeCall));
    });

    this.SRT = portConfig.getInt("l2.rtt", DEFAULT_RTT_MS);
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

  public Collection<HasInfo> peekIFrames() {
    return new ArrayList<>(pendingInfoFrames);
  }
  public void sendDataLinkPrimitive(DataLinkPrimitive event) {
    outgoingEvents.accept(event);
  }

  public void internalDisconnectRequest() {
    AX25StateEvent stateEvent = AX25StateEvent.createDisconnectEvent(remoteNodeCall);
    internalEvents.accept(stateEvent);
  }

  public void clearAckPending() {
    ackPending = false;
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

  private Timer ackTimer = null;

  public void enqueueInfoAck(Consumer<AX25Packet> outgoingPackets) {
    if(ackTimer == null) {
      ackTimer = Timer.create(portConfig.getInt("l2.ack.delay", 30), () -> {
        if(ackPending) {
          SFrame rr = SFrame.create(
              getRemoteNodeCall(),
              getLocalNodeCall(),
              Command.RESPONSE,
              SupervisoryFrame.ControlType.RR,
              getReceiveState(),
              true);
          outgoingPackets.accept(rr);
          clearAckPending();
        }
      });
    }
    ackTimer.start();
    ackPending = true;
  }

  public int getRC() {
    return RC;
  }

  public void resetRC() {
    RC = 0;
  }

  public void incrementRC() {
    RC++;
  }

  // Config read-throughs

  public int getMaxRetryCount() {
    return portConfig.getInt("l2.retries.max", 4);
  }

  public String getWelcomeMessage() {
    return portConfig.getString("l2.connect.message", "");
  }

  public boolean checkRC() {
    return RC < portConfig.getInt("l2.retries.max", 4);
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


  IFrame[] oldFrames = new IFrame[8];

  public void storeSentIFrame(IFrame iFrame) {
    // Guard against array index?
    oldFrames[iFrame.getSendSequenceNumber()] = iFrame;
  }

  public IFrame getSentIFrame(int ns) {
    return oldFrames[ns];
  }

  public Byte getSendStateByte() {
    return (byte)(vs.get() % 8);
  }

  public void incrementSendState() {
    vs.getAndIncrement();
  }

  public void setSendState(int vs) {
    this.vs.set(vs);
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

  public void setReceiveState(int vr) {
    this.vr.set(vr);
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

  /**
   * If V(S) is equal to V(A) + window size (7) means we can't transmit any more until we get an ACK
   * @return
   */
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
            "local=" + localNodeCall +
            ", remote=" + remoteNodeCall +
            ", state=" + currentState +
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

  public enum State {
    DISCONNECTED,
    AWAITING_CONNECTION,
    CONNECTED,
    TIMER_RECOVERY,
    AWAITING_RELEASE;
  }
}
