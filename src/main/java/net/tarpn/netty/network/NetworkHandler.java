package net.tarpn.netty.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.TypeParameterMatcher;
import net.tarpn.netty.app.Application;
import net.tarpn.netty.app.Context;
import net.tarpn.netty.app.SysopApplicationHandler;
import net.tarpn.network.netrom.NetworkPrimitive;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class NetworkHandler extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkHandler.class);

    private final Map<Integer, Consumer<NetworkPrimitive>> openCircuits = new HashMap<>();
    private final NetworkLinkMultiplexer networkMultiplexer;

    public NetworkHandler() {
        networkMultiplexer = new NetworkLinkMultiplexer();
        networkMultiplexer.listen(AX25Call.create("KM4NKU-1"), () -> (context, message) -> {
            context.write("BBS");
        });
        networkMultiplexer.listen(AX25Call.create("KM4NKU-9"), () -> (context, message) -> {
            context.write("CHAT");
        });
        // ECHO
        networkMultiplexer.listen(AX25Call.create("KM4NKU-2"), () -> (context, message) -> {
            context.write("You said: " + Util.ascii(message));
        });

    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        TypeParameterMatcher matcher = TypeParameterMatcher.get(NetworkPrimitive.class);
        if (matcher.match(msg)) {
            NetworkPrimitive np = (NetworkPrimitive) msg;
            // Check if the packet is for us, if so, send to the proper circuit
            if (networkMultiplexer.forUs(((NetworkPrimitive) msg).getLocalCall())) {
                networkMultiplexer.demux(np, ctx::writeAndFlush);
            } else {
                // forwarding should have already happened, so... reject it?
                LOG.warn("Unhandled L4 message: " + msg);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }
    /**
     * Outside of here:
     *
     * multiplexer.listen("KM4NKU-2", sysop);
     * multiplexer.listen("KM4NKU-9", chat);
     * multiplexer.listen("KM4NKU-1", bbs);
     *
     * Then in here:
     *
     * networkChannel = multiplexer.bind("KM4NKU-2", nl -> ctx.writeAndFlush(nl));
     * networkChannel.write(nl);
     */

}
