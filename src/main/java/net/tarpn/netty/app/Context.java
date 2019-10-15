package net.tarpn.netty.app;

import java.net.SocketAddress;

public interface Context {
    void write(String msg);

    void flush();

    void close();

    SocketAddress remoteAddress();
}
