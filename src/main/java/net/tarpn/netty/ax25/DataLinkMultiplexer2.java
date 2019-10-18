package net.tarpn.netty.ax25;

import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.netty.app.Application;
import net.tarpn.netty.app.Context;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.util.Util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class DataLinkMultiplexer2  {

    Map<AX25Address, MultiplexedPort> dataPorts = new HashMap<>();

    // This channel is opened in the SerialChannel pipeline
    public PortChannel bind(AX25Address localAddress, Consumer<DataLinkPrimitive> dataLinkConsumer) throws IOException {
        if (dataPorts.containsKey(localAddress)) {
            throw new DataLinkMultiplexer.PortInUseException(localAddress.port);
        } else {
            MultiplexedPort mp = new MultiplexedPort(localAddress);
            mp.attachPort(dataLinkConsumer);
            dataPorts.put(localAddress, mp);
            return new PortChannel() {
                @Override
                public int getPort() {
                    return localAddress.port;
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

    public DataLinkChannel connect(AX25Address localAddress, AX25Address remoteAddress, Consumer<DataLinkPrimitive> l3Consumer) throws IOException {
        MultiplexedPort port = dataPorts.get(localAddress);
        if (port == null) {
            throw new DataLinkMultiplexer.NoSuchPortException(localAddress.port);
        } else {
            if (port.attachLayer3(remoteAddress.call, l3Consumer)) {
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
                        port.closeLayer3(remoteAddress.call);
                    }
                };
            } else {
                throw new DataLinkMultiplexer.PortInUseException(localAddress.port);
            }
        }
    }

    // This is called from the application main after the ports are all bound
    public void listen(AX25Address listenAddress, Supplier<Application> onAccept) throws IOException {
        MultiplexedPort port = dataPorts.get(listenAddress);
        if (port == null) {
            throw new DataLinkMultiplexer.NoSuchPortException(listenAddress.port);

        }

        // Attach a catch-all consumer
        boolean attached = port.attachLayer3(AX25Call.WILDCARD, dl -> {
            Consumer<DataLinkPrimitive> l3Consumer = port.l3Consumers.get(dl.getRemoteCall());
            if (l3Consumer == null) {
                // first time seeing this remote call on this port, set up new consumer
                Application application = onAccept.get();
                Consumer<DataLinkPrimitive> newL3Consumer = dl1 -> {
                    Context appContext = new Context() {
                        @Override
                        public void write(String msg) {
                            // TODO don't actually do this. We should not adulterate messages passed to L3
                            msg = port.localAddress + "} " + msg;
                            port.writeToPort(DataLinkPrimitive.newDataRequest(
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
                            port.closeLayer3(dl1.getRemoteCall());
                        }

                        @Override
                        public String remoteAddress() {
                            return dl1.getRemoteCall().toString();
                        }
                    };

                    try {
                        switch (dl1.getType()) {
                            case DL_CONNECT:
                                application.onConnect(appContext);
                                break;
                            case DL_DISCONNECT:
                                application.onDisconnect(appContext);
                                break;
                            case DL_DATA:
                            case DL_UNIT_DATA:
                                application.read(appContext, dl1.getLinkInfo().getInfoAsASCII());
                                break;
                            case DL_ERROR:
                                application.onError(appContext, new Exception(dl1.getError().getMessage()));
                                break;
                        }
                    } catch (Exception e) {
                        try {
                            application.onError(appContext, e);
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }
                };
                port.attachLayer3(dl.getRemoteCall(), newL3Consumer);
                l3Consumer = newL3Consumer;
            }
            l3Consumer.accept(dl);
        });

        if (!attached) {
            throw new DataLinkMultiplexer.PortInUseException(listenAddress.port);
        }
    }


    private class MultiplexedPort {
        final AX25Address localAddress;
        Consumer<DataLinkPrimitive> portConsumer;

        // Map of remote address to l3 consumer
        Map<AX25Call, Consumer<DataLinkPrimitive>> l3Consumers = new HashMap<>();

        public MultiplexedPort(AX25Address localAddress) {
            this.localAddress = localAddress;
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
                l3 = l3Consumers.getOrDefault(AX25Call.WILDCARD, __ -> { });
            }
            l3.accept(msg);
        }

        PortChannel portChannel() {
            return new PortChannel() {
                @Override
                public int getPort() {
                    return localAddress.port;
                }

                @Override
                public void write(DataLinkPrimitive dl) {
                    writeToL3(dl);
                }

                @Override
                public void close() {
                    //l3Consumers.keySet().forEach(this::closeLayer3);
                }
            };
        }

    }
}
