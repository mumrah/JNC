package net.tarpn.packet.impl.ax25;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;

public class UFrame extends BaseAX25Packet implements AX25Packet.UnnumberedFrame {

  private final boolean pollFinalSet;
  private final ControlType controlType;

  public UFrame(byte[] packet, AX25Call destination, AX25Call source, List<AX25Call> paths, byte control, boolean pollFinalSet) {
    super(packet, destination, source, paths, control);
    this.pollFinalSet = pollFinalSet;
    this.controlType = ControlType.fromControlByte(control);
  }

  public static UFrame create(
      AX25Call destCall,
      AX25Call sourceCall,
      Command command,
      ControlType controlType,
      boolean pollFinalSet) {
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
    buffer.put(controlType.asByte(pollFinalSet));

    int len = buffer.position();
    byte[] packet = new byte[len];
    buffer.position(0);
    buffer.get(packet, 0, len);

    return new UFrame(packet, destCall, sourceCall, Collections.emptyList(),
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

  @Override
  public String toLogString(int port) {
    return getSource() + ">" + getDestination() + " Port=" + port + " " + getControlType() + " P/F=" + (isPollFinalSet() ? 1 : 0);
  }

  @Override
  public FrameType getFrameType() {
    return FrameType.U;
  }
}
