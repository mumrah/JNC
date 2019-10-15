package net.tarpn.netty.app;

import java.net.SocketAddress;

public interface Application {
    void onConnect(Context context) throws Exception;

    void onDisconnect(Context context) throws Exception;

    void onError(Context context, Throwable t) throws Exception;

    void read(Context context, String message) throws Exception;
}
