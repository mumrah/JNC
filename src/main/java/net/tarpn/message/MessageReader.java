package net.tarpn.message;

import java.util.function.Consumer;
import net.tarpn.packet.Packet;

public interface MessageReader {
  void accept(Packet packet, Consumer<Message> messageHandler);
}
