package net.tarpn.io.impl;

import net.tarpn.io.DataPort;

import java.io.*;
import java.nio.channels.Pipe;

public class LoopBackPort implements DataPort {

  private final int port;
  private final InputStream inputStream;
  private final OutputStream outputStream;

  LoopBackPort(int port, InputStream inputStream, OutputStream outputStream) {
    this.port = port;
    this.inputStream = inputStream;
    this.outputStream = outputStream;
  }


  public static LoopBackPort[] createPair() throws Exception {
    PipedInputStream pipe1In = new PipedInputStream();
    PipedOutputStream pipe1Out = new PipedOutputStream();

    PipedInputStream pipe2In = new PipedInputStream();
    PipedOutputStream pipe2Out = new PipedOutputStream();

    pipe1In.connect(pipe2Out);
    pipe2In.connect(pipe1Out);

    LoopBackPort port1 = new LoopBackPort(
        99,
        pipe1In,
        pipe1Out
    );

    LoopBackPort port2 = new LoopBackPort(
        100,
        pipe2In,
        pipe2Out
    );

    return new LoopBackPort[]{port1, port2};
  }

  @Override
  public void open() throws IOException {

  }

  @Override
  public void close() throws IOException {
    inputStream.close();
    outputStream.close();
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
    return "loopback-" + port;
  }

  @Override
  public String getType() {
    return "loopback";
  }
}
