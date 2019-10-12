package net.tarpn.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import net.tarpn.frame.impl.KISS;
import net.tarpn.frame.impl.KISSFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;

public class KISSFrameDecoder extends ByteToMessageDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(KISSFrameDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        LOG.info("KISS read");

        int len = in.readableBytes();
        byte b;
        int idx = 0;

        boolean inFrame = false;


        // Read from FEND to FEND, then decode

        int frameStart = -1;
        int frameEnd;
        while(idx < len) {
            b = in.getByte(idx++);
            // Keep reading FEND until we get something else
            if(KISS.Protocol.FEND.equalsTo(b)) {
                if(inFrame) {
                    frameEnd = idx;
                    //LOG.info("KISS deframe");
                    out.add(decodeFrame(in, frameStart, frameEnd-1));
                    in.readerIndex(frameEnd);
                    frameStart = -1;
                    inFrame = false;
                } else {
                    // Keep consuming FENDs
                    continue;
                }
            } else {
                // Start of a frame
                if (!inFrame) {
                    inFrame = true;
                    frameStart = idx - 1;
                }
            }
        }
    }

    KISSFrame decodeFrame(ByteBuf in, int frameStart, int frameEnd) {
        boolean inEscape = false;
        KISS.Command kissCommand = KISS.Command.Unknown;
        int hdlcPort = -1;
        if (frameStart > 0 && frameEnd > frameStart) {
            int frameIdx = frameStart;
            ByteBuffer frame = ByteBuffer.allocate(frameEnd - frameStart);
            byte b = in.getByte(frameIdx++);
            hdlcPort = (b >> 4) & 0x0F;
            kissCommand = KISS.Command.fromInt(b & 0x0F);

            while (frameIdx < frameEnd) {
                b = in.getByte(frameIdx++);
                if(KISS.Protocol.FESC.equalsTo(b)) {
                    inEscape = true;
                } else {
                    if(inEscape) {
                        if (KISS.Protocol.TFEND.equalsTo(b)) b = KISS.Protocol.FEND.asByte();
                        if (KISS.Protocol.TFESC.equalsTo(b)) b = KISS.Protocol.FESC.asByte();
                        inEscape = false;
                    }
                    frame.put(b);
                }
            }

            int len = frame.position();
            byte[] frameBytes = new byte[len];
            frame.position(0);
            frame.get(frameBytes);
            return new KISSFrame(hdlcPort, kissCommand, frameBytes);
        } else {
            // Illegal indexes
            throw new DecoderException();
        }
    }
}
