package net.tarpn.config.impl;

import net.tarpn.config.AppConfig;
import org.apache.commons.configuration2.Configuration;

public class AppConfigImpl extends NodeConfigImpl implements AppConfig {

    private final String appName;

    AppConfigImpl(String appName, Configuration delegate) {
        super(delegate);
        this.appName = appName;
    }

    @Override
    public String getAppName() {
        return appName;
    }

    @Override
    public String getAppCall() {
        return getString("app.call");
    }

    @Override
    public String getAppAlias() {
        return getString("app.alias");
    }
}
