package net.tarpn.datalink;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.function.Consumer;

import net.tarpn.util.Clock;
import net.tarpn.util.Util;
import net.tarpn.config.PortConfig;
import net.tarpn.frame.Frame;
import net.tarpn.frame.FrameHandler;
import net.tarpn.frame.FrameReader;
import net.tarpn.frame.FrameWriter;
import net.tarpn.frame.impl.CompositeFrameHandler;
import net.tarpn.frame.impl.DefaultFrameRequest;
import net.tarpn.frame.impl.KISS.Command;
import net.tarpn.frame.impl.KISSCommandHandler;
import net.tarpn.frame.impl.KISSFrame;
import net.tarpn.frame.impl.KISSFrameReader;
import net.tarpn.frame.impl.KISSFrameWriter;
import net.tarpn.frame.impl.PacketReadingFrameHandler;
import net.tarpn.io.DataPort;
import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketHandler;
import net.tarpn.packet.PacketReader;
import net.tarpn.packet.impl.AX25PacketReader;
import net.tarpn.packet.impl.CompositePacketHandler;
import net.tarpn.packet.impl.DefaultPacketRequest;
import net.tarpn.packet.impl.DestinationFilteringPacketHandler;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;
import net.tarpn.packet.impl.ax25.AX25StateEvent;
import net.tarpn.packet.impl.ax25.AX25StateMachine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides a data link layer interface. Supports connecting to a remote station, sending and receiving data frames, and
 * disconnecting.
 */
public class DataLink {

    private static final ScheduledExecutorService PORT_RECOVERY_EXECUTOR = Executors
            .newSingleThreadScheduledExecutor();

    private static final Logger LOG = LoggerFactory.getLogger(DataLinkManager.class);

    private final PortConfig portConfig;
    private final DataPort dataPort;
    private final Queue<Packet> outboundPackets;
    private final AX25StateMachine ax25StateHandler;
    private final Object portLock = new Object();
    private final ScheduledExecutorService executorService;
    private final Map<String, Consumer<DataLinkPrimitive>> dataLinkListeners;
    private final Clock clock;

    private PacketHandler extraPacketHandler;
    private IOException fault;
    private ScheduledFuture<?> recoveryThread;

    private DataLink(
            PortConfig portConfig,
            DataPort dataPort,
            ScheduledExecutorService executorService,
            Clock clock) {
        this.portConfig = portConfig;
        this.dataPort = dataPort;
        this.outboundPackets = new ConcurrentLinkedQueue<>();
        this.ax25StateHandler = new AX25StateMachine(portConfig, outboundPackets::add, this::onDataLinkEvent, clock);
        this.executorService = executorService;
        this.dataLinkListeners = new HashMap<>();
        this.clock = clock;
    }

    public static DataLink create(
            PortConfig config,
            DataPort port) {
        return create(config, port, Executors.newScheduledThreadPool(128), Clock.getRealClock());
    }

    public static DataLink create(
            PortConfig config,
            DataPort port,
            ScheduledExecutorService executorService,
            Clock clock) {

        DataLink dataLink = new DataLink(
                config,
                port,
                executorService,
                clock
        );

        //String level = config.getString("log.level", "info");
       // Level dataLinkLevel = Level.getLevel(level.toUpperCase());
        //Configurator.setLevel(DataLinkManager.class.getName(), dataLinkLevel);

        try {
            port.open();
        } catch (IOException e) {
            dataLink.setFault(e);
            LOG.warn("Could not open port " + port + ". Continuing anyways", e);
        }

        // Start automated ID broadcast
        if (config.getIdInterval() > 0) {
            executorService.scheduleWithFixedDelay(() -> {
                LOG.info("Sending automatic ID message on " + port);
                dataLink.getAx25StateHandler().getEventQueue().add(
                        AX25StateEvent.createUnitDataEvent(
                                AX25Call.create("ID", 0),
                                Protocol.NO_LAYER3,
                                config.getIdMessage().getBytes(StandardCharsets.US_ASCII)));
            }, 5, config.getIdInterval(), TimeUnit.SECONDS);
        } else {
            LOG.warn("Continuing without ID broadcast enabled. You shouldn't do this if you're actually on the air.");
        }
        return dataLink;
    }

    /**
     * Called when we receive DL events from the AX.25 state machine
     * @param dataLinkPrimitive
     */
    private void onDataLinkEvent(DataLinkPrimitive dataLinkPrimitive) {
        dataLinkListeners.values().forEach(consumer -> consumer.accept(dataLinkPrimitive));
    }

    public void addDataLinkListener(String listenerName, Consumer<DataLinkPrimitive> listener) {
        dataLinkListeners.put(listenerName, listener);
    }

    public void removeDataLinkListener(String listenerName) {
        dataLinkListeners.remove(listenerName);
    }

    public void setExtraPacketHandler(PacketHandler packetHandler) {
        extraPacketHandler = packetHandler;
    }

    public DataPort getDataPort() {
        return dataPort;
    }

    public PortConfig getPortConfig() {
        return portConfig;
    }

    public boolean hasFault() {
        return fault != null;
    }

    public IOException getFault() {
        return fault;
    }

    public AX25StateMachine getAx25StateHandler() {
        return ax25StateHandler;
    }

    public void start() {
        executorService.submit(getReaderRunnable());
        executorService.submit(getWriterRunnable());
        executorService.submit(getAx25StateHandler().getRunnable());
        // TODO submit recurring thread to send KISS params to TNC (Persist, SlotTime, etc)
    }

    public void join() {
        while(!executorService.isShutdown()) {
            try {
                clock.sleep(10);
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void stop() {
        executorService.shutdownNow();
        try {
            dataPort.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Accept a {@link DataLinkPrimitive} from a higher layer (Network or Application)
     * @param event
     */
    public void sendDataLinkEvent(DataLinkPrimitive event) {
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
                                event.getLinkInfo().getProtocol(),
                                event.getLinkInfo().getInfo()));
                break;
            case DL_UNIT_DATA:
                ax25StateHandler.getEventQueue().add(
                        AX25StateEvent.createUnitDataEvent(
                                event.getRemoteCall(),
                                event.getLinkInfo().getProtocol(),
                                event.getLinkInfo().getInfo()));
                break;
            case DL_ERROR:
            default:
                break;
        }
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
                LOG.debug("Still no connection to " + dataPort + ". Trying again later");
            }
        }, 100, 5000, TimeUnit.MILLISECONDS); // TODO configure this timer?
    }

    private Runnable getReaderRunnable() {
        return () -> {
            FrameReader frameReader = new KISSFrameReader(dataPort.getPortNumber());
            PacketReader packetReader = new AX25PacketReader();
            PacketHandler packetHandler = CompositePacketHandler.wrap(
                    packetRequest -> LOG.debug("Got Packet: " + packetRequest.getPacket()),       // trace logs
                    extraPacketHandler != null ? extraPacketHandler : packetRequest -> {},        // Add the extra handler, if defined
                    new DestinationFilteringPacketHandler(portConfig.getNodeCall()),              // ignore packets not bound for us
                    ax25StateHandler                                                              // pass to the ax.25 state machine
            );

            Consumer<Packet> packetConsumer = packet -> packetHandler.onPacket(
                    new DefaultPacketRequest(getDataPort().getPortNumber(), packet, outboundPackets::add));

            // Run these as we get new data frames from the port
            FrameHandler frameHandler = CompositeFrameHandler.wrap(
                    frameRequest -> LOG.trace("Got Frame: " + frameRequest.getFrame()), // trace logs
                    new KISSCommandHandler(),                                           // kiss deframing
                    new PacketReadingFrameHandler(packetReader, packetConsumer)         // packet handler
            );

            Consumer<Frame> frameConsumer = frame ->
                    outboundPackets.add(new PayloadOnlyPacket("Fake", frame.getData()));

            // Read bytes from the DataPort and feed into the frame reader
            try {
                while(!Thread.currentThread().isInterrupted()) {
                    // don't write anything while we're reading
                    synchronized (portLock) {
                        if(hasFault()) {
                            LOG.warn(dataPort + " has a fault, cannot read");
                        } else if(!dataPort.isOpen()) {
                            setFault(new IOException("Port is unexpectedly closed, cannot read"));
                        } else {
                            // Read off any input data
                            InputStream inputStream = dataPort.getInputStream();
                            try {
                                while(inputStream.available() > 0) {
                                    int d = inputStream.read();
                                    LOG.trace("Read byte from port " + portConfig.getPortNumber() + ": " + Util.toHexString(d));
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
                    clock.sleep(10);
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

            Util.queueProcessingLoop(
                outboundPackets::poll,
                outgoingPacket -> {
                    LOG.debug("Sending " + outgoingPacket + " to port " + dataPort.getPortNumber());
                    Frame outgoingFrame = new KISSFrame(0, Command.Data, outgoingPacket.getPayload());
                    frameWriter.accept(outgoingFrame, bytes -> {
                        synchronized (portLock) {
                            if(hasFault()) {
                                LOG.warn(dataPort + " has a fault, cannot write", getFault());
                            } else if(!dataPort.isOpen()) {
                                setFault(new IOException("Port is unexpectedly closed, cannot write"));
                            } else {
                                LOG.trace("Writing to port " + dataPort.getPortNumber() + "\n" + Util.toHexDump(bytes));
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
                },
                (failedPacket, t) -> LOG.error("Error handling outgoing packet " + failedPacket, t),
                clock
            );
        };
    }

    /**
     * A "fake" packet which just contains the payload, no source or destination information.
     * This is used for things like responding to KISS commands.
     */
    public static class PayloadOnlyPacket implements Packet {

        private final String id;
        private final byte[] payload;

        public PayloadOnlyPacket(String id, byte[] payload) {
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
