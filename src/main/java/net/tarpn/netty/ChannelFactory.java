package net.tarpn.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.oio.OioEventLoopGroup;
import net.tarpn.config.PortConfig;
import net.tarpn.netty.i2c.I2CChannel;
import net.tarpn.netty.serial.SerialChannel;
import net.tarpn.netty.serial.SerialChannelOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChannelFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ChannelFactory.class);

    private final OioEventLoopGroup oioGroup = new OioEventLoopGroup();

    public <C extends Channel> ChannelFuture createChannel(
            PortConfig portConfig, ChannelInitializer<C> channelInitializer) {
        switch (portConfig.getPortType().toLowerCase()) {
            case "serial":
                return createSerialPort(portConfig, channelInitializer);
            case "i2c":
                return createI2CPort(portConfig, channelInitializer);
            default:
                throw new RuntimeException("Unknown port type " + portConfig.getPortType());
        }
    }

    ChannelFuture createI2CPort(PortConfig portConfig, ChannelInitializer<?> channelInitializer) {
        Bootstrap b = new Bootstrap();
        b.group(oioGroup)
                .channel(I2CChannel.class)
                // TODO I2C options
                .handler(channelInitializer);
        return b.connect(new I2CChannel.I2CDeviceAddress(
                portConfig.getI2CBus(), portConfig.getI2CDeviceAddress()));
    }

    ChannelFuture createSerialPort(PortConfig portConfig, ChannelInitializer<?> channelInitializer) {
        Bootstrap b = new Bootstrap();
        b.group(oioGroup)
                .channel(SerialChannel.class)
                .option(SerialChannelOption.WAIT_TIME_MS, 3000)
                .option(SerialChannelOption.BAUD_RATE, portConfig.getSerialSpeed())
                .option(SerialChannelOption.READ_TIMEOUT_MS, 100)
                .handler(channelInitializer);
        return b.connect(new SerialChannel.SerialDeviceAddress(portConfig.getSerialDevice()));
    }
}
