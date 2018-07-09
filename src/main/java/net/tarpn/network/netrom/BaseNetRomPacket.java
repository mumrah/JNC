package net.tarpn.network.netrom;

import java.nio.ByteBuffer;
import net.tarpn.Util;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Call.ByteConsumer;

public class BaseNetRomPacket implements NetRomPacket {

  private final byte[] packet;
  private final AX25Call originNode;
  private final AX25Call destNode;
  private final byte ttl;
  private final byte circuitIndex;
  private final byte circuitId;
  private final byte txSeqNumber;
  private final byte rxSeqNumber;
  private final byte opCode;

  BaseNetRomPacket(byte[] packet, AX25Call originNode, AX25Call destNode, byte ttl,
      byte circuitIndex, byte circuitId, byte txSeqNumber, byte rxSeqNumber, byte opCode) {
    this.packet = packet;
    this.originNode = originNode;
    this.destNode = destNode;
    this.ttl = ttl;
    this.circuitIndex = circuitIndex;
    this.circuitId = circuitId;
    this.txSeqNumber = txSeqNumber;
    this.rxSeqNumber = rxSeqNumber;
    this.opCode = opCode;
  }

  static void writeHeaders(ByteConsumer byteConsumer, AX25Call originNode, AX25Call destNode,
      byte ttl, byte circuitIndex, byte circuitId, byte txSeqNumber, byte rxSeqNumber,
      byte opCode) {
    originNode.write(byteConsumer);
    destNode.write(byteConsumer);
    byteConsumer.accept(ttl);
    byteConsumer.accept(circuitIndex);
    byteConsumer.accept(circuitId);
    byteConsumer.accept(txSeqNumber);
    byteConsumer.accept(rxSeqNumber);
    byteConsumer.accept(opCode);
  }

  public static BaseNetRomPacket createDisconnectAck(AX25Call originNode, AX25Call destNode,
      byte ttl, byte circuitIndex, byte circuitId) {
    byte opByte = OpType.DisconnectAcknowledge.asByte(false, false, false);
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    writeHeaders(buffer::put, originNode, destNode, ttl, circuitIndex, circuitId, (byte)0x00, (byte)0x00, opByte);
    byte[] packet = Util.copyFromBuffer(buffer);
    return new BaseNetRomPacket(
        packet,
        originNode,
        destNode,
        ttl,
        circuitIndex,
        circuitId,
        (byte)0x00,
        (byte)0x00,
        opByte
    );
  }

  public static BaseNetRomPacket createDisconnectRequest(AX25Call originNode, AX25Call destNode,
      byte ttl, byte circuitIndex, byte circuitId) {
    byte opByte = OpType.DisconnectRequest.asByte(false, false, false);
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    writeHeaders(buffer::put, originNode, destNode, ttl, circuitIndex, circuitId, (byte)0x00, (byte)0x00, opByte);
    byte[] packet = Util.copyFromBuffer(buffer);
    return new BaseNetRomPacket(
        packet,
        originNode,
        destNode,
        ttl,
        circuitIndex,
        circuitId,
        (byte)0x00,
        (byte)0x00,
        opByte
    );
  }

  public static BaseNetRomPacket createInfoAck(AX25Call originNode, AX25Call destNode, byte ttl,
      byte circuitIndex, byte circuitId, byte rxSeqNumber) {
    byte opByte = OpType.InformationAcknowledge.asByte(false, false, false);
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    writeHeaders(buffer::put, originNode, destNode, ttl, circuitIndex, circuitId, (byte)0x00, rxSeqNumber, opByte);
    byte[] packet = Util.copyFromBuffer(buffer);
    return new BaseNetRomPacket(
        packet,
        originNode,
        destNode,
        ttl,
        circuitIndex,
        circuitId,
        (byte)0x00,
        rxSeqNumber,
        opByte
    );
  }

  @Override
  public byte[] getPayload() {
    return packet;
  }

  @Override
  public AX25Call getOriginNode() {
    return originNode;
  }

  @Override
  public AX25Call getDestNode() {
    return destNode;
  }

  @Override
  public byte getTTL() {
    return ttl;
  }

  @Override
  public byte getCircuitIndex() {
    return circuitIndex;
  }

  @Override
  public byte getCircuitId() {
    return circuitId;
  }

  @Override
  public byte getTxSeqNumber() {
    return txSeqNumber;
  }

  @Override
  public byte getRxSeqNumber() {
    return rxSeqNumber;
  }

  @Override
  public byte getOpCode() {
    return opCode;
  }

  @Override
  public OpType getOpType() {
    return OpType.fromOpCodeByte(getOpCode());
  }

  @Override
  public String toString() {
    return "NetRom{" +
        "op=" + getOpType() +
        ", origin=" + getOriginNode() +
        ", dest=" + getDestNode() +
        ", ttl=" + getTTL() +
        ", idx=" + getCircuitIndex() +
        ", id=" + getCircuitId() +
        ", tx=" + getTxSeqNumber() +
        ", rx=" + getRxSeqNumber() +
        '}';
  }
}
