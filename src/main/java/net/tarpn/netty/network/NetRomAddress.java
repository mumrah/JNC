package net.tarpn.netty.network;

import net.tarpn.packet.impl.ax25.AX25Call;

import java.util.Objects;

public class NetRomAddress {
    public final int circuitId;
    public final AX25Call call;

    public NetRomAddress(int circuitId, AX25Call call) {
        this.circuitId = circuitId;
        this.call = call;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NetRomAddress that = (NetRomAddress) o;
        return circuitId == that.circuitId &&
                Objects.equals(call, that.call);
    }

    @Override
    public int hashCode() {
        return Objects.hash(circuitId, call);
    }


    @Override
    public String toString() {
        return call + ":" + circuitId;
    }
}
