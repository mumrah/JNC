package net.tarpn.config;

public interface AppConfig extends NodeConfig, Configuration {
    String getAppName();
    String getAppCall();
    String getAppAlias();
}
