package net.tarpn.network;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import net.tarpn.datalink.DataLink;
import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.network.netrom.*;
import net.tarpn.network.netrom.packet.NetRomPacket;
import net.tarpn.util.Clock;
import net.tarpn.util.Util;
import net.tarpn.config.NetRomConfig;
import net.tarpn.config.PortConfig;
import net.tarpn.datalink.DataLinkPrimitive.Type;
import net.tarpn.io.DataPort;
import net.tarpn.io.impl.PortFactory;
import net.tarpn.network.netrom.NetRomRoutingTable.Neighbor;
import net.tarpn.packet.PacketHandler;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.packet.impl.ax25.AX25Packet.FrameType;
import net.tarpn.packet.impl.ax25.AX25Packet.Protocol;
import net.tarpn.packet.impl.ax25.AX25State;
import net.tarpn.packet.impl.ax25.AX25State.State;
import net.tarpn.packet.impl.ax25.AX25StateEvent;
import net.tarpn.packet.impl.ax25.UIFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * Networking layer (level 3)
 *
 * Handle incoming NET/ROM packets from the Data Link layer (level 2) and decide what to do with
 * them.
 *
 */
public class NetworkManager2 {

    private static final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(128);

    private static final Logger LOG = LoggerFactory.getLogger(NetworkManager.class);

    private final NetRomConfig netromConfig;
    private final Queue<DataLinkPrimitive> dataLinkPrimitiveQueue;
    private final Map<Integer, DataLink> dataPorts;
    private final NetRomRoutingTable router;
    private final NetRomCircuitManager circuitManager;
    private final Map<String, Consumer<NetworkPrimitive>> networkLinkListeners;
    private final Clock clock;

    private NetworkManager2(NetRomConfig netromConfig, Clock clock) {
        this.netromConfig = netromConfig;
        this.dataLinkPrimitiveQueue = new ConcurrentLinkedQueue<>();
        this.dataPorts = new HashMap<>();
        this.router = new NetRomRoutingTable(netromConfig, portNum -> dataPorts.get(portNum).getPortConfig(),
                getNewNeighborHandler());
        this.circuitManager = new NetRomCircuitManager(netromConfig, this::route, this::handleNetworkPrimitive);
        this.networkLinkListeners = new HashMap<>();
        this.clock = clock;
        MDC.put("node", netromConfig.getNodeAlias());
    }

    public static NetworkManager2 create(NetRomConfig config) {
        return new NetworkManager2(config, Clock.getRealClock());
    }

    public static NetworkManager2 create(NetRomConfig config, Clock clock) {
        return new NetworkManager2(config, clock);
    }

    public void addNetworkLinkListener(String listenerName, Consumer<NetworkPrimitive> listener) {
        networkLinkListeners.put(listenerName, listener);
    }

    public void removeNetworkLinkListener(String listenerName) {
        networkLinkListeners.remove(listenerName);
    }

    public void initialize(PortConfig portConfig) {
        if(portConfig.isEnabled()) {
            DataPort dataPort = PortFactory.createPortFromConfig(portConfig);
            DataLink dataLink = DataLink.create(
                    portConfig, dataPort, executorService, clock);
            // A a listener for layer 2 events (connected, disconnected, etc)
            dataLink.addDataLinkListener("network", dataLinkPrimitive -> {
                dataLinkPrimitive.setPort(dataPort.getPortNumber());
                dataLinkPrimitiveQueue.add(dataLinkPrimitive);
            });
            dataLink.setExtraPacketHandler(getNetRomNodesHandler());
            dataPorts.put(dataPort.getPortNumber(), dataLink);
        }
    }

    public void start() {
        // Start up ports
        dataPorts.values().forEach(DataLink::start);

        // Start up router and event processor
        executorService.submit(() -> {
            MDC.put("node", netromConfig.getNodeAlias());

            // Start event processing
            Util.queueProcessingLoop(
                    dataLinkPrimitiveQueue::poll,
                    dataLinkPrimitive -> {
                        LOG.info("Got Layer 2 event from data link: " + dataLinkPrimitive);
                        // Learn about new neighbors
                        if (dataLinkPrimitive.getPort() != -1) {
                            router.addNeighbor(dataLinkPrimitive.getRemoteCall(), dataLinkPrimitive.getPort());
                        }

                        // Only pass data and unit data up to L3, everything else is ignored
                        if(dataLinkPrimitive.getType().equals(Type.DL_DATA) || dataLinkPrimitive.getType().equals(Type.DL_UNIT_DATA)) {
                            circuitManager.handleInfo(dataLinkPrimitive.getLinkInfo());
                        }
                    },
                    (failedEvent, t) -> LOG.error("Error processing event " + failedEvent, t), clock);
        });

        // Send automatic NODES packets
        if (netromConfig.getNodesInterval() > 0) {
            executorService.scheduleAtFixedRate(this::broadcastRoutingTable, 15,
                    netromConfig.getNodesInterval(), TimeUnit.SECONDS);

            // Prune routing table TODO separate timer
            executorService.scheduleAtFixedRate(router::pruneRoutes,
                    30, netromConfig.getNodesInterval(), TimeUnit.SECONDS);
        }


    }

    public void join() {
        while(!executorService.isShutdown()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void stop() {
        try {
            executorService.shutdownNow();
            executorService.awaitTermination(10, TimeUnit.SECONDS);
            dataPorts.values().forEach(dataPort -> {
                try {
                    dataPort.getDataPort().close();
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (InterruptedException | UncheckedIOException e) {
            throw new RuntimeException("Clean shutdown failed", e);
        }
    }

    public NetRomSocket open(AX25Call address) {
        return new NetRomSocket(address, circuitManager);
    }

    /**
     * Accept a {@link NetworkPrimitive} and send it to the circuit manager.
     * @param networkPrimitive
     */
    public void acceptNetworkPrimitive(NetworkPrimitive networkPrimitive) {
        int circuitId = circuitManager.getOrCreateCircuit(networkPrimitive.getRemoteCall());
        switch (networkPrimitive.getType()) {
            case NL_CONNECT:
                circuitManager.onCircuitEvent(
                        new NetRomCircuitEvent(circuitId, networkPrimitive.getRemoteCall(),
                                NetRomCircuitEvent.Type.NL_CONNECT));
                break;
            case NL_DISCONNECT:
                circuitManager.onCircuitEvent(
                        new NetRomCircuitEvent(circuitId, networkPrimitive.getRemoteCall(),
                                NetRomCircuitEvent.Type.NL_DISCONNECT));
                break;
            case NL_INFO:
                circuitManager.onCircuitEvent(
                        new NetRomCircuitEvent.UserDataEvent(circuitId, networkPrimitive.getRemoteCall(),
                                networkPrimitive.getInfo()));
                break;
        }
    }

    void broadcastRoutingTable() {
        MDC.put("node", netromConfig.getNodeAlias());

        NetRomNodes nodes = router.getNodes();
        byte[] nodesData = NetRomNodes.write(nodes);
        for(DataLink portManager : dataPorts.values()) {
            LOG.info("Sending automatic NODES message on " + portManager.getDataPort() + ": " + nodes);
            portManager.getAx25StateHandler().getEventQueue().add(
                    AX25StateEvent.createUnitDataEvent(AX25Call.create("NODES"), Protocol.NETROM, nodesData));
        }
    }

    NetRomCircuit getCircuit(AX25Call remoteCall) {
        return circuitManager.getCircuit(remoteCall).orElse(null);
    }

    /**
     * Handle a {@link NetworkPrimitive} emitted from the circuit manager
     * @param linkPrimitive
     */
    private void handleNetworkPrimitive(NetworkPrimitive linkPrimitive) {
        LOG.debug("Layer 3 primitive: " + linkPrimitive);
        networkLinkListeners.values().forEach(consumer -> consumer.accept(linkPrimitive));
    }

    /**
     * Accept a NET/ROM packet, construct a L2 packet, find the appropriate port to route it to,
     * re-address the packet, and send it.
     *
     * This method implements {@link NetRomRouter}.
     */
    private boolean route(NetRomPacket netRomPacket) {
        DataLinkPrimitive dataLinkPrimitive = DataLinkPrimitive.newDataRequest(
                netRomPacket.getDestNode(),
                Protocol.NETROM,
                netRomPacket.getPayload());
        List<AX25Call> potentialRoutes = router.routePacket(dataLinkPrimitive.getRemoteCall());
        boolean routed = false;

        for(AX25Call route : potentialRoutes) {
            Neighbor neighbor = router.getNeighbors().get(route);
            int routePort = neighbor.getPort();
            DataLink portManager = dataPorts.get(routePort);
            AX25State state = portManager.getAx25StateHandler().getState(route);
            if(state.getState().equals(State.DISCONNECTED)) {
                portManager.sendDataLinkEvent(DataLinkPrimitive.newConnectRequest(neighbor.getNodeCall()));
            }
            // Change the dest address to the neighbor and send it
            DataLinkPrimitive readdressed = dataLinkPrimitive.copyOf(neighbor.getNodeCall());
            portManager.sendDataLinkEvent(readdressed);
            routed = true;
            break;
        }

        if(!routed) {
            LOG.warn("No route to destination " + dataLinkPrimitive.getRemoteCall() + ", known routes: " + potentialRoutes);
        }

        return routed;
    }

    /**
     * A special {@link PacketHandler} which handles routing table packets sent to the NODES destination
     * @return
     */
    private PacketHandler getNetRomNodesHandler() {
        return packetRequest -> {
            if(packetRequest.getPacket() instanceof AX25Packet) {
                AX25Packet ax25Packet = (AX25Packet)packetRequest.getPacket();
                if(ax25Packet.getFrameType().equals(FrameType.UI)) {
                    UIFrame uiFrame = (UIFrame)ax25Packet;
                    if(uiFrame.getDestCall().equals(AX25Call.create("NODES", 0))) {
                        NetRomNodes nodes = NetRomNodes.read(uiFrame.getInfo());
                        router.updateNodes(uiFrame.getSourceCall(), packetRequest.getPort(), nodes);
                        packetRequest.abort();
                    }
                }
            }
        };
    }

    private Consumer<Neighbor> getNewNeighborHandler() {
        return neighbor -> {
            LOG.info("Connecting to new neighbor " + neighbor);
            int routePort = neighbor.getPort();
            DataLink portManager = dataPorts.get(routePort);
            AX25State state = portManager.getAx25StateHandler().getState(neighbor.getNodeCall());
            if(state.getState().equals(State.DISCONNECTED)) {
                portManager.sendDataLinkEvent(DataLinkPrimitive.newConnectRequest(neighbor.getNodeCall()));
            }
        };
    }
}
