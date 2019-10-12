package net.tarpn.datalink2;

import net.tarpn.packet.impl.ax25.AX25Call;

import java.nio.channels.ByteChannel;

public interface AX25Channel extends ByteChannel {

    /**
     * Connect this channel's data link to a remote callsign.
     * @param remote
     * @return
     */
    boolean connect(AX25Call remote);

    static AX25Channel open() {
        return null;
    }
}
