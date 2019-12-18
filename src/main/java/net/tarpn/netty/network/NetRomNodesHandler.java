package net.tarpn.netty.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.TypeParameterMatcher;
import net.tarpn.config.PortConfig;
import net.tarpn.netty.AX25PacketFilter;
import net.tarpn.netty.Attributes;
import net.tarpn.network.netrom.NetRomNodes;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.UIFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class NetRomNodesHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(AX25PacketFilter.class);

    private static final AX25Call NODES_CALL = AX25Call.create("NODES", 0);

    private final PortConfig portConfig;

    public NetRomNodesHandler(PortConfig portConfig) {
        this.portConfig = portConfig;
    }


    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.executor().scheduleAtFixedRate(() -> {
            LOG.info("Sending our NODES");
            List<NetRomNodes.NodeDestination> destinations = new ArrayList<>();
            destinations.add(new NetRomNodes.NodeDestination(
                    AX25Call.create("KM4NKU-9"),
                    "ZNKU09",
                    AX25Call.create("KM4NKU-2"),
                    200
            ));
            NetRomNodes ourNodes = new NetRomNodes("DAVID2", destinations);
            UIFrame outgoing = UIFrame.create(NODES_CALL, AX25Call.create("KM4NKU-2"), AX25Packet.Protocol.NETROM,
                    NetRomNodes.write(ourNodes));
            ctx.writeAndFlush(outgoing);
        }, 3, 60, TimeUnit.SECONDS);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        TypeParameterMatcher matcher = TypeParameterMatcher.get(UIFrame.class);
        if (matcher.match(msg)) {
            UIFrame packet = (UIFrame) msg;
            if (packet.getProtocol().equals(AX25Packet.Protocol.NETROM) && packet.getDestCall().callMatches(NODES_CALL)) {
                NetRomNodes nodes = NetRomNodes.read(packet.getInfo());
                LOG.info("Got NET/ROM Nodes: " + nodes);
                ctx.pipeline().fireUserEventTriggered(
                    new NodesEvent(
                        ctx.channel().attr(Attributes.PortNumber).get(),
                        packet.getSourceCall(),
                        nodes));
            }
        }

        // pass everything else through
        ctx.fireChannelRead(msg);
    }

    public static class NodesEvent {
        final int heardOnPort;
        final AX25Call heardFrom;
        final NetRomNodes nodes;

        public NodesEvent(int heardOnPort, AX25Call heardFrom, NetRomNodes nodes) {
            this.heardOnPort = heardOnPort;
            this.heardFrom = heardFrom;
            this.nodes = nodes;
        }
    }
}
