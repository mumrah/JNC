package net.tarpn.frame.impl;

import net.tarpn.frame.Frame;

public class KISSFrame extends Frame {

  private final KISS.Command kissCommand;

  public KISSFrame(int port, KISS.Command kissCommand, byte[] data) {
    super(port, data);
    this.kissCommand = kissCommand;
  }

  public KISS.Command getKissCommand() {
    return kissCommand;
  }
}
