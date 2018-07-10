package net.tarpn.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.function.Supplier;

public interface DataPort {

  /**
   * Open and start receiving data on this port
   * @throws IOException
   */
  void open() throws IOException;

  /**
   * Stop receiving data and close this port
   * @throws IOException
   */
  void close() throws IOException;

  boolean isOpen();

  /**
   * Attempt to re-open a port which experienced a fault. Will attempt to close the port before
   * re-opening it in an effort to clean up any lingering resources.
   * @return
   */
  boolean reopen();

  /**
   * Return an InputStream for the data coming into this port
   * @return
   */
  InputStream getInputStream();

  /**
   * Return an OutputStream for sending data to this port
   * @return
   */
  OutputStream getOutputStream();

  /**
   * Get this port's index number
   * @return
   */
  int getPortNumber();

  /**
   * Get the logical name of this port
   * @return
   */
  String getName();

  /**
   * Return the type of this port
   * @return
   */
  String getType();
}
