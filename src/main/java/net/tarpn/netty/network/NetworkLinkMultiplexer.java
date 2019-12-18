package net.tarpn.netty.network;

import net.tarpn.netty.app.Application;
import net.tarpn.netty.app.Context;
import net.tarpn.network.netrom.NetworkPrimitive;
import net.tarpn.packet.impl.ax25.AX25Call;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class NetworkLinkMultiplexer  {

    private static final Logger LOG = LoggerFactory.getLogger(NetworkLinkMultiplexer.class);

    Map<AX25Call, Supplier<Application>> listeners = new HashMap<>();

    Map<Integer, NetworkLink> networkLinks = new HashMap<>();

    private class NetworkLink {
        final AX25Call localAddress;
        final AX25Call remoteAddress;
        Consumer<NetworkPrimitive> outboundConsumer;
        Consumer<NetworkPrimitive> inboundConsumer;

        public NetworkLink(AX25Call localAddress, AX25Call remoteAddress) {
            this.localAddress = localAddress;
            this.remoteAddress = remoteAddress;
        }

        void attachOutbound(Consumer<NetworkPrimitive> outboundConsumer) {
            this.outboundConsumer = outboundConsumer;
        }

        void attachInbound(Consumer<NetworkPrimitive> inboundConsumer) {
            this.inboundConsumer = inboundConsumer;
        }
    }

    /**
     * Listen for connections to a local callsign.
     * @param listenCall
     * @param onAccept
     * @throws IOException
     */
    public void listen(AX25Call listenCall, Supplier<Application> onAccept) {
        listeners.put(listenCall, onAccept);
    }

    public boolean forUs(AX25Call destCall) {
        return listeners.containsKey(destCall);
    }

    public void demux(NetworkPrimitive np, Consumer<NetworkPrimitive> outbound) throws IOException {
        NetworkLink networkLink = networkLinks.get(np.getCircuitId() & 0xff);
        if (networkLink == null) {
            LOG.info("Creating new circuit " + (np.getCircuitId() & 0xff) + " for " + np.getRemoteCall());
            networkLink = new NetworkLink(np.getLocalCall(), np.getRemoteCall());
            networkLink.attachOutbound(outbound);
            Application application = listeners.get(np.getLocalCall()).get();
            Consumer<NetworkPrimitive> newConsumer = networkPrimitive -> {
                Context appContext = new Context() {
                    @Override
                    public void write(byte[] msg) {
                        outbound.accept(NetworkPrimitive.newData(
                                networkPrimitive.getRemoteCall(),
                                networkPrimitive.getLocalCall(),
                                msg,
                                networkPrimitive.getCircuitId()
                        ));
                    }

                    @Override
                    public void flush() {
                        // no op
                    }

                    @Override
                    public void close() {
                        outbound.accept(NetworkPrimitive.newDisconnect(
                                networkPrimitive.getRemoteCall(),
                                networkPrimitive.getLocalCall(),
                                networkPrimitive.getCircuitId()
                        ));
                    }

                    @Override
                    public String remoteAddress() {
                        return np.getRemoteCall().toString();
                    }
                };

                // Handle the incoming DL event
                try {
                    switch (networkPrimitive.getType()) {
                        case NL_CONNECT:
                            application.onConnect(appContext);
                            break;
                        case NL_DISCONNECT:
                            // Here we get a disconnect indication or confirmation
                            application.onDisconnect(appContext);
                            networkLinks.remove(networkPrimitive.getCircuitId() & 0xff);
                            break;
                        case NL_INFO:
                            application.read(appContext, networkPrimitive.getInfo());
                            break;
                    }
                } catch (Exception e) {
                    try {
                        application.onError(appContext, e);
                    } catch (Exception e1) {
                        LOG.error("Something went wrong in our error handler", e1);
                    }
                }
            };
            networkLink.attachInbound(newConsumer);
        }
        // Validate
        if (!networkLink.localAddress.equals(np.getLocalCall())) {
            String msg = "Attempting to use circuit " + (np.getCircuitId() & 0xff) + " for " + np.getLocalCall() +
                    " but it has already been opened for " + networkLink.localAddress;
            LOG.error(msg);
        } else if (!networkLink.remoteAddress.equals(np.getRemoteCall())) {
            throw new IOException("Circuit " + (np.getCircuitId() & 0xff) + " is busy");
        } else {
            networkLink.inboundConsumer.accept(np);
        }
    }
}
