package net.tarpn.packet;

import java.util.Date;

public class PacketRequest {
  private final Packet packet;
  private final String dataPort;
  private final Date received;

  public PacketRequest(Packet packet, String dataPort, Date received) {
    this.packet = packet;
    this.dataPort = dataPort;
    this.received = received;
  }

  public Packet getPacket() {
    return packet;
  }

  public String getDataPort() {
    return dataPort;
  }

  public Date getReceived() {
    return received;
  }

  @Override
  public String toString() {
    return "PacketRequest{" +
        "packet=" + packet +
        ", dataPort='" + dataPort + '\'' +
        ", received=" + received +
        '}';
  }
}
