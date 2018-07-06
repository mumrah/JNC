package net.tarpn.packet.impl.netrom;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Command;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;
import net.tarpn.packet.impl.ax25.IFrame;
import net.tarpn.packet.impl.netrom.NetRomPacket.OpType;

public class NetRomHandler {

  public void onPacket(AX25Packet packet, Consumer<AX25Packet> outgoing) {
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

        // TODO for now, just have this here for testing
        if (netRomPacket.getOpType().equals(OpType.ConnectRequest)) {
          NetRomConnectRequest netromReq = (NetRomConnectRequest) netRomPacket;
          NetRomConnectAck netromResp = NetRomConnectAck.create(
              netromReq.getDestNode(),
              netromReq.getOriginNode(),
              (byte) 0x07,
              netromReq.getCircuitIndex(),
              netromReq.getCircuitId(),
              (byte) 0x64, // my circuit idx (100)
              (byte) 0x02, // my circuit id  (2)
              netromReq.getProposedWindowSize()
          );

          IFrame resp = IFrame.create(infoFrame.getSourceCall(), infoFrame.getDestCall(),
              Command.COMMAND, (byte) 0, (byte) 0, true, Protocol.NETROM, netromResp.getPayload());
          outgoing.accept(resp);
        }

        if (netRomPacket.getOpType().equals(OpType.Information)) {
          NetRomInfo netromReq = (NetRomInfo) netRomPacket;
          NetRomPacket netromResp = BaseNetRomPacket.createInfoAck(
              netromReq.getDestNode(),
              netromReq.getOriginNode(),
              (byte) 0x07,
              netromReq.getCircuitIndex(),
              netromReq.getCircuitId(),
              (byte)(netromReq.getTxSeqNumber() + 1)
          );

          IFrame resp = IFrame.create(infoFrame.getSourceCall(), infoFrame.getDestCall(),
              Command.COMMAND, (byte) 0, (byte) 0, true, Protocol.NETROM, netromResp.getPayload());
          outgoing.accept(resp);
        }
      }
    }
  }
}
