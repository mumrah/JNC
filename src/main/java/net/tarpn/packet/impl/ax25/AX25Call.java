package net.tarpn.packet.impl.ax25;

import java.nio.ByteBuffer;
import net.tarpn.packet.impl.ax25.UIFrame.ByteConsumer;

public class AX25Call {
  private final String call;
  private final int ssid;
  private final int rr;
  private final boolean cFlag;
  private final boolean last;

  AX25Call(String call, int ssid, int rr, boolean cFlag, boolean last) {
    this.call = call;
    this.ssid = ssid;
    this.rr = rr;
    this.cFlag = cFlag;
    this.last = last;
  }

  public static AX25Call read(ByteBuffer buffer) {
    StringBuilder call = new StringBuilder();
    for(int i=0; i<6; i++) {
      char c = (char)((buffer.get() & 0xFF) >> 1);
      if(c != ' ') {
        call.append(c);
      }
    }

    byte ssidByte = buffer.get();
    int ssid = (ssidByte & 0x1E) >> 1;
    int rr = (ssidByte & 0x60) >> 5;
    boolean cFlag = (ssidByte & 0x80) != 0;
    boolean last = (ssidByte & 0x01) != 0;
    return new AX25Call(call.toString(), ssid, rr, cFlag, last);
  }

  public void write(ByteConsumer byteConsumer) {
    for(int i=0; i<6; i++) {
      final char c;
      if(i > call.length() - 1) {
        c = ' ';
      } else {
        c = call.charAt(i);
      }
      byteConsumer.accept((byte)((c & 0xFF) << 1));
    }
    byte ssidByte = (byte)((ssid << 1 & 0x1E) | (last ? 1 : 0));
    // TODO RR and C
    byteConsumer.accept(ssidByte);
  }

  public String getCall() {
    return call;
  }

  public int getSSID() {
    return ssid;
  }

  public int getRr() {
    return rr;
  }

  public boolean isCFlag() {
    return cFlag;
  }

  public boolean isLast() {
    return last;
  }

  @Override
  public String toString() {
    return String.format("%s-%d", call, ssid);
  }
}
