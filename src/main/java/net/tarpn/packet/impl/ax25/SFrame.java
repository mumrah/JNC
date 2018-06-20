package net.tarpn.packet.impl.ax25;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

public class SFrame extends BaseAX25Packet implements AX25Packet.SupervisoryFrame {
  private final boolean pollFinalSet;
  private final byte recvSeqNumber;
  private final ControlType controlType;

  public SFrame(byte[] packet, String destination, String source, List<String> paths, byte control, boolean pollFinalSet) {
    super(packet, destination, source, paths, control);
    this.pollFinalSet = pollFinalSet;
    this.recvSeqNumber = (byte)((control & 0xE0) >> 5);
    this.controlType = ControlType.fromControlByte(control);
  }

  public static SFrame create(
      String source,
      String destination,
      SupervisoryFrame.ControlType control,
      boolean pollFinalSet) {
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    UIFrame.writeCall(destination, false, buffer::put);
    UIFrame.writeCall(source, true, buffer::put);
    // TODO repeater paths
    buffer.put(control.asByte(pollFinalSet));

    int len = buffer.position();
    byte[] packet = new byte[len];
    buffer.position(0);
    buffer.get(packet, 0, len);

    return new SFrame(packet, destination, source, Collections.emptyList(),
        control.asByte(pollFinalSet), pollFinalSet);
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
}