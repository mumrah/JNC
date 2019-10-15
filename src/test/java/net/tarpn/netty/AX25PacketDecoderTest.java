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

import java.util.HashMap;
import java.util.Map;

import static net.tarpn.netty.KISSFrameDecoderTest.fromInts;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AX25PacketDecoderTest {
    private static final Logger LOG = LoggerFactory.getLogger(AX25PacketDecoderTest.class);

    @Test
    public void test() {
        EmbeddedChannel ch = new EmbeddedChannel(
                new KISSFrameDecoder(),
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
        server1.run(portConfig1);

        Node server2 = new Node();
        server1.run(portConfig2);
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
            .addLast(new KISSFrameEncoder())
            .addLast(new KISSFrameDecoder())
            .addLast(new AX25PacketEncoder())
            .addLast(new AX25PacketDecoder())
            .addLast(new AX25Handler(portConfig1))
            .addLast(new DataLinkHandler(null));
        ch1.attr(Attributes.PortNumber).set(1);
        ch1.attr(Attributes.NodeCall).set(call1);



        EmbeddedChannel ch2 = new EmbeddedChannel();
        ch2.pipeline()
            .addLast(new KISSFrameEncoder())
            .addLast(new KISSFrameDecoder())
            .addLast(new AX25PacketEncoder())
            .addLast(new AX25PacketDecoder())
            .addLast(new AX25Handler(portConfig2))
            .addLast(new DataLinkHandler(null));
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
}
