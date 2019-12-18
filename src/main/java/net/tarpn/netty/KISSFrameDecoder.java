package net.tarpn.netty;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.DecoderException;
import net.tarpn.config.PortConfig;
import net.tarpn.frame.impl.KISS;
import net.tarpn.frame.impl.KISSFrame;
import net.tarpn.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.function.Consumer;

public class KISSFrameDecoder extends ByteToMessageDecoder {

    private static final Logger LOG = LoggerFactory.getLogger(KISSFrameDecoder.class);

    private final PortConfig portConfig;

    public KISSFrameDecoder(PortConfig portConfig) {
        this.portConfig = portConfig;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        LOG.trace("KISS read");

        int len = in.readableBytes();
        byte b;
        boolean inFrame = false;


        int idx = 0;
        // Consume bytes until we reach a FEND
        while (idx < len) {
            b = in.getByte(idx);
            if (KISS.Protocol.FEND.equalsTo(b)) {
                break;
            } else {
                // keep consuming
                idx++;
            }
        }

        if (idx != 0) {
            byte[] discarded = new byte[idx];
            in.getBytes(0, discarded);
            in.readerIndex(idx);
            LOG.warn("Discarding " + idx + " bytes which appeared before the frame delimiter: " + Util.toHexString(discarded));
        }

        int frameStart = -1;
        int frameEnd;
        while(idx < len) {
            b = in.getByte(idx++);
            if(KISS.Protocol.FEND.equalsTo(b)) {
                if(inFrame) {
                    // Finished consuming a frame
                    frameEnd = idx;
                    decodeFrame(in, frameStart, frameEnd-1, out::add);
                    in.readerIndex(frameEnd);
                    frameStart = -1;
                    inFrame = false;
                } else {
                    // Keep consuming a sequence of FENDs
                    continue;
                }
            } else {
                if (!inFrame) {
                    // Start of a frame
                    inFrame = true;
                    frameStart = idx - 1;
                }
            }
        }
    }

    void decodeFrame(ByteBuf in, int frameStart, int frameEnd, Consumer<KISSFrame> frameConsumer) {
        boolean inEscape = false;
        KISS.Command kissCommand = KISS.Command.Unknown;
        int hdlcPort = -1;
        if (frameStart > 0 && frameEnd > frameStart) {
            int frameIdx = frameStart;
            ByteBuffer frame = ByteBuffer.allocate(frameEnd - frameStart);
            int crc = 0;
            byte b = in.getByte(frameIdx++);
            crc ^= b;
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

            if (portConfig.getKISSFlags().contains("CHECKSUM")) {
                int len = frame.position() - 1;
                byte[] frameBytes = new byte[len];
                frame.position(0);
                frame.get(frameBytes);
                int kissCrc = frame.get() & 0xFF;
                for (int i = 0; i < len; i++) {
                    crc ^= frameBytes[i];
                }
                crc &= 0xFF;
                if (kissCrc == crc) {
                    frameConsumer.accept(new KISSFrame(hdlcPort, kissCommand, frameBytes));
                } else {
                    // checksum is bad, drop it
                }
            } else {
                int len = frame.position();
                byte[] frameBytes = new byte[len];
                frame.position(0);
                frame.get(frameBytes);
                frameConsumer.accept(new KISSFrame(hdlcPort, kissCommand, frameBytes));
            }
        } else {
            // Illegal indexes
            throw new DecoderException();
        }
    }
}
