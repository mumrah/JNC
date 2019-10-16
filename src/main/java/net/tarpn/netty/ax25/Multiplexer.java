package net.tarpn.netty.ax25;

import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.packet.impl.ax25.AX25Call;

import java.io.IOException;
import java.util.function.Consumer;

public interface Multiplexer {
    PortChannel bind(int port, Consumer<DataLinkPrimitive> dataLinkConsumer) throws IOException;

    DataLinkChannel connect(int port, AX25Call remoteCall, Consumer<DataLinkPrimitive> l3Consumer) throws IOException;
}
