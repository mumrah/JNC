package net.tarpn.netty.app;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.packet.impl.ax25.AX25Call;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Date;
import java.util.function.Consumer;

public class SysopApplicationHandler extends SimpleChannelInboundHandler<String> {

    private static final Logger LOG = LoggerFactory.getLogger(SysopApplicationHandler.class);

    private final Consumer<DataLinkPrimitive> l2Consumer;

    public SysopApplicationHandler(Consumer<DataLinkPrimitive> l2Consumer) {
        this.l2Consumer = l2Consumer;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // Send greeting for a new connection.
        ctx.write("Welcome to " + InetAddress.getLocalHost().getHostName() + "!\r\n");
        ctx.write("It is " + new Date() + " now.\r\n");
        ctx.flush();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOG.error("We had an error", cause);
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        LOG.info("SYSOP: " + msg + " from " + ctx.channel().remoteAddress());
        if (msg.equals("?")) {
            // help
            ctx.write("HELP: here is some help output.\r\n");
            ctx.flush();
        } else if (msg.startsWith("C")) {

            // connect
            DataLinkPrimitive connectReq = DataLinkPrimitive.newConnectRequest(
                    AX25Call.create("KN4ORB-2"), AX25Call.create("K4DBZ-2"));
            l2Consumer.accept(connectReq);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                ctx.writeAndFlush("Bye!\r\n");
                ctx.close();
            } else if (e.state() == IdleState.WRITER_IDLE) {
                ctx.writeAndFlush("KeepAlive\r\n");
            }
        }
    }
}
