package net.tarpn.network.netrom;

import java.util.Queue;
import java.util.function.Consumer;
import net.tarpn.datalink.LinkPrimitive;
import net.tarpn.network.netrom.NetRomCircuitEvent.Type;
import net.tarpn.network.netrom.NetRomCircuitEvent.UserDataEvent;
import net.tarpn.packet.impl.ax25.AX25Call;

public class NetRomSession {

  private final int circuitId;
  private final AX25Call address;
  private final NetRomCircuitManager circuitManager;
  private final Queue<LinkPrimitive> networkEvents;

  private boolean connected = false;

  public NetRomSession(int circuitId, AX25Call address, NetRomCircuitManager circuitManager,
      Queue<LinkPrimitive> networkEvents) {
    this.circuitId = circuitId;
    this.address = address;
    this.circuitManager = circuitManager;
    this.networkEvents = networkEvents;
  }

  public boolean isConnected() {
    return connected;
  }

  public LinkPrimitive read() {
    return networkEvents.poll();
  }

  public void connect() {
    circuitManager.onCircuitEvent(new NetRomCircuitEvent(circuitId, address, Type.NL_CONNECT));
  }

  public void write(byte[] data) {
    circuitManager.onCircuitEvent(new UserDataEvent(circuitId, address, data));
  }

  public void close() {
    circuitManager.onCircuitEvent(new NetRomCircuitEvent(circuitId, address, Type.NL_DISCONNECT));
  }
}
