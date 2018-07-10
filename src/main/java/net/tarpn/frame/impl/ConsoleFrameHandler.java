package net.tarpn.frame.impl;

import net.tarpn.frame.FrameHandler;
import net.tarpn.frame.FrameRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleFrameHandler implements FrameHandler {

  private static final Logger LOG = LoggerFactory.getLogger(ConsoleFrameHandler.class);

  @Override
  public void onFrame(FrameRequest frameRequest) {
    LOG.info("Got Frame: " + frameRequest.getFrame());
  }
}
