package net.tarpn.netty.app;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import net.tarpn.util.Util;

import java.net.SocketAddress;

/**
 * Adapt a channel handler to an application. Used for telnet
 */
public class ApplicationInboundHandlerAdaptor extends SimpleChannelInboundHandler<String> {

    private final Application application;

    public ApplicationInboundHandlerAdaptor(Application application) {
        this.application = application;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
        application.read(adaptContext(ctx), Util.ascii(msg));
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        application.onConnect(adaptContext(ctx));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        application.close(adaptContext(ctx));
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        application.onError(adaptContext(ctx), cause);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            if (e.state() == IdleState.READER_IDLE) {
                ctx.writeAndFlush("Bye!\r\n");
                application.close(adaptContext(ctx));
            } else if (e.state() == IdleState.WRITER_IDLE) {
                ctx.writeAndFlush("KeepAlive\r\n");
            }
        }
    }

    private Context adaptContext(ChannelHandlerContext ctx) {
        return new Context() {
            @Override
            public void write(byte[] msg) {
                ctx.write(Util.ascii(msg) + "\r\n$ ");
            }

            @Override
            public void flush() {
                ctx.flush();
            }

            @Override
            public void close() {
                ctx.close();
            }

            @Override
            public String remoteAddress() {
                return ctx.channel().remoteAddress().toString();
            }
        };
    }
}
