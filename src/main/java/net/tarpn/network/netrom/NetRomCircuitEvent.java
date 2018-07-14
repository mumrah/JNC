package net.tarpn.network.netrom;

import net.tarpn.packet.impl.ax25.AX25Call;

public class NetRomCircuitEvent {

  private final int circuitId;
  private final AX25Call remoteCall;
  private final Type type;

  public NetRomCircuitEvent(int circuitId, AX25Call remoteCall, Type type) {
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

  public int getCircuitId() {
    return circuitId;
  }

  public static class DataLinkEvent extends NetRomCircuitEvent {

    private final NetRomPacket netRomPacket;

    public DataLinkEvent(int circuitId, AX25Call remoteCall, NetRomPacket netRomPacket, Type type) {
      super(circuitId, remoteCall, type);
      this.netRomPacket = netRomPacket;
    }

    public NetRomPacket getNetRomPacket() {
      return netRomPacket;
    }
  }

  public static class UserDataEvent extends NetRomCircuitEvent {

    private final byte[] data;

    public UserDataEvent(int circuitId, AX25Call remoteCall, byte[] data) {
      super(circuitId, remoteCall, Type.NL_DATA);
      this.data = data;
    }

    public byte[] getData() {
      return data;
    }
  }

  public enum Type {
    NETROM_CONNECT,
    NETROM_CONNECT_ACK,
    NETROM_DISCONNECT,
    NETROM_DISCONNECT_ACK,
    NETROM_INFO,
    NETROM_INFO_ACK,
    NL_CONNECT,
    NL_DISCONNECT,
    NL_DATA;
  };
}
