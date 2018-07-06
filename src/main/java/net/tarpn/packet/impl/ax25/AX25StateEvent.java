package net.tarpn.packet.impl.ax25;

import java.util.List;

/**
 * A uniform event type to allow for sequential processing of all AX.25 state primitives, packets,
 * and timers.
 */
public class AX25StateEvent {

  private final AX25Call remoteCall;
  private final AX25Packet packet;
  private final Type type;

  private AX25StateEvent(AX25Call remoteCall, AX25Packet packet, Type type) {
    this.remoteCall = remoteCall;
    this.packet = packet;
    this.type = type;
  }

  /**
   * Create an event for an incoming packet. The session ID will be the source callsign of this packet
   */
  public static AX25StateEvent createIncomingEvent(AX25Packet packet, Type type) {
    return new AX25StateEvent(packet.getSourceCall(), packet, type);
  }

  /**
   * Create an event for an outgoing packet. The session ID will be the destination callsign of this packet
   */
  public static AX25StateEvent createOutgoingEvent(AX25Packet packet, Type type) {
    return new AX25StateEvent(packet.getDestCall(), packet, type);
  }

  /**
   * Create a UI event. A static session ID of "UI" will be used.
   */
  public static AX25StateEvent createUIEvent(AX25Packet packet, Type type) {
    return new AX25StateEvent(packet.getDestCall(), packet, type);
  }

  public static AX25StateEvent createT1ExpireEvent(AX25Call retryConnectTo) {
    return new AX25StateEvent(retryConnectTo, DummyAX25Packet.empty(), Type.T1_EXPIRE);
  }

  public static AX25StateEvent createT3ExpireEvent(AX25Call retryConnectTo) {
    return new AX25StateEvent(retryConnectTo, DummyAX25Packet.empty(), Type.T3_EXPIRE);
  }

  public static AX25StateEvent createConnectEvent(AX25Call dest) {
    return new AX25StateEvent(dest, DummyAX25Packet.empty(), Type.DL_CONNECT);
  }

  public static AX25StateEvent createDisconnectEvent(AX25Call dest) {
    return new AX25StateEvent(dest, DummyAX25Packet.empty(), Type.DL_DISCONNECT);
  }

  public AX25Call getRemoteCall() {
    return remoteCall;
  }

  public AX25Packet getPacket() {
    return packet;
  }

  public Type getType() {
    return type;
  }

  public enum Type {
    AX25_UA,
    AX25_DM,
    AX25_UI,
    AX25_DISC,
    AX25_SABM,
    AX25_SABME,
    AX25_UNKNOWN,
    AX25_INFO,
    AX25_FRMR,
    AX25_RR,
    AX25_RNR,
    AX25_SREJ,
    AX25_REJ,
    T1_EXPIRE,
    T3_EXPIRE,
    DL_CONNECT,
    DL_DISCONNECT,
    DL_DATA,
    DL_UNIT_DATA,
    //DL_FLOW_OFF,
    //DL_FLOW_ON,
  }

  public static final class DummyAX25Packet implements AX25Packet {

    private final AX25Call destCall;
    private final AX25Call sourceCall;

    private DummyAX25Packet(AX25Call destCall, AX25Call sourceCall) {
      this.destCall = destCall;
      this.sourceCall = sourceCall;
    }

    public static DummyAX25Packet empty() {
      return new DummyAX25Packet(AX25Call.create("NOCALL", 0), AX25Call.create("NOCALL", 0));
    }

    @Override
    public AX25Call getDestCall() {
      return destCall;
    }

    @Override
    public AX25Call getSourceCall() {
      return sourceCall;
    }

    @Override
    public List<AX25Call> getRepeaterPaths() {
      throw new UnsupportedOperationException();
    }

    @Override
    public byte getControlByte() {
      throw new UnsupportedOperationException();
    }

    @Override
    public FrameType getFrameType() {
      throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getPayload() {
      throw new UnsupportedOperationException();
    }
  }
}
