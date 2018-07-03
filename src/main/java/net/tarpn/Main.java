package net.tarpn;

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
import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketHandler;
import net.tarpn.packet.PacketRequest;
import net.tarpn.packet.impl.CompositePacketHandler;
import net.tarpn.packet.impl.ConsolePacketHandler;
import net.tarpn.packet.impl.DefaultPacketRequest;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;
import net.tarpn.packet.impl.ax25.UIFrame;
import net.tarpn.packet.impl.ax25.fsm.AX25StateHandler;
import net.tarpn.packet.impl.ax25.fsm.StateEvent;
import net.tarpn.packet.impl.ax25.fsm.StateEvent.Type;
import net.tarpn.packet.impl.netrom.NetworkManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  static Logger LOG = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) throws Exception {
    ExecutorService executorService = Executors.newCachedThreadPool();
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    // Define a port and initialize it with a DataPortManager
    DataPort port1 = SerialDataPort.openPort(1, "/dev/tty.wchusbserial1410", 9600);
    //DataPort port1 = SerialDataPort.openPort(1, "/tmp/vmodem0", 9600);
    port1.open();

    DataPortManager portManager = DataPortManager.initialize(port1);
    executorService.submit(portManager.getReaderRunnable());
    executorService.submit(portManager.getWriterRunnable());

    // In general a data link can accept packets from multiple senders, so the output from the data
    // link layer should include the port number.

    // Level 3 network layer
    NetworkManager network = NetworkManager.create();

    // Start up the AX.25 layer
    AX25StateHandler ax25StateHandler = new AX25StateHandler(
        portManager.getOutboundPackets()::add,
        network.getInboundPackets()::add);
    executorService.submit(ax25StateHandler.getRunnable());

    // Poll the port manager for incoming frames and pass them to the packet handler
    executorService.submit(() -> {
      PacketHandler packetHandler = CompositePacketHandler.wrap(
          new ConsolePacketHandler(),
          ax25StateHandler
      );
      while(true) {
        PacketRequest packetRequest = portManager.getInboundPackets().poll();
        try {
          if (packetRequest != null) {
            packetHandler.onPacket(packetRequest);
          } else {
            Thread.sleep(50);
          }
        } catch (Throwable t) {
          LOG.error("Failure when processing incoming packets", t);
        }
      }
    });


    // TODO fixme, for now just send all pending outbound networking packets
    // TODO run this through the AX.25 state machine
    executorService.submit(() -> {
      while(true) {
        AX25Packet packet = network.getOutboundPackets().poll();
        try {
          if (packet != null) {
            portManager.getOutboundPackets().add(packet);
          } else {
            Thread.sleep(50);
          }
        }  catch (Throwable t) {
          LOG.error("Failure when sending network packets", t);
        }
      }
    });

    // Start the network layer
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
      for (String sessionId : ax25StateHandler.getSessionIds()) {
        ax25StateHandler.getEventQueue().add(
            StateEvent.create(sessionId, idPacket, Type.DL_UNIT_DATA));
      }
    }, 5, 30, TimeUnit.SECONDS);


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
  }
}