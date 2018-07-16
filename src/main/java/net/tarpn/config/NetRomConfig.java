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

  public int getMinObs() {
    return getInt("netrom.obs.min", 4);
  }

  public int getInitialObs() {
    return getInt("netrom.obs.init", 6);
  }

  public int getNodesInterval() {
    return getInt("netrom.nodes.interval", 300);
  }

  public byte getTTL() {
    return (byte)(getInt("netrom.ttl", 7) & 0xff);
  }

  public byte getWindowSize() {
    return (byte)(getInt("netrom.circuit.window", 2) & 0xff);
  }
}
