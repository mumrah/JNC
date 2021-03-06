package net.tarpn.datalink;


import net.tarpn.netty.Primitive;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet.HasInfo;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;
import net.tarpn.packet.impl.ax25.IFrame;
import net.tarpn.packet.impl.ax25.UIFrame;

import java.util.Objects;

/**
 * Used for interfacing with a {@link DataLinkManager}
 */
public class DataLinkPrimitive implements Primitive {

  private final AX25Call remoteCall;
  private final AX25Call localCall;
  private final Type type;
  private final boolean isConfirmation;
  private final HasInfo linkInfo;
  private final ErrorType error;
  private int port = -1;

  private DataLinkPrimitive(AX25Call remoteCall, AX25Call localCall, Type type, boolean isConfirmation,
                            HasInfo linkInfo, ErrorType error) {
    this.remoteCall = remoteCall;
    this.localCall = localCall;
    this.type = type;
    this.isConfirmation = isConfirmation;
    this.linkInfo = linkInfo;
    this.error = error;
  }

  public DataLinkPrimitive readdress(AX25Call newRemoteCall) {
    return new DataLinkPrimitive(newRemoteCall, this.localCall, this.type, this.isConfirmation, this.linkInfo, this.error);
  }

  public static DataLinkPrimitive newConnectRequest(AX25Call remoteCall, AX25Call localCall) {
    return new DataLinkPrimitive(remoteCall, localCall, Type.DL_CONNECT, false, null, ErrorType.NONE);
  }

  public static DataLinkPrimitive newDisconnectRequest(AX25Call remoteCall, AX25Call localCall) {
    return new DataLinkPrimitive(remoteCall, localCall, Type.DL_DISCONNECT, false, null, ErrorType.NONE);
  }

  public static DataLinkPrimitive newDataRequest(AX25Call destCall, AX25Call localCall, Protocol protocol, byte[] data) {
    //InternalInfo iFrame = new InternalInfo(protocol, data, FrameType.I);
    LinkInfo iFrame = new LinkInfo(protocol, data);
    return new DataLinkPrimitive(destCall, localCall, Type.DL_DATA, false, iFrame, ErrorType.NONE);
  }

  public static DataLinkPrimitive newUnitDataRequest(AX25Call destCall, AX25Call localCall, Protocol protocol, byte[] data) {
    //InternalInfo uiFrame = new InternalInfo(protocol, data, FrameType.UI);
    LinkInfo uiFrame = new LinkInfo(protocol, data);
    return new DataLinkPrimitive(destCall, localCall, Type.DL_UNIT_DATA, false, uiFrame, ErrorType.NONE);
  }

  public static DataLinkPrimitive newConnectIndication(AX25Call remoteCall, AX25Call localCall) {
    return new DataLinkPrimitive(remoteCall, localCall, Type.DL_CONNECT, false, null, ErrorType.NONE);
  }

  public static DataLinkPrimitive newDisconnectIndication(AX25Call remoteCall, AX25Call localCall) {
    return new DataLinkPrimitive(remoteCall, localCall, Type.DL_DISCONNECT, false, null, ErrorType.NONE);
  }

  public static DataLinkPrimitive newConnectConfirmation(AX25Call remoteCall, AX25Call localCall) {
    return new DataLinkPrimitive(remoteCall, localCall, Type.DL_CONNECT, true, null, ErrorType.NONE);
  }

  public static DataLinkPrimitive newDisconnectConfirmation(AX25Call remoteCall, AX25Call localCall) {
    return new DataLinkPrimitive(remoteCall, localCall, Type.DL_DISCONNECT, true, null, ErrorType.NONE);
  }

  public static DataLinkPrimitive newDataIndication(AX25Call remoteCall, AX25Call localCall, Protocol protocol, byte[] info) {
    return new DataLinkPrimitive(remoteCall, localCall, Type.DL_DATA, false, new LinkInfo(protocol, info), ErrorType.NONE);
  }

  public static DataLinkPrimitive newDataIndication(IFrame iFrame) {
    return new DataLinkPrimitive(iFrame.getSourceCall(), iFrame.getDestCall(), Type.DL_DATA, false, iFrame, ErrorType.NONE);
  }

  public static DataLinkPrimitive newUnitDataIndication(UIFrame uiFrame) {
    return new DataLinkPrimitive(uiFrame.getSourceCall(), uiFrame.getDestCall(), Type.DL_UNIT_DATA, false, uiFrame, ErrorType.NONE);
  }

  public static DataLinkPrimitive newErrorResponse(AX25Call remoteCall, AX25Call localCall, ErrorType error) {
    return new DataLinkPrimitive(remoteCall, localCall, Type.DL_ERROR, false, null, error);
  }

  public AX25Call getRemoteCall() {
    return remoteCall;
  }

  public AX25Call getLocalCall() {
    return localCall;
  }

  public Type getType() {
    return type;
  }

  public boolean isConfirmation() {
    return isConfirmation;
  }

  public HasInfo getLinkInfo() {
    return linkInfo;
  }

  public ErrorType getError() {
    return error;
  }

  public int getPort() {
      return port;
  }

  public void setPort(int port) {
      this.port = port;
  }

  @Override
  public String toString() {
    return "DataLinkPrimitive{" +
        "remoteCall=" + remoteCall +
        ", localCall=" + localCall +
        ", type=" + type +
        ", info=" + getLinkInfo() +
        ", port=" + port +
        ", isConfirmation=" + isConfirmation +
        ", error=" + getError() +
        '}';
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DataLinkPrimitive that = (DataLinkPrimitive) o;
    return isConfirmation == that.isConfirmation &&
            Objects.equals(remoteCall, that.remoteCall) &&
            Objects.equals(localCall, that.localCall) &&
            type == that.type &&
            Objects.equals(linkInfo, that.linkInfo) &&
            error == that.error;
  }

  @Override
  public int hashCode() {
    return Objects.hash(remoteCall, localCall, type, isConfirmation, linkInfo, error);
  }


  public enum Type {
    DL_CONNECT(false),
    DL_DISCONNECT(false),
    DL_DATA(true),
    DL_UNIT_DATA(true),
    DL_ERROR(false);

    private final boolean hasData;

    Type(boolean hasData) {
      this.hasData = hasData;
    }

    public boolean hasData() {
      return hasData;
    }
  }

  public enum ErrorType {
    A("F=1 received but P=1 not outstanding"),
    B("Unexpected DM with F=1 in states 3, 4 or 5"),
    C("Unexpected UA in states 3, 4 or 5"),
    D("UA received without F=1 when SABM or DISC was sent P=1"),
    E("DM received in states 3, 4 or 5"),
    F("Data link reset; i.e., SABM received in state 3, 4 or 5"),
    G("Retry count exceeded in non-connected state(?)"),
    H("Retry count exceeded in connected state(?)"),
    I("N2 timeouts: unacknowledged data"),
    J("N(r) sequence error"),
    K("FRMR Received"),
    L("Control field invalid or not implemented"),
    M("Information field was received in a U- or S-type frame"),
    N("Length of frame incorrect for frame type"),
    O("I frame exceeded maximum allowed length"),
    P("N(s) out of the window"),
    Q("UI response received, or UI command with P=1 received"),
    R("UI frame exceeded maximum allowed length"),
    S("I response received"),
    T("N2 timeouts: no response to enquiry"),
    U("N2 timeouts: extended peer busy condition"),
    V("No DL machines available to establish connection"),
    NONE("Used to indicate no error");

    private final String msg;

    ErrorType(String msg) {
      this.msg = msg;
    }

    public String getMessage() {
      return msg;
    }
  }

  public static final class LinkInfo implements HasInfo {
    private final Protocol protocol;
    private final byte[] data;

    public LinkInfo(Protocol protocol, byte[] data) {
      this.protocol = protocol;
      this.data = data;
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

    @Override
    public String toString() {
      return "LinkInfo{" +
              "protocol=" + protocol +
              ", data=" + getInfoAsASCII() +
              '}';
    }
  }
}
