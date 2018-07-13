package net.tarpn.datalink;


import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet.HasInfo;
import net.tarpn.packet.impl.ax25.IFrame;
import net.tarpn.packet.impl.ax25.UIFrame;

public class DataLinkPrimitive {

  private final String dataLinkId;
  private final AX25Call remoteCall;
  private final Type type;
  private final boolean isConfirmation;
  private final HasInfo infoPacket;
  private final ErrorType error;

  private DataLinkPrimitive(String dataLinkId, AX25Call remoteCall,
      Type type, boolean isConfirmation, HasInfo infoPacket, ErrorType error) {
    this.dataLinkId = dataLinkId;
    this.remoteCall = remoteCall;
    this.type = type;
    this.isConfirmation = isConfirmation;
    this.infoPacket = infoPacket;
    this.error = error;
  }

  public static DataLinkPrimitive newConnectRequest(AX25Call remoteCall) {
    return new DataLinkPrimitive(remoteCall.toString(), remoteCall, Type.DL_CONNECT, false,
        null, ErrorType.NONE);
  }

  public static DataLinkPrimitive newDisconnectRequest(AX25Call remoteCall) {
    return new DataLinkPrimitive(remoteCall.toString(), remoteCall, Type.DL_DISCONNECT, false,
       null, ErrorType.NONE);
  }

  public static DataLinkPrimitive newDataRequest(IFrame iFrame) {
    return new DataLinkPrimitive(iFrame.getDestCall().toString(), iFrame.getDestCall(), Type.DL_DATA,
        false, iFrame, ErrorType.NONE);
  }

  public static DataLinkPrimitive newUnitDataRequest(UIFrame uiFrame) {
    return new DataLinkPrimitive(uiFrame.getDestCall().toString(), uiFrame.getDestCall(), Type.DL_UNIT_DATA,
        false, uiFrame, ErrorType.NONE);
  }

  public static DataLinkPrimitive newConnectIndication(AX25Call remoteCall) {
    return new DataLinkPrimitive(remoteCall.toString(), remoteCall, Type.DL_CONNECT, false,
        null, ErrorType.NONE);
  }

  public static DataLinkPrimitive newDisconnectIndication(AX25Call remoteCall) {
    return new DataLinkPrimitive(remoteCall.toString(), remoteCall, Type.DL_DISCONNECT, false,
        null, ErrorType.NONE);
  }

  public static DataLinkPrimitive newConnectConfirmation(AX25Call remoteCall) {
    return new DataLinkPrimitive(remoteCall.toString(), remoteCall, Type.DL_CONNECT, true,
        null, ErrorType.NONE);
  }

  public static DataLinkPrimitive newDisconnectConfirmation(AX25Call remoteCall) {
    return new DataLinkPrimitive(remoteCall.toString(), remoteCall, Type.DL_DISCONNECT, true,
        null, ErrorType.NONE);
  }

  public static DataLinkPrimitive newDataResponse(IFrame iFrame) {
    return new DataLinkPrimitive(iFrame.getSourceCall().toString(), iFrame.getSourceCall(), Type.DL_DATA,
        false, iFrame, ErrorType.NONE);
  }

  public static DataLinkPrimitive newUnitDataResponse(UIFrame uiFrame) {
    return new DataLinkPrimitive(uiFrame.getSourceCall().toString(), uiFrame.getSourceCall(), Type.DL_UNIT_DATA,
        false, uiFrame, ErrorType.NONE);
  }

  public static DataLinkPrimitive newErrorResponse(AX25Call remoteCall, ErrorType error) {
    return new DataLinkPrimitive(remoteCall.toString(), remoteCall, Type.DL_ERROR, false,
        null, error);
  }

  public String getDataLinkId() {
    return dataLinkId;
  }

  public AX25Call getRemoteCall() {
    return remoteCall;
  }

  public Type getType() {
    return type;
  }

  public boolean isConfirmation() {
    return isConfirmation;
  }

  public HasInfo getPacket() {
    return infoPacket;
  }

  public ErrorType getError() {
    return error;
  }

  @Override
  public String toString() {
    return "DataLinkPrimitive{" +
        "dataLinkId='" + dataLinkId + '\'' +
        ", remoteCall=" + remoteCall +
        ", type=" + type +
        ", isConfirmation=" + isConfirmation +
        ", packet=" + getPacket() +
        ", error=" + error +
        '}';
  }


  public enum Type {
    DL_CONNECT,
    DL_DISCONNECT,
    DL_DATA,
    DL_UNIT_DATA,
    DL_ERROR;
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
}
