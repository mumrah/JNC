package net.tarpn.frame.impl;

import java.nio.charset.StandardCharsets;
import net.tarpn.frame.FrameHandler;

public class ConsoleFrameHandler implements FrameHandler {

  @Override
  public void onFrame(byte[] frame) {
    System.err.println(">>> " + new String(frame, StandardCharsets.UTF_8));
  }
}
