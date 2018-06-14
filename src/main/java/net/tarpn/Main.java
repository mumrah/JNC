package net.tarpn;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import net.tarpn.frame.FrameHandler;
import net.tarpn.frame.FrameReader;
import net.tarpn.frame.FrameWriter;
import net.tarpn.frame.impl.CompositeFrameHandler;
import net.tarpn.frame.impl.ConsoleFrameHandler;
import net.tarpn.frame.impl.KISSFrameReader;
import net.tarpn.frame.impl.KISSFrameWriter;
import net.tarpn.frame.impl.PacketReadingFrameHandler;
import net.tarpn.frame.impl.SimpleFrameReader;
import net.tarpn.frame.impl.SimpleFrameWriter;
import net.tarpn.impl.SerialDataPort;
import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketAction;
import net.tarpn.packet.PacketRequestHandler;
import net.tarpn.packet.PacketProtocol;
import net.tarpn.packet.impl.CommandProcessingPacketHandler;
import net.tarpn.packet.impl.CompositePacketHandler;
import net.tarpn.packet.impl.ConsolePacketHandler;
import net.tarpn.packet.impl.LastHeardPacketRouter;

public class Main {

  static Runnable newRunnable(FrameHandler handler, DataPort port) {
    return () -> {
      FrameReader frame = new KISSFrameReader(port.getName());
      InputStream stream = port.getInputStream();
      try {
        while(true) {
          while(stream.available() > 0) {
            frame.accept(stream.read(), handler);
          }
          Thread.sleep(50);
        }
      } catch (IOException | InterruptedException e) {
        throw new RuntimeException("Failed when polling " + port.getName(), e);
      }
    };
  }

  public static void main(String[] args) throws Exception {

    ScheduledExecutorService executorService = Executors.newScheduledThreadPool(4);

    // Set up a list of things to do
    Queue<PacketAction> actionQueue = new ConcurrentLinkedQueue<>();
    Queue<Packet> outgoingPackets = new ConcurrentLinkedQueue<>();

    Map<String, DataPort> portMap = new ConcurrentHashMap<>();

    // Special handler for keeping track of last-heard
    LastHeardPacketRouter router = new LastHeardPacketRouter(portMap::get);

    // List of packet handlers
    PacketRequestHandler packetHandler = CompositePacketHandler.wrap(
        router,
        new ConsolePacketHandler(),
        new CommandProcessingPacketHandler(actionQueue::add)
    );

    // Set up a list of handlers for incoming frames
    FrameHandler handler = CompositeFrameHandler.wrap(
        new ConsoleFrameHandler(),
        new PacketReadingFrameHandler(packetHandler, PacketProtocol.SIMPLE::fromBytes)
    );

    // Create a Port, configure it with a frame reader and connect it to our frame handler
    DataPort port0 = SerialDataPort.openPort("/dev/tty.usbmodem141321", 9600);
    port0.open();
    portMap.put(port0.getName(), port0);

    executorService.submit(newRunnable(handler, port0));

    // Port 1
    //DataPort port1 = SerialDataPort.openPort("/dev/tty.usbmodem141321", 9600);
    //port1.open();
    //portMap.put(port1.getName(), port1);
    //executorService.submit(newRunnable(handler, port1));

    executorService.scheduleWithFixedDelay(() -> {
      PacketAction action = actionQueue.poll();
      if(action != null) {
        System.err.println(action);
        action.run(outgoingPackets::add);
      }
    }, 100, 10, TimeUnit.MILLISECONDS);

    executorService.scheduleWithFixedDelay(() -> {
      Packet packet = outgoingPackets.poll();
      if(packet != null) {
        System.err.println("Sending " + packet);
        FrameWriter writer = new KISSFrameWriter();
        byte[] frame = PacketProtocol.SIMPLE.toBytes(packet);
        writer.accept(frame, bytes -> {
          try {
            port0.getOutputStream().write(bytes);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        });
      }
    }, 100, 10, TimeUnit.MILLISECONDS);

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
      try {
        port0.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
      try {
        executorService.awaitTermination(1000, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }));
  }
}