package net.tarpn.frame;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A Data Frame
 *
 * This represents the physical packet heard on the data link. It is a data stream heard by one of
 * our links. No packet has been decoded at this point, this object only contains the data as it
 * was heard by the link and sent in a {@link net.tarpn.io.DataPort}
 *
 * TODO:
 * <li>frame check sequence</li>
 * <li>frame index number</li>
 */
public class Frame {
  private final int port;
  private final byte[] data;

  public Frame(int port, byte[] data) {
    this.port = port;
    this.data = data;
  }

  public int getPort() {
    return port;
  }

  public byte[] getData() {
    return data;
  }

  @Override
  public String toString() {
    return "Frame{" +
        "port='" + port + '\'' +
        ", data=" + Arrays.toString(data) +
        ", ascii=" + new String(data, StandardCharsets.US_ASCII) +
        '}';
  }
}
