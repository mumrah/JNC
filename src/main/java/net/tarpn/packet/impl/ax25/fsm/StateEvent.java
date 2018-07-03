package net.tarpn.packet.impl.ax25.fsm;


import net.tarpn.packet.impl.ax25.AX25Packet;

public class StateEvent {

  private final String sessionId;
  private final AX25Packet packet;
  private final Type type;

  public StateEvent(AX25Packet packet, Type type) {
    this(packet.getSource(), packet, type);
  }

  public StateEvent(String sessionId, AX25Packet packet, Type type) {
    this.sessionId = sessionId;
    this.packet = packet;
    this.type = type;
  }

  public String getSessionId() {
    return sessionId;
  }

  public AX25Packet getPacket() {
    return packet;
  }

  public Type getType() {
    return type;
  }

  enum Type {
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
    //DL_CONNECT,
    //DL_DISCONNECT,
    //DL_DATA,
    DL_UNIT_DATA,
    //DL_FLOW_OFF,
    //DL_FLOW_ON,
  }
}
