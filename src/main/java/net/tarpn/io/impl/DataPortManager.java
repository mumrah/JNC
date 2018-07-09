package net.tarpn.io.impl;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import net.tarpn.Util;
import net.tarpn.config.Configuration;
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
import net.tarpn.frame.impl.PCapDumpFrameHandler;
import net.tarpn.frame.impl.PacketReadingFrameHandler;
import net.tarpn.io.DataPort;
import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketHandler;
import net.tarpn.packet.PacketReader;
import net.tarpn.packet.PacketRequest;
import net.tarpn.packet.impl.AX25PacketReader;
import net.tarpn.packet.impl.CompositePacketHandler;
import net.tarpn.packet.impl.ConsolePacketHandler;
import net.tarpn.packet.impl.DefaultPacketRequest;
import net.tarpn.packet.impl.DestinationFilteringPacketHandler;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25PacketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataPortManager {
  private static final Logger LOG = LoggerFactory.getLogger(DataPortManager.class);

  private final Configuration config;
  private final DataPort dataPort;
  private final Queue<PacketRequest> inboundPackets;
  private final Queue<Packet> outboundPackets;
  private final PCapDumpFrameHandler pCapDumpFrameHandler;
  private final PacketHandler externalHandler;
  private final AX25PacketHandler ax25StateHandler;
  private final Object portLock = new Object();

  private DataPortManager(
      Configuration config,
      DataPort dataPort,
      Queue<PacketRequest> inboundPackets,
      Queue<Packet> outboundPackets,
      Consumer<AX25Packet> networkPacketConsumer,
      PacketHandler externalHandler) {
    this.config = config;
    this.dataPort = dataPort;
    this.inboundPackets = inboundPackets;
    this.outboundPackets = outboundPackets;
    this.pCapDumpFrameHandler = new PCapDumpFrameHandler();
    this.ax25StateHandler = new AX25PacketHandler(config, outboundPackets::add, networkPacketConsumer);
    this.externalHandler = externalHandler;
  }

  public static DataPortManager initialize(
      Configuration config,
      DataPort port,
      Consumer<AX25Packet> networkPacketConsumer,
      PacketHandler externalHandler) {
    return new DataPortManager(
        config,
        port,
        new ConcurrentLinkedQueue<>(),
        new ConcurrentLinkedQueue<>(),
        networkPacketConsumer,
        externalHandler
    );
  }

  public DataPort getDataPort() {
    return dataPort;
  }

  public Queue<PacketRequest> getInboundPackets() {
    return inboundPackets;
  }

  public Queue<Packet> getOutboundPackets() {
    return outboundPackets;
  }

  public AX25PacketHandler getAx25StateHandler() {
    return ax25StateHandler;
  }

  public Runnable getReaderRunnable() {
    return () -> {
      FrameReader frameReader = new KISSFrameReader(dataPort.getPortNumber());
      PacketReader packetReader = new AX25PacketReader();
      PacketHandler packetHandler = CompositePacketHandler.wrap(
          new ConsolePacketHandler(),
          externalHandler,
          new DestinationFilteringPacketHandler(config.getNodeCall()),
          ax25StateHandler
      );

      Consumer<Packet> packetConsumer = packet -> packetHandler.onPacket(
          new DefaultPacketRequest(getDataPort().getPortNumber(), packet, getOutboundPackets()::add));


      // Run these as we get new data frames from the port
      FrameHandler frameHandler = CompositeFrameHandler.wrap(
          new ConsoleFrameHandler(),
          pCapDumpFrameHandler,
          new KISSCommandHandler(),
          new PacketReadingFrameHandler(packetReader, packetConsumer)
      );

      Consumer<Frame> frameConsumer = frame ->
          outboundPackets.add(new PayloadOnlyPacket("Fake", frame.getData()));

      // Read bytes from the DataPort and feed into the frame reader
      InputStream inputStream = new BufferedInputStream(dataPort.getInputStream());
      try {
        while(true) {
          // don't write anything while we're reading
          synchronized (portLock) {
            // Read off any input data
            while(inputStream.available() > 0) {
              int d = inputStream.read();
              frameReader.accept(d, frame -> {
                frameHandler.onFrame(new DefaultFrameRequest(frame, frameConsumer));
              });
            }
          }
          // yield the lock so the writer may obtain it
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
        synchronized (portLock) {
          LOG.info("Sending data to port " + dataPort.getPortNumber());
          try {
            outputStream.write(bytes);
            outputStream.flush();
          } catch (IOException e) {
            LOG.error("Error writing to DataPort " + dataPort.getPortNumber(), e);
          }
        }
      };

      FrameWriter frameWriter = new KISSFrameWriter();

      while(true) {
        try {
          Packet outgoingPacket = outboundPackets.poll();
          if (outgoingPacket != null) {
            LOG.info("Sending " + outgoingPacket);
            if(outgoingPacket instanceof AX25Packet) {
              pCapDumpFrameHandler.dump(outgoingPacket.getPayload());
            }
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

  /**
   * A "fake" packet which just contains the payload, no source or destination information.
   * This is used for things like responding to KISS commands.
   */
  private static class PayloadOnlyPacket implements Packet {

    private final String id;
    private final byte[] payload;

    PayloadOnlyPacket(String id, byte[] payload) {
      this.id = id;
      this.payload = payload;
    }

    @Override
    public byte[] getPayload() {
      return payload;
    }

    @Override
    public String getSource() {
      return id;
    }

    @Override
    public String getDestination() {
      return id;
    }

    @Override
    public String toString() {
      return "PayloadOnlyPacket{" +
          "id='" + id + '\'' +
          ", payload=" + Util.toEscapedASCII(payload) +
          '}';
    }
  }
}
