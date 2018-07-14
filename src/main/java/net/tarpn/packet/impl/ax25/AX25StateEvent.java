package net.tarpn.packet.impl.ax25;

import java.util.List;
import net.tarpn.packet.impl.ax25.AX25Packet.FrameType;
import net.tarpn.packet.impl.ax25.AX25Packet.HasInfo;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;

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

  public static AX25StateEvent createIncomingEvent(AX25Packet packet, Type type) {
    return new AX25StateEvent(packet.getSourceCall(), packet, type);
  }

  public static AX25StateEvent createDataEvent(AX25Call destCall, Protocol protocol, byte[] data) {
    return new AX25StateEvent(destCall, new InternalInfo(protocol, data, FrameType.I), Type.DL_DATA);
  }

  public static AX25StateEvent createUnitDataEvent(AX25Call destCall, Protocol protocol, byte[] data) {
    return new AX25StateEvent(destCall, new InternalInfo(protocol, data, FrameType.UI), Type.DL_UNIT_DATA);
  }

  public static AX25StateEvent createT1ExpireEvent(AX25Call retryConnectTo) {
    return new AX25StateEvent(retryConnectTo, DummyAX25Packet.empty(), Type.T1_EXPIRE);
  }

  public static AX25StateEvent createT3ExpireEvent(AX25Call retryConnectTo) {
    return new AX25StateEvent(retryConnectTo, DummyAX25Packet.empty(), Type.T3_EXPIRE);
  }

  public static AX25StateEvent createIFrameQueueEvent(AX25Call iFrameDest) {
    return new AX25StateEvent(iFrameDest, DummyAX25Packet.empty(), Type.IFRAME_READY);
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

  @Override
  public String toString() {
    return "AX25StateEvent(" + type + "){" +
        "remoteCall=" + remoteCall +
        '}';
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
    IFRAME_READY
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

  public static final class InternalInfo implements AX25Packet, HasInfo {
    private final Protocol protocol;
    private final byte[] data;
    private final FrameType frameType;

    public InternalInfo(Protocol protocol, byte[] data, FrameType frameType) {
      this.protocol = protocol;
      this.data = data;
      this.frameType = frameType;
    }

    @Override
    public AX25Call getDestCall() {
      throw new UnsupportedOperationException();
    }

    @Override
    public AX25Call getSourceCall() {
      throw new UnsupportedOperationException();
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
      return frameType;
    }

    @Override
    public byte[] getPayload() {
      throw new UnsupportedOperationException();
    }

    @Override
    public byte[] getInfo() {
      return data;
    }

    @Override
    public byte getProtocolByte() {
      return protocol.asByte();
    }

    @Override
    public Protocol getProtocol() {
      return protocol;
    }
  }
}
