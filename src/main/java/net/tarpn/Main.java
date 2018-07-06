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
import net.tarpn.io.DataPort;
import net.tarpn.io.impl.DataPortManager;
import net.tarpn.io.impl.SerialDataPort;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Command;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;
import net.tarpn.packet.impl.ax25.IFrame;
import net.tarpn.packet.impl.ax25.UIFrame;
import net.tarpn.packet.impl.ax25.StateEvent;
import net.tarpn.packet.impl.ax25.StateEvent.Type;
import net.tarpn.packet.impl.netrom.NetRomConnectRequest;
import net.tarpn.packet.impl.netrom.NetworkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  static Logger LOG = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) throws Exception {
    ExecutorService executorService = Executors.newCachedThreadPool();
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    // Level 3: The network layer
    NetworkManager network = NetworkManager.create();

    // Level 2: The data link layer. Define a port and initialize it
    DataPort port1 = SerialDataPort.openPort(1, "/dev/tty.wchusbserial1410", 9600);
    //DataPort port1 = SerialDataPort.openPort(1, "/tmp/vmodem0", 9600);
    port1.open();

    DataPortManager portManager = DataPortManager.initialize(port1, network.getInboundPackets()::add);
    executorService.submit(portManager.getReaderRunnable()); // read data off the incoming port
    executorService.submit(portManager.getWriterRunnable()); // write outbound packets to the port
    executorService.submit(portManager.getAx25StateHandler().getRunnable()); // process ax.25 packets on this port

    // In general a data link can accept packets from multiple senders, so the output from the data
    // link layer should include the port number.

    executorService.submit(() -> {
      while(true) {
        AX25Packet packet = network.getOutboundPackets().poll();
        try {
          if (packet != null) {
            switch(packet.getFrameType()) {
              case I: {
                portManager.getAx25StateHandler().getEventQueue().add(
                    StateEvent.createOutgoingEvent(packet, Type.DL_DATA)
                );
                break;
              }
              case UI: {
                portManager.getAx25StateHandler().getEventQueue().add(
                    StateEvent.createUIEvent(packet, Type.DL_UNIT_DATA)
                );
                break;
              }
              default: {
                break;
              }
            }
          } else {
            Thread.sleep(50);
          }
        }  catch (Throwable t) {
          LOG.error("Failure when sending network packets", t);
        }
      }
    });

    // Start up the network
    executorService.submit(network.getRunnable());




    //Queue<Frame> outgoingFrames = new ConcurrentLinkedQueue<>();
    //executorService.submit(newPortReader(port1, outgoingFrames));
    //executorService.submit(newPortWriter(port1, outgoingFrames));

    scheduledExecutorService.scheduleWithFixedDelay(() -> {
      LOG.info("Sending automatic ID message");
      AX25Packet idPacket = UIFrame.create(
          AX25Call.create("ID"),
          AX25Call.create("K4DBZ"),
          Protocol.NO_LAYER3,
          "Terrestrial Amateur Radio Packet Network node DAVID2 op is K4DBZ\r"
              .getBytes(StandardCharsets.US_ASCII));
      // Send this message to each port
      portManager.getAx25StateHandler().getEventQueue().add(
          StateEvent.createUIEvent(idPacket, Type.DL_UNIT_DATA));
    }, 5, 300, TimeUnit.SECONDS);


    String[] nodes = new String[]{"TWO   ", "THREE ", "FOUR  "};
    AtomicInteger n = new AtomicInteger(0);
    scheduledExecutorService.scheduleWithFixedDelay(() -> {
      LOG.info("Sending automatic NODES message");
      // TODO on all ports
      //int i = n.getAndIncrement() % 3;
      //String node = nodes[i];

      ByteBuffer buffer = ByteBuffer.allocate(1024);
      buffer.put((byte)0xff);
      buffer.put("DAVID2".getBytes(StandardCharsets.US_ASCII));

      AX25Call.create("K4DBZ", 3, 0x03, true, true).write(buffer::put);
      buffer.put("JUDE  ".getBytes(StandardCharsets.US_ASCII));
      AX25Call.create("K4DBZ", 2, 0x03, true, true).write(buffer::put);
      buffer.put((byte)(223));

      AX25Call.create("K4DBZ", 4, 0x03, true, true).write(buffer::put);
      buffer.put("FIONA ".getBytes(StandardCharsets.US_ASCII));
      AX25Call.create("K4DBZ", 2, 0x03, true, true).write(buffer::put);
      buffer.put((byte)(224));

      AX25Call.create("K4DBZ", 5, 0x03, true, true).write(buffer::put);
      buffer.put("FELCTY".getBytes(StandardCharsets.US_ASCII));
      AX25Call.create("K4DBZ", 2, 0x03, true, true).write(buffer::put);
      buffer.put((byte)(225));


      byte[] msg = Util.copyFromBuffer(buffer);
      UIFrame ui = UIFrame.create(
          AX25Call.create("NODES", 0),
          AX25Call.create("K4DBZ", 2),
          Protocol.NETROM, msg);
      portManager.getOutboundPackets().add(ui);
    }, 10, 300, TimeUnit.SECONDS);


    // NET/ROM connect
    scheduledExecutorService.schedule(() -> {
      NetRomConnectRequest req = NetRomConnectRequest.create(
          AX25Call.create("K4DBZ", 3), // JUDE
          AX25Call.create("K4DBZ", 1), // RPi
          (byte) 7, // ttl
          (byte) 99, // my circuit idx
          (byte) 0, // my circuit id
          (byte) 0, // tx
          (byte) 0, // rx
          (byte) 1, // proposed window
          AX25Call.create("K4DBZ", 0),
          AX25Call.create("K4DBZ", 3)
      );

      IFrame frame = IFrame.create(AX25Call.create("K4DBZ", 1), AX25Call.create("K4DBZ", 2),
          Command.COMMAND, (byte) 0, (byte) 0, true, Protocol.NETROM, req.getPayload());
      //portManager.getOutboundPackets().add(frame);
    }, 60, TimeUnit.SECONDS);



    // Initiate a connect
    scheduledExecutorService.schedule(() -> {
      //portManager.getAx25StateHandler().getEventQueue().add(
      //    StateEvent.createConnectEvent(AX25Call.create("K4DBZ", 1))
      //);
    }, 7, TimeUnit.SECONDS);

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


    ServerSocket serverSocket = new ServerSocket(7777);
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
              portManager.getAx25StateHandler().getEventQueue().add(
                  StateEvent.createConnectEvent(AX25Call.create("K4DBZ", 9))
              );
            } else if(line.trim().equalsIgnoreCase("D")) {
              System.err.println("Trying for DISCONNECT");
              portManager.getAx25StateHandler().getEventQueue().add(
                  StateEvent.createDisconnectEvent(AX25Call.create("K4DBZ", 9))
              );
            } else {
              System.err.println("Sending INFO");
              IFrame data = IFrame.create(AX25Call.create("K4DBZ", 9), Configuration.getOwnNodeCallsign(),
                  Command.COMMAND, 0, 0, true, Protocol.NO_LAYER3,
                  line.trim().concat("\r").getBytes(StandardCharsets.US_ASCII));
              portManager.getAx25StateHandler().getEventQueue().add(
                  StateEvent.createOutgoingEvent(data, Type.DL_DATA)
              );
            }
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      });
    }
  }
}