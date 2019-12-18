package net.tarpn.netty.network;

import net.tarpn.network.netrom.NetworkPrimitive;

public interface NetworkChannel {
    int getCircuitId();

    void write(NetworkPrimitive networkPrimitive);

    void close();
}
