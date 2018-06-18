package net.tarpn.packet;

import java.util.function.Consumer;
import net.tarpn.frame.Frame;

public interface PacketReader {
  void accept(Frame frame, Consumer<Packet> packetHandler);
}
