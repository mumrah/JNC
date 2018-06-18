package net.tarpn.message;

import java.util.Arrays;

/**
 * A Complete Datagram Message (Layer 4)
 *
 * A fully reconstituted message from a distant node which was sent to this node.
 *
 * A single message could have been composed of multiple packets. By the time we get here, the
 * users message has been reconstructed in the correct order and a checksum has been applied.
 */
public class Message {
  private final String source;
  private final String destination;
  private final byte[] message;

  public Message(String source, String destination, byte[] message) {
    this.source = source;
    this.destination = destination;
    this.message = message;
  }

  public String getSource() {
    return source;
  }

  public String getDestination() {
    return destination;
  }

  public byte[] getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return "Message{" +
        "source='" + source + '\'' +
        ", destination='" + destination + '\'' +
        ", message=" + Arrays.toString(message) +
        '}';
  }
}
