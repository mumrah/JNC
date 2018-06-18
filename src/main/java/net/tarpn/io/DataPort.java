package net.tarpn.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface DataPort {
  void open() throws IOException;
  void close() throws IOException;
  InputStream getInputStream();
  OutputStream getOutputStream();
  int getPortNumber();
  String getName();
  String getType();
}
