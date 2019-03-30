package net.tarpn.app;

import java.nio.charset.StandardCharsets;
import net.tarpn.config.Configs;
import net.tarpn.network.NetworkManager;
import net.tarpn.network.netrom.NetRomSession;
import net.tarpn.network.netrom.NetRomSocket;
import net.tarpn.packet.impl.ax25.AX25Call;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetworkMain {
  private static final Logger LOG = LoggerFactory.getLogger(NetworkMain.class);

  public static void main(String[] args) throws Exception {
    Configs config = Configs.read("src/dist/conf/sample2.ini");

    String level = config.getNodeConfig().getString("log.level", "info");
    Level rootLevel = Level.getLevel(level.toUpperCase());
    Configurator.setRootLevel(rootLevel);

    NetworkManager networkManager = NetworkManager.create(config.getNetRomConfig());
    config.getPortConfigs().forEach(
        (portNumber, portConfig) -> networkManager.initialize(portConfig));
    networkManager.start();

    NodeApplication application = new NodeApplication(networkManager);
    application.handleLine("CONNECT K4DBZ-2", LOG::info);
    application.handleLine("Hello there!", LOG::info);
    application.handleLine("BYE", LOG::info);

  }
}
