package net.tarpn.message.impl;

import java.util.function.Consumer;
import net.tarpn.Configuration;
import net.tarpn.message.Message;
import net.tarpn.message.MessageHandler;

public class ForwardingMessageHandler implements MessageHandler {

  @Override
  public void onMessage(Message inboundMessage, Consumer<Message> outboundMessageSink) {
    if(!inboundMessage.getDestination().equalsIgnoreCase(Configuration.getOwnAddress())) {
      outboundMessageSink.accept(inboundMessage);
    }
  }
}
