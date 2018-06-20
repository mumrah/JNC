package net.tarpn.packet;


/**
 * Represents a {@link Packet} coming in from a source address
 */
public interface PacketRequest {
  /**
   * The port this packet was heard on
   *
   */
  int getPort();

  /**
   * The incoming Packet
   * @return
   */
  Packet getPacket();

  /**
   * Reply to the current incoming Packet with a response immediately and cancel further processing
   * @param response
   */
  void replyWith(Packet response);

  /**
   * Abort the current request with no response
   */
  void abort();

  /**
   * Indicates if we should continnue processing this request
   * @return
   */
  boolean shouldContinue();
}
