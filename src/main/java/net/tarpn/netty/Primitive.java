package net.tarpn.netty;

import net.tarpn.packet.impl.ax25.AX25Call;

public interface Primitive {
    AX25Call getRemoteCall();
}
