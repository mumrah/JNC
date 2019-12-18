package net.tarpn.netty.network;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.internal.TypeParameterMatcher;
import net.tarpn.config.Configs;
import net.tarpn.config.NetRomConfig;
import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.netty.ax25.AX25Address;
import net.tarpn.netty.ax25.Multiplexer;
import net.tarpn.network.netrom.*;
import net.tarpn.network.netrom.handlers.*;
import net.tarpn.network.netrom.packet.NetRomPacket;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class NetRomStateHandler extends ChannelDuplexHandler {

    private static final Logger LOG = LoggerFactory.getLogger(NetRomStateHandler.class);

    private final Map<NetRomCircuit.State, StateHandler> stateHandlers = new HashMap<>();
    private final Map<Integer, NetRomCircuit> circuits = new ConcurrentHashMap<>();
    private final NetRomConfig netRomConfig;
    private final NetRomRoutingTable routingTable;
    private final Multiplexer multiplexer;

    public NetRomStateHandler(Configs allConfigs, Multiplexer multiplexer) {
        this.netRomConfig = allConfigs.getNetRomConfig();
        this.multiplexer = multiplexer;
        this.routingTable = new NetRomRoutingTable(netRomConfig, allConfigs.getPortConfigs()::get, this::onNewNeighbor);
        this.stateHandlers.put(NetRomCircuit.State.AWAITING_CONNECTION, new AwaitingConnectionStateHandler());
        this.stateHandlers.put(NetRomCircuit.State.CONNECTED, new ConnectedStateHandler());
        this.stateHandlers.put(NetRomCircuit.State.AWAITING_RELEASE, new AwaitingReleaseStateHandler());
        this.stateHandlers.put(NetRomCircuit.State.DISCONNECTED, new DisconnectedStateHandler());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        TypeParameterMatcher matcher = TypeParameterMatcher.get(NetRomPacket.class);
        if (matcher.match(msg)) {
            NetRomPacket packet = (NetRomPacket) msg;
            LOG.debug("NET/ROM read: " + packet);
            if (packet.getDestNode().callMatches(netRomConfig.getNodeCall())) {
                // Check if destination address is for us
                NetRomCircuitEvent event = toEvent(packet);
                processCircuitEvent(ctx, event);
            } else {
                // If it's not for us, forward it
                route(packet);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        TypeParameterMatcher matcher = TypeParameterMatcher.get(NetworkPrimitive.class);
        if (matcher.match(msg)) {
            NetworkPrimitive np = (NetworkPrimitive) msg;
            LOG.debug("NET/ROM write: " + np);
            NetRomCircuitEvent event = toEvent(np);
            processCircuitEvent(ctx, event);
        } else {
            ctx.write(msg);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof NetRomNodesHandler.NodesEvent) {
            NetRomNodesHandler.NodesEvent event = (NetRomNodesHandler.NodesEvent) evt;
            routingTable.updateNodes(event.heardFrom, event.heardOnPort, event.nodes);
        }
    }

    private void processCircuitEvent(ChannelHandlerContext ctx, NetRomCircuitEvent event) {
        NetRomCircuit circuit = circuits.computeIfAbsent(event.getCircuitId() & 0xff, newCircuitId -> {
            // if we're creating a new circuit, need to look-up the
            return new NetRomCircuit(newCircuitId, event.getRemoteCall(), netRomConfig.getNodeCall(), netRomConfig);
        });
        StateHandler handler = stateHandlers.get(circuit.getState());
        if(handler != null) {
            LOG.info("BEFORE: " + circuit + " got " + event);
            NetRomCircuit.State newState = handler.handle(circuit, event, nl -> {
                LOG.info("Got NL: " + nl + " from circuit " + circuit.getCircuitId());
                ctx.fireChannelRead(nl);
            }, this::route);
            circuit.setState(newState);
            LOG.info("AFTER : " + circuit);
        } else {
            LOG.error("No handler found for state " + circuit.getState());
        }
    }

    private void onNewNeighbor(NetRomRoutingTable.Neighbor neighbor) {
        LOG.info("Got new neighbor: " + neighbor);
    }

    private boolean route(NetRomPacket netRomPacket) {
        DataLinkPrimitive dataLinkPrimitive = DataLinkPrimitive.newDataRequest(
                netRomPacket.getDestNode(),
                netRomPacket.getOriginNode(),
                AX25Packet.Protocol.NETROM,
                netRomPacket.getPayload());
        List<AX25Call> potentialRoutes = routingTable.routePacket(dataLinkPrimitive.getRemoteCall());
        boolean routed = false;

        for(AX25Call route : potentialRoutes) {
            NetRomRoutingTable.Neighbor neighbor = routingTable.getNeighbors().get(route);
            int routePort = neighbor.getPort();
            AX25Address localAddress = new AX25Address(routePort, netRomConfig.getNodeCall());
            DataLinkPrimitive readdressed = dataLinkPrimitive.readdress(neighbor.getNodeCall());
            try {
                multiplexer.write(localAddress, readdressed);
                LOG.info("Routing " + netRomPacket + " via " + localAddress);
                routed = true;
            } catch (IOException e) {
                LOG.error("Couldn't write NET/ROM", e);
            }
            break;
        }

        if(!routed) {
            LOG.warn("No route to destination " + dataLinkPrimitive.getRemoteCall() + ", known routes: " + potentialRoutes);
        }

        return routed;
    }

    private NetRomCircuitEvent toEvent(NetworkPrimitive primitive) {
        switch (primitive.getType()) {
            case NL_CONNECT:
                return new NetRomCircuitEvent(
                        primitive.getCircuitId(),
                        primitive.getRemoteCall(),
                        NetRomCircuitEvent.Type.NETROM_CONNECT);
            case NL_DISCONNECT:
                return new NetRomCircuitEvent(
                        primitive.getCircuitId(),
                        primitive.getRemoteCall(),
                        NetRomCircuitEvent.Type.NETROM_DISCONNECT);
            case NL_INFO:
                return new NetRomCircuitEvent.UserDataEvent(
                        primitive.getCircuitId(),
                        primitive.getRemoteCall(),
                        primitive.getInfo());
            default:
                throw new IllegalStateException();
        }
    }

    private NetRomCircuitEvent toEvent(NetRomPacket packet) {
        final NetRomCircuitEvent.Type eventType;
        switch (packet.getOpType()) {
            case ConnectRequest:
                eventType = NetRomCircuitEvent.Type.NETROM_CONNECT;
                break;
            case ConnectAcknowledge:
                eventType = NetRomCircuitEvent.Type.NETROM_CONNECT_ACK;
                break;
            case DisconnectRequest:
                eventType = NetRomCircuitEvent.Type.NETROM_DISCONNECT;
                break;
            case DisconnectAcknowledge:
                eventType = NetRomCircuitEvent.Type.NETROM_DISCONNECT_ACK;
                break;
            case Information:
                eventType = NetRomCircuitEvent.Type.NETROM_INFO;
                break;
            case InformationAcknowledge:
                eventType = NetRomCircuitEvent.Type.NETROM_INFO_ACK;
                break;
            case Unknown:
            default:
                throw new IllegalStateException("Unknown NET/ROM packet type: " + packet);
        }

        return new NetRomCircuitEvent.DataLinkEvent(
                packet.getCircuitId(),
                packet.getOriginNode(),
                packet, eventType);
    }
}
