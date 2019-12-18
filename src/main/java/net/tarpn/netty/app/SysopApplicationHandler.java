package net.tarpn.netty.app;

import net.tarpn.config.Configs;
import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.netty.ax25.AX25Address;
import net.tarpn.netty.ax25.DataLinkChannel;
import net.tarpn.netty.ax25.Multiplexer;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Consumer;
import java.util.stream.Stream;

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
        /*context.write("Welcome to " + configs.getNodeConfig().getNodeAlias() + "!");
        context.write("You are connected to " + InetAddress.getLocalHost().getHostName());
        context.write("It is " + new Date() + " now.");
        context.write("\033[31;1;4mHello\033[0m");
        context.flush();
        */
    }

    @Override
    public void onDisconnect(Context context) {
        LOG.info("We are disconnected");
    }

    @Override
    public void onError(Context context, Throwable t) {
        LOG.error("We had an error: " + t.getMessage());
        //context.write("We had an error: " + t.getMessage());
        //context.close();
    }

    @Override
    public void read(Context context, byte[] messageBytes) throws Exception {
        String message = Util.ascii(messageBytes);
        message = message.replaceAll("[\r\n]", "");
        message = message.trim();

        LOG.info("SYSOP: " + message + " from " + context.remoteAddress());
        String[] tokens = message.toLowerCase().split("\\s+");
        String command = tokens[0];

        if (message.trim().isEmpty()) {
            // enable shell-like behavior for empty commands
            context.write("\b");
            return;
        }

        if (channel == null) {
            // not connected anywhere, handle local commands
            handleNodeCommand(context, tokens);
            context.flush();
        } else {
            // we're connected, so relay the input
            if (command.equalsIgnoreCase("B") || command.equalsIgnoreCase("BYE")) {
                // disconnect the data link
                DataLinkPrimitive discReq = DataLinkPrimitive.newDisconnectRequest(remoteCall, configs.getNodeConfig().getNodeCall());
                this.channel.write(discReq);
            } else {
                // TODO what about non-string data?
                DataLinkPrimitive info = DataLinkPrimitive.newDataRequest(remoteCall, configs.getNodeConfig().getNodeCall(),
                        AX25Packet.Protocol.NO_LAYER3, (message + "\r").getBytes(StandardCharsets.US_ASCII));
                this.channel.write(info);
            }
        }
    }

    @Override
    public void close(Context context) throws Exception {
        if (this.channel != null) {
            DataLinkPrimitive discReq = DataLinkPrimitive.newDisconnectRequest(remoteCall, configs.getNodeConfig().getNodeCall());
            this.channel.write(discReq);
            this.channel.close();
            this.channel = null;
            this.remoteCall = null;
        }
        context.close();
    }



    @CommandLine.Command(description = "Node Command Interface", name = "", abbreviateSynopsis = true)
    private class NodeCommand implements Runnable {
        @Override
        public void run() {
            // no-op
        }
    }

    @CommandLine.Command(description = "Connect to remote station", name = "connect")
    private class ConnectCommand implements Runnable {

        @CommandLine.Parameters(index = "0", description = "Port to use", paramLabel = "PORT")
        Integer port;

        @CommandLine.Parameters(index = "1", description = "Remote station's address", paramLabel = "ADDRESS")
        String remoteCallsign;

        //@CommandLine.Option(names = {"-l2", "--layer-2-only"}, description = "Force a layer 2 connection")
        //boolean l2;

        private final Context context;

        private ConnectCommand(Context context) {
            this.context = context;
        }

        @Override
        public void run() {
            AX25Call remoteCall = AX25Call.create(remoteCallsign);

            Consumer<DataLinkPrimitive> l3Consumer = primitive -> {
                // Handle DL events and write to application output
                switch (primitive.getType()) {
                    case DL_CONNECT:
                        context.write("\033[31;1;4mConnected to " + primitive.getRemoteCall() + "\033[0m");
                        context.flush();
                        break;
                    case DL_DISCONNECT:
                        context.write("Disconnected from " + primitive.getRemoteCall());
                        context.flush();
                        SysopApplicationHandler.this.channel.close();
                        SysopApplicationHandler.this.channel = null;
                        SysopApplicationHandler.this.remoteCall = null;
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
            };
            try {
                AX25Address localAddress = new AX25Address(port, configs.getNodeConfig().getNodeCall());
                AX25Address remoteAddress = new AX25Address(port, remoteCall);
                SysopApplicationHandler.this.channel = multiplexer.connect(localAddress, remoteAddress, l3Consumer);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            SysopApplicationHandler.this.remoteCall = remoteCall;

            // connect
            DataLinkPrimitive connectReq = DataLinkPrimitive.newConnectRequest(remoteCall, configs.getNodeConfig().getNodeCall());
            SysopApplicationHandler.this.channel.write(connectReq);
        }

        @Override
        public String toString() {
            return "ConnectCommand{" +
                    "port=" + port +
                    ", remoteCallsign='" + remoteCallsign + '\'' +
                    '}';
        }
    }

    @CommandLine.Command(description = "Show help", name = "help")
    private class HelpCommand implements Runnable {

        @CommandLine.Parameters(description = "Command", defaultValue = "help")
        String subCommand;

        private final Context context;
        private final CommandLine parent;

        private HelpCommand(Context context, CommandLine parent) {
            this.context = context;
            this.parent = parent;
        }

        @Override
        public void run() {
            if (subCommand.equalsIgnoreCase("help")) {
                String commands = String.join(", ", parent.getSubcommands().keySet());
                context.write("Commands: " + commands);
            } else {
                CommandLine sub = parent.getSubcommands().get(subCommand);
                if (sub == null) {
                    context.write("Unknown command '" + subCommand + "'");
                } else {
                    StringBuilder builder = new StringBuilder();
                    CommandLine.Command annot = sub.getCommand().getClass()
                            .getDeclaredAnnotation(CommandLine.Command.class);
                    if (annot != null) {
                        builder.append(Stream.of(annot.description()).findFirst().orElse(""));
                        builder.append(", ");
                    }
                    builder.append("usage: ");
                    CommandLine.Help.ColorScheme.Builder colorBuilder = new CommandLine.Help.ColorScheme.Builder();
                    colorBuilder.ansi(CommandLine.Help.Ansi.ON);
                    colorBuilder.commands(CommandLine.Help.Ansi.Style.fg_red, CommandLine.Help.Ansi.Style.underline);
                    CommandLine.Help.ColorScheme colors = colorBuilder.build();
                    builder.append(colors.commandText(sub.getCommandName()));
                    sub.getCommandSpec().args().forEach(argSpec -> {
                        if(argSpec.isOption()) {
                            builder.append(" " + ((CommandLine.Model.OptionSpec) argSpec).shortestName());
                        } else if (argSpec.isPositional()) {
                            builder.append(" " + argSpec.paramLabel());
                        }
                    });

                    context.write(builder.toString());
                }
            }
        }

        @Override
        public String toString() {
            return "HelpCommand{" +
                    "subCommand='" + subCommand + '\'' +
                    '}';
        }
    }

    @CommandLine.Command(description = "List configured ports", name = "ports")
    private class PortsCommand implements Runnable {
        private final Context context;

        private PortsCommand(Context context) {
            this.context = context;
        }

        @Override
        public void run() {
            context.write("PORTS:");
            configs.getPortConfigs().forEach((portNum, portConfig) -> {
                context.write(portNum + ": " + portConfig.getPortDescription());
            });
        }

        @Override
        public String toString() {
            return "PortsCommand{}";
        }
    }

    @CommandLine.Command(description = "Disconnect from Node", name = "bye")
    private class ByeCommand implements Runnable {
        private final Context context;

        private ByeCommand(Context context) {
            this.context = context;
        }

        @Override
        public void run() {
            context.write("Bye!");
            context.flush();
            context.close();
        }

        @Override
        public String toString() {
            return "ByeCommand{}";
        }
    }


    private static PrintStream printer(Context context) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        return new PrintStream(baos, false) {
            @Override
            public void flush() {
                super.flush();
                context.write(new String(baos.toByteArray(), StandardCharsets.UTF_8));
            }
        };
    }

    private void handleNodeCommand(Context context, String[] args) throws Exception {
        PrintStream printStream = printer(context);
        CommandLine cmd = new CommandLine(new NodeCommand());
        cmd.addSubcommand("connect", new ConnectCommand(context), "c", "conn");
        cmd.addSubcommand("help", new HelpCommand(context, cmd), "h", "?");
        cmd.addSubcommand("ports", new PortsCommand(context), "p");
        cmd.addSubcommand("bye", new ByeCommand(context), "b", "q", "quit");

        try {
            CommandLine.ParseResult result = cmd.parseArgs(args);
            new CommandLine.RunLast().execute(result);
        } catch (CommandLine.UnmatchedArgumentException e) {
            if (e.getCommandLine().getCommand().getClass().equals(NodeCommand.class)) {
                context.write("Unknown command: " + e.getUnmatched().get(0));
            } else {
                e.getCommandLine().usage(printStream);
            }
        } catch (CommandLine.ParameterException e) {
            if (e.getCommandLine().getCommand().getClass().equals(NodeCommand.class)) {
                context.write("Command parsing error: " + e.getMessage());
            } else {
                context.write(e.getCommandLine().getCommandName() + " had an error:");
                context.write(e.getMessage());
            }
        } catch (CommandLine.ExecutionException e) {
            final Throwable actual;
            if (e.getCause() instanceof UncheckedIOException) {
                actual = e.getCause().getCause();
            } else {
                actual = e.getCause();
            }
            context.write(e.getCommandLine().getCommandName() + " had an error: " + actual.getMessage());
        }
    }
}
