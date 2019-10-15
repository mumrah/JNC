package net.tarpn.netty.ax25;

import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.packet.impl.ax25.AX25Call;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class DataLinkMultiplexer {
    private static Map<Integer, MultiplexedPort> knownPorts = new HashMap<>();

    private static class MultiplexedPort {

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
            l3Consumers.getOrDefault(msg.getRemoteCall(), __ -> { }).accept(msg);
        }
    }

    public static PortChannel bind(int port, Consumer<DataLinkPrimitive> dataLinkConsumer) throws IOException {
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

    public static DataLinkChannel connect(int port, AX25Call remoteCall, Consumer<DataLinkPrimitive> l3Consumer) throws IOException {
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
