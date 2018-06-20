package net.tarpn.packet.impl.ax25;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

public class UFrame extends BaseAX25Packet implements AX25Packet.UnnumberedFrame {

  private final boolean pollFinalSet;
  private final ControlType controlType;

  public UFrame(byte[] packet, String destination, String source, List<String> paths, byte control, boolean pollFinalSet) {
    super(packet, destination, source, paths, control);
    this.pollFinalSet = pollFinalSet;
    this.controlType = ControlType.fromControlByte(control);
  }

  public static UFrame create(String source, String destination, ControlType controlType, boolean pollFinalSet) {
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    UIFrame.writeCall(destination, false, buffer::put);
    UIFrame.writeCall(source, true, buffer::put);
    // TODO repeater paths
    buffer.put(controlType.asByte(pollFinalSet));

    int len = buffer.position();
    byte[] packet = new byte[len];
    buffer.position(0);
    buffer.get(packet, 0, len);

    return new UFrame(packet, destination, source, Collections.emptyList(),
        controlType.asByte(pollFinalSet), pollFinalSet);
  }

  @Override
  public UFrame.ControlType getControlType() {
    return controlType;
  }

  @Override
  public boolean isPollFinalSet() {
    return pollFinalSet;
  }

  @Override
  public String toString() {
    return "UFrame{" +
        "source=" + getSource() +
        ", dest=" + getDestination() +
        ", controlType=" + getControlType() +
        '}';
  }
}
