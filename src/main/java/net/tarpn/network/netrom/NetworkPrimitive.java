package net.tarpn.network.netrom;

import net.tarpn.netty.Primitive;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.util.Util;

public class NetworkPrimitive implements Primitive {
    private final AX25Call remoteCall;
    private final AX25Call localCall;
    private final Type type;
    private final boolean isResponse;
    private final byte[] info;
    private final byte circuitId;

    NetworkPrimitive(AX25Call remoteCall, AX25Call localCall, Type type, boolean isResponse, byte[] info, int circuitId) {
        this.remoteCall = remoteCall;
        this.localCall = localCall;
        this.type = type;
        this.isResponse = isResponse;
        this.info = info;
        this.circuitId = (byte)(circuitId & 0xff);
    }

    public static NetworkPrimitive newConnect(AX25Call remoteCall, AX25Call localCall, int circuitId) {
        return new NetworkPrimitive(remoteCall, localCall, Type.NL_CONNECT, false, new byte[0], circuitId);
    }

    public static NetworkPrimitive newConnectAck(AX25Call remoteCall, AX25Call localCall, int circuitId) {
        return new NetworkPrimitive(remoteCall, localCall, Type.NL_CONNECT, true, new byte[0], circuitId);
    }

    public static NetworkPrimitive newConnectIndication(AX25Call remoteCall, AX25Call localCall, int circuitId) {
        return new NetworkPrimitive(remoteCall, localCall, Type.NL_CONNECT, false, new byte[0], circuitId);
    }

    public static NetworkPrimitive newDisconnect(AX25Call remoteCall, AX25Call localCall, int circuitId) {
        return new NetworkPrimitive(remoteCall, localCall, Type.NL_DISCONNECT, false, new byte[0], circuitId);
    }

    public static NetworkPrimitive newDisconnectAck(AX25Call remoteCall, AX25Call localCall, int circuitId) {
        return new NetworkPrimitive(remoteCall, localCall, Type.NL_DISCONNECT, true, new byte[0], circuitId);
    }

    public static NetworkPrimitive newDisconnectIndication(AX25Call remoteCall, AX25Call localCall, int circuitId) {
        return new NetworkPrimitive(remoteCall, localCall, Type.NL_DISCONNECT, false, new byte[0], circuitId);
    }

    public static NetworkPrimitive newData(AX25Call remoteCall, AX25Call localCall, byte[] data, int circuitId) {
        return new NetworkPrimitive(remoteCall, localCall, Type.NL_INFO, false, data, circuitId);
    }

    public static NetworkPrimitive newDataIndication(AX25Call remoteCall, AX25Call localCall, byte[] data, int circuitId) {
        return new NetworkPrimitive(remoteCall, localCall, Type.NL_INFO, false, data, circuitId);
    }

    public AX25Call getRemoteCall() {
        return remoteCall;
    }

    public AX25Call getLocalCall() {
        return localCall;
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

    public byte getCircuitId() {
        return circuitId;
    }

    @Override
    public String toString() {
        return "NetworkPrimitive{" +
                "remoteCall=" + remoteCall +
                ", localCall=" + localCall +
                ", circuitId=" + (circuitId & 0xff) +
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
