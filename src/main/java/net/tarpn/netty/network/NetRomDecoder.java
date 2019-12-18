package net.tarpn.netty.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.internal.TypeParameterMatcher;
import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.netty.Attributes;
import net.tarpn.network.netrom.NetRomNodes;
import net.tarpn.network.netrom.packet.*;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

public class NetRomDecoder extends ChannelInboundHandlerAdapter {

    private static final Logger LOG = LoggerFactory.getLogger(NetRomDecoder.class);

    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        TypeParameterMatcher matcher = TypeParameterMatcher.get(DataLinkPrimitive.class);
        if (matcher.match(msg)) {
            DataLinkPrimitive dlPrimitive = (DataLinkPrimitive) msg;
            // Handle I and UI NET/ROM packets
            if (dlPrimitive.getType().hasData() &&
                    dlPrimitive.getLinkInfo().getProtocol().equals(AX25Packet.Protocol.NETROM)) {
                parseInfo(dlPrimitive.getLinkInfo(), netrom -> {
                    LOG.info(netrom.toLogString(ctx.channel().attr(Attributes.PortNumber).get()));
                    ctx.fireChannelRead(netrom);
                });
            } else {
                // Drop DL
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    public static void parseInfo(AX25Packet.HasInfo info, Consumer<NetRomPacket> netRomConsumer) {
        ByteBuffer infoBuffer = ByteBuffer.wrap(info.getInfo());

        AX25Call originNode = AX25Call.read(infoBuffer);
        AX25Call destNode = AX25Call.read(infoBuffer);

        byte ttl = infoBuffer.get();
        byte circuitIdx = infoBuffer.get();
        byte circuitId = infoBuffer.get();
        byte txSeqNum = infoBuffer.get();
        byte rxSeqNum = infoBuffer.get();
        byte opcode = infoBuffer.get();
        NetRomPacket.OpType opType = NetRomPacket.OpType.fromOpCodeByte(opcode);
        boolean choke = (opcode & 0x80) == 0x80;
        boolean nak = (opcode & 0x40) == 0x40;
        boolean moreFollows = (opcode & 0x20) == 0x20;

        final NetRomPacket netRomPacket;
        switch (opType) {
            case ConnectRequest: {
                byte proposeWindowSize = infoBuffer.get();
                AX25Call originatingUser = AX25Call.read(infoBuffer);
                AX25Call originatingNode = AX25Call.read(infoBuffer);
                netRomPacket = NetRomConnectRequest.create(originNode, destNode, ttl,
                        circuitIdx, circuitId, proposeWindowSize, originatingUser, originatingNode);
                break;
            }
            case ConnectAcknowledge: {
                byte acceptWindowSize = infoBuffer.get();
                netRomPacket = NetRomConnectAck.create(originNode, destNode, ttl,
                        circuitIdx, circuitId,
                        txSeqNum, rxSeqNum,
                        acceptWindowSize,
                        NetRomPacket.OpType.ConnectAcknowledge.asByte(choke, nak, moreFollows));
                break;
            }
            case Information: {
                int len = infoBuffer.remaining();
                byte[] l3Info = new byte[len];
                infoBuffer.get(l3Info);
                netRomPacket = NetRomInfo.create(originNode, destNode, ttl,
                        circuitIdx, circuitId,
                        txSeqNum, rxSeqNum, l3Info);
                break;
            }
            case InformationAcknowledge: {
                netRomPacket = BaseNetRomPacket.createInfoAck(
                        originNode,
                        destNode,
                        ttl,
                        circuitIdx,
                        circuitId,
                        rxSeqNum,
                        NetRomPacket.OpType.InformationAcknowledge.asByte(false, false, false));
                break;
            }
            case DisconnectRequest: {
                netRomPacket = BaseNetRomPacket.createDisconnectRequest(originNode, destNode, ttl,
                        circuitIdx, circuitId);
                break;
            }
            case DisconnectAcknowledge: {
                netRomPacket = BaseNetRomPacket.createDisconnectAck(originNode, destNode, ttl,
                        circuitIdx, circuitId);
                break;
            }
            case Unknown: {
                LOG.warn("Got unknown NET/ROM opcode " + opcode + "\n" + Util.toHexDump(info.getInfo()));
                return;
            }
            default:
                throw new IllegalStateException("Cannot get here");
        }
        netRomConsumer.accept(netRomPacket);
    }
}
