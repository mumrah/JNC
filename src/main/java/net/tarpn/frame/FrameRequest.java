package net.tarpn.frame;

/**
 * Represents an incoming {@link Frame} from a {@link net.tarpn.io.DataPort}.
 */
public interface FrameRequest {

  /**
   * The incoming Frame
   * @return
   */
  Frame getFrame();

  /**
   * Reply to the current incoming Frame with a response immediately and cancel further processing
   * @param response
   */
  void replyWith(Frame response);

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
