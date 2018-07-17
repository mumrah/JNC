package net.tarpn.frame.impl;

import java.nio.ByteBuffer;
import java.util.function.Consumer;
import net.tarpn.util.Util;
import net.tarpn.frame.Frame;
import net.tarpn.frame.FrameReader;
import net.tarpn.frame.impl.KISS.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Read KISS framed data one byte at a time.
 *
 * TODO add read timeout ?
 */
public class KISSFrameReader implements FrameReader {

  public static final Integer L2_TIMEOUT = 100;

  private static final Logger LOG = LoggerFactory.getLogger(KISSFrameReader.class);

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
    LOG.debug("KISS READ " + b + "\t" + String.format("%02X", b) + "\t" + Character.toString((char)b));
    // Clean up our state if we haven't heard anything in a while
    long currentTime = System.currentTimeMillis();
    if(currentTime - frameTime > L2_TIMEOUT) {
      LOG.warn("KISS timeout");
      reset();
    }
    frameTime = System.currentTimeMillis();

    if(Protocol.FEND.equalsTo(b)) {
      // either beginning or end of frame (or sync)
      if(inFrame) {
        // end of a new frame
        int len = buffer.position();
        if(len > 0) {
          buffer.position(0);
          byte[] frame = new byte[len];
          buffer.get(frame, 0, len);
          try {
            frameHandler.accept(new KISSFrame(port, kissCommand, frame));
          } catch (Throwable t) {
            LOG.warn("Failed to parse packet\n" + Util.toHexDump(frame));
          }
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
