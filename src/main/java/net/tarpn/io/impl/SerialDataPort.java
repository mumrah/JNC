package net.tarpn.io.impl;

import com.fazecast.jSerialComm.SerialPort;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import net.tarpn.io.DataPort;

public class SerialDataPort implements DataPort {
  private final int portNumber;
  private final String portName;
  private final SerialPort serialPort;

  private SerialDataPort(int portNumber, String portName, SerialPort serialPort) {
    this.portNumber = portNumber;
    this.portName = portName;
    this.serialPort = serialPort;
  }

  @Override
  public void open() throws IOException {
    boolean res = serialPort.openPort();
    if(!res) {
      throw new IOException("Could not open port " + portName);
    }
  }

  @Override
  public void close() throws IOException {
    boolean res = serialPort.closePort();
    if(!res) {
      throw new IOException("Could not close port " + portName);
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
  public int getPortNumber() {
    return portNumber;
  }

  @Override
  public String getName() {
    return portName;
  }

  @Override
  public String getType() {
    return "serial";
  }


  public static DataPort openPort(int number, String tty, int baud) {
    SerialPort port = SerialPort.getCommPort(tty);
    port.setBaudRate(baud);
    return new SerialDataPort(number, tty, port);
  }
}
