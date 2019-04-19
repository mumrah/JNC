package net.tarpn.network;

import net.tarpn.DataLinkTest;
import net.tarpn.config.NetRomConfig;
import net.tarpn.config.PortConfig;
import net.tarpn.config.impl.NetRomConfigImpl;
import net.tarpn.network.netrom.NetRomCircuit;
import net.tarpn.network.netrom.NetworkPrimitive;
import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.util.Util;
import org.apache.commons.configuration2.MapConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import static net.tarpn.DataLinkTest.createVirtualPortConfig;
import static net.tarpn.DataLinkTest.waitUntil;

public class NetworkTest {
    AX25Call call1 = AX25Call.create("T3ST", 1);
    NetworkManager2 nl1;

    AX25Call call2 = AX25Call.create("T3ST", 2);
    NetworkManager2 nl2;

    DataLinkTest.TestClock clock1 = new DataLinkTest.TestClock();
    DataLinkTest.TestClock clock2 = new DataLinkTest.TestClock();

    @Before
    public void setup() {
        Map<String, Object> config1 = new HashMap<>();
        config1.put("node.call", "T3ST-1");
        config1.put("node.alias", "TEST1");
        config1.put("port.type", "virtual");
        config1.put("port.name", "pty0");
        config1.put("port.piped.to", "pty1");
        PortConfig portConfig1 = createVirtualPortConfig(0, config1);


        Map<String, Object> config2 = new HashMap<>();
        config2.put("node.call", "T3ST-2");
        config2.put("node.alias", "TEST2");
        config2.put("port.type", "virtual");
        config2.put("port.name", "pty1");
        config2.put("port.piped.to", "pty0");
        PortConfig portConfig2 = createVirtualPortConfig(0, config2);

        nl1 = NetworkManager2.create(createNetromConfig(config1), clock1);
        nl1.initialize(portConfig1);
        nl1.start();

        nl2 = NetworkManager2.create(createNetromConfig(config2), clock2);
        nl2.initialize(portConfig2);
        nl2.start();
    }

    @After
    public void teardown() {
        nl1.stop();
        nl2.stop();
    }

    @Test
    public void testBasicConnect() throws InterruptedException {
        Queue<NetworkPrimitive> primitives = new LinkedList<>();
        nl1.addNetworkLinkListener("test-listener", networkPrimitive -> {
            System.err.println("TEST1 got: " + networkPrimitive);
        });
        nl2.addNetworkLinkListener("test-listener", networkPrimitive -> {
            System.err.println("TEST2 got: " + networkPrimitive);
        });


        nl2.broadcastRoutingTable();
        Thread.sleep(3000);

        nl1.acceptNetworkPrimitive(NetworkPrimitive.newConnect(call2));
        //waitUntil(() -> primitives.size() > 0, 3000, "Timed out");

        Thread.sleep(3000);
        nl1.acceptNetworkPrimitive(NetworkPrimitive.newDataIndication(call2, Util.ascii("Hello, world")));

        nl1.join();
    }

    NetRomConfig createNetromConfig(Map<String, Object> extra) {
        MapConfiguration mapConfiguration = new MapConfiguration(extra);

        return new NetRomConfigImpl(mapConfiguration) {

            @Override
            public String getIdMessage() {
                return "This is " + getNodeAlias();
            }

            @Override
            public int getIdInterval() {
                return -1;
            }
        };
    }
}
