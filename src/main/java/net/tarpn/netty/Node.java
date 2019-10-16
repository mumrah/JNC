package net.tarpn.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
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
import net.tarpn.config.Configs;
import net.tarpn.config.PortConfig;
import net.tarpn.netty.app.ApplicationInboundHandlerAdaptor;
import net.tarpn.netty.app.SysopApplicationHandler;
import net.tarpn.netty.ax25.DataLinkMultiplexer;
import net.tarpn.netty.serial.SerialChannel;
import net.tarpn.netty.serial.SerialChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Node {

    private static final Logger LOG = LoggerFactory.getLogger(Node.class);

    private final OioEventLoopGroup oioGroup = new OioEventLoopGroup();
    private final EventLoopGroup nioGroup = new NioEventLoopGroup();

    private ChannelFuture createSerialPort(PortConfig portConfig, DataLinkMultiplexer multiplexer) {
        Bootstrap b = new Bootstrap();
        b.group(oioGroup)
                .channel(SerialChannel.class)
                .option(SerialChannelOption.WAIT_TIME, 3000)
                .handler(new ChannelInitializer<SerialChannel>() {
                    @Override
                    protected void initChannel(SerialChannel ch) {
                        ch.pipeline()
                                .addLast(new KISSFrameEncoder())
                                .addLast(new KISSFrameDecoder())
                                .addLast(new AX25PacketEncoder())
                                .addLast(new AX25PacketDecoder())
                                .addLast(new AX25StateHandler(portConfig, multiplexer));
                        ch.attr(Attributes.PortNumber).set(portConfig.getPortNumber());
                        ch.attr(Attributes.NodeCall).set(portConfig.getNodeCall());
                    }
                });
        return b.connect(new SerialChannel.SerialDeviceAddress(portConfig.getSerialDevice()));
    }

    private ChannelFuture createTelnetPort(Configs allConfigs, DataLinkMultiplexer multiplexer) {
        // Telnet server
        ServerBootstrap b = new ServerBootstrap();
        b.group(nioGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 100)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline p = ch.pipeline();
                        p.addLast(new DelimiterBasedFrameDecoder(8192, Delimiters.lineDelimiter()));
                        p.addLast(new StringDecoder());
                        p.addLast(new StringEncoder());
                        p.addLast(new IdleStateHandler(30, 60, 0));
                        p.addLast(new ApplicationInboundHandlerAdaptor(new SysopApplicationHandler(allConfigs, multiplexer)));
                    }
                });
        return b.bind(allConfigs.getNodeConfig().getInt("tcp.port"));
    }

    public static void main(String[] args) throws Exception {
        System.setProperty("io.netty.noUnsafe", "true");

        Configs configs = Configs.read(args[0]);

        List<ChannelFuture> futures = new ArrayList<>();

        Node node = new Node();
        {
            DataLinkMultiplexer multiplexer = new DataLinkMultiplexer();
            SysopApplicationHandler sysop = new SysopApplicationHandler(configs, multiplexer);
            configs.getPortConfigs().forEach((portNum, portConfig) -> {
                futures.add(node.createSerialPort(portConfig, multiplexer)
                        .addListener(future -> multiplexer.attach(portNum, sysop)
                ));
            });
            futures.add(node.createTelnetPort(configs, multiplexer));
        }

        futures.forEach(channelFuture -> {
            try {
                channelFuture.sync();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        futures.stream().findFirst().get().channel().closeFuture().sync();
    }
}
