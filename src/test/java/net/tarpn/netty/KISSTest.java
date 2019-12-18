package net.tarpn.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.embedded.EmbeddedChannel;
import net.tarpn.config.PortConfig;
import net.tarpn.config.impl.PortConfigImpl;
import net.tarpn.frame.impl.KISS;
import net.tarpn.frame.impl.KISSFrame;
import net.tarpn.util.Util;
import org.apache.commons.configuration2.MapConfiguration;
import org.junit.Test;

import java.util.Collections;
import java.util.Queue;

import static net.tarpn.netty.KISSFrameDecoderTest.fromInts;
import static org.junit.Assert.*;

public class KISSTest {
    static PortConfig portConfig() {
        return new PortConfigImpl(0, new MapConfiguration(Collections.emptyMap()));
    }
    @Test
    public void testEncodeDecode() {
        KISSFrame frame = new KISSFrame(1, KISS.Command.Data, Util.ascii("Test data"));
        EmbeddedChannel ch = new EmbeddedChannel(new KISSFrameEncoder(portConfig()), new KISSFrameDecoder(portConfig()));
        ch.writeOutbound(frame);
        ch.writeInbound((ByteBuf)ch.readOutbound());
        Queue<Object> inbound = ch.inboundMessages();
        assertEquals(inbound.size(), 1);
        KISSFrame out = (KISSFrame)inbound.poll();
        assertNotNull(out);
        assertEquals(frame.getPort(), out.getPort());
        assertEquals(frame.getKissCommand(), out.getKissCommand());
        assertArrayEquals(frame.getData(), out.getData());
    }

    @Test
    public void testDecodeEncode() {
        int[] ints = new int[] {
            192, 0, 150, 104, 136, 132, 180, 64, 228, 150, 104, 136, 132, 180, 64, 115, 17, 192
        };
        ByteBuf buf = fromInts(ints);
        EmbeddedChannel ch = new EmbeddedChannel(new KISSFrameEncoder(portConfig()), new KISSFrameDecoder(portConfig()));
        ch.writeInbound(buf);
        ch.writeOutbound((KISSFrame)ch.readInbound());

        ByteBuf out = ch.readOutbound();
        byte[] buffer = new byte[18];
        out.readBytes(buffer);

        ByteBuf orig = fromInts(ints);
        ByteBuf test = fromInts(buffer);
        assertTrue(ByteBufUtil.equals(orig, test));
    }
}
