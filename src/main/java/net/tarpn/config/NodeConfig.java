package net.tarpn.config;

import net.tarpn.packet.impl.ax25.AX25Call;

public interface NodeConfig extends Configuration {
    AX25Call getNodeCall();

    String getNodeAlias();

    String getIdMessage();

    int getIdInterval();
}
