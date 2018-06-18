package net.tarpn.frame;

import java.util.function.Consumer;

/**
 * Byte oriented frame reader. Bytes are passed in from the {@link net.tarpn.io.DataPort}
 * one at a time to this reader which sends decoded {@link Frame} objects to the given consumer.
 */
public interface FrameReader {

  /**
   * Accept a byte of data from the {@link net.tarpn.io.DataPort}, possibly emit a {@link Frame}.
   * @param b
   * @param frameHandler
   */
  void accept(int b, Consumer<Frame> frameHandler);
}
