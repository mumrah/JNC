package net.tarpn.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.TypeParameterMatcher;
import net.tarpn.config.PortConfig;
import net.tarpn.packet.impl.ax25.AX25Packet;

public class AX25PacketFilter extends ChannelInboundHandlerAdapter {

    private final PortConfig portConfig;

    public AX25PacketFilter(PortConfig portConfig) {
        this.portConfig = portConfig;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        TypeParameterMatcher matcher = TypeParameterMatcher.get(AX25Packet.class);
        if (matcher.match(msg)) {
            AX25Packet packet = (AX25Packet) msg;
            if (packet.getDestCall().callMatches(portConfig.getNodeCall())) {
                ctx.fireChannelRead(msg);
            } else {
                // drop it
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }
}
