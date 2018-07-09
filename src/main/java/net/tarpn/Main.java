package net.tarpn;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import net.tarpn.config.Configuration;
import net.tarpn.io.DataPort;
import net.tarpn.io.impl.SerialDataPort;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Command;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;
import net.tarpn.packet.impl.ax25.IFrame;
import net.tarpn.packet.impl.ax25.UIFrame;
import net.tarpn.packet.impl.ax25.AX25StateEvent;
import net.tarpn.network.netrom.NetRomConnectRequest;
import net.tarpn.network.NetworkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  static Logger LOG = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) throws Exception {
    Configuration config = Configuration.newBuilder()
        .setAlias("DAVID2")
        .setNodeCall(AX25Call.create("K4DBZ", 2))
        .build();

    ExecutorService executorService = Executors.newCachedThreadPool();
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    // Level 3: The network layer
    NetworkManager network = NetworkManager.create(config);

    // Level 2: The data link layer. Define a port and initialize it
    DataPort port1 = SerialDataPort.openPort(1, "/dev/tty.wchusbserial1410", 9600);
    port1.open();

    DataPort port2 = SerialDataPort.openPort(2, "/tmp/vmodem0", 9600);
    port2.open();

    /*
    DataPortManager portManager = DataPortManager.initialize(port1, network.getInboundPackets()::add);
    executorService.submit(portManager.getReaderRunnable()); // read data off the incoming port
    executorService.submit(portManager.getWriterRunnable()); // write outbound packets to the port
    executorService.submit(portManager.getAx25StateHandler().getRunnable()); // process ax.25 packets on this port
    */

    network.addPort(port1);
    network.addPort(port2);
    network.start();

    //SocketDataPortServer server = new SocketDataPortServer(outgoingFrames);
    //executorService.submit(server);

    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      System.err.println("Shutting down");
      executorService.shutdown();
    //  try {
    //    port0.close();
    //  } catch (IOException e) {
    //    e.printStackTrace();
    //  }
      try {
        executorService.awaitTermination(1000, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
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