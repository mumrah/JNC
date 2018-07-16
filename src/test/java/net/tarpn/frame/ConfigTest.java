package net.tarpn.frame;

import net.tarpn.config.Configs;
import net.tarpn.config.PortConfig;
import org.junit.Test;

public class ConfigTest {
  @Test
  public void testReadConfig() throws Exception {
    Configs config = Configs.read("conf/sample.ini");
    System.err.println(config.getNetRomConfig().getTimeToLive());
    System.err.println(config.getNodeConfig().getString("id.message"));

    for(PortConfig portConfig : config.getPortConfigs().values()) {
      System.err.println(portConfig.getNodeCall());

      System.err.println(portConfig.getString("id.message"));

    }

  }
}
