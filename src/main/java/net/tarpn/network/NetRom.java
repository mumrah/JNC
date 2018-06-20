package net.tarpn.network;

import java.nio.ByteBuffer;
import net.tarpn.packet.impl.ax25.AX25Call;

public class NetRom {

  // 15:11:52.105 [pool-2-thread-1] INFO  net.tarpn.packet.impl.ConsolePacketHandler - Got Packet: AX25Packet{port=1, source=K4DBZ, dest=DAVID2, payload=[], ascii=, paths=[], controlByte=0x3f, pidByte=0x91}
  // 15:11:55.636 [pool-2-thread-1] INFO  net.tarpn.packet.impl.ConsolePacketHandler - Got Packet: AX25Packet{port=1, source=K4DBZ, dest=DAVID2, payload=[], ascii=, paths=[], controlByte=0x3f, pidByte=0x91}

  // ID Packet: K4DBZ-9>ID, UI frame, no layer 3, node callsign
  // NODES Packet: K4DBZ-9>NODES, UI frame, NETROM layer 3, node callsign
  // CONNECT Packet: K4DBZ>DAVID2, SABM frame, G8BPQ layer 3, op callsign > node alias


  public static byte NETROM_AX25_PID = (byte)0xCF;
  public static byte G8BPQ_LAYER_3_PID = (byte)0x91; // yy01yyyy
  public static byte NO_LAYER_3_PID = (byte)0xF0; // APRS, ID frames, etc

  public static byte AX25_SABM_CTL = (byte)0x3F;
  public static byte AX25_DISC_CTL = (byte)0x53;
  public static byte AX25_UI_CTL = (byte)0x03;
  public static byte AX25_UA_CTL = (byte)0x73;

  private final AX25Call originCall;
  private final AX25Call destCall;
  private final byte ttl;
  private final byte circuitIndex;
  private final byte circuitID;
  private final byte txSeqNumber;
  private final byte rxSeqNumber;
  private final byte opCode;

  NetRom(AX25Call originCall, AX25Call destCall, byte ttl, byte circuitIndex, byte circuitID,
      byte txSeqNumber, byte rxSeqNumber, byte opCode) {
    this.originCall = originCall;
    this.destCall = destCall;
    this.ttl = ttl;
    this.circuitIndex = circuitIndex;
    this.circuitID = circuitID;
    this.txSeqNumber = txSeqNumber;
    this.rxSeqNumber = rxSeqNumber;
    this.opCode = opCode;
  }

  public static NetRom read(ByteBuffer buffer) {
    AX25Call origin = AX25Call.read(buffer);
    AX25Call dest = AX25Call.read(buffer);
    byte ttl = buffer.get();
    byte circuitIdx = buffer.get();
    byte circuitId = buffer.get();
    byte txSeqNum = buffer.get();
    byte rxSeqNum = buffer.get();
    byte opcode = buffer.get();
    OpType opType = OpType.fromOpCodeByte(opcode);
    switch (opType) {
      case ConnectRequest:
        byte proposeWindowSize = buffer.get();
        AX25Call originUser = AX25Call.read(buffer);
        AX25Call originNode = AX25Call.read(buffer);
        return new NetRomConnect(origin, dest, ttl, circuitIdx, circuitId, txSeqNum, rxSeqNum,
            opcode, proposeWindowSize, originUser, originNode);
      case ConnectAcknowledge:
        byte acceptWindowSize = buffer.get();
        return new NetRomConnectAck(origin, dest, ttl, circuitIdx, circuitId, txSeqNum, rxSeqNum,
            opcode, acceptWindowSize);
      case Information:
        int len = buffer.remaining();
        byte[] info = new byte[len];
        buffer.get(info);
        return new NetRomInfo(origin, dest, ttl, circuitIdx, circuitId, txSeqNum, rxSeqNum, opcode, info);
      case DisconnectRequest:
      case DisconnectAcknowledge:
      case InformationAcknowledge:
      default:
        return new NetRom(origin, dest, ttl, circuitIdx, circuitId, txSeqNum, rxSeqNum, opcode);
    }

  }

  public void write(ByteBuffer buffer) {
    originCall.write(buffer::put);
    destCall.write(buffer::put);
    buffer.put(ttl);
    buffer.put(circuitIndex);
    buffer.put(circuitID);
    buffer.put(txSeqNumber);
    buffer.put(rxSeqNumber);
    buffer.put(opCode);
  }


  public AX25Call getOriginCall() {
    return originCall;
  }

  public AX25Call getDestCall() {
    return destCall;
  }

  public byte getTtl() {
    return ttl;
  }

  public byte getCircuitIndex() {
    return circuitIndex;
  }

  public byte getCircuitID() {
    return circuitID;
  }

  public byte getTxSeqNumber() {
    return txSeqNumber;
  }

  public byte getRxSeqNumber() {
    return rxSeqNumber;
  }

  public byte getOpCode() {
    return opCode;
  }

  public OpType getOpType() {
    return OpType.fromOpCodeByte(opCode);
  }

  public boolean isChoke() {
    return (opCode & 0x80) == 0x80;
  }

  public boolean isNAK() {
    return (opCode & 0x40) == 0x40;
  }

  public boolean isMoreFollows() {
    return (opCode & 0x20) == 0x20;
  }

  @Override
  public String toString() {
    return "NetRom{" +
        "originCall=" + getOriginCall() +
        ", destCall=" + getDestCall() +
        ", ttl=" + Integer.toHexString(getTtl()) +
        ", circuitIndex=" + Integer.toHexString(getCircuitIndex()) +
        ", circuitID=" + Integer.toHexString(getCircuitID()) +
        ", txSeqNumber=" + Integer.toHexString(getTxSeqNumber()) +
        ", rxSeqNumber=" + Integer.toHexString(getRxSeqNumber()) +
        ", opCode=" + getOpType() +
        '}';
  }

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

    byte asByte() {
      return opNibble;
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
