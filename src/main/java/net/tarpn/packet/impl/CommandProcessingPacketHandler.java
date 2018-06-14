package net.tarpn.packet.impl;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import net.tarpn.CommandAction;
import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketHandler;

public class CommandProcessingPacketHandler implements PacketHandler {

  private final Consumer<CommandAction> actionConsumer;

  public CommandProcessingPacketHandler(Consumer<CommandAction> actionConsumer) {
    this.actionConsumer = actionConsumer;
  }

  @Override
  public void onPacket(Packet packet) {
    String message = new String(packet.getMessage(), StandardCharsets.UTF_8);
    if(message.equalsIgnoreCase("PING")) {
      actionConsumer.accept(requestConsumer -> {
        requestConsumer.accept(new SimplePacket("ME", packet.getSource(), "PONG"));
      });
    }
  }
}
