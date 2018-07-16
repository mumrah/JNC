package net.tarpn.packet.impl.ax25;

import java.nio.ByteBuffer;
import java.util.Objects;

public class AX25Call {

  @FunctionalInterface
  public interface ByteConsumer {
    void accept(byte b);
  }

  private final String call;
  private final int ssid;

  private int rr;
  private boolean cFlag;
  private boolean last;

  AX25Call(String call, int ssid, int rr, boolean cFlag, boolean last) {
    this.call = call;
    this.ssid = ssid;
    this.rr = rr;
    this.cFlag = cFlag;
    this.last = last;
  }

  public static AX25Call create(String call) {
    return create(call, 0);
  }

  public static AX25Call fromString(String callWithSSID) {
    String[] tokens = callWithSSID.split("-");
    try {
      return create(tokens[0].trim(), Integer.parseInt(tokens[1]));
    } catch (Throwable t) {
      throw new IllegalArgumentException("Could not parse '" + callWithSSID + "' as a callsign with ssid");
    }
  }

  public static AX25Call create(String call, int ssid) {
    return create(call, ssid, 0, false, false);
  }

  public static AX25Call create(String call, int ssid, int rr, boolean cFlag, boolean last) {
    return new AX25Call(call, ssid, rr, cFlag, last);
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
    ssidByte |= (byte)(cFlag ? 0x80 : 0x00);
    ssidByte |= (byte)((rr << 5) & 0x60);
    byteConsumer.accept(ssidByte);
  }

  public String getCall() {
    return call;
  }

  public int getSSID() {
    return ssid;
  }

  public int getRR() {
    return rr;
  }

  public boolean isCommandFlagSet() {
    return cFlag;
  }

  public boolean isLast() {
    return last;
  }

  public void clearFlags() {
    this.rr = 0;
    this.cFlag = false;
    this.last = false;
  }

  public void setRR(int rr) {
    this.rr = rr;
  }

  public void setCommandFlag(boolean cFlag) {
    this.cFlag = cFlag;
  }

  public void setLast(boolean last) {
    this.last = last;
  }

  @Override
  public String toString() {
    return String.format("%s-%d", call, ssid);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AX25Call)) {
      return false;
    }
    AX25Call ax25Call = (AX25Call) o;
    return ssid == ax25Call.ssid &&
        Objects.equals(getCall(), ax25Call.getCall());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getCall(), ssid);
  }
}
