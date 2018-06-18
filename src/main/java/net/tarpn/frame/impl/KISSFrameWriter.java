package net.tarpn.frame.impl;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import net.tarpn.frame.Frame;
import net.tarpn.frame.FrameWriter;
import net.tarpn.frame.impl.KISS.Protocol;

public class KISSFrameWriter implements FrameWriter {

  private final ByteBuffer buffer = ByteBuffer.allocate(1024);

  @Override
  public void accept(Frame frame, Consumer<byte[]> dataSink) {
    KISSFrame kissFrame = (KISSFrame) frame;
    buffer.put(Protocol.FEND.asByte());
    buffer.put(kissFrame.getKissCommand().asByte());
    for(int i=0; i<frame.getData().length; i++) {
      int b = frame.getData()[i];
      if(Protocol.FEND.equalsTo(b)) {
        buffer.put(Protocol.FESC.asByte());
        buffer.put(Protocol.TFEND.asByte());
      } else if(Protocol.FESC.equalsTo(b)) {
        buffer.put(Protocol.FESC.asByte());
        buffer.put(Protocol.TFESC.asByte());
      } else {
        buffer.put((byte)b);
      }
    }
    buffer.put(Protocol.FEND.asByte());

    int len = buffer.position();
    byte[] out = new byte[len];
    buffer.position(0);
    buffer.get(out, 0, len);
    buffer.clear();
    dataSink.accept(out);
  }
}
