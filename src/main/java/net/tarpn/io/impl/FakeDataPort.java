package net.tarpn.io.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import net.tarpn.io.DataPort;

public class FakeDataPort implements DataPort {

  private String message;

  public FakeDataPort(String message) {
    this.message = message;
  }

  @Override
  public void open() throws IOException {

  }

  @Override
  public void close() throws IOException {

  }

  @Override
  public InputStream getInputStream() {
    return new ByteArrayInputStream(message.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  public OutputStream getOutputStream() {
    return new ByteArrayOutputStream();
  }

  @Override
  public int getPortNumber() {
    return 99;
  }

  @Override
  public String getName() {
    return "Fake Port";
  }

  @Override
  public String getType() {
    return "fake";
  }
}
