package net.tarpn.main;

import net.tarpn.config.impl.Configs;
import net.tarpn.network.NetworkManager2;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkMain {
  private static final Logger LOG = LoggerFactory.getLogger(NetworkMain.class);

  public static void main(String[] args) throws Exception {
    Configs configs = Configs.read(args[0]);

    String level = configs.getNodeConfig().getString("log.level", "info");
    //Level rootLevel = Level.getLevel(level.toUpperCase());
    //Configurator.setRootLevel(rootLevel);

    NetworkManager2 networkManager = NetworkManager2.create(configs.getNetRomConfig());
    configs.getPortConfigs().forEach(
        (portNumber, portConfig) -> networkManager.initialize(portConfig));
    networkManager.start();
    networkManager.join();
  }
}
