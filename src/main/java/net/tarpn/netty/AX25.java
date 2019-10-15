package net.tarpn.netty;

import net.tarpn.packet.impl.ax25.AX25Call;
import net.tarpn.packet.impl.ax25.AX25State;
import net.tarpn.packet.impl.ax25.handlers.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// TODO no need for this class here, move to AX25StateHadler
public class AX25 {

    public static final Map<AX25State.State, StateHandler> handlers = new HashMap<>();
    public static final Map<AX25Call, AX25State> sessions = new ConcurrentHashMap<>();

    static {
        handlers.put(AX25State.State.DISCONNECTED, new DisconnectedStateHandler());
        handlers.put(AX25State.State.CONNECTED, new ConnectedStateHandler());
        handlers.put(AX25State.State.AWAITING_CONNECTION, new AwaitingConnectionStateHandler());
        handlers.put(AX25State.State.TIMER_RECOVERY, new TimerRecoveryStateHandler());
        handlers.put(AX25State.State.AWAITING_RELEASE, new AwaitingReleaseStateHandler());
    }
}
