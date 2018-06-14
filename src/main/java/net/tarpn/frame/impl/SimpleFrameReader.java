package net.tarpn.frame.impl;

import java.nio.charset.StandardCharsets;
import net.tarpn.frame.FrameHandler;
import net.tarpn.frame.FrameReader;

public class SimpleFrameReader implements FrameReader {

  private final String portName;
  private StringBuffer stringBuffer = new StringBuffer();

  public SimpleFrameReader(String portName) {
    this.portName = portName;
  }

  @Override
  public void accept(int b, FrameHandler frameHandler) {
    stringBuffer.append((char)b);
    String possibleFrame = stringBuffer.toString();
    if(possibleFrame.endsWith("\n") || possibleFrame.endsWith("\r")) {
      String trimmed = possibleFrame.trim();
      if(!trimmed.isEmpty()) {
        frameHandler.onFrame(portName, trimmed.getBytes(StandardCharsets.UTF_8));
      }
      stringBuffer = new StringBuffer();
    }
  }
}
