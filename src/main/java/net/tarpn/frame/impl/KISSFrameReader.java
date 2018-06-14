package net.tarpn.frame.impl;

import static net.tarpn.frame.impl.KISSFrameWriter.*;

import java.nio.ByteBuffer;
import net.tarpn.frame.FrameHandler;
import net.tarpn.frame.FrameReader;

public class KISSFrameReader implements FrameReader {



  public static final int CMD_UNKNOWN     = 0xFE;
  public static final int CMD_DATA        = 0x00;
  public static final int CMD_TXDELAY     = 0x01;
  public static final int CMD_P           = 0x02;
  public static final int CMD_SLOTTIME    = 0x03;
  public static final int CMD_TXTAIL      = 0x04;
  public static final int CMD_FULLDUPLEX  = 0x05;
  public static final int CMD_SETHARDWARE = 0x06;
  public static final int CMD_RETURN      = 0xFF;


  private volatile boolean inFrame = false;
  private volatile boolean inEscape = false;
  private volatile int kissCommand = CMD_UNKNOWN;
  private volatile int hdlcPort = -1;

  private final ByteBuffer buffer = ByteBuffer.allocate(1024);
  private final String portName;

  public KISSFrameReader(String portName) {
    this.portName = portName;
  }

  @Override
  public void accept(int b, FrameHandler frameHandler) {
    if(inFrame && b == FEND) {
      // end of a new frame
      // TODO consume continuous FENDs
      inFrame = false;
      int len = buffer.position();
      buffer.position(0);
      byte[] frame = new byte[len];
      buffer.get(frame, 0, len);
      reset();
      frameHandler.onFrame(portName, frame);
    } else if(b == FEND) {
      // beginning of a new frame
      kissCommand = CMD_UNKNOWN;
      inFrame = true;
    } else {
      if(buffer.position() == 0 && kissCommand == CMD_UNKNOWN) {
        // first byte is command
        hdlcPort = b & 0xF0;
        kissCommand = b & 0x0F;
      } else if(kissCommand == CMD_DATA) {
        if(b == FESC) {
          inEscape = true;
        } else {
          if(inEscape) {
            if (b == TFEND) b = FEND;
            if (b == TFESC) b = FESC;
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
    kissCommand = CMD_UNKNOWN;
    hdlcPort = -1;
  }
}
