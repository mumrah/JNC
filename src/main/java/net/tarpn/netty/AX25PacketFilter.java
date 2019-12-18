package net.tarpn.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.TypeParameterMatcher;
import net.tarpn.config.PortConfig;
import net.tarpn.packet.impl.ax25.AX25Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Drop any L2 packets not addressed to this node
 */
public class AX25PacketFilter extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(AX25PacketFilter.class);

    private final PortConfig portConfig;

    public AX25PacketFilter(PortConfig portConfig) {
        this.portConfig = portConfig;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        TypeParameterMatcher matcher = TypeParameterMatcher.get(AX25Packet.class);
        if (matcher.match(msg)) {
            AX25Packet packet = (AX25Packet) msg;
            if (packet.getDestCall().callMatches(portConfig.getNodeCall())) {
                // Only pass through packets for us
                ctx.fireChannelRead(msg);
            } else {
                LOG.info("Dropping packet not for us: " + packet.toLogString(portConfig.getPortNumber()));
                // TODO record who we heard though (ID and CQ packets)
            }
        }
    }
}
