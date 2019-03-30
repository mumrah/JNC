package net.tarpn.network.netrom;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import net.tarpn.datalink.LinkPrimitive;
import net.tarpn.network.netrom.NetRomCircuitEvent.Type;
import net.tarpn.network.netrom.NetRomCircuitEvent.UserDataEvent;
import net.tarpn.packet.impl.ax25.AX25Call;

public class NetRomSocket {
  private final AX25Call address;
  private final NetRomCircuitManager circuitManager;

  private int circuitId; // kind of like client port in sockets
  private BlockingQueue<LinkPrimitive> circuitEvents;

  private boolean connected = false;
  private boolean fault = false;
  private boolean closed = false;

  public NetRomSocket(AX25Call address, NetRomCircuitManager circuitManager) {
    this.address = address;
    this.circuitManager = circuitManager;
  }

  public void connect() throws InterruptedException, IOException {
    // open a circuit
    this.circuitId = circuitManager.open(address);
    this.circuitEvents = circuitManager.getCircuitBuffer(circuitId);

    // send the connect request
    circuitManager.onCircuitEvent(new NetRomCircuitEvent(circuitId, address, Type.NL_CONNECT));
    LinkPrimitive response = circuitEvents.poll(10, TimeUnit.SECONDS);
    if(response == null) {
      connected = false;
      fault = true;
      closed = false;
      throw new InterruptedIOException("Timed out while trying to connect to " + address);
    } else {
      if(response.getType().equals(LinkPrimitive.Type.DL_CONNECT)) {
        connected = true;
        fault = false;
        closed = false;
      } else {
        connected = false;
        fault = true;
        closed = false;
        throw new IOException("Could not connect to " + address + ". Got " + response);
      }
    }
  }

  public boolean tryConnect() throws IOException {
    try {
      connect();
    } catch (InterruptedIOException e) {
      // do nothing
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return connected;
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

  public byte[] recv() {
    if(connected) {
      LinkPrimitive response = circuitEvents.poll();
      if(response == null) {
        return new byte[]{};
      } else {
        return response.getLinkInfo().getInfo();
      }
    } else {
      throw new IllegalStateException("Cannot receive data, session is not connected");
    }
  }

  public void close() {
    circuitManager.onCircuitEvent(new NetRomCircuitEvent(circuitId, address, Type.NL_DISCONNECT));
    try {
      LinkPrimitive response = circuitEvents.poll(1, TimeUnit.SECONDS);
      while(response == null || response.getType().equals(LinkPrimitive.Type.DL_DISCONNECT)) {
        response = circuitEvents.poll(1, TimeUnit.SECONDS);
      }
      closed = true;
      fault = false;
      connected = false;
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      closed = true;
      fault = true;
      connected = false;
    }
  }

  public AX25Call getAddress() {
    return address;
  }
}
