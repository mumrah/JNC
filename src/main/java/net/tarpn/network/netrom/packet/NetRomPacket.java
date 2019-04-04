package net.tarpn.network.netrom.packet;

import net.tarpn.packet.Packet;
import net.tarpn.packet.impl.ax25.AX25Call;

public interface NetRomPacket extends Packet {

  default String getDestination() {
    return getDestNode().toString();
  }

  default String getSource() {
    return getOriginNode().toString();
  }

  AX25Call getOriginNode();
  AX25Call getDestNode();
  byte getTTL();
  byte getCircuitIndex();
  byte getCircuitId();
  byte getTxSeqNumber();
  byte getRxSeqNumber();
  byte getOpCode();
  OpType getOpType();

  enum OpType {
    ConnectRequest(0x01),
    ConnectAcknowledge(0x02),
    DisconnectRequest(0x03),
    DisconnectAcknowledge(0x04),
    Information(0x05),
    InformationAcknowledge(0x06);

    private final byte opNibble;

    OpType(int opNibble) {
      this.opNibble = (byte)(opNibble & 0x0F);
    }

    public byte asByte(boolean choke, boolean nak, boolean moreFollows) {
      byte opcodeByte = opNibble;
      opcodeByte |= (choke ? 0x80 : 0x00);
      opcodeByte |= (nak ? 0x40 : 0x00);
      opcodeByte |= (moreFollows ? 0x20 : 0x00);
      return opcodeByte;
    }

    public static OpType fromOpCodeByte(byte opCode) {
      byte masked = (byte)(opCode & 0x0F);
      for(OpType type : OpType.values()) {
        if(type.opNibble == masked) {
          return type;
        }
      }
      throw new IllegalArgumentException("Unknown opcode for Net/Rom " + Integer.toHexString(masked));
    }
  }
}
