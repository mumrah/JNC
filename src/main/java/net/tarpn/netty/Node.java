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
import net.tarpn.netty.app.Application;
import net.tarpn.netty.app.ApplicationInboundHandlerAdaptor;
import net.tarpn.netty.app.Context;
import net.tarpn.netty.app.SysopApplicationHandler;
import net.tarpn.netty.ax25.AX25Address;
import net.tarpn.netty.ax25.DataLinkMultiplexer;
import net.tarpn.netty.ax25.Multiplexer;
import net.tarpn.netty.i2c.I2CChannel;
import net.tarpn.netty.network.*;
import net.tarpn.netty.serial.SerialChannel;
import net.tarpn.netty.serial.SerialChannelOption;
import net.tarpn.network.netrom.NetRomNodes;
import net.tarpn.packet.impl.ax25.AX25Call;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Node {

    private static final Logger LOG = LoggerFactory.getLogger(Node.class);

    private final OioEventLoopGroup oioGroup = new OioEventLoopGroup();
    private final EventLoopGroup nioGroup = new NioEventLoopGroup();

    static class NodeChannelInitializer<T extends Channel> extends ChannelInitializer<T> {

        private final Configs configs;
        private final PortConfig portConfig;
        private final Multiplexer multiplexer;

        NodeChannelInitializer(Configs configs, PortConfig portConfig, Multiplexer multiplexer) {
            this.configs = configs;
            this.portConfig = portConfig;
            this.multiplexer = multiplexer;
        }

        @Override
        protected void initChannel(T ch) {
            ch.pipeline()
                    .addLast(new KISSFrameEncoder(portConfig))
                    .addLast(new KISSFrameDecoder(portConfig))
                    .addLast(new AX25PacketEncoder())
                    .addLast(new AX25PacketDecoder())
                    .addLast(new NetRomNodesHandler(portConfig))
                    .addLast(new AX25PacketFilter(portConfig))
                    .addLast(new AX25StateHandler(portConfig))
                    .addLast(new DataLinkHandler(portConfig, multiplexer))
                    .addLast(new NetRomDecoder())
                    .addLast(new NetRomStateHandler(configs, multiplexer))
                    .addLast(new NetworkHandler());
            ch.attr(Attributes.PortNumber).set(portConfig.getPortNumber());
            ch.attr(Attributes.NodeCall).set(portConfig.getNodeCall());
        }
    }

    private ChannelFuture createI2CPort(Configs configs, PortConfig portConfig, Multiplexer multiplexer) {
        Bootstrap b = new Bootstrap();
        b.group(oioGroup)
                .channel(I2CChannel.class)
                .handler(new NodeChannelInitializer<I2CChannel>(configs, portConfig, multiplexer));

        ChannelFuture channelFuture = b.connect(new I2CChannel.I2CDeviceAddress(
                portConfig.getI2CBus(), portConfig.getI2CDeviceAddress()));
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    AX25Address portAddress = new AX25Address(portConfig.getPortNumber(), portConfig.getNodeCall());
                    LOG.info("Initialized i2c port " + portAddress);
                    multiplexer.listen(portAddress, () -> new SysopApplicationHandler(configs, multiplexer));
                } else {
                    future.channel().close().sync();
                    Thread.sleep(1000);
                    LOG.info("Retrying failed port " + portConfig.getPortNumber());
                    b.connect(new I2CChannel.I2CDeviceAddress(
                            portConfig.getI2CBus(), portConfig.getI2CDeviceAddress())).addListener(this);
                }
            }
        });
        return channelFuture;

    }

    private ChannelFuture createSerialPort(Configs configs, PortConfig portConfig, Multiplexer multiplexer) {
        Bootstrap b = new Bootstrap();
        b.group(oioGroup)
                .channel(SerialChannel.class)
                .option(SerialChannelOption.WAIT_TIME_MS, 3000)
                .option(SerialChannelOption.BAUD_RATE, portConfig.getSerialSpeed())
                .option(SerialChannelOption.READ_TIMEOUT_MS, 100)
                .handler(new NodeChannelInitializer<SerialChannel>(configs, portConfig, multiplexer));

        ChannelFuture channelFuture = b.connect(new SerialChannel.SerialDeviceAddress(portConfig.getSerialDevice()));
        channelFuture.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    LOG.info("Initialized serial port " + portConfig.getPortNumber());

                    // Node system handler
                    multiplexer.listen(new AX25Address(portConfig.getPortNumber(), portConfig.getNodeCall()), () ->
                            new SysopApplicationHandler(configs, multiplexer)
                    );
                } else {
                    future.channel().close().sync();
                    Thread.sleep(1000);
                    LOG.info("Retrying failed port " + portConfig.getPortNumber());
                    b.connect(new SerialChannel.SerialDeviceAddress(portConfig.getSerialDevice())).addListener(this);
                }
            }
        });
        return channelFuture;
    }

    private ChannelFuture createTelnetPort(Configs allConfigs, Multiplexer multiplexer) {
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
            //DataLinkMultiplexer multiplexer = new DataLinkMultiplexer();
            Multiplexer multiplexer = new DataLinkMultiplexer();

            //SysopApplicationHandler sysop = new SysopApplicationHandler(configs, multiplexer);
            configs.getPortConfigs().forEach((portNum, portConfig) -> {
                switch (portConfig.getPortType().toLowerCase()) {
                    case "serial":
                        futures.add(node.createSerialPort(configs, portConfig, multiplexer));
                        break;
                    case "i2c":
                        futures.add(node.createI2CPort(configs, portConfig, multiplexer));
                        break;
                    default:
                        LOG.warn("Ignoring unknown port type " + portConfig.getPortType());
                        break;

                }
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
