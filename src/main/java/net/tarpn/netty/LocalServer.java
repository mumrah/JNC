package net.tarpn.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import net.tarpn.config.PortConfig;

public class LocalServer {
    public ChannelFuture run(PortConfig portConfig) throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
               .channel(LocalServerChannel.class)
               .handler(new LoggingHandler(LogLevel.INFO))
               .childHandler(new ChannelInitializer<LocalChannel>() {
                   @Override
                   protected void initChannel(LocalChannel ch) {
                       ch.pipeline()
                           .addLast(new KISSFrameEncoder())
                           .addLast(new KISSFrameDecoder())
                           .addLast(new AX25PacketEncoder())
                           .addLast(new AX25PacketDecoder())
                           .addLast(new AX25Handler(portConfig))
                           .addLast(new DataLinkHandler());
                       ch.attr(Attributes.PortNumber).set(portConfig.getPortNumber());
                       ch.attr(Attributes.NodeCall).set(portConfig.getNodeCall());
                   }
               });

              return b.bind(new LocalAddress(String.format("port-%02d", portConfig.getPortNumber())))
                      .sync()
                      .channel()
                      .closeFuture();
          } finally {
              bossGroup.shutdownGracefully();
              workerGroup.shutdownGracefully();

        }
    }
}
