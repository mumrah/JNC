package net.tarpn.app;

import net.tarpn.datalink.DataLinkManager;
import net.tarpn.datalink.DataLinkSession;
import net.tarpn.datalink.LinkPrimitive;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.util.ParsedLine;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

// TODO commands:
//  USE port: set default port to use
//  CONNECT [port] CALL: issue a connect request L2 event
//  DISCONNECT [port] CALL: issue a disconnect request L2 event
//  ATTACH [port]: attach a port for sending and receiving UI frames
//  LISTEN [port]: list on a port, no sending enabled
//  DETACH: detach from a port

/**
 * An Application that interacts directly with the local data links (layer 2). Useful for debugging.
 */
public class DataLinkApplication implements Application<String> {
    private final Map<Integer, DataLinkManager> dataLinks;
    private Optional<Integer> usePort;
    private Optional<DataLinkSession> session;
    private boolean attached;

    public DataLinkApplication(Map<Integer, DataLinkManager> dataLinks) {
        this.dataLinks = dataLinks;
        this.usePort = Optional.empty();
        this.session = Optional.empty();
        this.attached = false;
    }

    @Override
    public String getName() {
        return "l2";
    }

    @Override
    public Class<String> getType() {
        return String.class;
    }

    @Override
    public void onConnect(Consumer<String> response) {

    }

    @Override
    public void handle(String request, Consumer<String> response, Runnable closer) {
        ParsedLine line = ParsedLine.parse(request);
        String cmd = line.word().get();

        if (session.isPresent() && attached) {
            session.get().send(line.line());
            return;
        }


        if (cmd.equalsIgnoreCase("A") || cmd.equalsIgnoreCase("ATTACH")) {
            if (session.isPresent()) {
                attached = true;
            }
        }
        if (cmd.equalsIgnoreCase("C") || cmd.equalsIgnoreCase("CONN")) {
            final int port;
            if (line.size() == 2) { // no port specified
               if (usePort.isPresent()) {
                   port = usePort.get();
               } else {
                   response.accept("Invalid syntax, must specific port or first 'USE port'");
                   return;
               }
            } else {
                port = line.word().asInt();
            }

            DataLinkManager dataLink = dataLinks.get(port);
            if (dataLink == null) {
                response.accept("Unknown port: " + port);
                response.accept("Known ports are: " + dataLinks.keySet().stream().map(Object::toString).collect(Collectors.joining(", ")));
            } else {
                AX25Call call = AX25Call.fromString(line.word().get());
                dataLink.acceptDataLinkPrimitive(LinkPrimitive.newConnectRequest(call));
                response.accept("Connecting to " + call + " on port " + port);
                DataLinkSession session = dataLink.attach(call, linkPrimitive -> {
                    switch(linkPrimitive.getType()) {
                        case DL_CONNECT:
                            response.accept("Connected to " + call);
                            break;
                        case DL_DISCONNECT:
                            response.accept("Disconnected from " + call);
                            break;
                        case DL_DATA:
                            response.accept(linkPrimitive.getLinkInfo().getInfoAsASCII());
                            break;
                        case DL_UNIT_DATA:
                            response.accept(linkPrimitive.getLinkInfo().getInfoAsASCII());
                            break;
                        case DL_ERROR:
                            response.accept("Error! " + linkPrimitive.getError());
                            break;
                    }
                });
                this.session = Optional.of(session);
                this.attached = true;
            }
        } else if (cmd.equalsIgnoreCase("L") || cmd.equalsIgnoreCase("LINKS")) {
            response.accept("> Links:");
            dataLinks.forEach((port, linkManager) -> {
                linkManager.getAx25StateHandler().forEachSession((sessionId, state) -> {
                    response.accept(" - On port " + port + ": " + sessionId + " is " + state.getState().name());
                });
            });
        } else if (cmd.equalsIgnoreCase("D") || cmd.equalsIgnoreCase("DETACH")) {

        } else {

        }
    }

    @Override
    public void onDisconnect(Consumer<String> response) {

    }
}
