package net.tarpn.network.netrom;

import java.nio.ByteBuffer;
import net.tarpn.util.Util;
import net.tarpn.packet.impl.ax25.AX25Call;

public class NetRomConnectRequest extends BaseNetRomPacket {

  private final byte proposedWindowSize;
  private final AX25Call originatingUser;
  private final AX25Call originatingNode;

  NetRomConnectRequest(
      byte[] packet,
      AX25Call originNode,
      AX25Call destNode,
      byte ttl,
      byte circuitIndex,
      byte circuitId,
      byte txSeqNumber,
      byte rxSeqNumber,
      byte opCode,
      byte proposedWindowSize,
      AX25Call originatingUser,
      AX25Call originatingNode) {
    super(packet, originNode, destNode, ttl, circuitIndex, circuitId, txSeqNumber, rxSeqNumber, opCode);
    this.proposedWindowSize = proposedWindowSize;
    this.originatingUser = originatingUser;
    this.originatingNode = originatingNode;
  }

  public static NetRomConnectRequest create(
      AX25Call originNode,
      AX25Call destNode,
      byte ttl,
      byte circuitIndex,
      byte circuitId,
      byte txSeqNumber,
      byte rxSeqNumber,
      byte proposedWindowSize,
      AX25Call originatingUser,
      AX25Call originatingNode) {
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    byte opByte = OpType.ConnectRequest.asByte(false, false, false);
    writeHeaders(buffer::put, originNode, destNode, ttl, circuitIndex, circuitId, txSeqNumber,
        rxSeqNumber, opByte);
    buffer.put(proposedWindowSize);
    originatingUser.write(buffer::put);
    originatingNode.write(buffer::put);
    byte[] packet = Util.copyFromBuffer(buffer);
    return new NetRomConnectRequest(packet, originNode, destNode, ttl, circuitIndex, circuitId,
        txSeqNumber, rxSeqNumber, opByte,
        proposedWindowSize, originatingUser, originatingNode
    );
  }

  public byte getProposedWindowSize() {
    return proposedWindowSize;
  }

  public AX25Call getOriginatingUser() {
    return originatingUser;
  }

  public AX25Call getOriginatingNode() {
    return originatingNode;
  }

  @Override
  public String toString() {
    return "NetRom{" +
        "op=" + getOpType() +
        ", origin=" + getOriginNode() +
        ", dest=" + getDestNode() +
        ", originUser=" + getOriginatingUser() +
        ", originNode=" + getOriginatingNode() +
        ", ttl=" + getTTL() +
        ", idx=" + getCircuitIndex() +
        ", id=" + getCircuitId() +
        ", tx=" + getTxSeqNumber() +
        ", rx=" + getRxSeqNumber() +
        '}';
  }
}
