package net.tarpn.netty.ax25;


public interface Multiplexer2 {
    void listen(AX25Address listenAddress, Runnable onAccept);
}
