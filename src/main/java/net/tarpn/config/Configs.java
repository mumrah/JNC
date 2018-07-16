package net.tarpn.config;

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
import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.SubnodeConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.DefaultExpressionEngine;
import org.apache.commons.configuration2.tree.DefaultExpressionEngineSymbols;
import org.apache.commons.configuration2.tree.ExpressionEngine;
import org.apache.commons.configuration2.tree.OverrideCombiner;

public class Configs extends BaseConfig {

  private static final ExpressionEngine EXPRESSION_ENGINE =
      new DefaultExpressionEngine(createDefaultSymbols());

  private static DefaultExpressionEngineSymbols createDefaultSymbols() {
    return new DefaultExpressionEngineSymbols.Builder()
        .setPropertyDelimiter(DEFAULT_PROPERTY_DELIMITER)
        .setEscapedDelimiter(DEFAULT_PROPERTY_DELIMITER) // Don't escape periods
        .setIndexStart(DEFAULT_INDEX_START)
        .setIndexEnd(DEFAULT_INDEX_END)
        .setAttributeStart(DEFAULT_ATTRIBUTE_START)
        .setAttributeEnd(DEFAULT_ATTRIBUTE_END).create();
  }

  private final NodeConfig nodeConfig;

  private final NetRomConfig netromConfig;

  private final Map<Integer, PortConfig> portConfigs;

  Configs(
      Configuration delegate,
      NodeConfig nodeConfig,
      NetRomConfig netromConfig,
      Map<Integer, PortConfig> portConfigs) {
    super(delegate);
    this.nodeConfig = nodeConfig;
    this.netromConfig = netromConfig;
    this.portConfigs = portConfigs;
  }

  public NodeConfig getNodeConfig() {
    return nodeConfig;
  }

  public NetRomConfig getNetRomConfig() {
    return netromConfig;
  }

  public Map<Integer, PortConfig> getPortConfigs() {
    return portConfigs;
  }

  public static Configs read(String fileName) throws IOException {
    InputStream is = Files.newInputStream(Paths.get(fileName), StandardOpenOption.READ);
    return read(is);
  }

  public static Configs read(InputStream is) {
    INIConfiguration configuration = new INIConfiguration();
    configuration.setExpressionEngine(EXPRESSION_ENGINE);

    try {
      configuration.read(new InputStreamReader(is));
    } catch (ConfigurationException | IOException e) {
      throw new RuntimeException("Could not load configuration", e);
    }

    SubnodeConfiguration nodeConfig = configuration.getSection("node");

    SubnodeConfiguration portDefaults = configuration.getSection("port:defaults");

    portDefaults.getKeys();

    // Read off Ports
    Map<Integer, PortConfig> portConfigs = configuration.getSections()
        .stream()
        .filter(Objects::nonNull)
        .map(String::toLowerCase)
        .filter(section -> section.startsWith("port:") && !section.equalsIgnoreCase("port:defaults"))
        .map(section -> {
          int portNum = Integer.parseInt(section.substring("port:".length()));
          CombinedConfiguration portConfig = new CombinedConfiguration(new OverrideCombiner());
          portConfig.setExpressionEngine(EXPRESSION_ENGINE);
          portConfig.addConfiguration(configuration.getSection(section), section);
          portConfig.addConfiguration(portDefaults, "defaults");
          portConfig.addConfiguration(nodeConfig, "node");
          return new PortConfig(portNum, portConfig);
        })
        .collect(Collectors.toMap(PortConfig::getPortNumber, Function.identity()));

    // NET/ROM config
    CombinedConfiguration netromCombinedConfig = new CombinedConfiguration(new OverrideCombiner());
    netromCombinedConfig.setExpressionEngine(EXPRESSION_ENGINE);
    netromCombinedConfig.addConfiguration(configuration.getSection("netrom"), "netrom");
    netromCombinedConfig.addConfiguration(nodeConfig, "node");

    NetRomConfig netromConfig = new NetRomConfig(netromCombinedConfig);
    return new Configs(configuration, new NodeConfig(nodeConfig), netromConfig, portConfigs);
  }
}
