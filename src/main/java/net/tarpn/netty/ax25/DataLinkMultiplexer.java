package net.tarpn.netty.ax25;

import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.netty.app.Application;
import net.tarpn.netty.app.Context;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DataLinkMultiplexer implements Multiplexer {

    private static final Logger LOG = LoggerFactory.getLogger(DataLinkMultiplexer.class);

    Map<AX25Address, MultiplexedPort> dataPorts = new HashMap<>();

    // This channel is opened in the SerialChannel pipeline
    @Override
    public PortChannel bind(AX25Address localAddress, Consumer<DataLinkPrimitive> dataLinkConsumer) throws IOException {
        if (dataPorts.containsKey(localAddress)) {
            throw new PortInUseException(localAddress);
        } else {
            LOG.info("Binding to " + localAddress);
            MultiplexedPort mp = new MultiplexedPort(localAddress);
            mp.attachPort(dataLinkConsumer);
            dataPorts.put(localAddress, mp);
            // Return a PortChannel that can be used to write to the port and to close it
            return new PortChannel() {
                @Override
                public int getPort() {
                    return localAddress.port;
                }

                @Override
                public void write(DataLinkPrimitive dl) {
                    mp.writeToDataLink(dl);
                }

                @Override
                public void close() {
                    for (AX25Call l3Consumer : mp.dataLinks.keySet()) {
                        mp.closeDataLink(l3Consumer);
                    }
                    dataPorts.remove(localAddress);
                }
            };
        }
    }

    /**
     * Connect a data link consumer to a port which has been bound. If the layer 2 port for the local address hasn't
     * been bound, throw an error. If a data link channel already exists for the remote address, also throw an error.
     *
     * Returns a DataLinkChannel that can be used for writing and closing the channel.
     *
     * @param localAddress
     * @param remoteAddress
     * @param dataLink
     * @return
     * @throws IOException
     */
    @Override
    public DataLinkChannel connect(AX25Address localAddress, AX25Address remoteAddress,
                                   Consumer<DataLinkPrimitive> dataLink) throws IOException {
        MultiplexedPort port = dataPorts.get(localAddress);
        if (port == null) {
            throw new NoSuchPortException(localAddress);
        } else {
            if (port.openDataLink(remoteAddress.call, dataLink)) {
                return new DataLinkChannel() {
                    @Override
                    public AX25Call getRemoteCall() {
                        return remoteAddress.call;
                    }

                    @Override
                    public AX25Call getLocalCall() {
                        return localAddress.call;
                    }

                    @Override
                    public void write(DataLinkPrimitive dl) {
                        port.writeToPort(dl);
                    }

                    @Override
                    public void close() {
                        port.closeDataLink(remoteAddress.call);
                    }
                };
            } else {
                throw new PortInUseException(localAddress);
            }
        }
    }

    @Override
    public void write(AX25Address localAddress, DataLinkPrimitive dl) throws IOException {
        MultiplexedPort port = dataPorts.get(localAddress);
        if (port == null) {
            throw new NoSuchPortException(localAddress);
        } else {
            port.writeToPort(dl);
        }
    }
    /**
     * Listen on a given address for incoming connections. Upon making a new connection,
     * create a new instance of an application using the given supplier and pass the DL
     * primitive up to the application.
     *
     * @param listenAddress
     * @param onAccept
     * @throws IOException
     */
    @Override
    public void listen(AX25Address listenAddress, Supplier<Application> onAccept) throws IOException {
        MultiplexedPort port = dataPorts.get(listenAddress);
        if (port == null) {
            throw new NoSuchPortException(listenAddress);
        }

        // Attach a catch-all consumer, accept connections from any source address
        boolean attached = port.openDataLink(AX25Call.WILDCARD, dl -> {
            Consumer<DataLinkPrimitive> dataLink = port.dataLinks.get(dl.getRemoteCall());
            if (dataLink == null) {
                // first time seeing this remote call on this port, set up new consumer
                Application application = onAccept.get();
                Consumer<DataLinkPrimitive> newDataLink = dl1 -> {
                    // Adapt to the Context interface, this is the callback passed to the application
                    Context appContext = new Context() {
                        @Override
                        public void write(byte[] msg) {
                            // TODO maybe don't do this here?
                            String stringMsg = port.localAddress + "} " + Util.ascii(msg) + "\r";
                            port.writeToPort(DataLinkPrimitive.newDataRequest(
                                    dl.getRemoteCall(),
                                    dl.getLocalCall(),
                                    AX25Packet.Protocol.NO_LAYER3,
                                    Util.ascii(stringMsg)
                            ));
                        }

                        @Override
                        public void flush() {
                            // no op
                        }

                        @Override
                        public void close() {
                            port.writeToPort(DataLinkPrimitive.newDisconnectRequest(
                                    dl.getRemoteCall(),
                                    dl.getLocalCall()
                            ));
                        }

                        @Override
                        public String remoteAddress() {
                            return dl1.getRemoteCall().toString();
                        }
                    };

                    // Handle the incoming DL event
                    try {
                        switch (dl1.getType()) {
                            case DL_CONNECT:
                                application.onConnect(appContext);
                                break;
                            case DL_DISCONNECT:
                                // Here we get a disconnect indication or confirmation
                                application.onDisconnect(appContext);
                                port.closeDataLink(dl1.getRemoteCall());
                                break;
                            case DL_DATA:
                            case DL_UNIT_DATA:
                                application.read(appContext, dl1.getLinkInfo().getInfo());
                                break;
                            case DL_ERROR:
                                throw new DataLinkException(dl1.getError());
                        }
                    } catch (Exception e) {
                        try {
                            application.onError(appContext, e);
                        } catch (Exception e1) {
                            LOG.error("Something went wrong in our error handler", e1);
                        }
                    }
                };
                port.openDataLink(dl.getRemoteCall(), newDataLink);
                dataLink = newDataLink;
            }
            dataLink.accept(dl);
        });

        // If we couldn't attach an data link consumer at this address, then it's already been attached.
        if (!attached) {
            throw new PortInUseException(listenAddress);
        } else {
            LOG.info("Listening at " + listenAddress);
        }
    }

    /**
     * Represents a connected L2 port. Incoming data is routed to a data-link based on the remote
     * callsign.
     */
    private class MultiplexedPort {
        final AX25Address localAddress;
        Consumer<DataLinkPrimitive> portConsumer;

        // Map of remote address to DL consumer
        Map<AX25Call, Consumer<DataLinkPrimitive>> dataLinks = new HashMap<>();

        public MultiplexedPort(AX25Address localAddress) {
            this.localAddress = localAddress;
        }

        public void attachPort(Consumer<DataLinkPrimitive> portConsumer) {
            LOG.info("Binding port " + localAddress);
            this.portConsumer = portConsumer;
        }

        public boolean openDataLink(AX25Call remoteCall, Consumer<DataLinkPrimitive> dataLinkConsumer) {
            LOG.info("Opening DataLink to " + remoteCall + " on port " + localAddress);
            return this.dataLinks.putIfAbsent(remoteCall, dataLinkConsumer) == null;
        }

        public boolean closeDataLink(AX25Call remoteCall) {
            LOG.info("Closing DataLink to " + remoteCall + " on port " + localAddress);
            return this.dataLinks.remove(remoteCall) != null;
        }

        public void writeToPort(DataLinkPrimitive msg) {
            if (portConsumer != null) {
                portConsumer.accept(msg);
            }
        }

        public void writeToDataLink(DataLinkPrimitive msg) {
            Consumer<DataLinkPrimitive> dataLink = dataLinks.get(msg.getRemoteCall());
            if (dataLink == null) {
                // If no specific handler has been register for this remote call, use the catch-all
                dataLink = dataLinks.getOrDefault(AX25Call.WILDCARD, __ -> { });
            }
            dataLink.accept(msg);
        }

        @Override
        public String toString() {
            return "MultiplexedPort{" +
                    "localAddress=" + localAddress +
                    '}';
        }
    }

    public static class PortInUseException extends IOException {
        PortInUseException(AX25Address address) {
            super("Address " + address + " is in use");
        }
    }

    public static class NoSuchPortException extends IOException {
        NoSuchPortException(AX25Address address) {
            super("No such port " + address.port);
        }
    }

    public static class DataLinkException extends IOException {
        DataLinkException(DataLinkPrimitive.ErrorType error) {
            super("Got an error from layer 2: " + error.getMessage());
        }
    }
}
