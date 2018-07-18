package net.tarpn.app;

import java.io.IOException;
import net.tarpn.config.Configs;
import net.tarpn.datalink.DataLinkManager;
import net.tarpn.io.DataPort;
import net.tarpn.io.impl.PortFactory;
import net.tarpn.network.NetworkManager;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

public class Main {
  public static void main(String[] args) throws IOException {
    String configFile = args[0];
    Configs config = Configs.read(configFile);

    String level = config.getNodeConfig().getString("log.level", "info");
    Level rootLevel = Level.getLevel(level.toUpperCase());
    Configurator.setRootLevel(rootLevel);

    if(config.getNodeConfig().getBoolean("network.enabled", true)) {
      NetworkManager networkManager = NetworkManager.create(config.getNetRomConfig(),
          event -> System.err.println("L3 event: " + event));
      config.getPortConfigs().forEach(
          (portNumber, portConfig) -> networkManager.initialize(portConfig));
      networkManager.start();
    } else {
      config.getPortConfigs().forEach((portNumber, portConfig) -> {
        DataPort port = PortFactory.createPortFromConfig(portConfig);
        DataLinkManager dataLinkManager = DataLinkManager.create(portConfig, port,
            event -> System.err.println("L2 event: " + event), packetRequest -> {});
        dataLinkManager.start();
      });
    }
  }
}
