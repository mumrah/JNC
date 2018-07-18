package net.tarpn.app;

import java.nio.charset.StandardCharsets;
import net.tarpn.config.Configs;
import net.tarpn.network.NetworkManager;
import net.tarpn.network.netrom.NetRomSession;
import net.tarpn.packet.impl.ax25.AX25Call;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;

public class NetworkMain {
  public static void main(String[] args) throws Exception {
    Configs config = Configs.read("src/dist/conf/sample.ini");

    String level = config.getNodeConfig().getString("log.level", "info");
    Level rootLevel = Level.getLevel(level.toUpperCase());
    Configurator.setRootLevel(rootLevel);

    NetworkManager networkManager = NetworkManager.create(config.getNetRomConfig());
    config.getPortConfigs().forEach(
        (portNumber, portConfig) -> networkManager.initialize(portConfig));
    networkManager.start();

    NetRomSession session = networkManager.open(AX25Call.create("K4DBZ", 9));
    session.connect();
    while(!session.isConnected()) {
      Thread.sleep(5000);
      session.connect();
    }

    session.write("testing".getBytes(StandardCharsets.US_ASCII));
    Thread.sleep(5000);
    session.close();
  }
}
