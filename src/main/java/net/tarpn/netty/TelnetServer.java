package net.tarpn.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.local.LocalAddress;
import io.netty.channel.local.LocalChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.oio.OioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import net.tarpn.config.PortConfig;
import net.tarpn.config.impl.PortConfigImpl;
import net.tarpn.netty.app.SysopApplicationHandler;
import net.tarpn.netty.serial.SerialChannel;
import net.tarpn.packet.impl.ax25.AX25Call;
import org.apache.commons.configuration2.MapConfiguration;

import java.util.HashMap;
import java.util.Map;

public class TelnetServer {
    public void run(PortConfig portConfig) throws InterruptedException {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        DataLinkHandler dataLinkHandler = new DataLinkHandler();

        try {

            {
                EventLoopGroup group = new OioEventLoopGroup();
                Bootstrap b = new Bootstrap();
                b.group(group)
                    .channel(SerialChannel.class)
                    .handler(new ChannelInitializer<SerialChannel>() {
                        @Override
                        protected void initChannel(SerialChannel ch) {
                            ch.pipeline()
                                .addLast(new KISSFrameEncoder())
                                .addLast(new KISSFrameDecoder())
                                .addLast(new AX25PacketEncoder())
                                .addLast(new AX25PacketDecoder())
                                .addLast(new AX25Handler(portConfig))
                                .addLast(dataLinkHandler);
                            ch.attr(Attributes.PortNumber).set(portConfig.getPortNumber());
                            ch.attr(Attributes.NodeCall).set(portConfig.getNodeCall());
                        }
                    });
                b.connect(new SerialChannel.SerialDeviceAddress("/tmp/vmodem0")).sync();
            }

            {
                ServerBootstrap b = new ServerBootstrap();
                b.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .option(ChannelOption.SO_BACKLOG, 100)
                        .handler(new LoggingHandler(LogLevel.INFO))
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ChannelPipeline p = ch.pipeline();
                                p.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
                                p.addLast(new StringDecoder());
                                p.addLast(new StringEncoder());
                                p.addLast(new IdleStateHandler(30, 60, 0));
                                p.addLast(new SysopApplicationHandler(dataLinkHandler::pushL2Event));
                            }
                        });
                b.bind(9999).sync().channel().closeFuture().sync();
            }
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        AX25Call call1 = AX25Call.create("K4DBZ", 2);

        Map<String, Object> config1 = new HashMap<>();
        config1.put("node.call", call1.toString());
        PortConfig portConfig1 = new PortConfigImpl(0, new MapConfiguration(config1));


        TelnetServer server = new TelnetServer();
        server.run(portConfig1);
    }
}
