package net.tarpn.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import net.tarpn.frame.impl.KISS;
import net.tarpn.frame.impl.KISSFrame;
import net.tarpn.packet.impl.ax25.AX25Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Encode an AX.25 packet into a frame
 */
public class AX25PacketEncoder extends MessageToMessageEncoder<AX25Packet> {
    private static final Logger LOG = LoggerFactory.getLogger(AX25PacketEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, AX25Packet msg, List<Object> out) throws Exception {
        LOG.info("Encode AX25: " + msg);
        out.add(new KISSFrame(0, KISS.Command.Data, msg.getPayload()));
    }
}
