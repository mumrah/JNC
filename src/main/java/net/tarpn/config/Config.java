package net.tarpn.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.SubnodeConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;

public class Config {
  private final NetRomConfig netromConfig;

  private final Set<Configuration> portConfigs;

  Config(Configuration netromConfig,
      Set<Configuration> portConfigs) {
    this.netromConfig = new NetRomConfig(netromConfig);
    this.portConfigs = portConfigs;
  }

  public NetRomConfig getNetRomConfig() {
    return netromConfig;
  }

  public Set<Configuration> getPortConfigs() {
    return portConfigs;
  }

  public static Config read(InputStream is) {
    INIConfiguration configuration = new INIConfiguration();
    try {
      configuration.read(new InputStreamReader(is));
    } catch (ConfigurationException | IOException e) {
      throw new RuntimeException("Could not load configuration", e);
    }

    // Read off Ports
    Set<Configuration> portConfigs = configuration.getSections().stream()
        .map(String::toLowerCase)
        .filter(section -> section.startsWith("port:"))
        .map(configuration::getSection)
        .collect(Collectors.toSet());


    // NET/ROM config
    SubnodeConfiguration netromConfig = configuration.getSection("network:netrom");

    return new Config(netromConfig, portConfigs);
  }

  public static class NetRomConfig {
    private final Configuration delegate;

    NetRomConfig(Configuration delegate) {
      this.delegate = delegate;
    }

    public int getTimeToLive() {
      return delegate.getInt("TimeToLive", 7);
    }

    public int getRetryCount() {
      return delegate.getInt("RetryCount", 1);
    }
  }
}
