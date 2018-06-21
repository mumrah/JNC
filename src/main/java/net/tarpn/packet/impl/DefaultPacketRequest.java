package net.tarpn.packet.impl;

import java.util.function.Consumer;
import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketRequest;

public class DefaultPacketRequest implements PacketRequest {

  private volatile boolean done = false;
  private final int port;
  private final Packet incomingPacket;
  private final Consumer<Packet> responsePacketConsumer;

  public DefaultPacketRequest(
      int port,
      Packet incomingPacket,
      Consumer<Packet> responsePacketConsumer) {
    this.port = port;
    this.incomingPacket = incomingPacket;
    this.responsePacketConsumer = responsePacketConsumer;
  }

  @Override
  public int getPort() {
    return port;
  }

  @Override
  public Packet getPacket() {
    return incomingPacket;
  }

  @Override
  public void replyWith(Packet response) {
    //if(!done) {
      responsePacketConsumer.accept(response);
      done = true;
    //}
  }

  @Override
  public void abort() {
    done = true;
  }

  @Override
  public boolean shouldContinue() {
    return !done;
  }
}
