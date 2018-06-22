package net.tarpn.io.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import net.tarpn.frame.Frame;
import net.tarpn.frame.FrameHandler;
import net.tarpn.frame.FrameReader;
import net.tarpn.frame.FrameWriter;
import net.tarpn.frame.impl.CompositeFrameHandler;
import net.tarpn.frame.impl.ConsoleFrameHandler;
import net.tarpn.frame.impl.DefaultFrameRequest;
import net.tarpn.frame.impl.KISS.Command;
import net.tarpn.frame.impl.KISSCommandHandler;
import net.tarpn.frame.impl.KISSFrame;
import net.tarpn.frame.impl.KISSFrameReader;
import net.tarpn.frame.impl.KISSFrameWriter;
import net.tarpn.frame.impl.PacketReadingFrameHandler;
import net.tarpn.io.DataPort;
import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketReader;
import net.tarpn.packet.impl.AX25PacketReader;
import net.tarpn.packet.impl.ax25.FakePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataPortManager {
  private static final Logger LOG = LoggerFactory.getLogger(DataPortManager.class);

  private final DataPort dataPort;
  private final Queue<Packet> inboundPackets;
  private final Queue<Packet> outboundPackets;

  private DataPortManager(
      DataPort dataPort,
      Queue<Packet> inboundPackets,
      Queue<Packet> outboundPackets) {
    this.dataPort = dataPort;
    this.inboundPackets = inboundPackets;
    this.outboundPackets = outboundPackets;
  }

  public static DataPortManager initialize(DataPort port) {
    return new DataPortManager(
        port,
        new ConcurrentLinkedQueue<>(),
        new ConcurrentLinkedQueue<>()
    );
  }

  public DataPort getDataPort() {
    return dataPort;
  }

  public Queue<Packet> getInboundPackets() {
    return inboundPackets;
  }

  public Queue<Packet> getOutboundPackets() {
    return outboundPackets;
  }

  public Runnable getReaderRunnable() {
    return () -> {
      FrameReader frameReader = new KISSFrameReader(dataPort.getPortNumber());
      PacketReader packetReader = new AX25PacketReader();

      // Run these as we get new data frames from the port
      FrameHandler frameHandler = CompositeFrameHandler.wrap(
          new ConsoleFrameHandler(),
          new KISSCommandHandler(),
          new PacketReadingFrameHandler(packetReader, inboundPackets::add)
      );

      Consumer<Frame> frameConsumer = frame ->
          outboundPackets.add(FakePacket.create("Fake", frame.getData()));

      // Read bytes from the DataPort and feed into the frame reader
      InputStream inputStream = new BufferedInputStream(dataPort.getInputStream());
      try {
        while(true) {
          // Read off any input data
          while(inputStream.available() > 0) {
            int d = inputStream.read();
            frameReader.accept(d, frame -> {
              frameHandler.onFrame(new DefaultFrameRequest(frame, frameConsumer));
            });
          }
          Thread.sleep(50);
        }
      } catch (IOException | InterruptedException e) {
        LOG.error("Failed when polling " + dataPort.getName(), e);
      }
    };
  }

  public Runnable getWriterRunnable() {
    return () -> {
      OutputStream outputStream = dataPort.getOutputStream();

      // TODO need to sync output to port (frame queue or a lock)
      Consumer<byte[]> toDataPort = bytes -> {
        LOG.info("Sending data to port " + dataPort.getPortNumber());
        try {
          for(int i=0; i<bytes.length; i++) {
            byte b = bytes[i];
            //System.err.println("KISS WRITE " + b + "\t" + String.format("%02X", b) + "\t" + Character.toString((char)b));
          }

          outputStream.write(bytes);
          outputStream.flush();
        } catch (IOException e) {
          LOG.error("Error writing to DataPort " + dataPort.getPortNumber(), e);
        }
      };

      FrameWriter frameWriter = new KISSFrameWriter();

      while(true) {
        try {
          Packet outgoingPacket = outboundPackets.poll();
          if (outgoingPacket != null) {
            LOG.info("Sending " + outgoingPacket);
            Frame outgoingFrame = new KISSFrame(0, Command.Data, outgoingPacket.getPayload());
            frameWriter.accept(outgoingFrame, toDataPort);
          } else {
            Thread.sleep(50);
          }
        } catch (Throwable t) {
          LOG.error("Error writing frame", t);
        }
      }
    };
  }
}
