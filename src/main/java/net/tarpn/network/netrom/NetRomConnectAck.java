package net.tarpn.network.netrom;

import java.nio.ByteBuffer;
import net.tarpn.Util;
import net.tarpn.packet.impl.ax25.AX25Call;

public class NetRomConnectAck extends BaseNetRomPacket {

  private final byte acceptWindowSize;

  NetRomConnectAck(
      byte[] packet,
      AX25Call originNode,
      AX25Call destNode,
      byte ttl,
      byte circuitIndex,
      byte circuitId,
      byte txSeqNumber,
      byte rxSeqNumber,
      byte opCode,
      byte acceptWindowSize) {
    super(packet, originNode, destNode, ttl, circuitIndex, circuitId, txSeqNumber, rxSeqNumber, opCode);
    this.acceptWindowSize = acceptWindowSize;
  }

  public static NetRomConnectAck create(
      AX25Call originNode,
      AX25Call destNode,
      byte ttl,
      byte circuitIndex,
      byte circuitId,
      byte myCircuitIdx,
      byte myCircuitId,
      byte acceptWindowSize) {
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    byte opByte = OpType.ConnectAcknowledge.asByte(false, false, false);
    writeHeaders(buffer::put, originNode, destNode, ttl, circuitIndex, circuitId, myCircuitIdx,
        myCircuitId, opByte);
    buffer.put(acceptWindowSize);
    byte[] packet = Util.copyFromBuffer(buffer);
    return new NetRomConnectAck(packet, originNode, destNode, ttl, circuitIndex, circuitId,
        myCircuitIdx, myCircuitId, opByte, acceptWindowSize
    );
  }

  public byte getAcceptWindowSize() {
    return acceptWindowSize;
  }
}
