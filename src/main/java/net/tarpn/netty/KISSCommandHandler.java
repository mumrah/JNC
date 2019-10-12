package net.tarpn.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import net.tarpn.frame.Frame;
import net.tarpn.frame.impl.KISS;
import net.tarpn.frame.impl.KISSFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class KISSCommandHandler extends MessageToMessageDecoder<KISSFrame> {

    private static final Logger LOG = LoggerFactory.getLogger(KISSCommandHandler.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, KISSFrame msg, List<Object> out) throws Exception {
        switch (msg.getKissCommand()) {
            case Unknown:
                break;
            case Data:
                out.add(msg);
                break;
            case TxDelay:
                break;
            case P:
                break;
            case SlotTime:
                break;
            case TxTail:
                break;
            case FullDuplex:
                break;
            case SetHardware:
                handleSetHardware(ctx, msg);
                break;
            case Return:
                break;
        }
    }

    void handleSetHardware(ChannelHandlerContext ctx, KISSFrame msg) {
        String hwCmd = new String(msg.getData(), StandardCharsets.US_ASCII);
        final String resp;
        switch (hwCmd) {
            case "TNC:":
                resp = "TNC:JNC 1.0";
                break;
            case "MODEM:":
                resp = "MODEM:JNCModem";
                break;
            case "MODEML:":
                resp = "MODEML:JNCModem";
                break;
            case "MODEMBW:":
                resp = "MODEMBW:2000";
                break;
            default:
                resp = "";
                break;
        }

        if (resp.isEmpty()) {
            LOG.warn("Got unknown SET_HARDWARE request '" + hwCmd + "', ignoring");
        } else {
            LOG.info("Got SET_HARDWARE request '" + hwCmd + "', responding with '" + resp + "'");
            Frame response = new KISSFrame(
                    msg.getPort(),
                    KISS.Command.SetHardware,
                    resp.getBytes(StandardCharsets.US_ASCII)
            );
            ctx.write(response);
        }
    }
}
