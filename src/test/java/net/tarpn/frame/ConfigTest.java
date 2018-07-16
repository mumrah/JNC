package net.tarpn.frame;

import net.tarpn.config.Config;
import net.tarpn.config.PortConfig;
import org.junit.Test;

public class ConfigTest {
  @Test
  public void testReadConfig() throws Exception {
    Config config = Config.read(ClassLoader.getSystemResourceAsStream("sample.ini"));
    System.err.println(config.getNetRomConfig().getTimeToLive());

    for (PortConfig portConfig : config.getPortConfigs()) {
      System.err.println(portConfig.getPortNumber() + " " + portConfig.isEnabled());
    }

  }
}
