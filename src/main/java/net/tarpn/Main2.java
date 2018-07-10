package net.tarpn;

import net.tarpn.config.Configuration;
import net.tarpn.io.DataPort;
import net.tarpn.io.impl.SerialDataPort;
import net.tarpn.network.NetworkManager;
import net.tarpn.packet.impl.ax25.AX25Call;

public class Main2 {
  public static void main(String[] args) {
    Configuration config = Configuration.newBuilder()
        .setAlias("JUDE")
        .setNodeCall(AX25Call.create("K4DBZ", 3))
        .build();

    // Level 3: The network layer
    NetworkManager network = NetworkManager.create(config);

    // Level 2: The data link layer. Define a port and initialize it
    DataPort port3 = SerialDataPort.openPort(1, "/tmp/vmodem1", 9600);

    network.addPort(port3);
    network.start();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.err.println("Shutting down");
      network.stop();
    }));
  }
}
