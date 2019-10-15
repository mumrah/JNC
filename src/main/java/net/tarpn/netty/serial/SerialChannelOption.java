package net.tarpn.netty.serial;

import io.netty.channel.ChannelOption;

public class SerialChannelOption<T> extends ChannelOption<T> {
    public static final ChannelOption<Integer> BAUD_RATE = valueOf(SerialChannelOption.class, "BAUD_RATE");
    public static final ChannelOption<Integer> WAIT_TIME = valueOf(SerialChannelOption.class, "WAIT_TIME");
    public static final ChannelOption<Integer> READ_TIMEOUT = valueOf(SerialChannelOption.class, "READ_TIMEOUT");

    protected SerialChannelOption(String name) {
        super(name);
    }
}
