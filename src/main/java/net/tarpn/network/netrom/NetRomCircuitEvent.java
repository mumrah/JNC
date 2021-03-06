package net.tarpn.network.netrom;

import net.tarpn.network.netrom.packet.NetRomPacket;
import net.tarpn.util.Util;
import net.tarpn.packet.impl.ax25.AX25Call;

public class NetRomCircuitEvent {

  private final byte circuitId;
  private final AX25Call remoteCall;
  private final Type type;

  public NetRomCircuitEvent(byte circuitId, AX25Call remoteCall, Type type) {
    this.circuitId = circuitId;
    this.remoteCall = remoteCall;
    this.type = type;
  }

  public AX25Call getRemoteCall() {
    return remoteCall;
  }

  public Type getType() {
    return type;
  }

  public byte getCircuitId() {
    return circuitId;
  }

  @Override
  public String toString() {
    return "NetRomCircuitEvent{" +
        "circuitId=" + (getCircuitId() & 0xff) +
        ", remoteCall=" + getRemoteCall() +
        ", type=" + getType() +
        '}';
  }

  public static class DataLinkEvent extends NetRomCircuitEvent {

    private final NetRomPacket netRomPacket;

    public DataLinkEvent(byte circuitId, AX25Call remoteCall, NetRomPacket netRomPacket, Type type) {
      super(circuitId, remoteCall, type);
      this.netRomPacket = netRomPacket;
    }

    public NetRomPacket getNetRomPacket() {
      return netRomPacket;
    }

    @Override
    public String toString() {
      return "DataLinkEvent{" +
          "circuitId=" + getCircuitId() +
          ", remoteCall=" + getRemoteCall() +
          ", type=" + getType() +
          ", packet=" + getNetRomPacket() +
          '}';
    }
  }

  public static class UserDataEvent extends NetRomCircuitEvent {

    private final byte[] data;

    public UserDataEvent(byte circuitId, AX25Call remoteCall, byte[] data) {
      super(circuitId, remoteCall, Type.NL_DATA);
      this.data = data;
    }

    public byte[] getData() {
      return data;
    }

    @Override
    public String toString() {
      return "UserDataEvent{" +
          "circuitId=" + getCircuitId() +
          ", remoteCall=" + getRemoteCall() +
          ", type=" + getType() +
          ", data=" + Util.toEscapedASCII(data) +
          '}';
    }
  }

  public enum Type {
    // 6 NET/ROM message types
    NETROM_CONNECT,
    NETROM_CONNECT_ACK,
    NETROM_DISCONNECT,
    NETROM_DISCONNECT_ACK,
    NETROM_INFO,
    NETROM_INFO_ACK,

    // Three SAP types
    NL_CONNECT,
    NL_DISCONNECT,
    NL_DATA;
  };
}
