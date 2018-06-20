package net.tarpn.network;

import java.nio.ByteBuffer;
import net.tarpn.Util;
import net.tarpn.packet.impl.ax25.AX25Call;

public class NetRomInfo extends NetRom {

  private final byte[] info;

  NetRomInfo(AX25Call originCall,
      AX25Call destCall,
      byte ttl, byte circuitIndex, byte circuitID, byte txSeqNumber, byte rxSeqNumber,
      byte opCode, byte[] info) {
    super(originCall, destCall, ttl, circuitIndex, circuitID, txSeqNumber, rxSeqNumber, opCode);
    this.info = info;
  }

  public byte[] getInfo() {
    return info;
  }

  @Override
  public void write(ByteBuffer buffer) {
    super.write(buffer);
    buffer.put(info);
  }

  @Override
  public String toString() {
    return "NetRomConnectInfo{" +
        "originCall=" + getOriginCall() +
        ", destCall=" + getDestCall() +
        ", ttl=" + Util.toHexString(getTtl()) +
        ", circuitIndex=" + Util.toHexString(getCircuitIndex()) +
        ", circuitID=" + Util.toHexString(getCircuitID()) +
        ", txSeqNumber=" + Util.toHexString(getTxSeqNumber()) +
        ", rxSeqNumber=" + Util.toHexString(getRxSeqNumber()) +
        ", opCode=" + getOpType() +
        ", infoASCII=" + Util.toEscapedASCII(getInfo()) +
        ", infoHex=" + Util.toHexString(getInfo()) +
        '}';
  }
}
