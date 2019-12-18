package net.tarpn.packet.impl.ax25;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.util.Clock;
import net.tarpn.util.Util;
import net.tarpn.config.PortConfig;
import net.tarpn.packet.PacketHandler;
import net.tarpn.packet.PacketRequest;
import net.tarpn.packet.impl.ax25.AX25State.State;
import net.tarpn.packet.impl.ax25.AX25StateEvent.Type;
import net.tarpn.packet.impl.ax25.handlers.AwaitingConnectionStateHandler;
import net.tarpn.packet.impl.ax25.handlers.AwaitingReleaseStateHandler;
import net.tarpn.packet.impl.ax25.handlers.ConnectedStateHandler;
import net.tarpn.packet.impl.ax25.handlers.DisconnectedStateHandler;
import net.tarpn.packet.impl.ax25.handlers.StateHandler;
import net.tarpn.packet.impl.ax25.handlers.TimerRecoveryStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle incoming AX.25 frames and outgoing data link primitives. Dispatch these messages to the
 * appropriate state handler.
 *
 * This state machine has two interfaces which support inbound and outbound messaging. The first is the packet layer
 * which accepts incoming packets and sends outgoing packets from/to the {@link net.tarpn.io.DataPort}. The second is
 * the data link layer which sends link primitives to/from the {@link net.tarpn.datalink.DataLinkManager}.
 */
public class AX25StateMachine implements PacketHandler {

  private static final Logger LOG = LoggerFactory.getLogger(AX25StateMachine.class);

  /**
   * Map of state handlers for the state machine
   */
  private final Map<State, StateHandler> handlers = new HashMap<>();

  /**
   * One event queue for all AX.25 state machines
   */
  private final Queue<AX25StateEvent> eventQueue = new ConcurrentLinkedQueue<>();

  /**
   * Map of AX.25 state machines, keyed by a "session id" which is the source call
   */
  private final Map<String, AX25State> sessions = new ConcurrentHashMap<>();

  /**
   * Outbound AX.25 packets
   */
  private final Consumer<AX25Packet> outgoingPackets;

  /**
   * Inbound level 3 packets (NET/ROM only for now)
   */
  private final Consumer<DataLinkPrimitive> dataLinkEvents;

  private final PortConfig portConfig;

  private final Clock clock;

  public AX25StateMachine(
      PortConfig portConfig,
      Consumer<AX25Packet> outgoingPackets,
      Consumer<DataLinkPrimitive> dataLinkEvents,
      Clock clock) {
    this.portConfig = portConfig;
    this.outgoingPackets = outgoingPackets;
    this.dataLinkEvents = dataLinkEvents;
    this.clock = clock;
    handlers.put(State.DISCONNECTED, new DisconnectedStateHandler());
    handlers.put(State.CONNECTED, new ConnectedStateHandler());
    handlers.put(State.AWAITING_CONNECTION, new AwaitingConnectionStateHandler());
    handlers.put(State.TIMER_RECOVERY, new TimerRecoveryStateHandler());
    handlers.put(State.AWAITING_RELEASE, new AwaitingReleaseStateHandler());
  }

  /**
   * Translate incoming packets to {@link AX25StateEvent} objects and put them
   * on the queue.
   * @param packet
   */
  @Override
  public void onPacket(PacketRequest packet) {
    AX25Packet ax25Packet = (AX25Packet)packet.getPacket();
    final AX25StateEvent event;
    switch (ax25Packet.getFrameType()) {
      case I: {
        event = AX25StateEvent.createIncomingEvent(ax25Packet, Type.AX25_INFO);
        break;
      } case S: {
        switch (((SFrame) ax25Packet).getControlType()) {
          case RR: {
            event = AX25StateEvent.createIncomingEvent(ax25Packet, Type.AX25_RR);
            break;
          }
          case RNR: {
            event = AX25StateEvent.createIncomingEvent(ax25Packet, Type.AX25_RNR);
            break;
          }
          case REJ: {
            event = AX25StateEvent.createIncomingEvent(ax25Packet, Type.AX25_REJ);
            break;
          }
          default:
            event = AX25StateEvent.createIncomingEvent(ax25Packet, Type.AX25_UNKNOWN);
            break;
        }
        break;
      } case U: {
        switch (((UFrame) ax25Packet).getControlType()) {
          case SABM: {
            event = AX25StateEvent.createIncomingEvent(ax25Packet, Type.AX25_SABM);
            break;
          }
          case DISC: {
            event = AX25StateEvent.createIncomingEvent(ax25Packet, Type.AX25_DISC);
            break;
          }
          case DM: {
            event = AX25StateEvent.createIncomingEvent(ax25Packet, Type.AX25_DM);
            break;
          }
          case UA: {
            event = AX25StateEvent.createIncomingEvent(ax25Packet, Type.AX25_UA);
            break;
          }
          case FRMR: {
            event = AX25StateEvent.createIncomingEvent(ax25Packet, Type.AX25_FRMR);
            break;
          }
          default: {
            event = AX25StateEvent.createIncomingEvent(ax25Packet, Type.AX25_UNKNOWN);
            break;
          }
        }
        break;
      } case UI: {
        event = AX25StateEvent.createIncomingEvent(ax25Packet, Type.AX25_UI);
        break;
      } default: {
        event = AX25StateEvent.createIncomingEvent(ax25Packet, Type.AX25_UNKNOWN);
        break;
      }
    }
    eventQueue.add(event);
  }

  public boolean poll() {
    return Util.queuePoll(eventQueue::poll, event -> {
      String sessionId = event.getRemoteCall().toString();
      // For each incoming event, figure out which state machine should handle it
      AX25State state = sessions.computeIfAbsent(sessionId,
              ax25Call -> new AX25State(
                      sessionId,
                      event.getRemoteCall(),
                      portConfig.getNodeCall(),
                      portConfig,
                      eventQueue::add

              )
      );
      StateHandler handler = handlers.get(state.getState());
      LOG.trace("AX25 BEFORE: " + state + " got " + event);
      State newState = handler.onEvent(state, event, outgoingPackets);
      state.setState(newState);
      LOG.trace("AX25 AFTER : " + state);
    }, (failedEvent, t) -> LOG.error("Error in AX.25 state machine", t));
  }
  public Runnable getRunnable() {
    return () -> {
      while(!Thread.currentThread().isInterrupted()) {
        boolean didPoll = poll();
        if (!didPoll) {
          try {
            clock.sleep(10);
          } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
          }
        }
      }
    };
  }

  public AX25State getState(AX25Call remoteCall) {
    return sessions.getOrDefault(remoteCall.toString(), AX25State.NO_STATE);
  }

  /**
   * Used for external components to add events to be processed by a state machine
   */
  public Queue<AX25StateEvent> getEventQueue() {
    return eventQueue;
  }

  /**
   * Return the current set of session IDs
   */
  public void forEachSession(BiConsumer<String, AX25State> biConsumer) {
    sessions.forEach(biConsumer);
  }
}
