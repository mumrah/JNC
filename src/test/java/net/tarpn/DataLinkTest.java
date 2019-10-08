package net.tarpn;

import net.tarpn.config.PortConfig;
import net.tarpn.config.impl.PortConfigImpl;
import net.tarpn.datalink.DataLink;
import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.io.impl.PortFactory;
import net.tarpn.packet.impl.ax25.*;
import net.tarpn.util.Clock;
import net.tarpn.util.Util;
import org.apache.commons.configuration2.MapConfiguration;
import org.junit.*;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class DataLinkTest {

    AX25Call call1 = AX25Call.create("TEST", 1);
    DataLink dl1;

    AX25Call call2 = AX25Call.create("TEST", 2);
    DataLink dl2;

    TestClock clock1 = new TestClock();
    TestClock clock2 = new TestClock();

    @Before
    public void setup() {
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
        PortConfig portConfig2 = createVirtualPortConfig(0, config2);

        dl1 = DataLink.create(portConfig1, PortFactory.createPortFromConfig(portConfig1),
                Executors.newScheduledThreadPool(4), clock1);
        dl2 = DataLink.create(portConfig2, PortFactory.createPortFromConfig(portConfig2),
                Executors.newScheduledThreadPool(4), clock2);

        dl1.start();
        dl2.start();
    }

    @After
    public void teardown() {
        dl1.stop();
        dl2.stop();
    }

    @Test
    public void testBasicConnectDisconnect() {
        List<DataLinkPrimitive> events = new ArrayList<>();
        dl1.addDataLinkListener("test-listener-1", events::add);
        dl2.addDataLinkListener("test-listener-2", events::add);

        Assert.assertEquals(dl1.getAx25StateHandler().getState(call2).getState(), AX25State.State.DISCONNECTED);
        dl1.sendDataLinkEvent(DataLinkPrimitive.newConnectRequest(call2));

        waitUntil(() -> dl1.getAx25StateHandler().getState(call2).getState().equals(AX25State.State.AWAITING_CONNECTION), 1000, "Timed out");
        waitUntil(() -> dl2.getAx25StateHandler().getState(call1).getState().equals(AX25State.State.CONNECTED), 1000, "Timed out");
        waitUntil(() -> dl1.getAx25StateHandler().getState(call2).getState().equals(AX25State.State.CONNECTED), 1000, "Timed out");

        Assert.assertEquals(events.size(), 2);
        Assert.assertEquals(events.get(0), DataLinkPrimitive.newConnectIndication(call1));
        Assert.assertEquals(events.get(1), DataLinkPrimitive.newConnectConfirmation(call2));
        events.clear();

        dl1.sendDataLinkEvent(DataLinkPrimitive.newDisconnectRequest(call2));
        waitUntil(() -> dl1.getAx25StateHandler().getState(call2).getState().equals(AX25State.State.AWAITING_RELEASE), 1000, "Timed out");
        waitUntil(() -> dl2.getAx25StateHandler().getState(call1).getState().equals(AX25State.State.DISCONNECTED), 1000, "Timed out");
        waitUntil(() -> dl1.getAx25StateHandler().getState(call2).getState().equals(AX25State.State.DISCONNECTED), 1000, "Timed out");

        Assert.assertEquals(events.size(), 2);
        Assert.assertEquals(events.get(0), DataLinkPrimitive.newDisconnectIndication(call1));
        Assert.assertEquals(events.get(1), DataLinkPrimitive.newDisconnectConfirmation(call2));
    }

    @Test
    public void testRemoteDisconnect() {
        List<DataLinkPrimitive> events = new ArrayList<>();
        dl1.addDataLinkListener("test-listener-1", events::add);
        dl2.addDataLinkListener("test-listener-2", events::add);

        Assert.assertEquals(dl1.getAx25StateHandler().getState(call2).getState(), AX25State.State.DISCONNECTED);
        dl1.sendDataLinkEvent(DataLinkPrimitive.newConnectRequest(call2));

        waitUntil(() -> dl1.getAx25StateHandler().getState(call2).getState().equals(AX25State.State.AWAITING_CONNECTION), 1000, "Timed out");
        waitUntil(() -> dl2.getAx25StateHandler().getState(call1).getState().equals(AX25State.State.CONNECTED), 1000, "Timed out");
        waitUntil(() -> dl1.getAx25StateHandler().getState(call2).getState().equals(AX25State.State.CONNECTED), 1000, "Timed out");

        Assert.assertEquals(events.size(), 2);
        Assert.assertEquals(events.get(0), DataLinkPrimitive.newConnectIndication(call1));
        Assert.assertEquals(events.get(1), DataLinkPrimitive.newConnectConfirmation(call2));
        events.clear();

        dl2.sendDataLinkEvent(DataLinkPrimitive.newDisconnectRequest(call1));
        waitUntil(() -> dl2.getAx25StateHandler().getState(call1).getState().equals(AX25State.State.AWAITING_RELEASE), 1000, "Timed out");
        waitUntil(() -> dl1.getAx25StateHandler().getState(call2).getState().equals(AX25State.State.DISCONNECTED), 1000, "Timed out");
        waitUntil(() -> dl2.getAx25StateHandler().getState(call1).getState().equals(AX25State.State.DISCONNECTED), 1000, "Timed out");

        Assert.assertEquals(events.size(), 2);
        Assert.assertEquals(events.get(0), DataLinkPrimitive.newDisconnectIndication(call2));
        Assert.assertEquals(events.get(1), DataLinkPrimitive.newDisconnectConfirmation(call1));
    }

    @Test
    public void sendManyIFrames() {
        List<DataLinkPrimitive> events = new ArrayList<>();
        dl1.sendDataLinkEvent(DataLinkPrimitive.newConnectRequest(call2));
        waitUntil(() -> dl1.getAx25StateHandler().getState(call2).getState().equals(AX25State.State.CONNECTED), 1000, "Timed out");

        dl2.addDataLinkListener("test-listener-1", events::add);
        for (int i = 0; i < 10; i++) {
            dl1.sendDataLinkEvent(DataLinkPrimitive.newDataRequest(call2, AX25Packet.Protocol.NO_LAYER3, Util.ascii("Test " + i)));
        }

        waitUntil(() -> events.size() >= 10, 5000, "Timed out");
        events.forEach(System.err::println);
    }

    @Test
    public void testStateMachine() {
        List<AX25Packet> outgoingPackets = new ArrayList<>();
        Queue<DataLinkPrimitive> dataLinkEvents = new LinkedList<>();

        AX25StateMachine stateMachine = new AX25StateMachine(dl1.getPortConfig(), outgoingPackets::add, dataLinkEvents::add, clock1);
        stateMachine.getEventQueue().add(AX25StateEvent.createConnectEvent(call2));
        stateMachine.poll();
        Assert.assertEquals(stateMachine.getState(call2).getState(), AX25State.State.AWAITING_CONNECTION);

        // simulate response
        stateMachine.getEventQueue().add(AX25StateEvent.createIncomingEvent(
                UFrame.create(call1, call2, AX25Packet.Command.RESPONSE,
                        AX25Packet.UnnumberedFrame.ControlType.UA, true),
                AX25StateEvent.Type.AX25_UA));
        stateMachine.poll();
        Assert.assertEquals(stateMachine.getState(call2).getState(), AX25State.State.CONNECTED);
        Assert.assertEquals(dataLinkEvents.poll(), DataLinkPrimitive.newConnectConfirmation(call2));

        // queue up 7 iframes with no ack
        for (int i = 0; i < 7; i++) {
            stateMachine.getEventQueue().add(
                    AX25StateEvent.createDataEvent(call2, AX25Packet.Protocol.NO_LAYER3, Util.ascii("IFrame " + i)));
        }

        // process the DL_DATA and IFRAME_READY events
        for (int i = 0; i < 14; i++) {
            stateMachine.poll();
        }

        // add another
        stateMachine.getEventQueue().add(
                AX25StateEvent.createDataEvent(call2, AX25Packet.Protocol.NO_LAYER3, Util.ascii("Another")));

        // handle the DL_DATA
        stateMachine.poll();

        // handle IFRAME_READY, fail
        stateMachine.poll();

        stateMachine.getEventQueue().add(AX25StateEvent.createT1ExpireEvent(call2));

        stateMachine.poll();

        stateMachine.getEventQueue().add(
                AX25StateEvent.createIncomingEvent(
                        SFrame.create(call1, call2, AX25Packet.Command.RESPONSE,
                                AX25Packet.SupervisoryFrame.ControlType.RR, 7, true),
                        AX25StateEvent.Type.AX25_RR));

        // Process RR response
        stateMachine.poll();

        stateMachine.poll();
        stateMachine.poll();
        stateMachine.poll();
        stateMachine.poll();
        stateMachine.poll();
        stateMachine.poll();
        stateMachine.poll();



    }

    @Test
    public void testWindowExceeded() {
        List<AX25Packet> outgoingPackets = new ArrayList<>();
        Queue<DataLinkPrimitive> dataLinkEvents = new LinkedList<>();
        AX25StateMachine stateMachine = new AX25StateMachine(dl1.getPortConfig(), outgoingPackets::add, dataLinkEvents::add, clock1);

        stateMachine.getEventQueue().add(AX25StateEvent.createIFrameQueueEvent(call2));
        stateMachine.poll();


        // 2019-04-16T17:05:52,979 - TRACE [r.pac.imp.ax2.AX25StateMachine@174] [] - AX25 AFTER : AX25State(T3ST-3){local=T3ST-2, remote=T3ST-3,
        // state=TIMER_RECOVERY, V(s)=15, N(s)=7, V(r)=5, N(r)=5, V(a)=0, SRT=2201, T1=4850/17608, T3=-1/180000}
        AX25State state = stateMachine.getState(call2);
        state.setState(AX25State.State.CONNECTED);

        for (int i = 0; i < 8; i++) {
            stateMachine.getEventQueue().add(AX25StateEvent.createDataEvent(call2, AX25Packet.Protocol.NO_LAYER3, Util.ascii("IFrame " + i)));
            stateMachine.poll();
            stateMachine.getEventQueue().clear();
        }

        state.setSendState(15);
        state.setReceiveState(5);
        state.setAcknowledgeState((byte)0);
        state.setSRT(2201);
        state.setState(AX25State.State.TIMER_RECOVERY);

        stateMachine.getEventQueue().add(AX25StateEvent.createIFrameQueueEvent(call2));
        stateMachine.poll();

        state.incrementRC();
        state.incrementRC();
        state.incrementRC();
        state.incrementRC();

        stateMachine.getEventQueue().add(AX25StateEvent.createT1ExpireEvent(call2));
        stateMachine.poll();


    }

    public static class TestClock implements Clock {

        AtomicLong time = new AtomicLong(0L);

        @Override
        public long millis() {
            return time.get();
        }

        @Override
        public void sleep(int millis) throws InterruptedException {
            time.addAndGet(millis);
            tick();
        }

        public void millis(long newTime) {
            time.set(newTime);
            tick();
        }

        public void tick() {

        }


    }

    public static void waitUntil(Supplier<Boolean> supplier, int timeoutMs, String message) {
        long t0 = System.currentTimeMillis();
        while (!supplier.get()) {
            if (System.currentTimeMillis() - t0 > timeoutMs) {
                Assert.fail(message);
            } else {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
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
