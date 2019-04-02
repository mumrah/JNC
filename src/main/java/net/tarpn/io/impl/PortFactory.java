package net.tarpn.io.impl;

import net.tarpn.config.PortConfig;
import net.tarpn.io.DataPort;

public class PortFactory {
  public static DataPort createPortFromConfig(PortConfig config) {
    if(config.getPortType().equalsIgnoreCase("serial")) {
      return SerialDataPort.createPort(config.getPortNumber(), config.getSerialDevice(), config.getSerialSpeed());
    } else if(config.getPortType().equalsIgnoreCase("i2c")) {
      return I2CDataPort.createPort(config.getPortNumber(), config.getI2CBus(), config.getI2CDeviceAddress());
    } else if (config.getPortType().equalsIgnoreCase("virtual")) {
      return VirtualPort.create(config.getPortNumber(), config.getString("port.name"),
              config.getString("port.piped.to"));
    } else {
      throw new IllegalArgumentException("Unknown port type: " + config.getPortType());
    }
  }
}
