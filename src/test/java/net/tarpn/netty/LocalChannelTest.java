package net.tarpn.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalEventLoopGroup;
import org.junit.Test;

public class LocalChannelTest {
    @Test
    public void test() throws Exception {
        Bootstrap b = new Bootstrap();
        b.group(new DefaultEventLoopGroup());
        b.channel(LocalChannel.class);
        b.handler(new ChannelInitializer<LocalChannel>() {
            @Override
            protected void initChannel(LocalChannel ch) throws Exception {
                ch.pipeline().addLast(new SimpleChannelInboundHandler<String>() {
                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, String msg) throws Exception {
                        System.err.println("Got " + msg);
                    }
                });
            }
        });

        Channel ch1 = b.connect(new LocalAddress("TEST")).sync().channel();
        Channel ch2 = b.connect(new LocalAddress("TEST")).sync().channel();
        ch1.write("ok");
        ch2.write("ko");
    }
}
