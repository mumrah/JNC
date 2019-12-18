package net.tarpn.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import net.tarpn.frame.impl.KISS;
import net.tarpn.frame.impl.KISSFrame;
import net.tarpn.packet.impl.AX25PacketReader;
import net.tarpn.packet.impl.ax25.AX25Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Decode an AX.25 packet from a frame
 */
public class AX25PacketDecoder extends MessageToMessageDecoder<KISSFrame> {
    private static final Logger LOG = LoggerFactory.getLogger(AX25PacketDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, KISSFrame msg, List<Object> out) throws Exception {
        if (msg.getKissCommand().equals(KISS.Command.Data)) {
            AX25Packet packet = AX25PacketReader.parse(msg.getData());
            int port = ctx.channel().attr(Attributes.PortNumber).get();
            LOG.info(packet.toLogString(port));
            out.add(packet);
        } else {
            LOG.warn("Unexpected KISS: " + msg);
            // other commands
        }
    }
}
