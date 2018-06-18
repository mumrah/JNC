package net.tarpn.io.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import net.tarpn.io.DataPort;

public class SocketDataPort implements DataPort {

  private final int port;
  private final String name;
  private final Socket clientSocket;

  public SocketDataPort(int port, String name, Socket clientSocket) {
    this.port = port;
    this.name = name;
    this.clientSocket = clientSocket;
  }

  @Override
  public void open() throws IOException {
    // No-op, already opened
  }

  @Override
  public void close() throws IOException {
    clientSocket.close();
  }

  @Override
  public InputStream getInputStream() {
    try {
      return clientSocket.getInputStream();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public OutputStream getOutputStream() {
    try {
      return clientSocket.getOutputStream();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
    return "tcp";
  }
}
