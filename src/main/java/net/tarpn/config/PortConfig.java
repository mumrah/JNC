package net.tarpn.config;

import java.util.Collections;
import java.util.List;

public interface PortConfig extends Configuration, NodeConfig {
    int getPortNumber();

    boolean isEnabled();

    String getPortType();

    default String getPortDescription() {
        return getString("port.description");
    }

    String getSerialDevice();

    int getSerialSpeed();

    String getSerialProtocol();

    List<String> getKISSFlags();

    int getI2CBus();

    int getI2CDeviceAddress();
}
