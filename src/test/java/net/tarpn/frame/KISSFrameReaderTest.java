package net.tarpn.frame;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import net.tarpn.util.Util;
import net.tarpn.frame.impl.KISS.Command;
import net.tarpn.frame.impl.KISS.Protocol;
import net.tarpn.frame.impl.KISSFrameReader;
import net.tarpn.packet.Packet;
import net.tarpn.packet.impl.AX25PacketReader;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.IFrame;
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

  @Test
  public void testBPQKISSSequence() {
    int[] seq = new int[]{192, 1, 0, 192, 192, 2, 255, 192, 192, 3, 2, 192, 192, 4, 0, 192, 192, 5, 0, 192};
    KISSFrameReader frameReader = new KISSFrameReader(99);
    List<Frame> framesOut = new ArrayList<>();

    IntStream.of(seq).forEach(i -> frameReader.accept(i, framesOut::add));
    Assert.assertEquals(framesOut.size(), 5);
  }

  @Test
  public void testConnectRequest() {
    /**
     * KISS READ 192	C0	À
     * KISS READ 0	00	 
     * KISS READ 168	A8	¨
     * KISS READ 130	82	
     * KISS READ 164	A4	¤
     * KISS READ 160	A0	 
     * KISS READ 156	9C	
     * KISS READ 64	40	@
     * KISS READ 224	E0	à
     * KISS READ 150	96	
     * KISS READ 104	68	h
     * KISS READ 136	88	
     * KISS READ 132	84	
     * KISS READ 180	B4	´
     * KISS READ 64	40	@
     * KISS READ 97	61	a
     * KISS READ 63	3F	?
     * KISS READ 192	C0	À
     */
    int[] seq = new int[]{
        192, 0, 168, 130, 164, 160, 156, 64, 224, 150, 104, 136, 132, 180, 64, 97, 63, 192
    };

    KISSFrameReader frameReader = new KISSFrameReader(99);
    List<Frame> framesOut = new ArrayList<>();

    IntStream.of(seq).forEach(i -> frameReader.accept(i, framesOut::add));
    Assert.assertEquals(framesOut.size(), 1);

    List<Packet> packetsOut = new ArrayList<>();
    AX25PacketReader reader = new AX25PacketReader();
    reader.accept(framesOut.get(0), packetsOut::add);


  }

  @Test
  public void testUnknown() throws IOException {
    // Is this an RR S frame? (0x11)
    // The PID makes no sense 0xF3
    /**
     KISS READ 192	C0	À
     KISS READ 0	00	 
     KISS READ 150	96	
     KISS READ 104	68	h
     KISS READ 136	88	
     KISS READ 132	84	
     KISS READ 180	B4	´
     KISS READ 64	40	@
     KISS READ 228	E4	ä
     KISS READ 150	96	
     KISS READ 104	68	h
     KISS READ 136	88	
     KISS READ 132	84	
     KISS READ 180	B4	´
     KISS READ 64	40	@
     KISS READ 115	73	s
     KISS READ 17	11	
     KISS READ 192	C0	À
     */

    int[] seq = new int[]{
      192, 0, 150, 104, 136, 132, 180, 64, 228, 150, 104, 136, 132, 180, 64, 115, 17, 192
    };

    int[] idseq = new int[]{
        192,0,146,136,64,64,64,64,224,150,104,136,132,180,64,115,3,240,84,101,114,114,
        101,115,116,114,105,97,108,32,65,109,97,116,101,117,114,32,82,97,100,105,111,
        32,80,97,99,107,101,116,32,78,101,116,119,111,114,107,32,110,111,100,101,32,
        68,65,86,73,68,49,32,32,111,112,32,105,115,32,107,52,100,98,122,32,13,192
    };

    int[] netrom = new int[]{
        192,0,150,104,136,132,180,64,228,150,104,136,132,180,64,99,0,207,150,104,136,132,
        180,64,98,150,104,136,132,180,64,4,7,1,132,0,0,1,2,150,104,136,132,180,64,96,150,
        104,136,132,180,64,98,180,0,192
    };


    KISSFrameReader frameReader = new KISSFrameReader(99);
    List<Frame> framesOut = new ArrayList<>();

    IntStream.of(netrom).forEach(i -> frameReader.accept(i, framesOut::add));
    Assert.assertEquals(framesOut.size(), 1);

    AX25PacketReader.parse(framesOut.get(0).getData());

    List<Packet> packetsOut = new ArrayList<>();
    AX25PacketReader reader = new AX25PacketReader();
    reader.accept(framesOut.get(0), packetsOut::add);

    IFrame iframe = (IFrame)packetsOut.get(0);
    System.err.println(iframe);
    Util.hexDump(iframe.getInfo(), System.err);

    ByteBuffer buffer = ByteBuffer.wrap(((IFrame)packetsOut.get(0)).getInfo());
    AX25Call dest = AX25Call.read(buffer);
    AX25Call src = AX25Call.read(buffer);
    byte ttl = buffer.get();
    byte circuitIdx = buffer.get();
    byte circuitId = buffer.get();
    byte txSeqNum = buffer.get();
    byte rxSeqNum = buffer.get();
    byte opcode = buffer.get();

    //UIFrame packet = (UIFrame) packetsOut.get(0);
    //UIFrame frame = UIFrame.create("K4DBZ-1", "K4DBZ-2", UFrame.ControlType.UI, AX25Packet.Protocol.NO_LAYER3, packet.getInfo());
  }
}