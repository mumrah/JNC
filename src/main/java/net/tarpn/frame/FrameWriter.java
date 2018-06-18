package net.tarpn.frame;

import java.util.function.Consumer;

public interface FrameWriter {
  void accept(Frame frame, Consumer<byte[]> dataSink);
}
