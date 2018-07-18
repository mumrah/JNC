package net.tarpn.network.netrom;

import net.tarpn.datalink.LinkPrimitive;
import net.tarpn.network.netrom.NetRomCircuitEvent.Type;
import net.tarpn.network.netrom.NetRomCircuitEvent.UserDataEvent;
import net.tarpn.packet.impl.ax25.AX25Call;

public class NetRomSession {

  private final int circuitId;
  private final AX25Call address;
  private final NetRomCircuitManager circuitManager;

  private boolean connected = false;

  public NetRomSession(int circuitId, AX25Call address, NetRomCircuitManager circuitManager) {
    this.circuitId = circuitId;
    this.address = address;
    this.circuitManager = circuitManager;
  }

  public boolean isConnected() {
    return connected;
  }

  public void accept(LinkPrimitive incoming) {
    switch (incoming.getType()) {
      case DL_CONNECT:
        connected = true;
        break;
      case DL_DISCONNECT:
        connected = false;
        break;
      case DL_DATA:
      case DL_UNIT_DATA: {
        System.err.println("NetRomSession got: " + incoming.getLinkInfo().getInfoAsASCII());
        break;
      }
      case DL_ERROR:
        break;
    }
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
