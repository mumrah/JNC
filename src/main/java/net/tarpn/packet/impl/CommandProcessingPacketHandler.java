package net.tarpn.packet.impl;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketAction;
import net.tarpn.packet.PacketRequest;
import net.tarpn.packet.PacketRequestHandler;

public class CommandProcessingPacketHandler implements PacketRequestHandler {

  private final Consumer<PacketAction> actionConsumer;

  public CommandProcessingPacketHandler(Consumer<PacketAction> actionConsumer) {
    this.actionConsumer = actionConsumer;
  }

  @Override
  public void onRequest(PacketRequest request) {
    String message = new String(request.getPacket().getMessage(), StandardCharsets.UTF_8);
    if(message.equalsIgnoreCase("PING")) {
      actionConsumer.accept(
          packetConsumer -> packetConsumer.accept(new SimplePacket("ME", request.getPacket().getSource(), "PONG")));
    }
  }
}
