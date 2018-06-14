package net.tarpn.frame.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import net.tarpn.frame.FrameHandler;

public class CompositeFrameHandler implements FrameHandler {

  private final Set<FrameHandler> handlerSet = new HashSet<FrameHandler>();

  private CompositeFrameHandler(Collection<FrameHandler> handlers) {
    handlerSet.addAll(handlers);
  }

  @Override
  public void onFrame(String portName, byte[] frame) {
    handlerSet.forEach(frameHandler -> frameHandler.onFrame(portName, frame));
  }

  public static FrameHandler wrap(FrameHandler... handlers) {
    return new CompositeFrameHandler(Arrays.asList(handlers));
  }
}
