package net.tarpn.netty.ax25;


import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.netty.app.Application;

import java.io.IOException;
import java.util.function.Consumer;
import java.util.function.Supplier;

public interface Multiplexer {
    PortChannel bind(AX25Address localAddress, Consumer<DataLinkPrimitive> dataLinkConsumer) throws IOException;

    DataLinkChannel connect(AX25Address localAddress, AX25Address remoteAddress,
                            Consumer<DataLinkPrimitive> l3Consumer) throws IOException;

    void listen(AX25Address listenAddress, Supplier<Application> onAccept) throws IOException;
}
