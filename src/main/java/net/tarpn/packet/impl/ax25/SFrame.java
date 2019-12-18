package net.tarpn.packet.impl.ax25;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

public class SFrame extends BaseAX25Packet implements AX25Packet.SupervisoryFrame {
  private final boolean pollFinalSet;
  private final byte recvSeqNumber;
  private final ControlType controlType;

  public SFrame(byte[] packet, AX25Call destination, AX25Call source, List<AX25Call> paths, byte control, boolean pollFinalSet) {
    super(packet, destination, source, paths, control);
    this.pollFinalSet = pollFinalSet;
    this.recvSeqNumber = (byte)((control & 0xE0) >> 5);
    this.controlType = ControlType.fromControlByte(control);
  }

  public static SFrame create(
      AX25Call destination,
      AX25Call source,
      Command command,
      SupervisoryFrame.ControlType control,
      int nr,
      boolean pollFinalSet) {
    ByteBuffer buffer = ByteBuffer.allocate(1024);

    // Update flags in SSID
    destination.clearFlags();
    source.clearFlags();
    source.setLast(true);
    command.updateCalls(destination, source);

    // Write out calls
    destination.write(buffer::put);
    source.write(buffer::put);

    // TODO repeater paths
    buffer.put(control.asByte(nr, pollFinalSet));

    int len = buffer.position();
    byte[] packet = new byte[len];
    buffer.position(0);
    buffer.get(packet, 0, len);

    return new SFrame(packet, destination, source, Collections.emptyList(),
        control.asByte(nr, pollFinalSet), pollFinalSet);
  }

  @Override
  public boolean isPollOrFinalSet() {
    return pollFinalSet;
  }

  @Override
  public byte getReceiveSequenceNumber() {
    return recvSeqNumber;
  }

  @Override
  public ControlType getControlType() {
    return controlType;
  }

  @Override
  public String toString() {
    return "SFrame{" +
        "source=" + getSource() +
        ", dest=" + getDestination() +
        ", controlType=" + getControlType() +
        ", N(R)=" + getReceiveSequenceNumber() +
        '}';
  }

  @Override
  public String toLogString(int port) {
    return super.toLogString(port) + " N(R)=" + getReceiveSequenceNumber() + " " + getControlType();
  }

  @Override
  public FrameType getFrameType() {
    return FrameType.S;
  }
}