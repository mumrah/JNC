package net.tarpn.packet.impl;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import net.tarpn.frame.Frame;
import net.tarpn.packet.Packet;
import net.tarpn.packet.PacketReader;

public class SimplePacketReader implements PacketReader {

  @Override
  public void accept(Frame frame, Consumer<Packet> packetHandler) {
    String frameAsString = new String(frame.getData(), StandardCharsets.US_ASCII);
    // SRC;DEST;MESSAGE (strlen)
    String[] tokens = frameAsString.split(";", 3);
    packetHandler.accept(new Packet(frame.getPort(), tokens[0], tokens[1], tokens[2].getBytes(StandardCharsets.US_ASCII)));
  }

  /*
  private volatile boolean inPacket = false;
  private volatile int messageLen = 0;
  private volatile Packet tmpPacket = null;

  @Override
  public void accept(Frame frame, Consumer<Packet> packetHandler) {
    String frameAsString = new String(frame.getData(), StandardCharsets.US_ASCII);

    if(inPacket) {
      // decoding an existing packet
      String partialMessage = new String(tmpPacket.getMessage(), StandardCharsets.US_ASCII) + frameAsString;
      Packet partialPacket = new Packet(tmpPacket.getPort(), tmpPacket.getSource(),
          tmpPacket.getDestination(), partialMessage.getBytes(StandardCharsets.US_ASCII));

      if(partialMessage.length() == messageLen) {
        // got the rest
        packetHandler.accept(partialPacket);
        reset();
      } else if(partialMessage.length() < messageLen) {
        tmpPacket = partialPacket;
      } else {
        // overflow!
        reset();
      }
    } else {
      // SRC;DEST;LEN;MESSAGE (strlen)
      String[] tokens = frameAsString.split(";", 4);
      int len = Integer.parseInt(tokens[2]);
      if(tokens[3].length() == len) {
        // got the whole packet in one frame
        packetHandler.accept(new Packet(frame.getPort(), tokens[1], tokens[2], tokens[3].getBytes(StandardCharsets.US_ASCII)));
        reset();
      } else if(tokens[3].length() < len) {
        // got a partial packet
        tmpPacket = new Packet(frame.getPort(), tokens[1], tokens[2], tokens[3].getBytes(StandardCharsets.US_ASCII));
        messageLen = len;
        inPacket = true;
      } else {
        // overflow
        reset();
      }
    }
  }

  private void reset() {
    inPacket = false;
    messageLen = 0;
    tmpPacket = null;
  }
  */
}
