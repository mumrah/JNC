package net.tarpn.frame.impl;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import net.tarpn.frame.FrameReader;

public class SimpleFrameReader implements FrameReader {

  private final Consumer<byte[]> frameConsumer;
  private StringBuffer stringBuffer = new StringBuffer();

  public SimpleFrameReader(Consumer<byte[]> frameConsumer) {
    this.frameConsumer = frameConsumer;
  }

  @Override
  public void accept(int b) {
    stringBuffer.append((char)b);
    String possibleFrame = stringBuffer.toString();
    if(possibleFrame.endsWith("\n") || possibleFrame.endsWith("\r")) {
      String trimmed = possibleFrame.trim();
      if(!trimmed.isEmpty()) {
        frameConsumer.accept(trimmed.getBytes(StandardCharsets.UTF_8));
      }
      stringBuffer = new StringBuffer();
    }
  }
}
