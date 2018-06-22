package net.tarpn.packet.impl.ax25;

import net.tarpn.packet.Packet;

public class FakePacket implements Packet {

  private final String id;
  private final byte[] payload;

  private FakePacket(String id, byte[] payload) {
    this.id = id;
    this.payload = payload;
  }

  /**
   * Create a "fake" packet used internally
   * @param id An arbitrary identifier, possibly useful for logging or debugging
   * @param payload The data of the packet
   * @return
   */
  public static FakePacket create(String id, byte[] payload) {
    return new FakePacket(id, payload);
  }

  @Override
  public byte[] getPayload() {
    return payload;
  }

  @Override
  public String getSource() {
    return id;
  }

  @Override
  public String getDestination() {
    return id;
  }
}
