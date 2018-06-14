package net.tarpn.frame;

import java.util.function.Consumer;

public interface FrameWriter {
  void accept(byte[] bytes, Consumer<byte[]> dataSink);
}
