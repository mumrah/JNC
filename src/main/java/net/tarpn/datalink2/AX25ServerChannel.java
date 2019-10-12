package net.tarpn.datalink2;

import net.tarpn.packet.impl.ax25.AX25Call;

import java.nio.channels.ByteChannel;

public interface DataLinkChannel extends ByteChannel {

    /**
     * Associate this channel with a local callsign. Any inbound packets addressed
     * to this callsign will be handled by this channel until it is closed.
     * @param local
     * @return
     */
    DataLinkChannel bind(AX25Call local);

    /**
     * Connect this channel's data link to a remote callsign.
     * @param remote
     * @return
     */
    boolean connect(AX25Call remote);

    /**
     * Return this channel's local callsign
     * @return
     */
    AX25Call getLocalCallsign();

    /**
     * Return this channel's remote callsign if it is connected to one.
     * @return
     */
    AX25Call getRemoteCallsign();

    static DataLinkChannel open() {
        return null;
    }
}
