package net.tarpn.datalink2;

import net.tarpn.DataLinkTest;
import net.tarpn.config.PortConfig;
import net.tarpn.config.impl.PortConfigImpl;
import net.tarpn.datalink.DataLink;
import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.packet.impl.ax25.AX25Call;
import org.apache.commons.configuration2.MapConfiguration;
import org.junit.Assert;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class DataLink2Test {
    @Test
    public void test() throws Exception {
        AX25Call call1 = AX25Call.create("TEST", 1);
        AX25Call call2 = AX25Call.create("TEST", 2);

        DataLinkTest.TestClock clock1 = new DataLinkTest.TestClock();
        DataLinkTest.TestClock clock2 = new DataLinkTest.TestClock();

        Map<String, Object> config1 = new HashMap<>();
        config1.put("node.call", call1.toString());
        config1.put("port.type", "virtual");
        config1.put("port.name", "pty0");
        config1.put("port.piped.to", "pty1");
        PortConfig portConfig1 = createVirtualPortConfig(0, config1);

        Map<String, Object> config2 = new HashMap<>();
        config2.put("node.call", call2.toString());
        config2.put("port.type", "virtual");
        config2.put("port.name", "pty1");
        config2.put("port.piped.to", "pty0");
        PortConfig portConfig2 = createVirtualPortConfig(1, config2);

        DataLink2 dl1 = DataLink2.create(portConfig1, clock1);
        DataLink2 dl2 = DataLink2.create(portConfig2, clock2);

        AX25ServerChannel server = AX25ServerChannel.open(1);
        server.bind(AX25Call.create("TEST-2"));

        dl1.sendDataLinkEvent(DataLinkPrimitive.newConnectRequest(call2, call1));
        dl1.poll(); // Send the SAMB
        dl2.poll(); // Send back a UA

        Assert.assertTrue(server.isAcceptable());
        AX25ClientChannel client = server.accept();
        Assert.assertTrue(client.isConnected());

        client.write("Hello, World!");
        dl1.poll(); // send
        dl2.poll(); // recv

        dl1.poll();
        dl2.poll();

        dl1.poll();
        dl2.poll();

    }

    public static PortConfig createVirtualPortConfig(int portNumber, Map<String, Object> extra) {
        MapConfiguration mapConfiguration = new MapConfiguration(new HashMap<>(extra));

        return new PortConfigImpl(portNumber, mapConfiguration) {
            @Override
            public boolean isEnabled() {
                return true;
            }

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
