package net.tarpn.config;

import net.tarpn.packet.impl.ax25.AX25Call;
import org.apache.commons.configuration2.Configuration;

public class PortConfig extends NodeConfig {

  private final int portNumber;

  public PortConfig(int portNumber, Configuration delegate) {
    super(delegate);
    this.portNumber = portNumber;
  }

  public int getPortNumber() {
    return portNumber;
  }

  public boolean isEnabled() {
    return getBoolean("port.enabled");
  }

  public String getPortType() {
    return getString("port.type");
  }

  public String getSerialDevice() {
    return getString("serial.device");
  }

  public int getSerialSpeed() {
    return getInt("serial.speed");
  }

  public String getSerialProtocol() {
    return getString("serial.protocol");
  }


}
