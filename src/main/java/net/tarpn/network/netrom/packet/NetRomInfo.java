package net.tarpn.network.netrom.packet;

import java.nio.ByteBuffer;
import net.tarpn.util.Util;
import net.tarpn.packet.impl.ax25.AX25Call;

public class NetRomInfo extends BaseNetRomPacket {

  private final byte[] info;

  NetRomInfo(
      byte[] packet,
      AX25Call originNode,
      AX25Call destNode,
      byte ttl,
      byte circuitIndex,
      byte circuitId,
      byte txSeqNumber,
      byte rxSeqNumber,
      byte opCode,
      byte[] info) {
    super(packet, originNode, destNode, ttl, circuitIndex, circuitId, txSeqNumber, rxSeqNumber, opCode);
    this.info = info;
  }

  public static NetRomInfo create(
      AX25Call originNode,
      AX25Call destNode,
      byte ttl,
      byte circuitIndex,
      byte circuitId,
      byte txSeqNumber,
      byte rxSeqNumber,
      byte[] info) {
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    byte opByte = OpType.Information.asByte(false, false, false);
    writeHeaders(buffer::put, originNode, destNode, ttl, circuitIndex, circuitId, txSeqNumber, rxSeqNumber, opByte);
    buffer.put(info);
    byte[] packet = Util.copyFromBuffer(buffer);
    return new NetRomInfo(packet, originNode, destNode, ttl, circuitIndex, circuitId,
        txSeqNumber, rxSeqNumber, opByte, info
    );
  }

  public byte[] getInfo() {
    return info;
  }

  @Override
  public String toString() {
    return "NetRom{" +
        "op=" + getOpType() +
        ", origin=" + getOriginNode() +
        ", dest=" + getDestNode() +
        ", info=" + Util.toEscapedASCII(getInfo()) +
        ", ttl=" + getTTL() +
        ", idx=" + getCircuitIndex() +
        ", id=" + getCircuitId() +
        ", tx=" + getTxSeqNumber() +
        ", rx=" + getRxSeqNumber() +
        '}';
  }
}
