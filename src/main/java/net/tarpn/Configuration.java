package net.tarpn;

import net.tarpn.packet.impl.ax25.AX25Call;

public class Configuration {

  public static AX25Call getOwnNodeCallsign() {
    return AX25Call.create("K4DBZ", 2);
  }

  public static String getOwnNodeAlias() {
    return "DAVID2";
  }


  public static int getMaxFrameLength() {
    return 1024;
  }
}
