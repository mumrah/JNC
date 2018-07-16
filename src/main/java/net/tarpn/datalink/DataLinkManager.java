package net.tarpn.datalink;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import net.tarpn.Util;
import net.tarpn.config.PortConfig;
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
import net.tarpn.packet.impl.AX25PacketReader;
import net.tarpn.packet.impl.CompositePacketHandler;
import net.tarpn.packet.impl.ConsolePacketHandler;
import net.tarpn.packet.impl.DefaultPacketRequest;
import net.tarpn.packet.impl.DestinationFilteringPacketHandler;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;
import net.tarpn.packet.impl.ax25.AX25StateEvent;
import net.tarpn.packet.impl.ax25.AX25StateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DataLinkManager {

  private static final ScheduledExecutorService PORT_RECOVERY_EXECUTOR = Executors
      .newSingleThreadScheduledExecutor();

  private static final Logger LOG = LoggerFactory.getLogger(DataLinkManager.class);

  private final PortConfig portConfig;
  private final DataPort dataPort;
  private final Queue<Packet> outboundPackets;
  private final PCapDumpFrameHandler pCapDumpFrameHandler;
  private final PacketHandler externalHandler;
  private final AX25StateMachine ax25StateHandler;
  private final Object portLock = new Object();
  private final ScheduledExecutorService executorService;

  private IOException fault;
  private ScheduledFuture<?> recoveryThread;

  private DataLinkManager(
      PortConfig portConfig,
      DataPort dataPort,
      Consumer<LinkPrimitive> dataLinkEvents,
      PacketHandler externalHandler,
      ScheduledExecutorService executorService) {
    this.portConfig = portConfig;
    this.dataPort = dataPort;
    this.outboundPackets = new ConcurrentLinkedQueue<>();
    this.pCapDumpFrameHandler = new PCapDumpFrameHandler();
    this.ax25StateHandler = new AX25StateMachine(portConfig, outboundPackets::add, dataLinkEvents);
    this.externalHandler = externalHandler;
    this.executorService = executorService;
  }

  public static DataLinkManager create(
      PortConfig config,
      DataPort port,
      Consumer<LinkPrimitive> dataLinkEvents,
      PacketHandler externalHandler) {
    return create(config, port, dataLinkEvents, externalHandler, Executors.newScheduledThreadPool(128));
  }

  public static DataLinkManager create(
      PortConfig config,
      DataPort port,
      Consumer<LinkPrimitive> dataLinkEvents,
      PacketHandler externalHandler,
      ScheduledExecutorService executorService) {

    DataLinkManager manager = new DataLinkManager(
        config,
        port,
        dataLinkEvents,
        externalHandler,
        executorService
    );

    try {
      port.open();
    } catch (IOException e) {
      manager.setFault(e);
      LOG.warn("Could not open port " + port + ". Continuing anyways", e);
    }

    // Start automated ID broadcast
    executorService.scheduleWithFixedDelay(() -> {
      LOG.info("Sending automatic ID message on " + port);
     manager.getAx25StateHandler().getEventQueue().add(
          AX25StateEvent.createUnitDataEvent(
              AX25Call.create("ID"),
              Protocol.NO_LAYER3,
              config.getIdMessage().getBytes(StandardCharsets.US_ASCII)));
    }, 5, config.getIdInterval(), TimeUnit.SECONDS);
    return manager;
  }

  public void start() {
    executorService.submit(getReaderRunnable());
    executorService.submit(getWriterRunnable());
    executorService.submit(getAx25StateHandler().getRunnable());
    // TODO submit recurring thread to send KISS params to TNC (Persist, SlotTime, etc)
  }

  public void stop() {
    executorService.shutdownNow();
  }

  public boolean hasFault() {
    return fault != null;
  }

  public IOException getFault() {
    return fault;
  }

  private void setFault(IOException e) {
    this.fault = e;
    this.recoveryThread = executorService.scheduleWithFixedDelay(() -> {
      if(dataPort.reopen()) {
        LOG.info("Recovered connection to " + dataPort);
        this.fault = null;
        this.recoveryThread.cancel(true);
        this.recoveryThread = null;
      } else {
        LOG.info("Still no connection to " + dataPort + ". Trying again later");
      }
    }, 100, 5000, TimeUnit.MILLISECONDS); // TODO configure this timer?
  }

  public DataPort getDataPort() {
    return dataPort;
  }

  public PortConfig getPortConfig() {
    return portConfig;
  }

  public void acceptDataLinkPrimitive(LinkPrimitive event) {
    switch (event.getType()) {

      case DL_CONNECT:
        ax25StateHandler.getEventQueue().add(AX25StateEvent.createConnectEvent(event.getRemoteCall()));
        break;
      case DL_DISCONNECT:
        ax25StateHandler.getEventQueue().add(AX25StateEvent.createDisconnectEvent(event.getRemoteCall()));
        break;
      case DL_DATA:
        ax25StateHandler.getEventQueue().add(
            AX25StateEvent.createDataEvent(
                event.getRemoteCall(),
                event.getPacket().getProtocol(),
                event.getPacket().getInfo()));
        break;
      case DL_UNIT_DATA:
        ax25StateHandler.getEventQueue().add(
            AX25StateEvent.createUnitDataEvent(
                event.getRemoteCall(),
                event.getPacket().getProtocol(),
                event.getPacket().getInfo()));
        break;
      case DL_ERROR:
      default:
        break;
    }
  }

  public AX25StateMachine getAx25StateHandler() {
    return ax25StateHandler;
  }

  private Runnable getReaderRunnable() {
    return () -> {
      FrameReader frameReader = new KISSFrameReader(dataPort.getPortNumber());
      PacketReader packetReader = new AX25PacketReader();
      PacketHandler packetHandler = CompositePacketHandler.wrap(
          new ConsolePacketHandler(),
          externalHandler,
          new DestinationFilteringPacketHandler(portConfig.getNodeCall()),
          ax25StateHandler
      );

      Consumer<Packet> packetConsumer = packet -> packetHandler.onPacket(
          new DefaultPacketRequest(getDataPort().getPortNumber(), packet, outboundPackets::add));

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
      try {
        while(!Thread.currentThread().isInterrupted()) {
          // don't write anything while we're reading
          synchronized (portLock) {
            if(hasFault()) {
              //LOG.warn(dataPort + " has a fault, cannot read");
            } else if(!dataPort.isOpen()) {
              setFault(new IOException("Port is unexpectedly closed, cannot read"));
            } else {
              // Read off any input data
              InputStream inputStream = dataPort.getInputStream();
              try {
                while(inputStream.available() > 0) {
                  int d = inputStream.read();
                  frameReader.accept(d, frame -> {
                    frameHandler.onFrame(new DefaultFrameRequest(frame, frameConsumer));
                  });
                }
              } catch (IOException e) {
                LOG.error("Failed when polling " + dataPort.getName(), e);
                setFault(e);
              }
            }
          }
          // yield the lock so the writer may obtain it
          Thread.sleep(50);
        }
      } catch( InterruptedException e) {
        Thread.currentThread().interrupt();
      }
    };
  }

  /**
   * Consuming packets to be written from our internal queue and pass them to a {@link FrameWriter}
   * which converts the packet as a byte stream suitable to write out to the port
   * @return
   */
  private Runnable getWriterRunnable() {
    return () -> {
      FrameWriter frameWriter = new KISSFrameWriter();

      Util.queueProcessingLoop(outboundPackets::poll, outgoingPacket -> {
        LOG.info("Sending " + outgoingPacket);
        if (outgoingPacket instanceof AX25Packet) {
          pCapDumpFrameHandler.dump(outgoingPacket.getPayload());
        }
        Frame outgoingFrame = new KISSFrame(0, Command.Data, outgoingPacket.getPayload());
        frameWriter.accept(outgoingFrame, bytes -> {
          synchronized (portLock) {
            if(hasFault()) {
              LOG.warn(dataPort + " has a fault, cannot write", getFault());
            } else if(!dataPort.isOpen()) {
              setFault(new IOException("Port is unexpectedly closed, cannot write"));
            } else {
              LOG.info("Sending data to port " + dataPort.getPortNumber());
              OutputStream outputStream = dataPort.getOutputStream();
              try {
                outputStream.write(bytes);
                outputStream.flush();
              } catch (IOException e) {
                LOG.error("Error writing to DataPort " + dataPort.getPortNumber(), e);
                setFault(e);
              }
            }
          }
        });
      }, (failedPacket, t) -> LOG.error("Error handling outgoing packet " + failedPacket, t));
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
