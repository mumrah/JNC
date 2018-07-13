package net.tarpn;

import net.tarpn.config.Configuration;
import net.tarpn.io.impl.LoopBackPort;
import net.tarpn.io.impl.SocketDataPortServer;
import net.tarpn.network.NetworkManager;
import net.tarpn.packet.impl.ax25.AX25Call;

public class Main2 {
  public static void main(String[] args) throws Exception {
    LoopBackPort[] ports = LoopBackPort.createPair();

    {
      Configuration config = Configuration.newBuilder()
          .setAlias("DAVID2")
          .setNodeCall(AX25Call.create("K4DBZ", 2))
          .build();

      // Level 3: The network layer
      NetworkManager network = NetworkManager.create(config);
      network.initialize(ports[0]);
      network.start();
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.err.println("Shutting down");
        network.stop();
      }));
    }

    {
      Configuration config = Configuration.newBuilder()
          .setAlias("DAVID3")
          .setNodeCall(AX25Call.create("K4DBZ", 3))
          .build();

      // Level 3: The network layer
      NetworkManager network = NetworkManager.create(config);
      network.initialize(ports[1]);
      network.start();
      Runtime.getRuntime().addShutdownHook(new Thread(() -> {
        System.err.println("Shutting down");
        network.stop();
      }));
      SocketDataPortServer socketDataPortServer = new SocketDataPortServer(network);
      socketDataPortServer.start();
    }


  }
}
