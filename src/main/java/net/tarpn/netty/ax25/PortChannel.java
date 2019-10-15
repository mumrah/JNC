package net.tarpn.netty.ax25;

import net.tarpn.datalink.DataLinkPrimitive;

public interface PortChannel {
    int getPort();

    void write(DataLinkPrimitive dl);

    void close();
}
