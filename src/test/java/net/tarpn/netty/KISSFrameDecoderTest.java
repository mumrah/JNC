package net.tarpn.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import net.tarpn.frame.impl.KISS;
import net.tarpn.frame.impl.KISSFrame;
import org.junit.Test;

import static org.junit.Assert.*;

public class KISSFrameDecoderTest {
    @Test
    public void testFullFrame() {
        EmbeddedChannel ch = new EmbeddedChannel(new KISSFrameDecoder());
        ByteBuf buf = fromInts(192, 0, 150, 104, 136, 132, 180, 64, 228, 150, 104, 136, 132, 180, 64, 115, 17, 192);
        ch.writeInbound(buf);
        assertEquals(ch.inboundMessages().size(), 1);
        KISSFrame frame = (KISSFrame) ch.inboundMessages().poll();
        assertNotNull(frame);
        assertEquals(frame.getKissCommand(), KISS.Command.Data);
        assertEquals(frame.getPort(), 0);
    }

    @Test
    public void testPartialFrame() {
        EmbeddedChannel ch = new EmbeddedChannel(new KISSFrameDecoder());
        ByteBuf buf = fromInts(192, 0, 150, 104, 136, 132, 180, 64, 228, 150, 104, 136, 132);
        ch.writeInbound(buf);
        assertEquals(ch.inboundMessages().size(), 0);

        buf = fromInts(180, 64, 115, 17, 192);
        ch.writeInbound(buf);

        assertEquals(ch.inboundMessages().size(), 1);
        KISSFrame frame = (KISSFrame) ch.inboundMessages().poll();
        assertNotNull(frame);
        assertEquals(frame.getKissCommand(), KISS.Command.Data);
        assertEquals(frame.getPort(), 0);
    }

    @Test
    public void testFENDSequence() {
        EmbeddedChannel ch = new EmbeddedChannel(new KISSFrameDecoder());
        ByteBuf buf = fromInts(192, 192, 192, 0, 150, 104, 136, 132, 180, 64, 228, 150, 104, 136, 132, 180, 64, 115, 17, 192);
        ch.writeInbound(buf);
        assertEquals(ch.inboundMessages().size(), 1);
        KISSFrame frame = (KISSFrame) ch.inboundMessages().poll();
        assertNotNull(frame);
        assertEquals(frame.getKissCommand(), KISS.Command.Data);
        assertEquals(frame.getPort(), 0);
    }

    @Test
    public void testMultipleFrames() {
        EmbeddedChannel ch = new EmbeddedChannel(new KISSFrameDecoder());
        ByteBuf buf = fromInts(192, 192, 192, 0, 150, 104, 136, 132, 180, 64, 228, 150, 104, 136, 132, 180,
                64, 115, 17, 192, 192, 192, 192, 0, 150, 104, 136, 132, 180, 64, 228, 150, 104, 136, 132, 180,
                64, 115, 17, 192);
        ch.writeInbound(buf);
        assertEquals(ch.inboundMessages().size(), 2);
        KISSFrame frame = (KISSFrame) ch.inboundMessages().poll();
        assertNotNull(frame);
        assertEquals(frame.getKissCommand(), KISS.Command.Data);
        assertEquals(frame.getPort(), 0);

        frame = (KISSFrame) ch.inboundMessages().poll();
        assertNotNull(frame);
        assertEquals(frame.getKissCommand(), KISS.Command.Data);
        assertEquals(frame.getPort(), 0);
    }


    static ByteBuf fromInts(byte... values) {
        ByteBuf buf = Unpooled.buffer(values.length);
        for (int i = 0; i < values.length; i++) {
            buf.writeByte(values[i]);
        }
        return buf;
    }

    static ByteBuf fromInts(int... values) {
        ByteBuf buf = Unpooled.buffer(values.length);
        for (int i = 0; i < values.length; i++) {
            buf.writeByte(values[i]);
        }
        return buf;
    }
}
