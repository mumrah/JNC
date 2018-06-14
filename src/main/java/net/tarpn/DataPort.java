package net.tarpn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface DataPort {
  void open() throws IOException;
  void close() throws IOException;
  InputStream getInputStream();
  OutputStream getOutputStream();
  String getName();
  String getType();
}
