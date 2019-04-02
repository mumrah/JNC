package net.tarpn.config.impl;

import net.tarpn.config.NetRomConfig;
import net.tarpn.config.impl.NodeConfigImpl;
import org.apache.commons.configuration2.Configuration;

public class NetRomConfigImpl extends NodeConfigImpl implements NetRomConfig {

  NetRomConfigImpl(Configuration delegate) {
    super(delegate);
  }

  @Override
  public int getTimeToLive() {
    return getInt("netrom.ttl", 7);
  }

  @Override
  public int getRetryCount() {
    return getInt("netrom.retry.count", 1);
  }

  @Override
  public int getMinObs() {
    return getInt("netrom.obs.min", 4);
  }

  @Override
  public int getInitialObs() {
    return getInt("netrom.obs.init", 6);
  }

  @Override
  public int getNodesInterval() {
    return getInt("netrom.nodes.interval", 300);
  }

  @Override
  public byte getTTL() {
    return (byte)(getInt("netrom.ttl", 7) & 0xff);
  }

  @Override
  public byte getWindowSize() {
    return (byte)(getInt("netrom.circuit.window", 2) & 0xff);
  }
}
