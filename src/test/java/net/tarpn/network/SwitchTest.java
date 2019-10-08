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

public class SwitchTest {
    AX25Call call1 = AX25Call.create("T3ST", 1);
    NetworkManager2 nl1;

    AX25Call call2 = AX25Call.create("T3ST", 2);
    NetworkManager2 nl2;

    AX25Call call3 = AX25Call.create("T3ST", 3);
    NetworkManager2 nl3;

    DataLinkTest.TestClock clock1 = new DataLinkTest.TestClock();
    DataLinkTest.TestClock clock2 = new DataLinkTest.TestClock();
    DataLinkTest.TestClock clock3 = new DataLinkTest.TestClock();

    @Before
    public void setup() {
        Map<String, Object> config1 = new HashMap<>();
        config1.put("node.call", "T3ST-1");
        config1.put("node.alias", "TEST1");
        config1.put("port.type", "virtual");
        config1.put("port.name", "pty0");
        config1.put("port.piped.to", "pty1");
        config1.put("netrom.nodes.interval", 0);
        PortConfig portConfig1 = createVirtualPortConfig(0, config1);

        nl1 = NetworkManager2.create(createNetromConfig(config1), clock1);
        nl1.initialize(portConfig1);
        nl1.start();

        Map<String, Object> config2 = new HashMap<>();
        config2.put("node.call", "T3ST-2");
        config2.put("node.alias", "TEST2");
        config2.put("port.type", "virtual");
        config2.put("port.name", "pty1");
        config2.put("port.piped.to", "pty0");
        config2.put("netrom.nodes.interval", 0);
        PortConfig portConfig2a = createVirtualPortConfig(0, config2);

        config2.put("port.type", "virtual");
        config2.put("port.name", "pty2");
        config2.put("port.piped.to", "pty3");
        PortConfig portConfig2b = createVirtualPortConfig(1, config2);

        nl2 = NetworkManager2.create(createNetromConfig(config2), clock2);
        nl2.initialize(portConfig2a);
        nl2.initialize(portConfig2b);
        nl2.start();

        Map<String, Object> config3 = new HashMap<>();
        config3.put("node.call", "T3ST-3");
        config3.put("node.alias", "TEST3");
        config3.put("port.type", "virtual");
        config3.put("port.name", "pty3");
        config3.put("port.piped.to", "pty2");
        config3.put("netrom.nodes.interval", 0);
        PortConfig portConfig3 = createVirtualPortConfig(0, config3);

        nl3 = NetworkManager2.create(createNetromConfig(config3), clock3);
        nl3.initialize(portConfig3);
        nl3.start();
    }

    @After
    public void teardown() {
        nl1.stop();
        nl2.stop();
        nl3.stop();
    }

    @Test
    public void testBasicConnect() throws InterruptedException {
        Queue<NetworkPrimitive> node1primitives = new LinkedList<>();
        Queue<NetworkPrimitive> node2primitives = new LinkedList<>();
        Queue<NetworkPrimitive> node3primitives = new LinkedList<>();

        nl1.addNetworkLinkListener("test-listener", networkPrimitive -> {
            System.err.println("TEST1 got: " + networkPrimitive);
            node1primitives.add(networkPrimitive);
        });
        nl2.addNetworkLinkListener("test-listener", networkPrimitive -> {
            System.err.println("TEST2 got: " + networkPrimitive);
            node2primitives.add(networkPrimitive);
        });
        nl3.addNetworkLinkListener("test-listener", networkPrimitive -> {
            System.err.println("TEST3 got: " + networkPrimitive);
            node3primitives.add(networkPrimitive);
        });

        nl1.broadcastRoutingTable();
        Thread.sleep(1000);

        nl3.broadcastRoutingTable();
        Thread.sleep(1000);

        nl2.broadcastRoutingTable();
        Thread.sleep(5000);

        nl1.acceptNetworkPrimitive(NetworkPrimitive.newConnect(call3));
        //waitUntil(() -> primitives.size() > 0, 3000, "Timed out");

        Thread.sleep(5000);
        for (int i = 0; i < 7; i++) {
            nl1.acceptNetworkPrimitive(NetworkPrimitive.newDataIndication(call3, Util.ascii("Hello, TEST3 " + i)));
            nl3.acceptNetworkPrimitive(NetworkPrimitive.newDataIndication(call1, Util.ascii("Hello, TEST1 " + i)));
            //Thread.sleep(1000);
        }

        waitUntil(() -> {
            NetworkPrimitive primitive = node3primitives.poll();
            if (primitive != null) {
                return primitive.getType().equals(NetworkPrimitive.Type.NL_INFO) &&
                        Util.ascii(primitive.getInfo()).equalsIgnoreCase("Hello, TEST3 6");
            } else {
                return false;
            }
        }, 60000, "Timed out");
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
