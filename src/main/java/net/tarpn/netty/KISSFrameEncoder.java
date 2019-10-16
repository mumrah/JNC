package net.tarpn.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.tarpn.frame.impl.KISS;
import net.tarpn.frame.impl.KISSFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KISSFrameEncoder extends MessageToByteEncoder<KISSFrame> {

    private static final Logger LOG = LoggerFactory.getLogger(KISSFrameEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, KISSFrame msg, ByteBuf out) throws Exception {
        LOG.trace("KISS write: " + msg);
        out.writeByte(KISS.Protocol.FEND.asByte());
        out.writeByte(((msg.getPort() << 4) & 0xF0) | (msg.getKissCommand().asByte() & 0x0F));
        for (int i = 0; i < msg.getData().length; i++) {
            int b = msg.getData()[i];
            if(KISS.Protocol.FEND.equalsTo(b)) {
                out.writeByte(KISS.Protocol.FESC.asByte());
                out.writeByte(KISS.Protocol.TFEND.asByte());
            } else if(KISS.Protocol.FESC.equalsTo(b)) {
                out.writeByte(KISS.Protocol.FESC.asByte());
                out.writeByte(KISS.Protocol.TFESC.asByte());
            } else {
                out.writeByte((byte)b);
            }
        }
        out.writeByte(KISS.Protocol.FEND.asByte());
    }
}
