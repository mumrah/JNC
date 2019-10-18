package net.tarpn.netty.ax25;

import net.tarpn.packet.impl.ax25.AX25Call;

import java.util.Objects;

public class AX25Address {
    public final int port;
    public final AX25Call call;

    public AX25Address(int port, AX25Call call) {
        this.port = port;
        this.call = call;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AX25Address that = (AX25Address) o;
        return port == that.port &&
                Objects.equals(call, that.call);
    }

    @Override
    public int hashCode() {
        return Objects.hash(port, call);
    }


    @Override
    public String toString() {
        return call + "," + port;
    }
}
