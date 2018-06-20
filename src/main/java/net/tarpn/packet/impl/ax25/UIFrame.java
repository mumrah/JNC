package net.tarpn.packet.impl.ax25;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import net.tarpn.packet.impl.ax25.AX25Packet.HasInfo;

public class UIFrame extends UFrame implements HasInfo {
  private final byte[] info;
  private final Protocol protocol;

  public UIFrame(byte[] packet, String destination, String source, List<String> paths, byte control,
      boolean pollFinalSet, byte[] info, byte pid) {
    super(packet, destination, source, paths, control, pollFinalSet);
    this.info = info;
    this.protocol = Protocol.valueOf(pid);
  }

  public static UIFrame create(
      String source,
      String destination,
      Protocol pid,
      byte[] info) {

    ControlType controlType = ControlType.UI;
    ByteBuffer buffer = ByteBuffer.allocate(1024);
    writeCall(destination, false, buffer::put);
    writeCall(source, true, buffer::put);
    // TODO repeater paths
    buffer.put(controlType.asByte(true));
    buffer.put(pid.asByte());
    buffer.put(info);
    int len = buffer.position();
    byte[] packet = new byte[len];
    buffer.position(0);
    buffer.get(packet, 0, len);
    return new UIFrame(packet, destination, source, Collections.emptyList(),
        controlType.asByte(true), true, info, pid.asByte());
  }

  static void writeCall(String callWithSSID, boolean last, ByteConsumer byteConsumer) {
    String[] callTokens = callWithSSID.split("-");
    String call = callTokens[0];
    for(int i=0; i<6; i++) {
      final char c;
      if(i > call.length() - 1) {
        c = ' ';
      } else {
        c = call.charAt(i);
      }
      byteConsumer.accept((byte)((c & 0xFF) << 1));
    }
    int ssid = Integer.parseInt(callTokens[1]);
    byte ssidByte = (byte)((ssid << 1 & 0x1E) | (last ? 1 : 0));
    byteConsumer.accept(ssidByte);
  }

  @FunctionalInterface
  public interface ByteConsumer {
    void accept(byte b);
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
}
