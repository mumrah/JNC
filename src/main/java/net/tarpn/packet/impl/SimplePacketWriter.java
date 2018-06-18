package net.tarpn.packet.impl;

import java.util.function.Consumer;
import net.tarpn.Configuration;
import net.tarpn.frame.Frame;
import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketWriter;

public class SimplePacketWriter implements PacketWriter {

  @Override
  public void accept(Packet packet, Consumer<Frame> frameSink) {
    int size = Configuration.getMaxFrameLength();
    byte[][] toSend = chunkArray(packet.getMessage(), size);
    for (int i = 0; i < toSend.length; i++) {
      frameSink.accept(new Frame(packet.getPort(), toSend[i]));
    }
  }

  public static byte[][] chunkArray(byte[] array, int chunkSize) {
    int numOfChunks = (int) Math.ceil((double) array.length / chunkSize);
    byte[][] output = new byte[numOfChunks][];

    for (int i = 0; i < numOfChunks; ++i) {
      int start = i * chunkSize;
      int length = Math.min(array.length - start, chunkSize);

      byte[] temp = new byte[length];
      System.arraycopy(array, start, temp, 0, length);
      output[i] = temp;
    }

    return output;
  }
}
