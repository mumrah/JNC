package net.tarpn.app;

import net.tarpn.config.Configs;
import net.tarpn.network.NetworkManager;
import net.tarpn.network.netrom.NetRomSession;
import net.tarpn.packet.impl.ax25.AX25Call;

public class NetworkMain {
  public static void main(String[] args) throws Exception {
    Configs config = Configs.read("conf/sample.ini");

    NetworkManager networkManager = NetworkManager.create(config.getNetRomConfig(),
        event -> System.err.println("L3 event: " + event));
    //config.getPortConfigs().forEach(
    //    (portNumber, portConfig) -> networkManager.initialize(portConfig));
    networkManager.initialize(config.getPortConfigs().get(1));
    networkManager.start();

    /*
    NetRomSocket socket = networkManager.connect("DAVID2");
    socket.send("Hello");
    socket.recv();
     */


    /*
    Thread.sleep(300000);
    NetRomSession session = networkManager.open(AX25Call.create("K4DBZ", 9));
    session.connect();
    while(!Thread.currentThread().isInterrupted()) {
      Thread.sleep(50);
    }
    */
  }
}
