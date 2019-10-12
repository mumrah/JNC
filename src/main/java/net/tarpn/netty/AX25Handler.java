package net.tarpn.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.internal.PendingWrite;
import io.netty.util.internal.TypeParameterMatcher;
import net.tarpn.config.PortConfig;
import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.packet.impl.ax25.*;
import net.tarpn.packet.impl.ax25.handlers.StateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;

// Check out IdleStateHandler
public class AX25Handler extends ChannelDuplexHandler {

    private static final Logger LOG = LoggerFactory.getLogger(AX25Handler.class);

    private final PortConfig portConfig;

    Queue<AX25StateEvent> internalStateEvents = new ArrayDeque<>();
    Queue<DataLinkPrimitive> inboundL2Events = new ArrayDeque<>();

    public AX25Handler(PortConfig portConfig) {
        this.portConfig = portConfig;
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        TypeParameterMatcher matcher = TypeParameterMatcher.get(AX25Packet.class);
        if (matcher.match(msg)) {
            LOG.info("AX25 read: " + msg);
            AX25StateEvent event = toEvent((AX25Packet) msg);
            if (event != null) {
                processStateEvent(ctx, event);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }


    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
        TypeParameterMatcher matcher = TypeParameterMatcher.get(DataLinkPrimitive.class);
        if (matcher.match(msg)) {
            LOG.info("DL write: " + msg);
            AX25StateEvent event = toEvent((DataLinkPrimitive) msg);
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
        super.channelRegistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        // This method will be invoked only if this handler was added
        // before channelActive() event is fired.  If a user adds this handler
        // after the channelActive() event, initialize() will be called by beforeAdd().
        initialize(ctx);
        super.channelActive(ctx);
    }

    private void initialize(ChannelHandlerContext ctx) {
        ctx.executor().scheduleAtFixedRate(() -> {
            flushStateEvents(ctx);
        }, 1000, 1000, TimeUnit.MILLISECONDS);
    }

    /**
     * Send an incoming or outgoing state event through the state machine. This can result
     * in new L2 events to be passed up to the next read handler, additional state machine events,
     * or AX25 packets to be sent out.
     *
     * @param ctx
     * @param stateEvent
     */
    void processStateEvent(ChannelHandlerContext ctx, AX25StateEvent stateEvent) {
        AX25State state = AX25.sessions.computeIfAbsent(stateEvent.getRemoteCall(),
                ax25Call -> new AX25State(
                        stateEvent.getRemoteCall().toString(),
                        stateEvent.getRemoteCall(),
                        portConfig.getNodeCall(),
                        portConfig,
                        internalStateEvent -> {
                            LOG.info("Internal State: " + internalStateEvent);
                            internalStateEvents.add(internalStateEvent);
                            flushStateEvents(ctx);

                        },
                        inboundL2 -> {
                            LOG.info("Inbound L2: " + inboundL2);
                            inboundL2Events.add(inboundL2);
                        }
                )
        );
        StateHandler handler = AX25.handlers.get(state.getState());
        LOG.info("Before " + stateEvent.getRemoteCall() + ": " + state.getState());
        AX25State.State newState = handler.onEvent(state, stateEvent, outgoing -> {
            // AX25 packet needs to get written out
            if (ctx.channel().isWritable()) {
                LOG.info("Outgoing immediately: " + outgoing);
                ctx.writeAndFlush(outgoing);
            } else {
                LOG.warn("Not ready to write");
            }
            //write(ctx, outgoing, ctx.newPromise());
            //pendingWrites.add(PendingWrite.newInstance(outgoing, promise));

        });
        state.setState(newState);
        LOG.info("After " + stateEvent.getRemoteCall() + ": " + state.getState());

        DataLinkPrimitive inboundL2;
        while ((inboundL2 = inboundL2Events.poll()) != null) {
            LOG.info("Inbound L2: " + inboundL2);
            // L2 event pass on to reading
            ctx.fireChannelRead(inboundL2);
        }

        flushStateEvents(ctx);
    }

    void flushStateEvents(ChannelHandlerContext ctx) {
        LOG.info("Flushing state events");
        AX25StateEvent internalEvent;
        while ((internalEvent = internalStateEvents.poll()) != null) {
            LOG.info("Internal AX.25 Event: " + internalEvent);
            // State event, needs to reprocess
            processStateEvent(ctx, internalEvent);
        }
    }

    AX25StateEvent toEvent(AX25Packet ax25Packet) {
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

    public AX25StateEvent toEvent(DataLinkPrimitive event) {
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
