package net.tarpn.packet.impl.ax25.fsm;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import net.tarpn.packet.PacketHandler;
import net.tarpn.packet.PacketRequest;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.SFrame;
import net.tarpn.packet.impl.ax25.UFrame;
import net.tarpn.packet.impl.ax25.fsm.StateEvent.Type;

/**
 * Handle incoming AX.25 frames and send them to the appropriate state handler.
 */
public class AX25StateHandler implements PacketHandler {

  /**
   * Map of state handlers for the state machine
   */
  private final Map<StateType, StateHandler> handlers = new HashMap<>();

  /**
   * One event queue for all AX.25 state machines
   */
  private final Queue<StateEvent> eventQueue = new ConcurrentLinkedQueue<>();

  /**
   * Map of AX.25 state machines, keyed by a "session id" which is the source call
   */
  private final Map<String, State> sessions = new ConcurrentHashMap<>();

  /**
   * Outbound AX.25 packets
   */
  private final Consumer<AX25Packet> outgoingPackets;

  /**
   * Inbound level 3 packets (NET/ROM only for now)
   */
  private final Consumer<AX25Packet> L3Packets;

  public AX25StateHandler(Consumer<AX25Packet> outgoingPackets, Consumer<AX25Packet> L3Packets) {
    this.outgoingPackets = outgoingPackets;
    this.L3Packets = L3Packets;
    handlers.put(StateType.DISCONNECTED, new DisconnectedStateHandler());
    handlers.put(StateType.CONNECTED, new ConnectedStateHandler());
    // TODO StateType.AWAITING_CONNECTION
  }

  /**
   * Translate incoming packets to {@link StateEvent} objects and put them
   * on the queue.
   * @param packet
   */
  @Override
  public void onPacket(PacketRequest packet) {
    AX25Packet ax25Packet = (AX25Packet)packet.getPacket();
    final StateEvent event;
    switch (ax25Packet.getFrameType()) {
      case I: {
        event = StateEvent.createIncomingEvent(ax25Packet, Type.AX25_INFO);
        break;
      } case S: {
        switch (((SFrame) ax25Packet).getControlType()) {
          case RR: {
            event = StateEvent.createIncomingEvent(ax25Packet, Type.AX25_RR);
            break;
          }
          case RNR: {
            event = StateEvent.createIncomingEvent(ax25Packet, Type.AX25_RNR);
            break;
          }
          case REJ: {
            event = StateEvent.createIncomingEvent(ax25Packet, Type.AX25_REJ);
            break;
          }
          default:
            event = StateEvent.createIncomingEvent(ax25Packet, Type.AX25_UNKNOWN);
            break;
        }
        break;
      } case U: {
        switch (((UFrame) ax25Packet).getControlType()) {
          case SABM: {
            event = StateEvent.createIncomingEvent(ax25Packet, Type.AX25_SABM);
            break;
          }
          case DISC: {
            event = StateEvent.createIncomingEvent(ax25Packet, Type.AX25_DISC);
            break;
          }
          case DM: {
            event = StateEvent.createIncomingEvent(ax25Packet, Type.AX25_DM);
            break;
          }
          case UA: {
            event = StateEvent.createIncomingEvent(ax25Packet, Type.AX25_UA);
            break;
          }
          case FRMR: {
            event = StateEvent.createIncomingEvent(ax25Packet, Type.AX25_FRMR);
            break;
          }
          default: {
            event = StateEvent.createIncomingEvent(ax25Packet, Type.AX25_UNKNOWN);
            break;
          }
        }
        break;
      } case UI: {
        event = StateEvent.createIncomingEvent(ax25Packet, Type.AX25_UI);
        break;
      } default: {
        event = StateEvent.createIncomingEvent(ax25Packet, Type.AX25_UNKNOWN);
        break;
      }
    }
    eventQueue.add(event);
  }

  public Runnable getRunnable() {
    return () -> {
      while(true) {
        StateEvent event = eventQueue.poll();
        try {
          if (event != null) {
            // For each incoming event, figure out which state machine should handle it
            AX25Packet packet = event.getPacket();
            State state = sessions.computeIfAbsent(event.getSessionId(),
                ax25Call -> new State(event.getSessionId(), packet.getSourceCall(), packet.getDestCall()));
            StateHandler handler = handlers.get(state.getState());
            handler.onEvent(state, event, outgoingPackets, L3Packets);
          } else {
            Thread.sleep(50);
          }
        } catch (Throwable t) {
          t.printStackTrace();
        }
      }
    };
  }

  /**
   * Used for external components to add events to be processed by a state machine
   */
  public Queue<StateEvent> getEventQueue() {
    return eventQueue;
  }

  /**
   * Return the current set of session IDs
   */
  public Set<String> getSessionIds() {
    return sessions.keySet();
  }
}
