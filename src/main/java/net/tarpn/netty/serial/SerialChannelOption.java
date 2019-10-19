package net.tarpn.netty.serial;

import io.netty.channel.ChannelOption;

public class SerialChannelOption<T> extends ChannelOption<T> {
    public static final ChannelOption<Integer> BAUD_RATE = valueOf(SerialChannelOption.class, "BAUD_RATE");
    public static final ChannelOption<Integer> WAIT_TIME_MS = valueOf(SerialChannelOption.class, "WAIT_TIME_MS");
    public static final ChannelOption<Integer> READ_TIMEOUT_MS = valueOf(SerialChannelOption.class, "READ_TIMEOUT_MS");

    protected SerialChannelOption(String name) {
        super(name);
    }
}
