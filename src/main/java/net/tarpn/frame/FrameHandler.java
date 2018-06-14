package net.tarpn.frame;

public interface FrameHandler {
  void onFrame(String portName, byte[] frame);
}
