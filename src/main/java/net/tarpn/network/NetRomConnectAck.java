package net.tarpn.network;

import java.nio.ByteBuffer;
import net.tarpn.Util;
import net.tarpn.packet.impl.ax25.AX25Call;

public class NetRomConnectAck extends NetRom {
  private final byte acceptWindowSize;

  NetRomConnectAck(AX25Call originCall,
      AX25Call destCall,
      byte ttl, byte circuitIndex, byte circuitID, byte txSeqNumber, byte rxSeqNumber,
      byte opCode, byte acceptWindowSize) {
    super(originCall, destCall, ttl, circuitIndex, circuitID, txSeqNumber, rxSeqNumber, opCode);
    this.acceptWindowSize = acceptWindowSize;
  }

  public byte getAcceptWindowSize() {
    return acceptWindowSize;
  }

  @Override
  public void write(ByteBuffer buffer) {
    super.write(buffer);
    buffer.put(acceptWindowSize);
  }

  @Override
  public String toString() {
    return "NetRomConnectAck{" +
        "originCall=" + getOriginCall() +
        ", destCall=" + getDestCall() +
        ", ttl=" + Util.toHexString(getTtl()) +
        ", circuitIndex=" + Util.toHexString(getCircuitIndex()) +
        ", circuitID=" + Util.toHexString(getCircuitID()) +
        ", txSeqNumber=" + Util.toHexString(getTxSeqNumber()) +
        ", rxSeqNumber=" + Util.toHexString(getRxSeqNumber()) +
        ", opCode=" + getOpType() +
        ", acceptWindowSize=" + getAcceptWindowSize() +
        '}';
  }
}
