package net.tarpn.packet.impl.ax25;


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
  }

  class ErrorIndicationDataLinkEvent extends BaseDataLinkEvent {
    private final ErrorType error;

    public ErrorIndicationDataLinkEvent(ErrorType error, String sessionId, Type type) {
      super(sessionId, type);
      this.error = error;
    }

    public ErrorType getError() {
      return error;
    }

    enum ErrorType {
      A("F=1 received but P=1 not outstanding"),
      B("Unexpected DM with F=1 in states 3, 4 or 5"),
      C("Unexpected UA in states 3, 4 or 5"),
      D("UA received without F=1 when SABM or DISC was sent P=1");
      // TODO others

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

