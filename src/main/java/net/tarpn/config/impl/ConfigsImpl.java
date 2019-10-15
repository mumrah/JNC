package net.tarpn.config.impl;

import static org.apache.commons.configuration2.tree.DefaultExpressionEngineSymbols.DEFAULT_ATTRIBUTE_END;
import static org.apache.commons.configuration2.tree.DefaultExpressionEngineSymbols.DEFAULT_ATTRIBUTE_START;
import static org.apache.commons.configuration2.tree.DefaultExpressionEngineSymbols.DEFAULT_INDEX_END;
import static org.apache.commons.configuration2.tree.DefaultExpressionEngineSymbols.DEFAULT_INDEX_START;
import static org.apache.commons.configuration2.tree.DefaultExpressionEngineSymbols.DEFAULT_PROPERTY_DELIMITER;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import net.tarpn.config.*;
import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.SubnodeConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.DefaultExpressionEngine;
import org.apache.commons.configuration2.tree.DefaultExpressionEngineSymbols;
import org.apache.commons.configuration2.tree.ExpressionEngine;
import org.apache.commons.configuration2.tree.OverrideCombiner;

public class ConfigsImpl extends BaseConfig implements Configs {



  private final NodeConfig nodeConfig;

  private final NetRomConfig netromConfig;

  private final Map<Integer, PortConfig> portConfigs;

  private final Map<String, AppConfig> appConfigs;

  public ConfigsImpl(Configuration delegate,
              NodeConfig nodeConfig,
              NetRomConfig netromConfig,
              Map<Integer, PortConfig> portConfigs,
              Map<String, AppConfig> appConfigs) {
    super(delegate);
    this.nodeConfig = nodeConfig;
    this.netromConfig = netromConfig;
    this.portConfigs = portConfigs;
    this.appConfigs = appConfigs;
  }

  @Override
  public NodeConfig getNodeConfig() {
    return nodeConfig;
  }

  @Override
  public NetRomConfig getNetRomConfig() {
    return netromConfig;
  }

  @Override
  public Map<Integer, PortConfig> getPortConfigs() {
    return portConfigs;
  }

  @Override
  public Map<String, AppConfig> getAppConfigs() {
    return appConfigs;
  }
}
