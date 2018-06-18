package net.tarpn.message;

import java.util.function.Consumer;
import net.tarpn.packet.Packet;

public interface MessageWriter {
  void accept(Message message, Consumer<Packet> packetSink);
}
