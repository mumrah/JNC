package net.tarpn.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import net.tarpn.config.PortConfig;
import net.tarpn.config.impl.PortConfigImpl;
import net.tarpn.frame.impl.KISS;
import net.tarpn.frame.impl.KISSFrame;
import org.apache.commons.configuration2.MapConfiguration;
import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.*;

public class KISSFrameDecoderTest {

    static PortConfig portConfig() {
        return new PortConfigImpl(0, new MapConfiguration(Collections.emptyMap()));
    }

    static PortConfig portConfig(Map<String, String> map) {
        return new PortConfigImpl(0, new MapConfiguration(map));
    }

    @Test
    public void testFullFrame() {
        EmbeddedChannel ch = new EmbeddedChannel(new KISSFrameDecoder(portConfig()));
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
        EmbeddedChannel ch = new EmbeddedChannel(new KISSFrameDecoder(portConfig()));
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
        EmbeddedChannel ch = new EmbeddedChannel(new KISSFrameDecoder(portConfig()));
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
        EmbeddedChannel ch = new EmbeddedChannel(new KISSFrameDecoder(portConfig()));
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

    @Test
    public void testChecksum() {
        EmbeddedChannel ch = new EmbeddedChannel(new KISSFrameDecoder(
                portConfig(Collections.singletonMap("kiss.flags", "CHECKSUM"))));
        /*
        $00: 96 68 88 84 b4 40 e4 96	.h....ä.
        $08: 9c 68 9e a4 84 65 b1 ea	.h...e.ê
         */
        ByteBuf buf = fromInts(
                0xc0, 0x96, 0x68, 0x88, 0x84, 0xb4, 0x40, 0xe4, 0x96,
                0x9c, 0x68, 0x9e, 0xa4, 0x84, 0x65, 0xb1, 0xea, 0xc0);
        ch.writeInbound(buf);
        assertEquals(ch.inboundMessages().size(), 1);
        KISSFrame frame = (KISSFrame) ch.inboundMessages().poll();
    }

    @Test
    public void testMissingFEND() {
        EmbeddedChannel ch = new EmbeddedChannel(new KISSFrameDecoder(portConfig()));
        ByteBuf buf = fromInts(192, 0, 150, 104, 136, 132, 180, 64, 228, 150, 104, 136, 132, 180,
                64, 115, 17, 192, /* missing FEND here */ 0, 150, 104, 136, 132, 180, 64, 228, 150, 104, 136, 132, 180,
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

    @Test
    public void testDataBeforeFEND() {
        EmbeddedChannel ch = new EmbeddedChannel(new KISSFrameDecoder(portConfig()));
        ByteBuf buf = fromInts(1, 2, 3, 4, 192, 0, 0, 0, 0, 0, 192);
        ch.writeInbound(buf);
        assertEquals(ch.inboundMessages().size(), 1);
        KISSFrame frame = (KISSFrame) ch.inboundMessages().poll();
        assertNotNull(frame);
        assertEquals(frame.getKissCommand(), KISS.Command.Data);
        assertEquals(frame.getPort(), 0);
        assertEquals(frame.getData().length, 4);
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
