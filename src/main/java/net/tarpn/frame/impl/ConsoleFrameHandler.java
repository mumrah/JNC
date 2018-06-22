package net.tarpn.frame.impl;

import java.io.IOException;
import net.tarpn.frame.FrameHandler;
import net.tarpn.frame.FrameRequest;
import org.apache.commons.io.HexDump;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleFrameHandler implements FrameHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ConsoleFrameHandler.class);

  @Override
  public void onFrame(FrameRequest frameRequest) {
    LOG.info("Got Frame: " + frameRequest.getFrame());
    try {
      HexDump.dump(frameRequest.getFrame().getData(), 0, System.err, 0);
    } catch (IOException e) {
      LOG.warn("Could not dump frame", e);
    }
  }
}
