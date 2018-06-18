package net.tarpn.message.impl;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import net.tarpn.message.Message;
import net.tarpn.message.MessageReader;
import net.tarpn.packet.Packet;

/**
 * Contrived DataGram format of: SOURCE>DEST>MESSAGE
 */
public class SimpleMessageReader implements MessageReader {

  private volatile boolean inMessage = false;
  private volatile int messageLen = 0;
  private volatile Message tmpMessage = null;

  @Override
  public void accept(Packet packet, Consumer<Message> messageHandler) {
    String frameAsString = new String(packet.getMessage(), StandardCharsets.US_ASCII);

    if(inMessage) {
      // decoding an existing message
      String concatMessage = new String(tmpMessage.getMessage(), StandardCharsets.US_ASCII) + frameAsString;
      Message partialMessage = new Message(tmpMessage.getSource(), tmpMessage.getDestination(),
          concatMessage.getBytes(StandardCharsets.US_ASCII));

      if(concatMessage.length() == messageLen) {
        // got the rest
        messageHandler.accept(partialMessage);
        reset();
      } else if(concatMessage.length() < messageLen) {
        tmpMessage = partialMessage;
      } else {
        // overflow!
        reset();
      }
    } else {
      // SRC>DEST>LEN>MESSAGE (strlen)
      String[] tokens = frameAsString.split(">", 4);
      int len = Integer.parseInt(tokens[2]);

      if(tokens[3].length() == len) {
        // got the whole packet in one frame
        messageHandler.accept(new Message(tokens[1], tokens[2], tokens[3].getBytes(StandardCharsets.US_ASCII)));
        reset();
      } else if(tokens[3].length() < len) {
        // got a partial packet
        tmpMessage = new Message(tokens[1], tokens[2], tokens[3].getBytes(StandardCharsets.US_ASCII));
        messageLen = len;
        inMessage = true;
      } else {
        // overflow or overflow, drop message
        reset();
      }
    }
  }

  private void reset() {
    inMessage = false;
    messageLen = 0;
    tmpMessage = null;
  }

}
