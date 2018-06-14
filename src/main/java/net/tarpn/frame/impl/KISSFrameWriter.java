package net.tarpn.frame.impl;

import static net.tarpn.frame.impl.KISSFrameReader.CMD_DATA;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import net.tarpn.frame.FrameWriter;

public class KISSFrameWriter implements FrameWriter {

  public static final int FEND  = 0xC0;
  public static final int FESC  = 0xDB;
  public static final int TFEND = 0xDC;
  public static final int TFESC = 0xDD;

  private final ByteBuffer buffer = ByteBuffer.allocate(1024);

  @Override
  public void accept(byte[] bytes, Consumer<byte[]> dataSink) {
    buffer.put((byte)FEND);
    buffer.put((byte)CMD_DATA);
    for(int i=0; i<bytes.length; i++) {
      int b = bytes[i];
      if(b == FEND) {
        buffer.put((byte)FESC);
        buffer.put((byte)TFEND);
      } else if(b == FESC) {
        buffer.put((byte)FESC);
        buffer.put((byte)TFESC);
      } else {
        buffer.put((byte)b);
      }
    }
    buffer.put((byte)FEND);

    int len = buffer.position();
    byte[] out = new byte[len];
    buffer.position(0);
    buffer.get(out, 0, len);
    buffer.clear();
    dataSink.accept(out);
  }
}
