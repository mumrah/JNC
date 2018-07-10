package net.tarpn;

import net.tarpn.config.Configuration;
import net.tarpn.io.DataPort;
import net.tarpn.io.impl.SerialDataPort;
import net.tarpn.network.NetworkManager;
import net.tarpn.packet.impl.ax25.AX25Call;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  public static Logger LOG = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    Configuration config = Configuration.newBuilder()
        .setAlias("DAVID2")
        .setNodeCall(AX25Call.create("K4DBZ", 2))
        .build();

    // Level 3: The network layer
    NetworkManager network = NetworkManager.create(config);

    // Level 2: The data link layer. Define a port and initialize it
    DataPort port1 = SerialDataPort.openPort(1, "/dev/tty.wchusbserial1410", 9600);
    //DataPort port2 = SerialDataPort.openPort(2, "/tmp/vmodem0", 9600);

    network.addPort(port1);
    //network.addPort(port2);
    network.start();

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.err.println("Shutting down");
      network.stop();
    }));


    /*ServerSocket serverSocket = new ServerSocket(7777);
    System.err.println("Started socket server on 7777");
    while(true) {
      Socket clientSocket = serverSocket.accept();
      executorService.submit(() -> {
        try {
          BufferedReader reader = new BufferedReader(
              new InputStreamReader(clientSocket.getInputStream()));
          String line;
          while ((line = reader.readLine()) != null) {
            if(line.trim().equalsIgnoreCase("C")) {
              System.err.println("Trying for CONNECT");
              network.getPortManager(1).getAx25StateHandler().getEventQueue().add(
                  AX25StateEvent.createConnectEvent(AX25Call.create("K4DBZ", 9))
              );
            } else if(line.trim().equalsIgnoreCase("D")) {
              System.err.println("Trying for DISCONNECT");
              network.getPortManager(1).getAx25StateHandler().getEventQueue().add(
                  AX25StateEvent.createDisconnectEvent(AX25Call.create("K4DBZ", 9))
              );
            } else {
              //System.err.println("Sending INFO");
              network.getPortManager(1).getAx25StateHandler().getEventQueue().add(
                  AX25StateEvent.createDataEvent(
                      AX25Call.create("K4DBZ", 9),
                      Protocol.NO_LAYER3,
                      line.trim().concat("\r").getBytes(StandardCharsets.US_ASCII))
              );
            }
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
    }
    */
  }
}