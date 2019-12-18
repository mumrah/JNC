package net.tarpn.config.impl;

import net.tarpn.config.PortConfig;
import org.apache.commons.configuration2.Configuration;

import java.util.List;

public class PortConfigImpl extends NodeConfigImpl implements PortConfig {

  private final int portNumber;

  public PortConfigImpl(int portNumber, Configuration delegate) {
    super(delegate);
    this.portNumber = portNumber;
  }

  @Override
  public int getPortNumber() {
    return portNumber;
  }

  @Override
  public boolean isEnabled() {
    return getBoolean("port.enabled");
  }

  @Override
  public String getPortType() {
    return getString("port.type");
  }

  @Override
  public String getSerialDevice() {
    return getString("serial.device");
  }

  @Override
  public int getSerialSpeed() {
    return getInt("serial.speed");
  }

  @Override
  public String getSerialProtocol() {
    return getString("serial.protocol");
  }

  @Override
  public List<String> getKISSFlags() {
    return getStrings("kiss.flags");
  }

  @Override
  public int getI2CBus() {
    return getInt("i2c.bus");
  }

  @Override
  public int getI2CDeviceAddress() {
    return getInt("i2c.address");
  }

}
