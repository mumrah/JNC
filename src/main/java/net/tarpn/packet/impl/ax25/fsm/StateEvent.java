package net.tarpn.packet.impl.ax25.fsm;


import net.tarpn.packet.impl.ax25.AX25Packet;

public class StateEvent {

  private final String sessionId;
  private final AX25Packet packet;
  private final Type type;

  private StateEvent(String sessionId, AX25Packet packet, Type type) {
    this.sessionId = sessionId;
    this.packet = packet;
    this.type = type;
  }

  /**
   * Create an event for an incoming packet. The session ID will be the source callsign of this packet
   */
  public static StateEvent createIncomingEvent(AX25Packet packet, Type type) {
    return new StateEvent(packet.getSource(), packet, type);
  }

  /**
   * Create an event for an outgoing packet. The session ID will be the destination callsign of this packet
   */
  public static StateEvent createOutgoingEvent(AX25Packet packet, Type type) {
    return new StateEvent(packet.getDestination(), packet, type);
  }

  /**
   * Create a UI event. A static session ID of "UI" will be used.
   */
  public static StateEvent createUIEvent(AX25Packet packet, Type type) {
    return new StateEvent("UI", packet, type);
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
    //DL_CONNECT,
    //DL_DISCONNECT,
    DL_DATA,
    DL_UNIT_DATA,
    //DL_FLOW_OFF,
    //DL_FLOW_ON,
  }
}
