package net.tarpn.frame.impl;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import net.tarpn.frame.FrameWriter;

public class SimpleFrameWriter implements FrameWriter {
  Consumer<byte[]> dataSink;
  ByteBuffer buffer = ByteBuffer.allocate(1024);

  public SimpleFrameWriter(Consumer<byte[]> dataSink) {
    this.dataSink = dataSink;
  }

  @Override
  public void accept(byte b) {
    buffer.put(b);
  }

  @Override
  public void flush() {
    buffer.put((byte)'\n');
    int len = buffer.position();
    byte[] out = new byte[len];
    buffer.position(0);
    buffer.get(out, 0, len);
    buffer.clear();
    dataSink.accept(out);
  }
}
