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
import net.tarpn.network.netrom.*;
import net.tarpn.util.Util;
import net.tarpn.config.NetRomConfig;
import net.tarpn.config.PortConfig;
import net.tarpn.datalink.LinkPrimitive;
import net.tarpn.datalink.LinkPrimitive.Type;
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
    private final Queue<LinkPrimitive> level2Events;
    private final Map<Integer, DataLink> dataPorts;
    private final NetRomRoutingTable router;
    private final NetRomCircuitManager circuitManager;

    private final Map<String, Consumer<NetworkPrimitive>> networkLinkListeners;


    private NetworkManager2(NetRomConfig netromConfig) {
        this.netromConfig = netromConfig;
        this.level2Events = new ConcurrentLinkedQueue<>();
        this.dataPorts = new HashMap<>();
        this.router = new NetRomRoutingTable(netromConfig, portNum -> dataPorts.get(portNum).getPortConfig(), getNewNeighborHandler());
        this.circuitManager = new NetRomCircuitManager(netromConfig, router(), this::onNetworkLinkEvent);
        this.networkLinkListeners = new HashMap<>();
    }

    public static NetworkManager2 create(NetRomConfig config) {
        return new NetworkManager2(config);
    }

    private void onNetworkLinkEvent(NetworkPrimitive linkPrimitive) {
        LOG.info("L3: " + linkPrimitive);
        networkLinkListeners.values().forEach(consumer -> consumer.accept(linkPrimitive));
    }

    public void addNetworkLinkListener(String listenerName, Consumer<NetworkPrimitive> listener) {
        networkLinkListeners.put(listenerName, listener);
    }

    public void removeNetworkLinkListener(String listenerName) {
        networkLinkListeners.remove(listenerName);
    }

    public void acceptNetworkPrimitive(NetworkPrimitive networkPrimitive) {
        int circuitId = circuitManager.getCircuit(networkPrimitive.getRemoteCall());
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

    public void initialize(PortConfig portConfig) {
        if(portConfig.isEnabled()) {
            DataPort dataPort = PortFactory.createPortFromConfig(portConfig);
            DataLink dataLink = DataLink.create(
                    portConfig, dataPort, executorService);
            dataLink.addDataLinkListener("network", level2Events::add);
            dataLink.setExtraPacketHandler(getNetRomNodesHandler());
            dataPorts.put(dataPort.getPortNumber(), dataLink);
        }
    }

    /**
     * Accept a level 2 primitive, find the appropriate port to route it to, and send it
     */
    private boolean route(LinkPrimitive event) {
        List<AX25Call> potentialRoutes = router.routePacket(event.getRemoteCall());
        boolean routed = false;

        for(AX25Call route : potentialRoutes) {
            Neighbor neighbor = router.getNeighbors().get(route);
            int routePort = neighbor.getPort();
            DataLink portManager = dataPorts.get(routePort);
            AX25State state = portManager.getAx25StateHandler().getState(route);
            if(state.getState().equals(State.DISCONNECTED)) {
                portManager.sendDataLinkEvent(LinkPrimitive.newConnectRequest(neighbor.getNodeCall()));
            }
            // Change the dest address to the neighbor and send it
            LinkPrimitive readdressed = event.copyOf(neighbor.getNodeCall());
            portManager.sendDataLinkEvent(readdressed);
            routed = true;
            break;
        }

        if(!routed) {
            LOG.warn("No route to destination " + event.getRemoteCall() + ", known routes: " + potentialRoutes);
        }

        return routed;
    }

    public void start() {
        // Start up ports
        dataPorts.values().forEach(DataLink::start);

        // Start up router and event processor
        executorService.submit(() -> {

            // Start event processing
            Util.queueProcessingLoop(
                    level2Events::poll,
                    dataLinkPrimitive -> {
                        LOG.info("Got DL event " + dataLinkPrimitive);
                        // Only pass data and unit data up to L3, everything else is ignored
                        if(dataLinkPrimitive.getType().equals(Type.DL_DATA) || dataLinkPrimitive.getType().equals(Type.DL_UNIT_DATA)) {
                            circuitManager.handleInfo(dataLinkPrimitive.getLinkInfo());
                        }
                    },
                    (failedEvent, t) -> LOG.error("Error processing event " + failedEvent, t));
        });

        // Send automatic NODES packets
        executorService.scheduleAtFixedRate(() -> {
            NetRomNodes nodes = router.getNodes();
            byte[] nodesData = NetRomNodes.write(nodes);
            for(DataLink portManager : dataPorts.values()) {
                LOG.info("Sending automatic NODES message on " + portManager.getDataPort() + ": " + nodes);
                portManager.getAx25StateHandler().getEventQueue().add(
                        AX25StateEvent.createUnitDataEvent(AX25Call.create("NODES"), Protocol.NETROM, nodesData));
            }
        }, 15, netromConfig.getNodesInterval(), TimeUnit.SECONDS);

        // Prune routing table
        executorService.scheduleAtFixedRate(router::pruneRoutes,
                30, netromConfig.getNodesInterval(), TimeUnit.SECONDS);
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

    private NetRomRouter router() {
        return netRomPacket -> route(
                LinkPrimitive.newDataRequest(
                        netRomPacket.getDestNode(),
                        Protocol.NETROM,
                        netRomPacket.getPayload()));
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
            int routePort = neighbor.getPort();
            DataLink portManager = dataPorts.get(routePort);
            AX25State state = portManager.getAx25StateHandler().getState(neighbor.getNodeCall());
            if(state.getState().equals(State.DISCONNECTED)) {
                portManager.sendDataLinkEvent(LinkPrimitive.newConnectRequest(neighbor.getNodeCall()));
            }
        };
    }
}
