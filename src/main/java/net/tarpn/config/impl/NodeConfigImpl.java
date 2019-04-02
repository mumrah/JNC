package net.tarpn.config.impl;

import net.tarpn.config.NodeConfig;
import net.tarpn.packet.impl.ax25.AX25Call;
import org.apache.commons.configuration2.Configuration;

public class NodeConfigImpl extends BaseConfig implements NodeConfig {

  NodeConfigImpl(Configuration delegate) {
    super(delegate);
  }

  @Override
  public AX25Call getNodeCall() {
    return AX25Call.fromString(getString("node.call"));
  }

  @Override
  public String getNodeAlias() {
    return getString("node.alias");
  }

  @Override
  public String getIdMessage() {
    String defaultMessage = "Packet node " + getNodeAlias() + ", op is " + getNodeCall();
    return getString("id.message", defaultMessage);
  }

  @Override
  public int getIdInterval() {
    return getInt("id.interval", 600);
  }
}
