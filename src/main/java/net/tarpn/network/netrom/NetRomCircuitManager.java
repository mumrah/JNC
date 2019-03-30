package net.tarpn.network.netrom;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import net.tarpn.config.NetRomConfig;
import net.tarpn.datalink.LinkPrimitive;
import net.tarpn.network.netrom.NetRomCircuit.State;
import net.tarpn.network.netrom.NetRomCircuitEvent.DataLinkEvent;
import net.tarpn.network.netrom.NetRomCircuitEvent.Type;
import net.tarpn.network.netrom.NetRomPacket.OpType;
import net.tarpn.network.netrom.handlers.AwaitingConnectionStateHandler;
import net.tarpn.network.netrom.handlers.AwaitingReleaseStateHandler;
import net.tarpn.network.netrom.handlers.ConnectedStateHandler;
import net.tarpn.network.netrom.handlers.DisconnectedStateHandler;
import net.tarpn.network.netrom.handlers.StateHandler;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet.HasInfo;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetRomCircuitManager {

  private static final Logger LOG = LoggerFactory.getLogger(NetRomCircuitManager.class);

  private final NetRomConfig config;

  private final Map<Integer, NetRomCircuit> circuits = new ConcurrentHashMap<>();

  private final Map<Integer, BlockingQueue<LinkPrimitive>> circuitBuffers = new ConcurrentHashMap<>();

  private final Map<State, StateHandler> stateHandlers = new HashMap<>();

  private final NetRomRouter outgoingNetRomPackets;

  private final Consumer<LinkPrimitive> networkEvents;

  public NetRomCircuitManager(
      NetRomConfig config,
      NetRomRouter outgoingNetRomPackets,
      Consumer<LinkPrimitive> networkEvents) {
    this.config = config;
    this.outgoingNetRomPackets = outgoingNetRomPackets;
    this.networkEvents = networkEvents;
    this.stateHandlers.put(State.AWAITING_CONNECTION, new AwaitingConnectionStateHandler());
    this.stateHandlers.put(State.CONNECTED, new ConnectedStateHandler());
    this.stateHandlers.put(State.AWAITING_RELEASE, new AwaitingReleaseStateHandler());
    this.stateHandlers.put(State.DISCONNECTED, new DisconnectedStateHandler());
  }

  public int getNextCircuitId() {
    OptionalInt nextFreeId = IntStream.range(0, 255)
        .filter(((IntPredicate)circuits::containsKey).negate())
        .findFirst();
    return nextFreeId.orElse(-1);
  }

  /**
   * Handle a AX.25 packet with PID=NETROM, convert it to a {@link NetRomCircuitEvent} and
   * pass it to the {@link NetRomCircuit}.
   */
  public void handleInfo(HasInfo linkInfo) {
    if (linkInfo.getProtocol().equals(Protocol.NETROM)) {
      ByteBuffer infoBuffer = ByteBuffer.wrap(linkInfo.getInfo());
      AX25Call originNode = AX25Call.read(infoBuffer);
      AX25Call destNode = AX25Call.read(infoBuffer);

      byte ttl = infoBuffer.get();
      byte circuitIdx = infoBuffer.get();
      byte circuitId = infoBuffer.get();
      byte txSeqNum = infoBuffer.get();
      byte rxSeqNum = infoBuffer.get();
      byte opcode = infoBuffer.get();
      OpType opType = OpType.fromOpCodeByte(opcode);
      boolean choke = (opcode & 0x80) == 0x80;
      boolean nak = (opcode & 0x40) == 0x40;
      boolean moreFollows = (opcode & 0x20) == 0x20;

      final DataLinkEvent event;
      switch (opType) {
        case ConnectRequest: {
          byte proposeWindowSize = infoBuffer.get();
          AX25Call originatingUser = AX25Call.read(infoBuffer);
          AX25Call originatingNode = AX25Call.read(infoBuffer);
          NetRomPacket netRomPacket = NetRomConnectRequest.create(originNode, destNode, ttl,
              circuitIdx, circuitId, proposeWindowSize, originatingUser, originatingNode);
          int newCircuitId = getNextCircuitId();
          event = new DataLinkEvent(newCircuitId, originNode, netRomPacket, Type.NETROM_CONNECT);
          break;
        }
        case ConnectAcknowledge: {
          byte acceptWindowSize = infoBuffer.get();
          NetRomPacket netRomPacket = NetRomConnectAck.create(originNode, destNode, ttl,
              circuitIdx, circuitId,
              txSeqNum, rxSeqNum,
              acceptWindowSize,
              OpType.ConnectAcknowledge.asByte(choke, nak, moreFollows));
          event = new DataLinkEvent(circuitId, originNode, netRomPacket, Type.NETROM_CONNECT_ACK);
          break;
        }
        case Information: {
          int len = infoBuffer.remaining();
          byte[] l3Info = new byte[len];
          infoBuffer.get(l3Info);
          NetRomPacket netRomPacket = NetRomInfo.create(originNode, destNode, ttl,
              circuitIdx, circuitId,
              txSeqNum, rxSeqNum, l3Info);
          event = new DataLinkEvent(circuitId, originNode, netRomPacket, Type.NETROM_INFO);
          break;
        }
        case InformationAcknowledge: {
          NetRomPacket netRomPacket = BaseNetRomPacket.createInfoAck(
              originNode,
              destNode,
              ttl,
              circuitIdx,
              circuitId,
              rxSeqNum,
              OpType.InformationAcknowledge.asByte(false, false, false));
          event = new DataLinkEvent(circuitId, originNode ,netRomPacket, Type.NETROM_INFO_ACK);
          break;
        }
        case DisconnectRequest: {
          NetRomPacket netRomPacket = BaseNetRomPacket.createDisconnectRequest(originNode, destNode, ttl,
              circuitIdx, circuitId);
          event = new DataLinkEvent(circuitId, originNode, netRomPacket, Type.NETROM_DISCONNECT);

          break;
        }
        case DisconnectAcknowledge: {
          NetRomPacket netRomPacket = BaseNetRomPacket.createDisconnectAck(originNode, destNode, ttl,
              circuitIdx, circuitId);
          event = new DataLinkEvent(circuitId, originNode, netRomPacket, Type.NETROM_DISCONNECT_ACK);
          break;
        }
        default:
          throw new IllegalStateException("Cannot get here");
      }

      LOG.info(event.toString());
      // Ignore KEEPLI-0, some INP3 thing

      if(!event.getNetRomPacket().getDestNode().equals(config.getNodeCall())) {
        // forward it
        // TODO change this to accept events?
        boolean routed = outgoingNetRomPackets.route(event.getNetRomPacket());
        if(!routed) {
          // TODO what now? emit a no-route-to-host event?
        }
      } else {
        // handle it
        onCircuitEvent(event);
      }
    }
  }

  public int open(AX25Call remoteNode) {
    int circuitId = getNextCircuitId();
    if(circuits.containsKey(circuitId)) {
      // All circuits busy
      return -1;
    } else {
      circuits.put(circuitId, new NetRomCircuit(circuitId, remoteNode, config.getNodeCall(), config));
      return circuitId;
    }
  }

  public BlockingQueue<LinkPrimitive> getCircuitBuffer(int circuitId) {
    return circuitBuffers.computeIfAbsent(
        circuitId, newCircuitId -> new LinkedBlockingQueue<>(1024));
  }

  /**
   * Dispatch a {@link NetRomCircuitEvent} to the appropriate {@link NetRomCircuit}
   * @param event
   */
  public void onCircuitEvent(NetRomCircuitEvent event) {
    NetRomCircuit circuit = circuits.computeIfAbsent(event.getCircuitId(), newCircuitId ->
        new NetRomCircuit(newCircuitId, event.getRemoteCall(), config.getNodeCall(), config)
    );
    StateHandler handler = stateHandlers.get(circuit.getState());
    if(handler != null) {
      LOG.debug("BEFORE: " + circuit + " got " + event);
      Consumer<LinkPrimitive> eventHandler = circuitEvent ->
          getCircuitBuffer(circuit.getCircuitId()).add(circuitEvent);
      State newState = handler.handle(circuit, event, eventHandler, outgoingNetRomPackets);
      circuit.setState(newState);
      LOG.debug("AFTER : " + circuit);
    } else {
      LOG.error("No handler found for state " + circuit.getState());
    }
  }
}
