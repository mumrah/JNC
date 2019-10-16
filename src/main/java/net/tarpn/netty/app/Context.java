package net.tarpn.netty.app;

public interface Context {
    void write(String msg);

    void flush();

    void close();

    String remoteAddress();
}
