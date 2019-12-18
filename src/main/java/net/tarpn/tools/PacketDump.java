package net.tarpn.tools;

import io.netty.channel.*;
import io.netty.util.internal.TypeParameterMatcher;
import net.tarpn.config.Configs;
import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.netty.AX25PacketDecoder;
import net.tarpn.netty.Attributes;
import net.tarpn.netty.ChannelFactory;
import net.tarpn.netty.KISSFrameDecoder;
import net.tarpn.netty.network.NetRomDecoder;
import net.tarpn.network.netrom.NetRomNodes;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.IFrame;
import net.tarpn.packet.impl.ax25.UIFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class PacketDump {

    private static final Logger LOG = LoggerFactory.getLogger(PacketDump.class);

    private static class L2Interceptor extends ChannelInboundHandlerAdapter {

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            TypeParameterMatcher matcher = TypeParameterMatcher.get(AX25Packet.class);
            if (matcher.match(msg)) {
                AX25Packet packet = (AX25Packet ) msg;
                if (((AX25Packet) msg).getDestCall().equals(AX25Call.create("NODES", 0))) {
                    LOG.info("Got NODES: " + NetRomNodes.read(((AX25Packet.HasInfo) packet).getInfo()));
                    return;
                }
                if (packet.getFrameType().equals(AX25Packet.FrameType.UI)) {
                    DataLinkPrimitive dl = DataLinkPrimitive.newUnitDataIndication((UIFrame) packet);
                    ctx.fireChannelRead(dl);
                } else if (packet.getFrameType().equals(AX25Packet.FrameType.I)) {
                    DataLinkPrimitive dl = DataLinkPrimitive.newDataIndication((IFrame) packet);
                    ctx.fireChannelRead(dl);
                }
            } else {
                ctx.fireChannelRead(msg);
            }
        }
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("io.netty.noUnsafe", "true");

        Configs configs = Configs.read(args[0]);
        ChannelFactory channelFactory = new ChannelFactory();

        List<ChannelFuture> futures = new ArrayList<>();
        configs.getPortConfigs().forEach((portNum, portConfig) -> {
            ChannelInitializer<?> readOnlyPipeline = new ChannelInitializer<Channel>() {
                @Override
                protected void initChannel(Channel ch) {
                    ch.pipeline()
                            .addLast(new KISSFrameDecoder(portConfig))
                            .addLast(new AX25PacketDecoder())
                            .addLast(new L2Interceptor())
                            .addLast(new NetRomDecoder());
                    ch.attr(Attributes.PortNumber).set(portConfig.getPortNumber());
                    ch.attr(Attributes.NodeCall).set(portConfig.getNodeCall());
                }
            };
            futures.add(channelFactory.createChannel(portConfig, readOnlyPipeline));
        });

        futures.forEach(channelFuture -> {
            try {
                channelFuture.sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        futures.stream().findFirst().get().channel().closeFuture().sync();

    }
}
