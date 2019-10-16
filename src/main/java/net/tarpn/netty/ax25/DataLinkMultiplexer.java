package net.tarpn.netty.ax25;

import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.netty.app.Application;
import net.tarpn.netty.app.Context;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.util.Util;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * This class is the nexus between layer 2 and layer 3
 *
 * The ports bind to this class so DL events can be written to them
 *
 * <pre>
 *     multiplexer.bind(1, ax25Handler);
 * </pre>
 *
 * And higher layer components can attach so they can receive DL events for a remote call
 *
 * <pre>
 *     multiplexer.attach(1, remoteCall, appHandler);
 * </pre>
 *
 */
public class DataLinkMultiplexer implements Multiplexer {
    private final Map<Integer, MultiplexedPort> knownPorts = new HashMap<>();

    public PortChannel bind(int port, Consumer<DataLinkPrimitive> dataLinkConsumer) throws IOException {
        if (knownPorts.containsKey(port)) {
            throw new PortInUseException(port);
        } else {
            MultiplexedPort mp = new MultiplexedPort(port);
            mp.attachPort(dataLinkConsumer);
            knownPorts.put(port, mp);
            return new PortChannel() {
                @Override
                public int getPort() {
                    return port;
                }

                @Override
                public void write(DataLinkPrimitive dl) {
                    mp.writeToL3(dl);
                }

                @Override
                public void close() {
                    mp.l3Consumers.keySet().forEach(mp::closeLayer3);
                }
            };
        }
    }

    // TODO figure this out ...
    public DataLinkChannel attach(int port, Application application) throws IOException {
        MultiplexedPort stream = knownPorts.get(port);
        if (stream == null) {
            throw new NoSuchPortException(port);
        } else {

            // This consumer is for DL events coming _from_ the port
            Consumer<DataLinkPrimitive> consumer = dl -> {
                Context appContext = new Context() {
                    @Override
                    public void write(String msg) {
                        msg = "(" + port + ") " + msg;
                        stream.writeToPort(DataLinkPrimitive.newDataRequest(
                                dl.getRemoteCall(),
                                dl.getLocalCall(),
                                AX25Packet.Protocol.NO_LAYER3,
                                Util.ascii(msg)
                        ));
                    }

                    @Override
                    public void flush() {
                        // no op
                    }

                    @Override
                    public void close() {
                        stream.closeLayer3(dl.getRemoteCall());
                    }

                    @Override
                    public String remoteAddress() {
                        return dl.getRemoteCall().toString();
                    }
                };

                try {
                    switch (dl.getType()) {
                        case DL_CONNECT:
                            application.onConnect(appContext);
                            break;
                        case DL_DISCONNECT:
                            application.onDisconnect(appContext);
                            break;
                        case DL_DATA:
                        case DL_UNIT_DATA:
                            application.read(appContext, dl.getLinkInfo().getInfoAsASCII());
                            break;
                        case DL_ERROR:
                            application.onError(appContext, new Exception(dl.getError().getMessage()));
                            break;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            };

            if (stream.attachLayer3(AX25Call.WILDCARD, consumer)) {
                return new DataLinkChannel() {
                    @Override
                    public AX25Call getRemoteCall() {
                        return AX25Call.WILDCARD;
                    }

                    @Override
                    public AX25Call getLocalCall() {
                        return null;
                    }

                    @Override
                    public void write(DataLinkPrimitive dl) {
                        stream.writeToL3(dl);
                    }

                    @Override
                    public void close() {
                        stream.closeLayer3(AX25Call.WILDCARD);
                    }
                };
            } else {
                throw new PortInUseException(port);
            }
        }
    }

    public DataLinkChannel connect(int port, AX25Call remoteCall, Consumer<DataLinkPrimitive> l3Consumer) throws IOException {
        MultiplexedPort stream = knownPorts.get(port);
        if (stream == null) {
            throw new NoSuchPortException(port);
        } else {
            if (stream.attachLayer3(remoteCall, l3Consumer)) {
                return new DataLinkChannel() {
                    @Override
                    public AX25Call getRemoteCall() {
                        return remoteCall;
                    }

                    @Override
                    public AX25Call getLocalCall() {
                        return null;
                    }

                    @Override
                    public void write(DataLinkPrimitive dl) {
                        stream.writeToPort(dl);
                    }

                    @Override
                    public void close() {
                        stream.closeLayer3(remoteCall);
                    }
                };
            } else {
                throw new PortInUseException(port);
            }
        }
    }

    private class MultiplexedPort {

        private final int portNumber;
        private Consumer<DataLinkPrimitive> portConsumer;
        private Map<AX25Call, Consumer<DataLinkPrimitive>> l3Consumers;

        public MultiplexedPort(int portNumber) {
            this.portNumber = portNumber;
            this.l3Consumers = new HashMap<>();
        }

        public void attachPort(Consumer<DataLinkPrimitive> portConsumer) {
            this.portConsumer = portConsumer;
        }

        public boolean attachLayer3(AX25Call remoteCall, Consumer<DataLinkPrimitive> l3Consumer) {
            return this.l3Consumers.putIfAbsent(remoteCall, l3Consumer) == null;
        }

        public boolean closeLayer3(AX25Call remoteCall) {
            return this.l3Consumers.remove(remoteCall) != null;
        }

        public void writeToPort(DataLinkPrimitive msg) {
            if (portConsumer != null) {
                portConsumer.accept(msg);
            }
        }

        public void writeToL3(DataLinkPrimitive msg) {
            Consumer<DataLinkPrimitive> l3 = l3Consumers.get(msg.getRemoteCall());
            if (l3 == null) {
                l3 = l3Consumers.get(AX25Call.WILDCARD);
                if (l3 != null) {
                    attachLayer3(msg.getRemoteCall(), l3);
                } else {
                    l3 = __ -> { };
                }
            }
            l3.accept(msg);
        }
    }

    public static class PortInUseException extends IOException {
        PortInUseException(int port) {
            super("Port " + port + " is in use");
        }
    }

    public static class NoSuchPortException extends IOException {
        NoSuchPortException(int port) {
            super("No such port " + port);
        }
    }
}
