package net.tarpn.app;

import net.tarpn.config.Configuration;
import net.tarpn.io.DataPort;
import net.tarpn.io.impl.SerialDataPort;
import net.tarpn.network.NetworkManager;
import net.tarpn.network.netrom.NetRomSession;
import net.tarpn.packet.impl.ax25.AX25Call;

public class NetworkMain {
  public static void main(String[] args) throws Exception {

    Configuration config = Configuration.newBuilder()
        .setAlias("DAVID2")
        .setNodeCall(AX25Call.create("K4DBZ", 2))
        .build();

    NetworkManager networkManager = NetworkManager.create(config);

    DataPort port1 = SerialDataPort.openPort(1, "/dev/tty.wchusbserial1410", 9600);

    networkManager.initialize(port1);
    networkManager.start();

    /*
    NetRomSocket socket = networkManager.connect("DAVID2");
    socket.send("Hello");
    socket.recv();
     */


    Thread.sleep(300000);
    NetRomSession session = networkManager.open(AX25Call.create("K4DBZ", 9));
    session.connect();
    while(!Thread.currentThread().isInterrupted()) {

      Thread.sleep(50);
    }
  }
}
