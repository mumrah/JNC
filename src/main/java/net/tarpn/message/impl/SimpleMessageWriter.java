package net.tarpn.message.impl;

import java.util.function.Consumer;
import net.tarpn.message.Message;
import net.tarpn.message.MessageWriter;
import net.tarpn.packet.Packet;

public class SimpleMessageWriter implements MessageWriter {

  @Override
  public void accept(Message message, Consumer<Packet> packetSink) {
    // TODO add routing
    packetSink.accept(new Packet(0, message.getSource(), message.getDestination(), message.getMessage()));
  }
}
