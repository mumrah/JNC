package net.tarpn;

import net.tarpn.config.Configuration;
import net.tarpn.io.DataPort;
import net.tarpn.io.impl.SerialDataPort;
import net.tarpn.network.NetworkManager;
import net.tarpn.packet.impl.ax25.AX25Call;

public class NetworkMain {
  public static void main(String[] args) {

    Configuration config = Configuration.newBuilder()
        .setAlias("DAVID2")
        .setNodeCall(AX25Call.create("K4DBZ", 2))
        .build();

    NetworkManager networkManager = NetworkManager.create(config);

    DataPort port1 = SerialDataPort.openPort(1, "/dev/tty.wchusbserial1410", 9600);

    networkManager.initialize(port1);
    networkManager.start();
  }
}
