package net.tarpn.packet.impl.ax25;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.tarpn.util.Util;
import net.tarpn.config.PortConfig;
import net.tarpn.datalink.LinkPrimitive;
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
 * Handle incoming AX.25 frames and send them to the appropriate state handler.
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
  private final Consumer<LinkPrimitive> dataLinkEvents;


  private final PortConfig portConfig;



  public AX25StateMachine(
      PortConfig portConfig,
      Consumer<AX25Packet> outgoingPackets,
      Consumer<LinkPrimitive> dataLinkEvents) {
    this.portConfig = portConfig;
    this.outgoingPackets = outgoingPackets;
    this.dataLinkEvents = dataLinkEvents;
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

  public Runnable getRunnable() {
    return () -> {
      Util.queueProcessingLoop(eventQueue::poll, event -> {
        String sessionId = event.getRemoteCall().toString();
        // For each incoming event, figure out which state machine should handle it
        AX25State state = sessions.computeIfAbsent(sessionId,
            ax25Call -> new AX25State(
                sessionId,
                event.getRemoteCall(),
                portConfig.getNodeCall(),
                portConfig,
                eventQueue::add,
                dataLinkEvents
            )
        );
        StateHandler handler = handlers.get(state.getState());
        LOG.debug("AX25 BEFORE: " + state + " got " + event);
        State newState = handler.onEvent(state, event, outgoingPackets);
        state.setState(newState);
        LOG.debug("AX25 AFTER : " + state);
      }, (failedEvent, t) -> LOG.error("Error in AX.25 state machine", t));
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
