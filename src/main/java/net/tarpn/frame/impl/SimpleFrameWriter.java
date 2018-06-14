package net.tarpn.frame.impl;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import net.tarpn.frame.FrameWriter;

public class SimpleFrameWriter implements FrameWriter {
  ByteBuffer buffer = ByteBuffer.allocate(1024);

  @Override
  public void accept(byte[] bytes, Consumer<byte[]> dataSink) {
    buffer.put(bytes);
    buffer.put((byte)'\n');
    int len = buffer.position();
    byte[] out = new byte[len];
    buffer.position(0);
    buffer.get(out, 0, len);
    buffer.clear();
    dataSink.accept(out);
  }
}
