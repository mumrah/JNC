package net.tarpn.packet;

import java.util.function.Consumer;
import net.tarpn.frame.Frame;

public interface PacketWriter {
  void accept(Packet packet, Consumer<Frame> frameSink);
}
