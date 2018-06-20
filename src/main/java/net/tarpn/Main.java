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
import net.tarpn.io.impl.SerialDataPort;
import net.tarpn.io.impl.SocketDataPortServer;
import net.tarpn.message.Message;
import net.tarpn.network.NetRomPacketHandler;
import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketHandler;
import net.tarpn.packet.PacketReader;
import net.tarpn.packet.PacketWriter;
import net.tarpn.packet.impl.AX25PacketReader;
import net.tarpn.packet.impl.AX25PacketWriter;
import net.tarpn.packet.impl.CompositePacketHandler;
import net.tarpn.packet.impl.ConsolePacketHandler;
import net.tarpn.packet.impl.DefaultPacketRequest;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;
import net.tarpn.packet.impl.ax25.AX25Packet.UnnumberedFrame.ControlType;
import net.tarpn.packet.impl.ax25.IFrame;
import net.tarpn.packet.impl.ax25.UFrame;
import net.tarpn.packet.impl.ax25.UIFrame;
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
          new NetRomPacketHandler()
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
          Thread.sleep(500);
        }
      } catch (IOException | InterruptedException e) {
        LOG.error("Failed when polling " + port.getName(), e);
      }
    };
  }

  public static void main(String[] args) throws Exception {
    ExecutorService executorService = Executors.newCachedThreadPool();
    ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();


    Queue<Message> inboundMessages = new ConcurrentLinkedQueue<>();
    Queue<Packet> outgoingPackets = new ConcurrentLinkedQueue<>();

    // Create a Port, configure it with a frame reader and connect it to our frame handler
    //DataPort port0 = SerialDataPort.openPort(0,"/dev/tty.usbmodem141321", 9600);
    //port0.open();

    //FakeDataPort port0 = new FakeDataPort("Python;Java;A>B>4>PING\n");
    //port0.open();
    //FrameReader frameReader = new SimpleFrameReader(port0.getPortNumber());
    //executorService.submit(newPortHandler(port0, frameReader, inboundMessages));

    // Port 1
    DataPort port1 = SerialDataPort.openPort(1, "/dev/tty.wchusbserial1410", 9600);
    port1.open();
    //portMap.put(port1.getName(), port1);


    Queue<Frame> outgoingFrames = new ConcurrentLinkedQueue<>();
    executorService.submit(newPortReader(port1, outgoingFrames));
    executorService.submit(newPortWriter(port1, outgoingFrames));

    scheduledExecutorService.scheduleWithFixedDelay(() -> {
      LOG.info("Sending automatic ID message");
      // TODO on all ports
      UIFrame ui = UIFrame.create("K4DBZ-3", "K4DBZ-2", Protocol.NO_LAYER3,
          "Testing packet forwarding\r".getBytes(StandardCharsets.US_ASCII));
      PacketWriter packetWriter = new AX25PacketWriter();
      //packetWriter.accept(ui, outgoingFrames::add);
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
      UIFrame ui = UIFrame.create("K4DBZ-2", "NODES-0", Protocol.NETROM, msg);
      PacketWriter packetWriter = new AX25PacketWriter();
      packetWriter.accept(ui, outgoingFrames::add);
    }, 5, 300, TimeUnit.SECONDS);



    // Input Messages
    /*
    executorService.scheduleWithFixedDelay(() -> {
      Message message = inboundMessages.poll();
      if(message != null) {
        messageHandler.onMessage(message, outgoingMessage -> {
          messageWriter.accept(outgoingMessage, outgoingPackets::add);
        });
        System.err.println("Got Message: " + new String(message.getMessage(), StandardCharsets.UTF_8));
      }
    }, 100, 10, TimeUnit.MILLISECONDS);
    */

    /*
    // Output Packets
    executorService.scheduleWithFixedDelay(() -> {
      Packet packet = outgoingPackets.poll();
      if(packet != null) {
        //DataPort targetPort = router.routePacket(packet.);
        System.err.println("Sending " + packet);
        FrameWriter frameWriter = new SimpleFrameWriter();
        PacketWriter packetWriter = new SimplePacketWriter();
        Consumer<byte[]> toDataPort = bytes -> {
          try {
            port0.getOutputStream().write(bytes);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        };
        packetWriter.accept(packet, frame -> frameWriter.accept(frame, toDataPort));
      }
    }, 100, 10, TimeUnit.MILLISECONDS);
    */

    SocketDataPortServer server = new SocketDataPortServer(outgoingFrames);
    executorService.submit(server);

    /*
    // Simulate sending some data to the frame reader
    FrameReader frame = new SimpleFrameReader(handler::onFrame);
    frame.accept("?\n");
    frame.accept("C 0 TARPN\n");

    // See what actions need to happen (send data somewhere?)
    System.err.println(actions);

    //DataPortRouter router = SimpleDataPortRouter.create();
    for(CommandAction action : actions) {
      action.run(request -> {
        System.err.println("Send " + request.getFrame() + " on Port " + request.getPort());

      });
    }

    */

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