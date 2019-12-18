package net.tarpn.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.embedded.EmbeddedChannel;
import net.tarpn.config.PortConfig;
import net.tarpn.config.impl.PortConfigImpl;
import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.SFrame;
import org.apache.commons.configuration2.MapConfiguration;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static net.tarpn.netty.KISSFrameDecoderTest.fromInts;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AX25PacketDecoderTest {
    private static final Logger LOG = LoggerFactory.getLogger(AX25PacketDecoderTest.class);


    static PortConfig portConfig() {
        return new PortConfigImpl(0, new MapConfiguration(Collections.emptyMap()));
    }

    @Test
    public void test() {
        EmbeddedChannel ch = new EmbeddedChannel(
                new KISSFrameDecoder(portConfig()),
                new AX25PacketDecoder()
        );
        ByteBuf buf = fromInts(192, 0, 150, 104, 136, 132, 180, 64, 228, 150, 104, 136, 132, 180, 64, 115, 17, 192);
        ch.writeInbound(buf);
        assertEquals(ch.inboundMessages().size(), 1);
        AX25Packet packet = (AX25Packet) ch.inboundMessages().poll();
        assertNotNull(packet);
        assertEquals(packet.getSource(), "K4DBZ-9");
        assertEquals(packet.getDestination(), "K4DBZ-2");
        assertEquals(packet.getFrameType(), AX25Packet.FrameType.S);
        assertEquals(((SFrame) packet).getControlType(), AX25Packet.SupervisoryFrame.ControlType.RR);
    }

    @Test
    public void testLocal() throws Exception {
        AX25Call call1 = AX25Call.create("K4DBZ", 2);
        AX25Call call2 = AX25Call.create("KN4ORB", 2);

        Map<String, Object> config1 = new HashMap<>();
        config1.put("node.call", call1.toString());
        PortConfig portConfig1 = new PortConfigImpl(0, new MapConfiguration(config1));


        Map<String, Object> config2 = new HashMap<>();
        config2.put("node.call", call2.toString());
        PortConfig portConfig2 = new PortConfigImpl(0, new MapConfiguration(config2));

        Node server1 = new Node();
        //server1.run(portConfig1);

        Node server2 = new Node();
        //server1.run(portConfig2);
    }

    @Test
    public void test2() throws InterruptedException {
        AX25Call call1 = AX25Call.create("K4DBZ", 2);
        AX25Call call2 = AX25Call.create("KN4ORB", 2);

        Map<String, Object> config1 = new HashMap<>();
        config1.put("node.call", call1.toString());
        PortConfig portConfig1 = new PortConfigImpl(0, new MapConfiguration(config1));


        Map<String, Object> config2 = new HashMap<>();
        config2.put("node.call", call2.toString());
        PortConfig portConfig2 = new PortConfigImpl(0, new MapConfiguration(config2));


        EmbeddedChannel ch1 = new EmbeddedChannel();
        ch1.pipeline()
            .addLast(new KISSFrameEncoder(portConfig()))
            .addLast(new KISSFrameDecoder(portConfig()))
            .addLast(new AX25PacketEncoder())
            .addLast(new AX25PacketDecoder())
            //.addLast(new AX25Handler(portConfig1))
            .addLast(new DataLinkHandler(null, null));
        ch1.attr(Attributes.PortNumber).set(1);
        ch1.attr(Attributes.NodeCall).set(call1);



        EmbeddedChannel ch2 = new EmbeddedChannel();
        ch2.pipeline()
            .addLast(new KISSFrameEncoder(portConfig()))
            .addLast(new KISSFrameDecoder(portConfig()))
            .addLast(new AX25PacketEncoder())
            .addLast(new AX25PacketDecoder())
            //.addLast(new AX25Handler(portConfig2))
            .addLast(new DataLinkHandler(null, null));
        ch2.attr(Attributes.PortNumber).set(2);
        ch2.attr(Attributes.NodeCall).set(call2);


        //ByteBuf buf = fromInts(192, 0, 150, 104, 136, 132, 180, 64, 228, 150, 104, 136, 132, 180, 64, 115, 17, 192);
        //ch.writeInbound(buf);

        // 1 -> SABM -> 2 -> UA -> 1

        /*
        //async test

        ch1.writeOutbound(DataLinkPrimitive.newConnectRequest(call2, call1)); // Send SABM
        for (int i = 0; i < 100; i++) {
            ch1.outboundMessages().forEach(obj -> {
                ByteBuf buf = (ByteBuf) obj;
                LOG.info("<<< " + ByteBufUtil.hexDump(buf));
                buf.readBytes(buf.readableBytes());
            });
            Thread.sleep(200);
        }
        */



        LOG.info(call1.toString());
        ch1.writeOutbound(DataLinkPrimitive.newConnectRequest(call2, call1)); // Send SABM
        for (int i = 0; i < 100; i++) {
            LOG.info(call2.toString());
            ch1.outboundMessages().forEach(obj -> {
                ByteBuf buf = (ByteBuf) obj;
                //LOG.info("CH1 " + ByteBufUtil.hexDump(buf.copy()));
                ch2.writeOneInbound(buf);
            });
            Thread.sleep(100);
            LOG.info(call1.toString());
            ch2.outboundMessages().forEach(obj -> {
                ByteBuf buf = (ByteBuf) obj;
                //LOG.info("CH2 " + ByteBufUtil.hexDump(buf.copy()));
                ch1.writeOneInbound(buf);
            });
            Thread.sleep(100);
        }

    }

    @Test
    public void testLive() {
        int[] d = new int[]{
                0x92, 0x88, 0x40, 0x40, 0x40, 0x40, 0xe0, 0x96,
                0x9c, 0x68, 0x9e, 0xa4, 0x84, 0x65, 0x03, 0xf0,
                0x4b, 0x4e, 0x34, 0x4f, 0x52, 0x42, 0x2d, 0x32,
                0x20, 0x20, 0x41, 0x41, 0x52, 0x4f, 0x4e, 0x4c,
                0x20, 0x20, 0x20, 0x68, 0x74, 0x74, 0x70, 0x3a,
                0x2f, 0x2f, 0x74, 0x61, 0x72, 0x70, 0x6e, 0x2e,
                0x6e, 0x65, 0x74, 0x20, 0x2e, 0x2e, 0x2e, 0x2e,
                0x2e, 0x2e, 0x2e, 0x2e, 0x2e, 0x2e, 0x2e, 0x2e,
                0x2e, 0x2e, 0x2e, 0x2e, 0x2e, 0x2e, 0x2e, 0x2e,
                0x2e, 0x2e, 0x2e, 0x2e, 0x2e, 0x2e, 0x2e, 0x2e,
                0x2e, 0x2e, 0x2e, 0x2e, 0x2e, 0x2e, 0x2e, 0x2e,
                0x2e, 0x2e, 0x2e, 0x2e, 0x2e, 0x2e, 0x2e, 0x2e,
                0x2e, 0x2e, 0x2e, 0x2e, 0x2e, 0x2e, 0x2e, 0x2e,
        };

        int crc = 0;
        for (int i = 0; i < d.length; i++ ) {
            crc ^= d[i];
        }

        System.err.println(crc);
        //System.err.println(CRCCCITT.compute_crc2(d));

        //System.err.println(CRCCCITT.getCRC16CCITT("", 0x1021, 0x0000, false));

    }
    /*
    $00: 92 88 40 40 40 40 e0 96	......à.
    $08: 9c 68 9e a4 84 65 03 f0	.h...e.ð
    $10: 4b 4e 34 4f 52 42 2d 32	KN4ORB.2
    $18: 20 20 41 41 52 4f 4e 4c	..AARONL
    $20: 20 20 20 68 74 74 70 3a	...http.
    $28: 2f 2f 74 61 72 70 6e 2e	..tarpn.
    $30: 6e 65 74 20 2e 2e 2e 2e	net.....
    $38: 2e 2e 2e 2e 2e 2e 2e 2e	........
    $40: 2e 2e 2e 2e 2e 2e 2e 2e	........
    $48: 2e 2e 2e 2e 2e 2e 2e 2e	........
    $50: 2e 2e 2e 2e 2e 2e 2e 2e	........
    $58: 2e 2e 2e 2e 2e 2e 2e 2e	........
    $60: 2e 2e 2e 2e 2e 2e 2e 2e	........
    $68: 0d d9	.Ù

    0x9B 0xB0 39856
     */
}
