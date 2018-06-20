package net.tarpn.packet.impl;

import java.util.function.Consumer;
import net.tarpn.frame.Frame;
import net.tarpn.frame.impl.KISS.Command;
import net.tarpn.frame.impl.KISSFrame;
import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketWriter;

public class AX25PacketWriter implements PacketWriter {
  @Override
  public void accept(Packet packet, Consumer<Frame> frameSink) {
    Frame frame = new KISSFrame(0, Command.Data, packet.getPayload());
    frameSink.accept(frame);
  }
}
