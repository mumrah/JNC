package net.tarpn.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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
