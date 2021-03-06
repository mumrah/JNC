package net.tarpn.netty;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.internal.TypeParameterMatcher;
import net.tarpn.config.PortConfig;
import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.netty.app.Application;
import net.tarpn.netty.app.SysopApplicationHandler;
import net.tarpn.netty.ax25.Multiplexer;
import net.tarpn.netty.ax25.PortChannel;
import net.tarpn.packet.impl.ax25.*;
import net.tarpn.packet.impl.ax25.handlers.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;


/**
 * Reads incoming AX.25 packets and handles them with a state machine. Outputs can be
 * outgoing AX.25 packets, incoming DL events, or both.
 */
public class AX25StateHandler extends ChannelDuplexHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AX25StateHandler.class);

    private final Map<AX25State.State, StateHandler> handlers = new HashMap<>();
    private final Map<AX25Call, AX25State> sessions = new ConcurrentHashMap<>();
    private final Queue<AX25StateEvent> internalStateEvents = new ArrayDeque<>();
    private final ReentrantLock stateLock = new ReentrantLock();

    private final PortConfig portConfig;

    //private PortChannel portChannel;

    public AX25StateHandler(PortConfig portConfig) {
        this.portConfig = portConfig;
        this.handlers.put(AX25State.State.DISCONNECTED, new DisconnectedStateHandler());
        this.handlers.put(AX25State.State.CONNECTED, new ConnectedStateHandler());
        this.handlers.put(AX25State.State.AWAITING_CONNECTION, new AwaitingConnectionStateHandler());
        this.handlers.put(AX25State.State.TIMER_RECOVERY, new TimerRecoveryStateHandler());
        this.handlers.put(AX25State.State.AWAITING_RELEASE, new AwaitingReleaseStateHandler());
    }

    /**
     * This is called when we read a decoded AX.25 packet
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        TypeParameterMatcher matcher = TypeParameterMatcher.get(AX25Packet.class);
        if (matcher.match(msg)) {
            LOG.debug("AX25 read: " + msg);
            AX25StateEvent event = toEvent((AX25Packet) msg);
            if (event != null) {
                processStateEvent(ctx, event);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        TypeParameterMatcher matcher = TypeParameterMatcher.get(DataLinkPrimitive.class);
        if (matcher.match(msg)) {
            DataLinkPrimitive dl = (DataLinkPrimitive) msg;
            AX25StateEvent event = toEvent(dl);
            if (event != null) {
                processStateEvent(ctx, event);
            }
        } else {
            ctx.write(msg);
        }
    }


    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        if (ctx.channel().isActive() && ctx.channel().isRegistered()) {
            // channelActive() event has been fired already, which means this.channelActive() will
            // not be invoked. We have to initialize here instead.
            initialize(ctx);
        } else {
            // channelActive() event has not been fired yet.  this.channelActive() will be invoked
            // and initialization will occur there.
        }
    }

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        // Initialize early if channel is active already.
        if (ctx.channel().isActive()) {
            initialize(ctx);
        }

        /*portChannel = multiplexer.bind(portConfig.getPortNumber(), datalinkPrimitive -> {
            AX25StateEvent event = toEvent(datalinkPrimitive);
            if (event != null) {
                processStateEvent(ctx, event);
            }
        });*/

        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        /*if (portChannel != null) {
            portChannel.close();
        }*/
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // This method will be invoked only if this handler was added
        // before channelActive() event is fired.  If a user adds this handler
        // after the channelActive() event, initialize() will be called by beforeAdd().
        initialize(ctx);
        super.channelActive(ctx);
    }

    /**
     * Create a scheduled task to flush and pending AX25 state events. This includes things
     * like T1 and T3 timeouts
     *
     * TODO let the scheduled rate be configured
     * @param ctx
     * @throws Exception
     */
    private void initialize(ChannelHandlerContext ctx) throws Exception {
        // Check pending internal events that ned processing
        ctx.executor().scheduleAtFixedRate(
                () -> flushStateEvents(ctx),1000, 1000, TimeUnit.MILLISECONDS);

        // Schedule ID broadcast for FCC rules
        ctx.executor().scheduleAtFixedRate(() -> {
            AX25StateEvent idStateEvent = AX25StateEvent.createUnitDataEvent(
                    AX25Call.create("ID", 0),
                    AX25Packet.Protocol.NO_LAYER3,
                    portConfig.getIdMessage().getBytes(StandardCharsets.US_ASCII));

            internalStateEvents.add(idStateEvent);
        }, 5, portConfig.getIdInterval(), TimeUnit.SECONDS);
    }

    /**
     * Send an incoming or outgoing state event through the state machine. This can result
     * in new DL events to be passed up to layer 2, additional state machine events,
     * or AX25 packets to be sent out.
     *
     * @param ctx
     * @param stateEvent
     */
    private void processStateEvent(ChannelHandlerContext ctx, AX25StateEvent stateEvent) {
        List<DataLinkPrimitive> dlEvents = new ArrayList<>();
        stateLock.lock();
        try {
            AX25State state = sessions.computeIfAbsent(stateEvent.getRemoteCall(),
                    ax25Call -> new AX25State(
                            stateEvent.getRemoteCall().toString(),
                            stateEvent.getRemoteCall(),
                            portConfig.getNodeCall(),
                            portConfig,
                            internalStateEvent -> {
                                // These are events like IFRAME_READY, T1_TIMER, and T3_TIMER
                                // See AX25StateEvent.Type for a full list
                                internalStateEvents.add(internalStateEvent);
                                //flushStateEvents(ctx);

                            }
                    )
            );
            StateHandler handler = handlers.get(state.getState());
            LOG.info("State Before " + state.getState() + " " + stateEvent);
            AX25State.State newState = handler.onEvent(state, stateEvent, outgoing -> {
                // AX25 packet needs to get written out. This is typically things related
                // to connected mode operation (SABM, UA, RR, etc)
                if (ctx.channel().isWritable()) {
                    //LOG.info("Outgoing immediately: " + outgoing);
                    ctx.writeAndFlush(outgoing);
                } else {
                    // TODO does this ever happen?
                    throw new RuntimeException("Not ready to write");
                }
            });
            if (state.getState() != newState) {
                LOG.info("AX.25 state for " + stateEvent.getRemoteCall() + " transitioning from " +
                        state.getState() + " to " + newState);
            }
            state.setState(newState);
            LOG.info("State After " + state.getState() + " " + stateEvent);

            DataLinkPrimitive dlEvent;
            while ((dlEvent = state.pollDLEvents()) != null) {
                dlEvents.add(dlEvent);
            }
        } finally {
            stateLock.unlock();
        }

        // Pass DL events up to next handler
        dlEvents.forEach(ctx::fireChannelRead);

        // Flush any internal state events generated
        flushStateEvents(ctx);
    }

    /**
     * Process any pending {@link AX25StateEvent} through the state machine. This
     * may result in new packets going out.
     * @param ctx
     */
    private void flushStateEvents(ChannelHandlerContext ctx) {
        AX25StateEvent internalEvent;
        while ((internalEvent = internalStateEvents.poll()) != null) {
            //LOG.info("Internal AX.25 Event: " + internalEvent);
            // State event, needs to reprocess
            processStateEvent(ctx, internalEvent);
        }
    }

    /**
     * Convert an {@link AX25Packet} to a state machine event {@link AX25StateEvent}
     * @return
     */
    private AX25StateEvent toEvent(AX25Packet ax25Packet) {
        final AX25StateEvent event;
        switch (ax25Packet.getFrameType()) {
            case I: {
                event = AX25StateEvent.createIncomingEvent(ax25Packet, AX25StateEvent.Type.AX25_INFO);
                break;
            } case S: {
                switch (((SFrame) ax25Packet).getControlType()) {
                    case RR: {
                        event = AX25StateEvent.createIncomingEvent(ax25Packet, AX25StateEvent.Type.AX25_RR);
                        break;
                    }
                    case RNR: {
                        event = AX25StateEvent.createIncomingEvent(ax25Packet, AX25StateEvent.Type.AX25_RNR);
                        break;
                    }
                    case REJ: {
                        event = AX25StateEvent.createIncomingEvent(ax25Packet, AX25StateEvent.Type.AX25_REJ);
                        break;
                    }
                    default:
                        event = AX25StateEvent.createIncomingEvent(ax25Packet, AX25StateEvent.Type.AX25_UNKNOWN);
                        break;
                }
                break;
            } case U: {
                switch (((UFrame) ax25Packet).getControlType()) {
                    case SABM: {
                        event = AX25StateEvent.createIncomingEvent(ax25Packet, AX25StateEvent.Type.AX25_SABM);
                        break;
                    }
                    case DISC: {
                        event = AX25StateEvent.createIncomingEvent(ax25Packet, AX25StateEvent.Type.AX25_DISC);
                        break;
                    }
                    case DM: {
                        event = AX25StateEvent.createIncomingEvent(ax25Packet, AX25StateEvent.Type.AX25_DM);
                        break;
                    }
                    case UA: {
                        event = AX25StateEvent.createIncomingEvent(ax25Packet, AX25StateEvent.Type.AX25_UA);
                        break;
                    }
                    case FRMR: {
                        event = AX25StateEvent.createIncomingEvent(ax25Packet, AX25StateEvent.Type.AX25_FRMR);
                        break;
                    }
                    default: {
                        event = AX25StateEvent.createIncomingEvent(ax25Packet, AX25StateEvent.Type.AX25_UNKNOWN);
                        break;
                    }
                }
                break;
            } case UI: {
                event = AX25StateEvent.createIncomingEvent(ax25Packet, AX25StateEvent.Type.AX25_UI);
                break;
            } default: {
                event = AX25StateEvent.createIncomingEvent(ax25Packet, AX25StateEvent.Type.AX25_UNKNOWN);
                break;
            }
        }
        return event;
    }

    /**
     * Convert a DL primitive {@link DataLinkPrimitive} to a state machine event {@link AX25StateEvent}
     * @return
     */
    private AX25StateEvent toEvent(DataLinkPrimitive event) {
        switch (event.getType()) {
            case DL_CONNECT:
                return AX25StateEvent.createConnectEvent(event.getRemoteCall());
            case DL_DISCONNECT:
                return AX25StateEvent.createDisconnectEvent(event.getRemoteCall());
            case DL_DATA:
                return AX25StateEvent.createDataEvent(
                        event.getRemoteCall(),
                        event.getLinkInfo().getProtocol(),
                        event.getLinkInfo().getInfo());
            case DL_UNIT_DATA:
                return AX25StateEvent.createUnitDataEvent(
                        event.getRemoteCall(),
                        event.getLinkInfo().getProtocol(),
                        event.getLinkInfo().getInfo());
            case DL_ERROR:
            default:
                return null;
        }
    }
}
