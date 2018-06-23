package net.tarpn.packet.impl.ax25;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import net.tarpn.packet.impl.ax25.AX25Packet.HasInfo;

public class UIFrame extends UFrame implements HasInfo {

  private final byte[] info;
  private final Protocol protocol;

  public UIFrame(byte[] packet, AX25Call destination, AX25Call source, List<AX25Call> paths, byte control,
      boolean pollFinalSet, byte[] info, byte pid) {
    super(packet, destination, source, paths, control, pollFinalSet);
    this.info = info;
    this.protocol = Protocol.valueOf(pid);
  }

  public static UIFrame create(
      AX25Call destCall,
      AX25Call sourceCall,
      Protocol pid,
      byte[] info) {
    ControlType controlType = ControlType.UI;
    ByteBuffer buffer = ByteBuffer.allocate(1024);

    // Update flags in SSID
    destCall.clearFlags();
    sourceCall.clearFlags();
    sourceCall.setLast(true);
    Command.COMMAND.updateCalls(destCall, sourceCall);

    // Write out calls
    destCall.write(buffer::put);
    sourceCall.write(buffer::put);

    // TODO repeater paths
    buffer.put(controlType.asByte(true));
    buffer.put(pid.asByte());
    buffer.put(info);
    int len = buffer.position();
    byte[] packet = new byte[len];

    buffer.position(0);
    buffer.get(packet, 0, len);
    return new UIFrame(packet, destCall, sourceCall, Collections.emptyList(),
        controlType.asByte(true), true, info, pid.asByte());
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
    return "UIFrame{" +
        "source=" + getSource() +
        ", dest=" + getDestination() +
        ", controlType=" + getControlType() +
        ", protocol=" + getProtocol() +
        ", info=" + getInfoAsASCII() +
        '}';
  }

  @Override
  public FrameType getFrameType() {
    return FrameType.UI;
  }
}
