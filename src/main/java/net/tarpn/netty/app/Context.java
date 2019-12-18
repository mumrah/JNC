package net.tarpn.netty.app;

import net.tarpn.util.Util;

/**
 * Callbacks to an {@link Application}'s channel
 */
public interface Context {
    /**
     * Write bytes to the channel
     * @param msg
     */
    void write(byte[] msg);

    /**
     * Write an ASCII string to the channel
     * @param msg
     */
    default void write(String msg) {
        write(Util.ascii(msg));
    }

    /**
     * Flush the channel
     */
    void flush();

    /**
     * Request to close the channel
     */
    void close();

    /**
     * Return the remote address of the channel
     * @return
     */
    String remoteAddress();
}
