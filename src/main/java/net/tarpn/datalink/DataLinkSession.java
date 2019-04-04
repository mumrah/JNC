package net.tarpn.datalink;

import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;

import java.nio.charset.StandardCharsets;

public class DataLinkSession {
    private final AX25Call destAddress;
    private final DataLinkManager dataLinkManager;

    public DataLinkSession(AX25Call destAddress, DataLinkManager dataLinkManager) {
        this.destAddress = destAddress;
        this.dataLinkManager = dataLinkManager;
    }

    public void send(String line) {
        dataLinkManager.acceptDataLinkPrimitive(DataLinkPrimitive.newUnitDataRequest(destAddress,
                AX25Packet.Protocol.NO_LAYER3, line.getBytes(StandardCharsets.UTF_8)));
    }

    public void close() {

    }
}
