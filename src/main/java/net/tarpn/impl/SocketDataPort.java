package net.tarpn.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import net.tarpn.DataPort;

public class SocketDataPort implements DataPort {

  private final String name;
  private final Socket socket;

  private SocketDataPort(String name, Socket socket) {
    this.name = name;
    this.socket = socket;
  }

  @Override
  public void open() throws IOException {
  }

  @Override
  public void close() throws IOException {
    socket.close();
  }

  @Override
  public InputStream getInputStream() {
    try {
      return socket.getInputStream();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public OutputStream getOutputStream() {
    try {
      return socket.getOutputStream();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
