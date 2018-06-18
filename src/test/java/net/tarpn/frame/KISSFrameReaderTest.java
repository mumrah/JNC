package net.tarpn.frame;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.tarpn.frame.impl.KISS.Command;
import net.tarpn.frame.impl.KISS.Protocol;
import net.tarpn.frame.impl.KISSFrameReader;
import org.junit.Assert;
import org.junit.Test;

public class KISSFrameReaderTest {

  @Test
  public void testSync() {
    KISSFrameReader frameReader = new KISSFrameReader(99);
    List<Frame> framesOut = new ArrayList<>();
    frameReader.accept(Protocol.FEND.asInt(), framesOut::add);
    frameReader.accept(Protocol.FEND.asInt(), framesOut::add);
    frameReader.accept(Protocol.FEND.asInt(), framesOut::add);
    frameReader.accept(Command.Data.asInt(), framesOut::add);
    frameReader.accept((int) 'T', framesOut::add);
    frameReader.accept((int) 'E', framesOut::add);
    frameReader.accept((int) 'S', framesOut::add);
    frameReader.accept((int) 'T', framesOut::add);
    frameReader.accept(Protocol.FEND.asInt(), framesOut::add);
    Assert.assertEquals(framesOut.size(), 1);
    Assert.assertArrayEquals(framesOut.get(0).getData(), "TEST".getBytes(StandardCharsets.US_ASCII));
  }

  @Test
  public void testBasicMessage() {
    String message = "Hello, world!";

    KISSFrameReader frameReader = new KISSFrameReader(99);
    List<Frame> framesOut = new ArrayList<>();
    frameReader.accept(Protocol.FEND.asInt(), framesOut::add);
    frameReader.accept(Command.Data.asInt(), framesOut::add);
    for (byte b : message.getBytes(StandardCharsets.US_ASCII)) {
      frameReader.accept(b, framesOut::add);
    }
    frameReader.accept(Protocol.FEND.asInt(), framesOut::add);
    Assert.assertEquals(framesOut.size(), 1);
    Assert.assertArrayEquals(framesOut.get(0).getData(), message.getBytes(StandardCharsets.US_ASCII));
  }
}