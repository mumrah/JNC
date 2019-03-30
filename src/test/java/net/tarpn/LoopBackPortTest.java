package net.tarpn;

import net.tarpn.app.ApplicationRegistry;
import net.tarpn.app.DataLinkApplication;
import net.tarpn.config.Configs;
import net.tarpn.config.PortConfig;
import net.tarpn.datalink.DataLinkManager;
import net.tarpn.datalink.LinkPrimitive;
import net.tarpn.io.impl.LoopBackPort;
import net.tarpn.io.socket.SocketDataPortServer;
import net.tarpn.packet.impl.ax25.AX25Call;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.stream.Collectors;

public class LoopBackPortTest {
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static final Logger LOG = LoggerFactory.getLogger(LoopBackPortTest.class);

    @Test
    public void test() throws Exception {
        Configs configs = Configs.read(ClassLoader.getSystemResourceAsStream("loopback1.ini"));
        PortConfig portConfig1 = configs.getPortConfigs().get(1);

        LoopBackPort[] pair = LoopBackPort.createPair();
        BlockingQueue<LinkPrimitive> inQueue = new LinkedBlockingQueue<>();

        DataLinkManager dataLinkManager1 = DataLinkManager.create(portConfig1, pair[0], inQueue::add, System.err::println);
        dataLinkManager1.start();

        configs = Configs.read(ClassLoader.getSystemResourceAsStream("loopback2.ini"));
        PortConfig portConfig2 = configs.getPortConfigs().get(1);

        DataLinkManager dataLinkManager2 = DataLinkManager.create(portConfig2, pair[1], inQueue::add, System.err::println);
        dataLinkManager2.start();

        Map<Integer, DataLinkManager> dataLinks = new HashMap<>();
        dataLinks.put(dataLinkManager1.getDataPort().getPortNumber(), dataLinkManager1);
        dataLinks.put(dataLinkManager2.getDataPort().getPortNumber(), dataLinkManager2);

        ApplicationRegistry registry = new ApplicationRegistry();
        registry.registerApplication(new ApplicationRegistry.DefaultApplication(registry));
        registry.registerApplication(new ApplicationRegistry.EchoApplication());
        registry.registerApplication(new DataLinkApplication(dataLinks));

        SocketDataPortServer server = new SocketDataPortServer(registry);
        server.start();
        server.join();

        //dataLinkManager2.acceptDataLinkPrimitive(LinkPrimitive.newConnectRequest(AX25Call.create("TEST", 1)));

    }
}
