package net.tarpn.message.impl;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import net.tarpn.Configuration;
import net.tarpn.message.Message;
import net.tarpn.message.MessageHandler;

public class CommandMessageHandler implements MessageHandler {

  @Override
  public void onMessage(Message inboundMessage, Consumer<Message> outboundMessageSink) {
    String message = new String(inboundMessage.getMessage(), StandardCharsets.US_ASCII);
    if (message.equalsIgnoreCase("PING")) {
      outboundMessageSink.accept(new Message(
          Configuration.getOwnAddress(),
          inboundMessage.getSource(),
          "PONG".getBytes(StandardCharsets.US_ASCII)
      ));
    }
  }
}
