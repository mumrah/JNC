package net.tarpn;

import net.tarpn.app.Application;
import net.tarpn.app.ApplicationRegistry;
import net.tarpn.app.DataLinkApplication;
import net.tarpn.config.impl.Configs;
import net.tarpn.config.PortConfig;
import net.tarpn.datalink.DataLinkManager;
import net.tarpn.datalink.LinkPrimitive;
import net.tarpn.io.DataPort;
import net.tarpn.io.impl.PortFactory;
import net.tarpn.io.impl.VirtualPort;
import net.tarpn.io.socket.SocketDataPortServer;
import net.tarpn.network.NetworkManager;
import net.tarpn.packet.impl.ax25.AX25Packet;
import net.tarpn.util.Util;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class VirtualPortTest {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final Logger LOG = LoggerFactory.getLogger(VirtualPortTest.class);

    @Test
    public void test() throws Exception {
        Configs configs = Configs.read(ClassLoader.getSystemResourceAsStream("loopback1.ini"));
        PortConfig portConfig1 = configs.getPortConfigs().get(99);

        DataPort port1 = PortFactory.createPortFromConfig(portConfig1);
        BlockingQueue<LinkPrimitive> l2events1 = new LinkedBlockingQueue<>();

        DataLinkManager dataLinkManager1 = DataLinkManager.create(portConfig1,
                port1, l2events1::add, System.err::println);
        dataLinkManager1.start();
        //NetworkManager networkManager1 = NetworkManager.create(configs.getNetRomConfig());
        //networkManager1.start();


        BlockingQueue<LinkPrimitive> l2events2 = new LinkedBlockingQueue<>();
        configs = Configs.read(ClassLoader.getSystemResourceAsStream("loopback2.ini"));
        PortConfig portConfig2 = configs.getPortConfigs().get(100);
        DataPort port2 = PortFactory.createPortFromConfig(portConfig2);
        DataLinkManager dataLinkManager2 = DataLinkManager.create(portConfig2, port2, l2events2::add, System.err::println);
        dataLinkManager2.start();
        //NetworkManager networkManager2 = NetworkManager.create(configs.getNetRomConfig());
        //networkManager2.start();

        Map<Integer, DataLinkManager> dataLinks = new HashMap<>();
        dataLinks.put(dataLinkManager1.getDataPort().getPortNumber(), dataLinkManager1);
        dataLinks.put(dataLinkManager2.getDataPort().getPortNumber(), dataLinkManager2);

        ApplicationRegistry registry = new ApplicationRegistry();
        registry.registerApplication(new ApplicationRegistry.DefaultApplication(registry));
        registry.registerApplication(new ApplicationRegistry.EchoApplication());
        registry.registerApplication(new DataLinkApplication(dataLinks));

        // Start listening on a socket and serve applications
        SocketDataPortServer server = new SocketDataPortServer(registry);
        server.start();
        //server.join();

        //dataLinkManager2.acceptDataLinkPrimitive(LinkPrimitive.newConnectRequest(AX25Call.create("TEST", 1)));

        // Server-side, configure default application, start DL event processing loop
        ApplicationRegistry.EchoApplication app = new ApplicationRegistry.EchoApplication();
        Util.queueProcessingLoop(
                l2events2::poll,
                dataLinkPrimitive -> {
                    LOG.info("Got DL event " + dataLinkPrimitive);

                    Consumer<String> responder = response -> dataLinkManager2.acceptDataLinkPrimitive(
                            LinkPrimitive.newUnitDataRequest(
                                    dataLinkPrimitive.getRemoteCall(),
                                    AX25Packet.Protocol.NO_LAYER3,
                                    response.getBytes(StandardCharsets.UTF_8)
                    ));
                    try {
                        if (dataLinkPrimitive.getType().equals(LinkPrimitive.Type.DL_DATA) ||
                                dataLinkPrimitive.getType().equals(LinkPrimitive.Type.DL_UNIT_DATA)) {
                            AX25Packet.HasInfo info = dataLinkPrimitive.getLinkInfo();
                            app.handle(info.getInfoAsASCII(), responder, () -> { });
                        } else if (dataLinkPrimitive.getType().equals(LinkPrimitive.Type.DL_CONNECT)) {
                            Thread.sleep(100);
                            app.onConnect(responder);
                        } else if (dataLinkPrimitive.getType().equals(LinkPrimitive.Type.DL_DISCONNECT)) {
                            Thread.sleep(100);
                            app.onDisconnect(responder);
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                },
                (failedEvent, t) -> LOG.error("Error processing event " + failedEvent, t));
    }

    Consumer<LinkPrimitive> appListener(Application<String> app, DataLinkManager dataLinkManager) {
        return dataLinkPrimitive -> {
            LOG.info("Got DL event " + dataLinkPrimitive);

            Consumer<String> responder = response -> dataLinkManager.acceptDataLinkPrimitive(
                    LinkPrimitive.newUnitDataRequest(
                            dataLinkPrimitive.getRemoteCall(),
                            AX25Packet.Protocol.NO_LAYER3,
                            response.getBytes(StandardCharsets.UTF_8)
                    ));
            try {
                if (dataLinkPrimitive.getType().equals(LinkPrimitive.Type.DL_DATA) ||
                        dataLinkPrimitive.getType().equals(LinkPrimitive.Type.DL_UNIT_DATA)) {
                    AX25Packet.HasInfo info = dataLinkPrimitive.getLinkInfo();
                    app.handle(info.getInfoAsASCII(), responder, () -> { });
                } else if (dataLinkPrimitive.getType().equals(LinkPrimitive.Type.DL_CONNECT)) {
                    Thread.sleep(100);
                    app.onConnect(responder);
                } else if (dataLinkPrimitive.getType().equals(LinkPrimitive.Type.DL_DISCONNECT)) {
                    Thread.sleep(100);
                    app.onDisconnect(responder);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
