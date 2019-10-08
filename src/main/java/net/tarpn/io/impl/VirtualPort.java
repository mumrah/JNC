package net.tarpn.io.impl;

import net.tarpn.io.DataPort;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

public class VirtualPort implements DataPort {

  private final int port;
  private final String name;
  private final PipedInputStream inputStream;
  private final PipedOutputStream outputStream;
  private VirtualPort remotePort;

  VirtualPort(int port, String name, PipedInputStream inputStream, PipedOutputStream outputStream) {
    this.port = port;
    this.name = name;
    this.inputStream = inputStream;
    this.outputStream = outputStream;
  }

  public static Map<String, VirtualPort> virtualPorts = new HashMap<>();

  public static synchronized VirtualPort create(int portNumber, String name, String pipedToName) {
    VirtualPort port = virtualPorts.computeIfAbsent(name, newName -> {
      PipedInputStream inputStream = new PipedInputStream();
      PipedOutputStream outputStream = new PipedOutputStream();
      return new VirtualPort(portNumber, newName, inputStream, outputStream);
    });

    VirtualPort other = virtualPorts.get(pipedToName);
    if (other != null) {
      try {
        port.connect(other);
      } catch (IOException e) {
        throw new RuntimeException("Could not connect ports " + port.name + " and " + other.name, e);
      }
    }

    return port;
  }

  private synchronized void connect(VirtualPort other) throws IOException {
    if (this.remotePort == null && other.remotePort == null) {
      this.inputStream.connect(other.outputStream);
      other.inputStream.connect(this.outputStream);
      this.remotePort = other;
      other.remotePort = this;
    } else {
      throw new IllegalStateException("VirtualPort " + name + " is already connected to " + other.name);
    }

  }

  @Override
  public void open() throws IOException {

  }

  @Override
  public synchronized void close() throws IOException {
    inputStream.close();
    outputStream.close();
    virtualPorts.remove(name);
  }

  @Override
  public boolean isOpen() {
    return true;
  }

  @Override
  public boolean reopen() {
    return true;
  }

  @Override
  public InputStream getInputStream() {
    return inputStream;
  }

  @Override
  public OutputStream getOutputStream() {
    return outputStream;
  }

  @Override
  public int getPortNumber() {
    return port;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getType() {
    return "virtual";
  }
}
