package net.tarpn.config;

public interface NetRomConfig extends Configuration, NodeConfig {
    int getTimeToLive();

    int getRetryCount();

    int getMinObs();

    int getInitialObs();

    int getNodesInterval();

    byte getTTL();

    byte getWindowSize();
}
