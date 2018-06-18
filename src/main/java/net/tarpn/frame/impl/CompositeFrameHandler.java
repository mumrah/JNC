package net.tarpn.frame.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import net.tarpn.frame.FrameHandler;
import net.tarpn.frame.FrameRequest;

public class CompositeFrameHandler implements FrameHandler {

  private final List<FrameHandler> handlerSet = new ArrayList<FrameHandler>();

  private CompositeFrameHandler(Collection<FrameHandler> handlers) {
    handlerSet.addAll(handlers);
  }

  public static FrameHandler wrap(FrameHandler... handlers) {
    return new CompositeFrameHandler(Arrays.asList(handlers));
  }

  @Override
  public void onFrame(FrameRequest frameRequest) {
    handlerSet.forEach(frameHandler -> {
      if(frameRequest.shouldContinue()) {
        frameHandler.onFrame(frameRequest);
      }
    });
  }
}
