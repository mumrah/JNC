package net.tarpn.network.netrom;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;
import net.tarpn.config.Configuration;
import net.tarpn.network.netrom.NetRomCircuit.State;
import net.tarpn.network.netrom.NetRomPacket.OpType;
import net.tarpn.network.netrom.handlers.AwaitingConnectionStateHandler;
import net.tarpn.network.netrom.handlers.AwaitingReleaseStateHandler;
import net.tarpn.network.netrom.handlers.ConnectedStateHandler;
import net.tarpn.network.netrom.handlers.DisconnectedStateHandler;
import net.tarpn.network.netrom.handlers.StateHandler;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;
import net.tarpn.packet.impl.ax25.IFrame;

public class NetRomCircuitManager {

  private final Configuration config;

  private final Map<Integer, NetRomCircuit> circuits = new ConcurrentHashMap<>();

  private final Map<State, StateHandler> stateHandlers = new HashMap<>();

  public NetRomCircuitManager(Configuration config) {
    this.config = config;
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

  public void onPacket(AX25Packet packet, Consumer<NetRomPacket> outgoing) {
    if(packet instanceof IFrame) {
      IFrame infoFrame = (IFrame) packet;
      if (infoFrame.getProtocol().equals(Protocol.NETROM)) {
        ByteBuffer infoBuffer = ByteBuffer.wrap(infoFrame.getInfo());
        AX25Call originNode = AX25Call.read(infoBuffer);
        AX25Call destNode = AX25Call.read(infoBuffer);

        byte ttl = infoBuffer.get();
        byte circuitIdx = infoBuffer.get();
        byte circuitId = infoBuffer.get();
        byte txSeqNum = infoBuffer.get();
        byte rxSeqNum = infoBuffer.get();
        byte opcode = infoBuffer.get();
        OpType opType = OpType.fromOpCodeByte(opcode);
        final NetRomPacket netRomPacket;
        switch (opType) {
          case ConnectRequest:
            byte proposeWindowSize = infoBuffer.get();
            AX25Call originatingUser = AX25Call.read(infoBuffer);
            AX25Call originatingNode = AX25Call.read(infoBuffer);
            netRomPacket = NetRomConnectRequest.create(originNode, destNode, ttl,
                circuitIdx, circuitId,
                txSeqNum, rxSeqNum,
                proposeWindowSize, originatingUser, originatingNode);
            break;
          case ConnectAcknowledge:
            byte acceptWindowSize = infoBuffer.get();
            netRomPacket = NetRomConnectAck.create(originNode, destNode, ttl,
                circuitIdx, circuitId,
                txSeqNum, rxSeqNum,
                acceptWindowSize);
            break;
          case Information:
            int len = infoBuffer.remaining();
            byte[] info = new byte[len];
            infoBuffer.get(info);
            netRomPacket = NetRomInfo.create(originNode, destNode, ttl,
                circuitIdx, circuitId,
                txSeqNum, rxSeqNum, info);
            break;
          case DisconnectRequest:
            netRomPacket = BaseNetRomPacket.createDisconnectRequest(originNode, destNode, ttl,
                circuitIdx, circuitId);
            break;
          case DisconnectAcknowledge:
            netRomPacket = BaseNetRomPacket.createDisconnectAck(originNode, destNode, ttl,
                circuitIdx, circuitId);
            break;
          case InformationAcknowledge:
            netRomPacket = BaseNetRomPacket.createInfoAck(originNode, destNode, ttl,
                circuitIdx, circuitId, rxSeqNum);
            break;
          default:
            throw new IllegalStateException("Cannot get here");
        }

        System.err.println(netRomPacket);

        // Ignore KEEPLI-0, some INP3 thing

        if(!netRomPacket.getDestNode().equals(config.getNodeCall())) {
          // forward it
          outgoing.accept(netRomPacket);
          return;
        } else {
          // handle it
          int theCircuitId;
          if(netRomPacket.getOpType().equals(OpType.ConnectRequest)) {
            // generate new circuit id
            theCircuitId = getNextCircuitId();
            // TODO handle -1
          } else {
            theCircuitId = circuitId;
          }
          NetRomCircuit circuit = circuits.computeIfAbsent(theCircuitId, NetRomCircuit::new);
          StateHandler handler = stateHandlers.get(circuit.getState());
          if(handler != null) {
            System.err.println("NETROM BEFORE: " + circuit + " got " + netRomPacket);
            State newState = handler.handle(circuit, netRomPacket, outgoing);
            circuit.setState(newState);
            System.err.println("NETROM AFTER : " + circuit);
          }
        }
      }
    }
  }
}