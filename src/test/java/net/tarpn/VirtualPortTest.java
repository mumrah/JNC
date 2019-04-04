package net.tarpn;

import net.tarpn.app.Application;
import net.tarpn.app.ApplicationAdaptor;
import net.tarpn.app.ApplicationRegistry;
import net.tarpn.config.impl.Configs;
import net.tarpn.config.PortConfig;
import net.tarpn.datalink.DataLinkManager;
import net.tarpn.io.socket.SocketDataPortServer;
import net.tarpn.network.NetworkManager2;
import net.tarpn.network.netrom.NetRomSocket;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.util.ParsedLine;
import net.tarpn.util.Timer;
import net.tarpn.util.Util;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class VirtualPortTest {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final Logger LOG = LoggerFactory.getLogger(VirtualPortTest.class);

    @Test
    public void test() throws Exception {
        Configs configs = Configs.read(ClassLoader.getSystemResourceAsStream("loopback1.ini"));
        PortConfig portConfig1 = configs.getPortConfigs().get(99);

        //DataPort port1 = PortFactory.createPortFromConfig(portConfig1);

        NetworkManager2 networkManager = NetworkManager2.create(configs.getNetRomConfig());
        networkManager.initialize(portConfig1);
        networkManager.start();
        //DataLinkManager dataLinkManager1 = DataLinkManager.create(portConfig1,
        //        port1, l2events1::add, System.err::println);
        //dataLinkManager1.start();
        //NetworkManager networkManager1 = NetworkManager.create(configs.getNetRomConfig());
        //networkManager1.start();


        configs = Configs.read(ClassLoader.getSystemResourceAsStream("loopback2.ini"));
        PortConfig portConfig2 = configs.getPortConfigs().get(100);
        //DataPort port2 = PortFactory.createPortFromConfig(portConfig2);
        //DataLinkManager dataLinkManager2 = DataLinkManager.create(portConfig2, port2, l2events2::add, System.err::println);
        //dataLinkManager2.start();
        //NetworkManager networkManager2 = NetworkManager.create(configs.getNetRomConfig());
        //networkManager2.start();

        //DataLink dataLink2 = DataLink.create(portConfig2, port2);
        NetworkManager2 networkManager2 = NetworkManager2.create(configs.getNetRomConfig());
        ApplicationRegistry.EchoApplication echoApp = new ApplicationRegistry.EchoApplication();
        ApplicationAdaptor appListener = new ApplicationAdaptor(echoApp, networkManager2::acceptNetworkPrimitive);

        networkManager2.initialize(portConfig2);
        networkManager2.addNetworkLinkListener("echo listener", appListener);
        networkManager2.start();

        Map<Integer, DataLinkManager> dataLinks = new HashMap<>();
        //dataLinks.putAll(networkManager.getPorts());

        ApplicationRegistry registry = new ApplicationRegistry();
        registry.registerApplication(new ApplicationRegistry.DefaultApplication(registry));
        registry.registerApplication(new ApplicationRegistry.EchoApplication());
        //registry.registerApplication(new DataLinkApplication(dataLinks));
        registry.registerApplication(new Application<String>() {

            private NetRomSocket netRomSocket;

            @Override
            public String getName() {
                return "l3";
            }

            @Override
            public Class<String> getType() {
                return String.class;
            }

            @Override
            public void onConnect(Consumer<String> response) {
                response.accept("Connected!");
            }

            @Override
            public void handle(String request, Consumer<String> response, Runnable closer) {
                ParsedLine line = ParsedLine.parse(request);
                String cmd = line.word().get();
                if (cmd.equalsIgnoreCase("C")) {
                    AX25Call call = AX25Call.fromString(line.word().get());

                    netRomSocket = networkManager.open(call);
                    try {
                        response.accept("Connecting to " + call);
                        netRomSocket.connect();
                        networkManager.addNetworkLinkListener(
                                "NetRom(" + netRomSocket.getCircuitId() + ")",
                                linkPrimitive -> {
                                    switch(linkPrimitive.getType()) {
                                        case NL_CONNECT:
                                            response.accept("Connected to " + netRomSocket.getAddress());
                                            break;
                                        case NL_DISCONNECT:
                                            response.accept("Disconnected from " + netRomSocket.getAddress());
                                            break;
                                        case NL_INFO:
                                            response.accept(Util.toEscapedASCII(linkPrimitive.getInfo()));
                                            break;
                                    }
                                });
                    } catch (Exception e) {
                        response.accept("Had an error: " + e.getMessage());
                    }
                } else if (cmd.equalsIgnoreCase("B")) {
                    if (netRomSocket != null) {
                        netRomSocket.close();
                        Timer.create(10, () -> {
                            networkManager.removeNetworkLinkListener(
                                    "NetRom(" + netRomSocket.getCircuitId() + ")");
                            netRomSocket = null;
                        });
                    }
                } else if(netRomSocket != null) {
                    netRomSocket.send(line.line().getBytes(StandardCharsets.UTF_8));
                } else {
                    response.accept("huh? " + line.line());
                }
            }

            @Override
            public void onDisconnect(Consumer<String> response) {

            }
        });

        // Start listening on a socket and serve applications
        SocketDataPortServer server = new SocketDataPortServer(registry);
        server.start();
        server.join();

        //dataLinkManager2.acceptDataLinkPrimitive(LinkPrimitive.newConnectRequest(AX25Call.create("TEST", 1)));

        // Server-side, configure default application, start DL event processing loop
    }
}
