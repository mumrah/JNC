package net.tarpn.network.netrom;

import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.util.Util;

public class NetworkPrimitive {
    private final AX25Call remoteCall;
    private final Type type;
    private final boolean isResponse;
    private final byte[] info;

    NetworkPrimitive(AX25Call remoteCall, Type type, boolean isResponse, byte[] info) {
        this.remoteCall = remoteCall;
        this.type = type;
        this.isResponse = isResponse;
        this.info = info;
    }

    public static NetworkPrimitive newConnect(AX25Call remoteCall) {
        return new NetworkPrimitive(remoteCall, Type.NL_CONNECT, false, new byte[0]);
    }

    public static NetworkPrimitive newConnectAck(AX25Call remoteCall) {
        return new NetworkPrimitive(remoteCall, Type.NL_CONNECT, true, new byte[0]);
    }

    public static NetworkPrimitive newConnectIndication(AX25Call remoteCall) {
        return new NetworkPrimitive(remoteCall, Type.NL_CONNECT, false, new byte[0]);
    }

    public static NetworkPrimitive newDisconnect(AX25Call remoteCall) {
        return new NetworkPrimitive(remoteCall, Type.NL_DISCONNECT, false, new byte[0]);
    }

    public static NetworkPrimitive newDisconnectAck(AX25Call remoteCall) {
        return new NetworkPrimitive(remoteCall, Type.NL_DISCONNECT, true, new byte[0]);
    }

    public static NetworkPrimitive newDisconnectIndication(AX25Call remoteCall) {
        return new NetworkPrimitive(remoteCall, Type.NL_DISCONNECT, false, new byte[0]);
    }

    public static NetworkPrimitive newData(AX25Call remoteCall, byte[] data) {
        return new NetworkPrimitive(remoteCall, Type.NL_INFO, false, data);
    }

    public static NetworkPrimitive newDataIndication(AX25Call remoteCall, byte[] data) {
        return new NetworkPrimitive(remoteCall, Type.NL_INFO, false, data);
    }

    public AX25Call getRemoteCall() {
        return remoteCall;
    }

    public Type getType() {
        return type;
    }

    public boolean isResponse() {
        return isResponse;
    }

    public byte[] getInfo() {
        return info;
    }

    @Override
    public String toString() {
        return "NetworkPrimitive{" +
                "remoteCall=" + remoteCall +
                ", type=" + type +
                ", isResponse=" + isResponse +
                ", info=" + Util.toEscapedASCII(info) +
                '}';
    }

    public enum Type {
        NL_CONNECT,
        NL_DISCONNECT,
        NL_INFO
    };
}
