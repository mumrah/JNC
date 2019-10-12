package net.tarpn.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

public class DataLinkHandler extends MessageToMessageDecoder<DataLinkPrimitive> {

    private static final Logger LOG = LoggerFactory.getLogger(DataLinkHandler.class);

    Queue<DataLinkPrimitive> l2Events = new ArrayDeque<>();

    public void pushL2Event(DataLinkPrimitive l2Event) {
        l2Events.offer(l2Event);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, DataLinkPrimitive msg, List<Object> out) throws Exception {

        LOG.info("DL: " + msg);
        switch (msg.getType()) {
            case DL_CONNECT:
                //ctx.channel().eventLoop().register();
                // TODO attach a new ax25 client channel when we get a connect
                // then we can pass I and UI to the child handler
                //Thread.sleep(100);
                ctx.write(DataLinkPrimitive.newDataRequest(msg.getRemoteCall(), msg.getLocalCall(),
                        AX25Packet.Protocol.NO_LAYER3, Util.ascii("Welcome to " + ctx.channel().attr(Attributes.NodeCall))));
                break;
            case DL_DISCONNECT:
                break;
            case DL_DATA:
                LOG.info("Data: " + msg.getLinkInfo().getInfoAsASCII());
                Thread.sleep(100);
                ctx.write(DataLinkPrimitive.newDataRequest(msg.getRemoteCall(), msg.getLocalCall(),
                        msg.getLinkInfo().getProtocol(), Util.ascii("You said: " + msg.getLinkInfo().getInfoAsASCII())));
                //out.add(msg.getLinkInfo().getInfoAsASCII());
                break;
            case DL_UNIT_DATA:
                LOG.info("Unit Data: " + msg.getLinkInfo().getInfoAsASCII());
                //out.add(msg.getLinkInfo().getInfoAsASCII());
                //ctx.write(DataLinkPrimitive.newUnitDataRequest(msg.getRemoteCall(), msg.getLocalCall(),
                //        msg.getLinkInfo().getProtocol(), msg.getLinkInfo().getInfo()));
                break;
            case DL_ERROR:
                break;
        }
    }

    // ctx.channel().eventLoop().register(ch)

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isActive() && ctx.channel().isRegistered()) {
            // channelActive() event has been fired already, which means this.channelActive() will
            // not be invoked. We have to initialize here instead.
            initialize(ctx);
        } else {
            // channelActive() event has not been fired yet.  this.channelActive() will be invoked
            // and initialization will occur there.
        }
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        // Initialize early if channel is active already.
        if (ctx.channel().isActive()) {
            initialize(ctx);
        }
        super.channelRegistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // This method will be invoked only if this handler was added
        // before channelActive() event is fired.  If a user adds this handler
        // after the channelActive() event, initialize() will be called by beforeAdd().
        initialize(ctx);
        super.channelActive(ctx);
    }

    private void initialize(ChannelHandlerContext ctx) {
        ctx.executor().scheduleAtFixedRate(() -> {
            DataLinkPrimitive event;
            while ((event = l2Events.poll()) != null) {
                ctx.write(event);
            }
            ctx.flush();
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

}
