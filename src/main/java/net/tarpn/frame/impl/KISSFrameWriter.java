package net.tarpn.frame.impl;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import net.tarpn.frame.Frame;
import net.tarpn.frame.FrameWriter;
import net.tarpn.frame.impl.KISS.Command;
import net.tarpn.frame.impl.KISS.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KISSFrameWriter implements FrameWriter {

  private static final Logger LOG = LoggerFactory.getLogger(KISSFrameWriter.class);

  private final ByteBuffer buffer = ByteBuffer.allocate(1024);

  @Override
  public void accept(Frame frame, Consumer<byte[]> dataSink) {
    final Command kissCommand;
    if(frame instanceof KISSFrame) {
      kissCommand = ((KISSFrame) frame).getKissCommand();
    } else {
      kissCommand = Command.Data;
    }
    buffer.put(Protocol.FEND.asByte());
    buffer.put(kissCommand.asByte());
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

    Consumer<byte[]> logger = bytes -> {
      for(int i=0; i<bytes.length; i++) {
        byte b = bytes[i];
        LOG.debug("KISS WRITE " + b + "\t" + String.format("%02X", b) + "\t" + Character.toString((char)b));
      }
    };
    //dataSink.accept(out);
    //try {
    //  HexDump.dump(frame.getData(), 0, System.err, 0);
    //} catch (IOException e) {
    //  e.printStackTrace();
    //}

    logger.andThen(dataSink).accept(out);
  }
}
