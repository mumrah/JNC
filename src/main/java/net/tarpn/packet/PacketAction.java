package net.tarpn.packet;

import java.util.function.Consumer;

public interface PacketAction {
  void run(Consumer<Packet> packetConsumer);
}
