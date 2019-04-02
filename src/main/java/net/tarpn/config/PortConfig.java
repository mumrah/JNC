package net.tarpn.config;

public interface PortConfig extends Configuration, NodeConfig {
    int getPortNumber();

    boolean isEnabled();

    String getPortType();

    String getSerialDevice();

    int getSerialSpeed();

    String getSerialProtocol();

    int getI2CBus();

    int getI2CDeviceAddress();
}
