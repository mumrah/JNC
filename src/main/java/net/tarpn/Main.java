package net.tarpn;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.tarpn.frame.Frame;
import net.tarpn.frame.FrameHandler;
import net.tarpn.frame.FrameReader;
import net.tarpn.frame.FrameWriter;
import net.tarpn.frame.impl.CompositeFrameHandler;
import net.tarpn.frame.impl.ConsoleFrameHandler;
import net.tarpn.frame.impl.DefaultFrameRequest;
import net.tarpn.frame.impl.KISSCommandHandler;
import net.tarpn.frame.impl.KISSFrameReader;
import net.tarpn.frame.impl.KISSFrameWriter;
import net.tarpn.frame.impl.PacketReadingFrameHandler;
import net.tarpn.io.DataPort;
import net.tarpn.io.impl.DataPortManager;
import net.tarpn.io.impl.SerialDataPort;
import net.tarpn.io.impl.SocketDataPortServer;
import net.tarpn.message.Message;
import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketHandler;
import net.tarpn.packet.PacketReader;
import net.tarpn.packet.PacketWriter;
import net.tarpn.packet.impl.AX25PacketHandler;
import net.tarpn.packet.impl.AX25PacketReader;
import net.tarpn.packet.impl.AX25PacketWriter;
import net.tarpn.packet.impl.CompositePacketHandler;
import net.tarpn.packet.impl.ConsolePacketHandler;
import net.tarpn.packet.impl.DefaultPacketRequest;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;
import net.tarpn.packet.impl.ax25.UIFrame;
import net.tarpn.packet.impl.ax25.fsm.AX25StateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  static Logger LOG = LoggerFactory.getLogger(Main.class);

  public static Runnable newPortWriter(
      DataPort port,
      Queue<Frame> outboundFrames) {
    return () -> {
      OutputStream outputStream = port.getOutputStream();

      // TODO need to sync output to port (frame queue or a lock)
      Consumer<byte[]> toDataPort = bytes -> {
        LOG.info("Sending data to port " + port.getPortNumber());
        try {
          for(int i=0; i<bytes.length; i++) {
            byte b = bytes[i];
            System.err.println("KISS WRITE " + b + "\t" + String.format("%02X", b) + "\t" + Character.toString((char)b));
          }

          outputStream.write(bytes);
          outputStream.flush();
        } catch (IOException e) {
          LOG.error("Error writing to DataPort " + port.getPortNumber(), e);
        }
      };

      FrameWriter frameWriter = new KISSFrameWriter();
      Consumer<Frame> framesOut = outFrame -> frameWriter.accept(outFrame, toDataPort);

      while(true) {
        Frame outFrame = outboundFrames.poll();
        try {
          if(outFrame != null) {
            framesOut.accept(outFrame);
          } else {
            Thread.sleep(50);
          }
        } catch (Throwable t) {
          LOG.error("Error writing frame", t);
        }
      }
    };
  }

  /**
   * For each port, setup a thread which is consuming its data and ultimately emitting messages
   * @param port
   * @return
   */
  public static Runnable newPortReader(
      DataPort port,
      Queue<Frame> outboundFrames) {
    return () -> {

      // Decoding pipeline for a single DataPort
      //MessageReader messageReader = new SimpleMessageReader();

      // Run these when we receive a packet on this port
      PacketHandler packetHandler = CompositePacketHandler.wrap(
          new ConsolePacketHandler(),
          new AX25PacketHandler()
          //new NetRomPacketHandler()
          //new MessageReadingPacketHandler(messageReader, inboundMessages::add)
      );


      // Read bytes from the DataPort and feed into the frame reader
      InputStream inputStream = new BufferedInputStream(port.getInputStream());

      FrameReader frameReader = new KISSFrameReader(port.getPortNumber());
      PacketReader packetReader = new AX25PacketReader();
      PacketWriter packetWriter = new AX25PacketWriter();
      Consumer<Packet> packetSink = packet -> packetWriter.accept(packet, outboundFrames::add);

      // Run these as we get new data frames from the port
      FrameHandler frameHandler = CompositeFrameHandler.wrap(
          new ConsoleFrameHandler(),
          new KISSCommandHandler(),
          new PacketReadingFrameHandler(packetReader,
              packet -> packetHandler.onPacket(new DefaultPacketRequest(0, packet, packetSink)))
      );

      try {
        while(true) {
          // Read off any input data
          //LOG.info("Polling port " + port.getPortNumber());
          while(inputStream.available() > 0) {
            int d = inputStream.read();
            frameReader.accept(d, frame -> {
              frameHandler.onFrame(new DefaultFrameRequest(frame, outboundFrames::add));
            });
          }
          Thread.sleep(50);
        }
      } catch (IOException | InterruptedException e) {
        LOG.error("Failed when polling " + port.getName(), e);
      }
    };
  }

  public static void main(String[] args) throws Exception {
    ExecutorService executorService = Executors.newCachedThreadPool();
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    // Port 1
    DataPort port1 = SerialDataPort.openPort(1, "/dev/tty.wchusbserial1410", 9600);
    port1.open();


    DataPortManager portManager = DataPortManager.initialize(port1);
    executorService.submit(portManager.getReaderRunnable());
    executorService.submit(portManager.getWriterRunnable());

    AX25StateHandler ax25StateHandler = new AX25StateHandler(portManager.getOutboundPackets()::add);
    ax25StateHandler.start();

    executorService.submit(() -> {
      PacketHandler packetHandler = CompositePacketHandler.wrap(
          new ConsolePacketHandler(),
          //new AX25PacketHandler()
          ax25StateHandler
      );
      while(true) {
        Packet packet = portManager.getInboundPackets().poll();
        try {
          if (packet != null) {
            packetHandler.onPacket(
                new DefaultPacketRequest(portManager.getDataPort().getPortNumber(), packet,
                    responsePacket -> portManager.getOutboundPackets().add(responsePacket)));
          } else {
            Thread.sleep(50);
          }
        } catch (Throwable t) {
          LOG.error("Failure when processing incoming packets", t);
        }
      }
    });



    //Queue<Frame> outgoingFrames = new ConcurrentLinkedQueue<>();
    //executorService.submit(newPortReader(port1, outgoingFrames));
    //executorService.submit(newPortWriter(port1, outgoingFrames));


    scheduledExecutorService.scheduleWithFixedDelay(() -> {
      //LOG.info("Sending automatic ID message");
      // TODO on all ports
      AX25Packet idPacket = UIFrame.create(
          AX25Call.create("ID"),
          AX25Call.create("K4DBZ"),
          Protocol.NO_LAYER3,
          "Terrestrial Amateur Radio Packet Network node DAVID2 op is K4DBZ\r".getBytes(StandardCharsets.US_ASCII));
      //portManager.getOutboundPackets().add(idPacket);
    }, 15, 30, TimeUnit.SECONDS);


    scheduledExecutorService.scheduleWithFixedDelay(() -> {
      LOG.info("Sending automatic NODES message");
      // TODO on all ports

      byte[] msg = new byte[] {
          (byte)0xff,
          'D',
          'A',
          'V',
          'I',
          'D',
          '2'
      };
      UIFrame ui = UIFrame.create(
          AX25Call.create("NODES", 0),
          AX25Call.create("K4DBZ", 2),
          Protocol.NETROM, msg);
      portManager.getOutboundPackets().add(ui);
    }, 5, 300, TimeUnit.SECONDS);


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