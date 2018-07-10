package net.tarpn.io.impl;

import com.fazecast.jSerialComm.SerialPort;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import net.tarpn.io.DataPort;

/**
 * A Serial data port. Typically something like /dev/ttyUSB0
 */
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
  public boolean isOpen() {
    return serialPort.isOpen();
  }

  @Override
  public boolean reopen() {
    serialPort.closePort();
    return serialPort.openPort();
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

  @Override
  public String toString() {
    return "SerialDataPort{" +
        "portNumber=" + portNumber +
        ", portName='" + portName + '\'' +
        '}';
  }
}
