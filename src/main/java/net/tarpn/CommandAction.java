package net.tarpn;

import java.util.function.Consumer;
import net.tarpn.packet.Packet;

public interface CommandAction {
  void run(Consumer<Packet> packetConsumer);
}
