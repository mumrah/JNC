package net.tarpn.config;

import net.tarpn.packet.impl.ax25.AX25Call;

public class Configuration {
  private final AX25Call nodeCall;

  private final String alias;

  Configuration(AX25Call nodeCall, String alias) {
    this.nodeCall = nodeCall;
    this.alias = alias;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public AX25Call getNodeCall() {
    return nodeCall;
  }

  public String getAlias() {
    return alias;
  }


  public static class Builder {

    private AX25Call nodeCall;
    private String alias;

    public Builder setNodeCall(AX25Call nodeCall) {
      this.nodeCall = nodeCall;
      return this;
    }

    public Builder setAlias(String alias) {
      this.alias = alias;
      return this;
    }

    public Configuration build() {
      return new Configuration(nodeCall, alias);
    }
  }
}
