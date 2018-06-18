package net.tarpn.frame.impl;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import net.tarpn.frame.Frame;
import net.tarpn.frame.FrameReader;
import net.tarpn.frame.impl.KISS.Command;
import net.tarpn.frame.impl.KISS.Protocol;

/**
 * Read KISS framed data one byte at a time.
 *
 * TODO add read timeout
 * TODO add FEND sync
 */
public class KISSFrameReader implements FrameReader {

  private volatile boolean inFrame = false;
  private volatile boolean inEscape = false;
  private volatile KISS.Command kissCommand = KISS.Command.Unknown;
  private volatile int hdlcPort = -1;
  private volatile long frameTime = 0;

  private final ByteBuffer buffer = ByteBuffer.allocate(1024);
  private final int port;

  public KISSFrameReader(int port) {
    this.port = port;
  }

  @Override
  public void accept(int b, Consumer<Frame> frameHandler) {
    // Clean up our state if we haven't heard anything in a while
    long currentTime = System.currentTimeMillis();
    if(currentTime - frameTime > 4000) {
      reset();
    }
    frameTime = System.currentTimeMillis();

    if(Protocol.FEND.equalsTo(b)) {
      // either beginning or end of frame (or sync)
      if(inFrame) {
        // end of a new frame
        inFrame = false;
        int len = buffer.position();
        if(len > 0) {
          buffer.position(0);
          byte[] frame = new byte[len];
          buffer.get(frame, 0, len);
          frameHandler.accept(new KISSFrame(port, kissCommand, frame));
          reset();
          return;
        }
      } else {
        return;
      }
    } else {
      // got something other than FEND, we're in a frame!
      inFrame = true;
      if(buffer.position() == 0 && kissCommand == KISS.Command.Unknown) {
        // first byte is command
        hdlcPort = b & 0xF0;
        kissCommand = KISS.Command.fromInt(b & 0x0F);
        if(kissCommand.equals(Command.Unknown)) {
          // got an unknown command... possibly bad data.
          reset();
          return;
        }
      } else { //if(kissCommand == CMD_DATA) {
        if(Protocol.FESC.equalsTo(b)) {
          inEscape = true;
        } else {
          if(inEscape) {
            if (Protocol.TFEND.equalsTo(b)) b = Protocol.FEND.asByte();
            if (Protocol.TFESC.equalsTo(b)) b = Protocol.FESC.asByte();
            inEscape = false;
          }
          buffer.put((byte)b);
        }
      }
    }
  }

  private void reset() {
    buffer.clear();
    inEscape = false;
    inFrame = false;
    kissCommand = KISS.Command.Unknown;
    hdlcPort = -1;
    frameTime = 0;
  }
}
