package net.tarpn.netty.app;

import io.netty.channel.*;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import net.tarpn.config.Configs;
import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.netty.ax25.DataLinkChannel;
import net.tarpn.netty.ax25.DataLinkMultiplexer;
import net.tarpn.netty.ax25.Multiplexer;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class SysopApplicationHandler implements Application {

    private static final Logger LOG = LoggerFactory.getLogger(SysopApplicationHandler.class);

    private final Configs configs;
    private final Multiplexer multiplexer;
    private DataLinkChannel channel;
    private AX25Call remoteCall;

    public SysopApplicationHandler(Configs configs, Multiplexer multiplexer) {
        this.configs = configs;
        this.multiplexer = multiplexer;
    }

    @Override
    public void onConnect(Context context) throws Exception {
        // Send greeting for a new connection.
        context.write("Welcome to " + configs.getNodeConfig().getNodeAlias() + "!");
        context.write("You are connected to " + InetAddress.getLocalHost().getHostName());
        context.write("It is " + new Date() + " now.");
        context.flush();
    }

    @Override
    public void onDisconnect(Context context) {
        context.close();
    }

    @Override
    public void onError(Context context, Throwable t) {
        LOG.error("We had an error", t);
        context.write("We had an error: " + t.getMessage());
        context.close();
    }

    @Override
    public void read(Context context, String message) throws Exception {
        LOG.info("SYSOP: " + message + " from " + context.remoteAddress());
        String[] tokens = message.trim().split("\\s+");
        String command = tokens[0];

        if (channel == null) {
            // when connected, pass data through as info frames
            if (command.equals("?") || command.equalsIgnoreCase("HELP")) {
                context.write("HELP: here is some help output.");
                context.flush();
            } else if (command.equalsIgnoreCase("C") || command.equalsIgnoreCase("CONNECT")) {
                int port = Integer.parseInt(tokens[1]);
                AX25Call remoteCall = AX25Call.create(tokens[2]);
                this.channel = multiplexer.connect(port, remoteCall, primitive -> {
                    // Handle DL events and write to application output
                    switch (primitive.getType()) {
                        case DL_CONNECT:
                            context.write("Connected to " + primitive.getRemoteCall());
                            context.flush();
                            break;
                        case DL_DISCONNECT:
                            context.write("Disconnected from " + primitive.getRemoteCall());
                            context.flush();
                            this.channel.close();
                            this.channel = null;
                            this.remoteCall = null;
                            break;
                        case DL_DATA:
                        case DL_UNIT_DATA:
                            context.write(primitive.getLinkInfo().getInfoAsASCII());
                            context.flush();
                            break;
                        case DL_ERROR:
                            context.write("Had an error: " + primitive.getError().getMessage());
                            context.flush();
                            break;
                    }
                });
                this.remoteCall = remoteCall;

                // connect
                DataLinkPrimitive connectReq = DataLinkPrimitive.newConnectRequest(remoteCall, configs.getNodeConfig().getNodeCall());
                this.channel.write(connectReq);
            } else if (command.equalsIgnoreCase("P") || command.equalsIgnoreCase("PORTS")) {
                // list the ports
                context.write("PORTS:");
                configs.getPortConfigs().forEach((portNum, portConfig) -> {
                    context.write(portNum + ": " + portConfig.getPortDescription());
                });
                context.flush();
            } else if (command.equalsIgnoreCase("B") || command.equalsIgnoreCase("BYE")) {
                // disconnect the tcp
                context.write("Bye!");
                context.flush();
                context.close();
            } else {
                context.write("Huh? You said: " + message);
                context.flush();
            }
        } else {
            if (command.equalsIgnoreCase("B") || command.equalsIgnoreCase("BYE")) {
                // disconnect the data link
                DataLinkPrimitive discReq = DataLinkPrimitive.newDisconnectRequest(remoteCall, configs.getNodeConfig().getNodeCall());
                this.channel.write(discReq);
            } else {
                DataLinkPrimitive info = DataLinkPrimitive.newDataRequest(remoteCall, configs.getNodeConfig().getNodeCall(),
                        AX25Packet.Protocol.NO_LAYER3, message.getBytes(StandardCharsets.US_ASCII));
                this.channel.write(info);
            }
        }
    }
}
