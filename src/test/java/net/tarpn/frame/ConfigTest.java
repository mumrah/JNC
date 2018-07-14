package net.tarpn.frame;

import net.tarpn.config.Config;
import org.junit.Test;

public class ConfigTest {
  @Test
  public void testReadConfig() throws Exception {
    Config config = Config.read(ClassLoader.getSystemResourceAsStream("sample.ini"));
    System.err.println(config.getNetRomConfig().getTimeToLive());
  }
}
