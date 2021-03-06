package net.tarpn.packet.impl.ax25;

import org.fusesource.jansi.Ansi;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

import static org.fusesource.jansi.Ansi.ansi;

public class IFrame extends BaseAX25Packet implements AX25Packet.InformationFrame {

  private final boolean pollSet;
  private final byte recvSeqNumber;
  private final byte sendSeqNumber;
  private final byte[] info;
  private final Protocol protocol;

  public IFrame(byte[] packet, AX25Call destination, AX25Call source, List<AX25Call> paths,
      byte control, byte[] info, byte pid) {
    super(packet, destination, source, paths, control);
    this.pollSet = (control & 0x10) != 0;
    this.recvSeqNumber = (byte)((control >> 5) & 0x07);
    this.sendSeqNumber = (byte)((control >> 1) & 0x07);
    this.info = info;
    this.protocol = Protocol.valueOf(pid);
  }

  public static IFrame create(
      AX25Call destCall,
      AX25Call sourceCall,
      Command command,
      byte sendSeqNumber,
      byte recvSeqNumber,
      boolean pollFinalSet,
      Protocol protocol,
      byte[] info) {
    ByteBuffer buffer = ByteBuffer.allocate(1024);

    // Update flags in SSID
    destCall.clearFlags();
    sourceCall.clearFlags();
    sourceCall.setLast(true);
    command.updateCalls(destCall, sourceCall);

    // Write out calls
    destCall.write(buffer::put);
    sourceCall.write(buffer::put);

    // TODO repeater paths
    byte controlByte = (byte)(((recvSeqNumber << 5) & 0xE0) | ((sendSeqNumber << 1) & 0x0E));
    controlByte |= (pollFinalSet ? 0x10 : 0x00);
    buffer.put(controlByte);
    buffer.put(protocol.asByte());
    buffer.put(info);
    int len = buffer.position();
    byte[] packet = new byte[len];
    buffer.position(0);
    buffer.get(packet, 0, len);
    return new IFrame(packet, destCall, sourceCall, Collections.emptyList(), controlByte, info, protocol.asByte());
  }

  @Override
  public boolean isPollBitSet() {
    return pollSet;
  }

  @Override
  public byte getReceiveSequenceNumber() {
    return recvSeqNumber;
  }

  @Override
  public byte getSendSequenceNumber() {
    return sendSeqNumber;
  }

  @Override
  public byte[] getInfo() {
    return info;
  }

  @Override
  public byte getProtocolByte() {
    return protocol.asByte();
  }

  @Override
  public Protocol getProtocol() {
    return protocol;
  }

  @Override
  public String toString() {
    return "IFrame{" +
        "source=" + getSource() +
        ", dest=" + getDestination() +
        ", protocol=" + getProtocol() +
        ", N(R)=" + getReceiveSequenceNumber() +
        ", N(S)=" + getSendSequenceNumber() +
        ", info=" + getInfoAsASCII() +
        '}';
  }

  @Override
  public String toLogString(int port) {
    if (protocol.equals(Protocol.NO_LAYER3)) {
      return super.toLogString(port) + " N(R)=" + getReceiveSequenceNumber() +
              " N(S)=" + getSendSequenceNumber() +
              "\n" + ansi().fg(Ansi.Color.GREEN).a(getInfoAsASCII()).reset();
    } else {
      return super.toLogString(port) + " N(R)=" + getReceiveSequenceNumber() +
              " N(S)=" + getSendSequenceNumber() + " " + getProtocol();
    }
  }

  @Override
  public FrameType getFrameType() {
    return FrameType.I;
  }
}
