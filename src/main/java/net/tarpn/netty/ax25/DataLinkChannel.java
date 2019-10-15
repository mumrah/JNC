package net.tarpn.netty.ax25;

import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.packet.impl.ax25.AX25Call;

public interface DataLinkChannel {
    AX25Call getRemoteCall();

    AX25Call getLocalCall();

    void write(DataLinkPrimitive dl);

    void close();
}
