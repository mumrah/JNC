package net.tarpn.packet.impl;

import java.util.function.Consumer;
import net.tarpn.message.Message;
import net.tarpn.message.MessageReader;
import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketHandler;

public class MessageReadingPacketHandler implements PacketHandler {

  private final MessageReader messageReader;
  private final Consumer<Message> messageConsumer;

  public MessageReadingPacketHandler(
      MessageReader messageReader,
      Consumer<Message> messageConsumer) {
    this.messageReader = messageReader;
    this.messageConsumer = messageConsumer;
  }

  @Override
  public void onPacket(Packet packet) {
    messageReader.accept(packet, messageConsumer);
  }
}
