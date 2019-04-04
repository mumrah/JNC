package net.tarpn.network.netrom;

import net.tarpn.network.netrom.NetRomCircuitEvent.Type;
import net.tarpn.network.netrom.NetRomCircuitEvent.UserDataEvent;
import net.tarpn.packet.impl.ax25.AX25Call;

public class NetRomSocket {
  private final AX25Call address;
  private final NetRomCircuitManager circuitManager;
  private final int circuitId; // kind of like client port in sockets

  private boolean connected = false;

  public NetRomSocket(AX25Call address, NetRomCircuitManager circuitManager) {
    this.address = address;
    this.circuitManager = circuitManager;
    this.circuitId = circuitManager.open(address);
  }

  public void connect() {
    circuitManager.onCircuitEvent(new NetRomCircuitEvent(circuitId, address, Type.NL_CONNECT));
    connected = true;
  }

  public boolean isConnected() {
    return connected;
  }

  public void send(byte[] payload) {
    if(connected) {
      circuitManager.onCircuitEvent(new UserDataEvent(circuitId, address, payload));
    } else {
      throw new IllegalStateException("Cannot send data, session is not connected");
    }
  }

  public void close() {
    circuitManager.onCircuitEvent(new NetRomCircuitEvent(circuitId, address, Type.NL_DISCONNECT));
  }

  public int getCircuitId() {
    return circuitId;
  }

  public AX25Call getAddress() {
    return address;
  }
}
