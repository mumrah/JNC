package net.tarpn.frame.impl;

import java.util.function.Consumer;
import net.tarpn.frame.Frame;
import net.tarpn.frame.FrameRequest;

public class DefaultFrameRequest implements FrameRequest {

  private volatile boolean done = false;
  private final Frame incomingFrame;
  private final Consumer<Frame> responseFrameConsumer;

  public DefaultFrameRequest(Frame incomingFrame,
      Consumer<Frame> responseFrameConsumer) {
    this.incomingFrame = incomingFrame;
    this.responseFrameConsumer = responseFrameConsumer;
  }

  @Override
  public Frame getFrame() {
    return incomingFrame;
  }

  @Override
  public void replyWith(Frame response) {
    if(!done) {
      responseFrameConsumer.accept(response);
      done = true;
    }
  }

  @Override
  public void abort() {
    done = true;
  }

  @Override
  public boolean shouldContinue() {
    return done == false;
  }
}
