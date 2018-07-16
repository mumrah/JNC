package net.tarpn.config;

import org.apache.commons.configuration2.Configuration;

public class NetRomConfig extends NodeConfig {

  NetRomConfig(Configuration delegate) {
    super(delegate);
  }

  public int getTimeToLive() {
    return getInt("netrom.ttl", 7);
  }

  public int getRetryCount() {
    return getInt("netrom.retry.count", 1);
  }
}
