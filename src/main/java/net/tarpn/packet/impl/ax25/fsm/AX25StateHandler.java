package net.tarpn.packet.impl.ax25.fsm;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import net.tarpn.packet.PacketHandler;
import net.tarpn.packet.PacketRequest;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.SFrame;
import net.tarpn.packet.impl.ax25.UFrame;
import net.tarpn.packet.impl.ax25.fsm.StateEvent.Type;

public class AX25StateHandler implements PacketHandler {

  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final Map<AX25Call, State> sessions = new ConcurrentHashMap<>();
  private final Map<StateType, StateHandler> handlers = new HashMap<>();
  private final Queue<StateEvent> eventQueue = new ConcurrentLinkedQueue<>();
  private final Consumer<AX25Packet> outgoingPackets;

  public AX25StateHandler(Consumer<AX25Packet> outgoingPackets) {
    this.outgoingPackets = outgoingPackets;

    handlers.put(StateType.DISCONNECTED, new DisconnectedStateHandler());
    handlers.put(StateType.CONNECTED, new ConnectedStateHandler());
  }

  @Override
  public void onPacket(PacketRequest packet) {
    AX25Packet ax25Packet = (AX25Packet)packet.getPacket();
    final StateEvent event;
    switch (ax25Packet.getFrameType()) {
      case I: {
        // TODO
        event = new StateEvent(ax25Packet, Type.AX25_INFO);
        break;
      } case S: {
        switch (((SFrame) ax25Packet).getControlType()) {
          case RR:
            event = new StateEvent(ax25Packet, Type.AX25_RR);
            break;
          case RNR:
          case REJ:
          default:
            // TODO
            event = new StateEvent(ax25Packet, Type.AX25_UNKNOWN);
            break;
        }
        break;
      } case U: {
        switch (((UFrame) ax25Packet).getControlType()) {
          case SABM: {
            event = new StateEvent(ax25Packet, Type.AX25_SABM);
            break;
          }
          case DISC: {
            event = new StateEvent(ax25Packet, Type.AX25_DISC);
            break;
          }
          case DM: {
            event = new StateEvent(ax25Packet, Type.AX25_DM);
            break;
          }
          case UA: {
            event = new StateEvent(ax25Packet, Type.AX25_UA);
            break;
          }
          case FRMR:
          default: {
            event = new StateEvent(ax25Packet, Type.AX25_UNKNOWN);
            break;
          }
        }
        break;
      } case UI: {
        event = new StateEvent(ax25Packet, Type.AX25_UI);
        break;
      } default: {
        event = new StateEvent(ax25Packet, Type.AX25_UNKNOWN);
        break;
      }
    }
    eventQueue.add(event);
  }

  public void start() {
    Future<?> future = executor.submit(() -> {
      while(true) {
        StateEvent event = eventQueue.poll();
        try {
          if (event != null) {
            AX25Packet packet = event.getPacket();
            State state = sessions.computeIfAbsent(packet.getSourceCall(),
                ax25Call -> new State(packet.getSourceCall(), packet.getDestCall()));
            StateHandler handler = handlers.get(state.getState());
            handler.onEvent(state, event, outgoingPackets::accept);
          } else {
            Thread.sleep(50);
          }
        } catch (Throwable t) {
          t.printStackTrace();
        }
      }
    });
  }
}
