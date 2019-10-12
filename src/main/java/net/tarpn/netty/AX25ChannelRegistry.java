package net.tarpn.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelException;
import io.netty.util.internal.PlatformDependent;
import io.netty.util.internal.StringUtil;
import net.tarpn.packet.impl.ax25.AX25Call;

import java.net.SocketAddress;
import java.util.concurrent.ConcurrentMap;

public class AX25ChannelRegistry {
    private static final ConcurrentMap<AX25Call, Channel> boundChannels = PlatformDependent.newConcurrentHashMap();

}
