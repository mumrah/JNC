package net.tarpn.packet.impl.ax25;


import net.tarpn.packet.impl.ax25.AX25Packet.HasInfo;

public interface DataLinkEvent {

  String getAX25SessionId();

  Type getType();

  enum Type {
    DL_CONNECT,
    DL_DISCONNECT,
    DL_DATA,
    DL_UNIT_DATA,
    DL_ERROR
  }

  class BaseDataLinkEvent implements DataLinkEvent {

    private final String sessionId;
    private final Type type;

    public BaseDataLinkEvent(String sessionId, Type type) {
      this.sessionId = sessionId;
      this.type = type;
    }

    @Override
    public String getAX25SessionId() {
      return sessionId;
    }

    @Override
    public Type getType() {
      return type;
    }

    @Override
    public String toString() {
      return "DataLinkEvent(" + sessionId +"){" +
          "type=" + type +
          '}';
    }
  }

  class DataIndicationDataLinkEvent extends BaseDataLinkEvent {
    private final AX25Packet packet;

    public DataIndicationDataLinkEvent(AX25Packet packet, String sessionId, Type type) {
      super(sessionId, type);
      this.packet = packet;
    }

    public AX25Packet getPacket() {
      return packet;
    }

    @Override
    public String toString() {
      return "DataLinkEvent(" + getAX25SessionId() +"){" +
          "type=" + getType() +
          ", data=" + ((HasInfo)getPacket()).getInfoAsASCII() +
          '}';
    }
  }

  class ErrorIndicationDataLinkEvent extends BaseDataLinkEvent {
    private final ErrorType error;

    public ErrorIndicationDataLinkEvent(ErrorType error, String sessionId) {
      super(sessionId, Type.DL_ERROR);
      this.error = error;
    }

    public ErrorType getError() {
      return error;
    }

    @Override
    public String toString() {
      return "DataLinkEvent(" + getAX25SessionId() +"){" +
          "type=" + getType() +
          ", error=" + getError().getMessage() +
          '}';
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
      V("No DL machines available to establish connection");

      private final String msg;

      ErrorType(String msg) {
        this.msg = msg;
      }

      public String getMessage() {
        return msg;
      }
    }
  }
}

