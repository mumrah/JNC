package net.tarpn.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.TypeParameterMatcher;
import net.tarpn.config.PortConfig;
import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.netty.ax25.AX25Address;
import net.tarpn.netty.ax25.Multiplexer;
import net.tarpn.netty.ax25.PortChannel;
import net.tarpn.packet.impl.ax25.AX25Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataLinkHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(DataLinkHandler.class);

    private final Multiplexer multiplexer;
    private final PortConfig portConfig;

    private PortChannel portChannel;

    public DataLinkHandler(PortConfig portConfig, Multiplexer multiplexer) {
        this.multiplexer = multiplexer;
        this.portConfig = portConfig;
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        AX25Address bindAddress = new AX25Address(portConfig.getPortNumber(), portConfig.getNodeCall());
        portChannel = multiplexer.bind(bindAddress, datalinkPrimitive -> {
            ctx.writeAndFlush(datalinkPrimitive);
        });
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        if (portChannel != null) {
            portChannel.close();
            portChannel = null;
        }
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (portChannel != null) {
            portChannel.close();
            portChannel = null;
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.warn("Caught exception, closing channel: " + cause.getMessage());
        if (portChannel != null) {
            portChannel.close();
            portChannel = null;
        }
        ctx.close();
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        TypeParameterMatcher matcher = TypeParameterMatcher.get(DataLinkPrimitive.class);
        if (matcher.match(msg)) {
            DataLinkPrimitive dl = (DataLinkPrimitive) msg;
            if (dl.getLinkInfo() != null && dl.getLinkInfo().getProtocol().equals(AX25Packet.Protocol.NETROM)) {
                // Pass up to NET/ROM handler
                //ctx.pipeline().addLast(new NetRomDecoder());
                ctx.fireChannelRead(msg);
            } else {
                // Pass to Node Control handler, if it's configured
                portChannel.write(dl);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }
}
