package net.tarpn.config;

import net.tarpn.packet.impl.ax25.AX25Call;
import org.apache.commons.configuration2.Configuration;

public class NodeConfig extends BaseConfig {

  NodeConfig(Configuration delegate) {
    super(delegate);
  }

  public AX25Call getNodeCall() {
    return AX25Call.fromString(getString("node.call"));
  }

  public String getNodeAlias() {
    return getString("node.alias");
  }
}
