package net.tarpn.frame.impl;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import net.tarpn.frame.Frame;
import net.tarpn.frame.FrameHandler;
import net.tarpn.frame.FrameReader;

public class SimpleFrameReader implements FrameReader {

  private final int port;
  private StringBuffer stringBuffer = new StringBuffer();

  public SimpleFrameReader(int port) {
    this.port = port;
  }

  @Override
  public void accept(int b, Consumer<Frame> frameHandler) {
    stringBuffer.append((char)b);
    String possibleFrame = stringBuffer.toString();
    if(possibleFrame.endsWith("\n") || possibleFrame.endsWith("\r")) {
      String trimmed = possibleFrame.trim();
      if(!trimmed.isEmpty()) {
        frameHandler.accept(new Frame(port, trimmed.getBytes(StandardCharsets.UTF_8)));
      }
      stringBuffer = new StringBuffer();
    }
  }
}
