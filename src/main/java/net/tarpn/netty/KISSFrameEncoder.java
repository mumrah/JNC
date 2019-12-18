package net.tarpn.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.tarpn.config.PortConfig;
import net.tarpn.frame.impl.KISS;
import net.tarpn.frame.impl.KISSFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KISSFrameEncoder extends MessageToByteEncoder<KISSFrame> {

    private static final Logger LOG = LoggerFactory.getLogger(KISSFrameEncoder.class);

    private final PortConfig portConfig;

    public KISSFrameEncoder(PortConfig portConfig) {
        this.portConfig = portConfig;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, KISSFrame msg, ByteBuf out) throws Exception {
        LOG.trace("KISS write: " + msg);
        out.writeByte(KISS.Protocol.FEND.asByte());
        int crc = 0;
        int commandByte = ((msg.getPort() << 4) & 0xF0) | (msg.getKissCommand().asByte() & 0x0F);
        crc ^= commandByte;
        out.writeByte(commandByte);
        for (int i = 0; i < msg.getData().length; i++) {
            int b = msg.getData()[i];
            crc ^= b;
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
        if (portConfig.getKISSFlags().contains("CHECKSUM")) {
            out.writeByte(crc & 0xFF);
        }
        out.writeByte(KISS.Protocol.FEND.asByte());
    }
}
