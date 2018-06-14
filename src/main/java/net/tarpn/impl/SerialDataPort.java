package net.tarpn.impl;

import com.fazecast.jSerialComm.SerialPort;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.tarpn.DataPort;

public class SerialDataPort implements DataPort {
  private final String name;
  private final SerialPort serialPort;

  private SerialDataPort(String name, SerialPort serialPort) {
    this.name = name;
    this.serialPort = serialPort;
  }

  @Override
  public void open() throws IOException {
    boolean res = serialPort.openPort();
    if(!res) {
      throw new IOException("Could not open port " + name);
    }
  }

  @Override
  public void close() throws IOException {
    boolean res = serialPort.closePort();
    if(!res) {
      throw new IOException("Could not close port " + name);
    }
  }

  @Override
  public InputStream getInputStream() {
    return serialPort.getInputStream();
  }

  @Override
  public OutputStream getOutputStream() {
    return serialPort.getOutputStream();
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getType() {
    return "serial";
  }


  public static DataPort openPort(String tty, int baud) {
    SerialPort port = SerialPort.getCommPort(tty);
    port.setBaudRate(baud);
    return new SerialDataPort(tty, port);
  }
}
