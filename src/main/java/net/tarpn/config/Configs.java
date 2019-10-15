package net.tarpn.config;

import net.tarpn.config.impl.*;
import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.INIConfiguration;
import org.apache.commons.configuration2.SubnodeConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.DefaultExpressionEngine;
import org.apache.commons.configuration2.tree.DefaultExpressionEngineSymbols;
import org.apache.commons.configuration2.tree.ExpressionEngine;
import org.apache.commons.configuration2.tree.OverrideCombiner;

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

import static org.apache.commons.configuration2.tree.DefaultExpressionEngineSymbols.*;

public interface Configs extends Configuration {
    NodeConfig getNodeConfig();

    NetRomConfig getNetRomConfig();

    Map<Integer, PortConfig> getPortConfigs();

    Map<String, AppConfig> getAppConfigs();

    ExpressionEngine EXPRESSION_ENGINE =
            new DefaultExpressionEngine(new DefaultExpressionEngineSymbols.Builder()
                    .setPropertyDelimiter(DEFAULT_PROPERTY_DELIMITER)
                    .setEscapedDelimiter(DEFAULT_PROPERTY_DELIMITER) // Don't escape periods
                    .setIndexStart(DEFAULT_INDEX_START)
                    .setIndexEnd(DEFAULT_INDEX_END)
                    .setAttributeStart(DEFAULT_ATTRIBUTE_START)
                    .setAttributeEnd(DEFAULT_ATTRIBUTE_END).create());

    static Configs read(String fileName) throws IOException {
        InputStream is = Files.newInputStream(Paths.get(fileName), StandardOpenOption.READ);
        return read(is);
    }

    static Configs read(InputStream is) {
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
                    return new PortConfigImpl(portNum, portConfig);
                })
                .collect(Collectors.toMap(PortConfig::getPortNumber, Function.identity()));

        // NET/ROM config
        CombinedConfiguration netromCombinedConfig = new CombinedConfiguration(new OverrideCombiner());
        netromCombinedConfig.setExpressionEngine(EXPRESSION_ENGINE);
        netromCombinedConfig.addConfiguration(configuration.getSection("netrom"), "netrom");
        netromCombinedConfig.addConfiguration(nodeConfig, "node");

        NetRomConfig netromConfig = new NetRomConfigImpl(netromCombinedConfig);

        // Application configs
        Map<String, AppConfig> appConfigs = configuration.getSections()
                .stream()
                .filter(Objects::nonNull)
                .map(String::toLowerCase)
                .filter(section -> section.startsWith("app:"))
                .map(section -> {
                    String appName = section.substring("port:".length());
                    CombinedConfiguration appConfig = new CombinedConfiguration(new OverrideCombiner());
                    appConfig.setExpressionEngine(EXPRESSION_ENGINE);
                    appConfig.addConfiguration(configuration.getSection(section), section);
                    appConfig.addConfiguration(nodeConfig, "node");
                    return new AppConfigImpl(appName, appConfig);
                })
                .collect(Collectors.toMap(AppConfig::getAppName, Function.identity()));

        return new ConfigsImpl(configuration, new NodeConfigImpl(nodeConfig), netromConfig, portConfigs, appConfigs);
    }
}
