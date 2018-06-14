package net.tarpn.frame;

public interface FrameWriter {
  void accept(byte b);
  void flush();
}
