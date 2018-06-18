package net.tarpn.message;

import java.util.function.Consumer;

public interface MessageHandler {
  void onMessage(Message inboundMessage, Consumer<Message> outboundMessageSink);
}
