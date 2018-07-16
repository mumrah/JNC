package net.tarpn.network.netrom;

import java.util.function.Consumer;
import net.tarpn.network.netrom.NetRomCircuitEvent.Type;
import net.tarpn.network.netrom.NetRomCircuitEvent.UserDataEvent;
import net.tarpn.packet.impl.ax25.AX25Call;

public class NetRomSession {

  private final int circuitId;
  private final AX25Call address;
  private final Consumer<NetRomCircuitEvent> eventConsumer;

  public NetRomSession(int circuitId, AX25Call address, Consumer<NetRomCircuitEvent> eventConsumer) {
    this.circuitId = circuitId;
    this.address = address;
    this.eventConsumer = eventConsumer;
  }

  public void connect() {
    eventConsumer.accept(new NetRomCircuitEvent(circuitId, address, Type.NL_CONNECT));
  }

  public void write(byte[] data) {
    eventConsumer.accept(new UserDataEvent(circuitId, address, data));
  }
}
