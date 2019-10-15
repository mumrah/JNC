package net.tarpn.main;

import net.tarpn.config.Configs;
import net.tarpn.config.PortConfig;
import net.tarpn.config.impl.ConfigsImpl;
import net.tarpn.datalink.DataLink;
import net.tarpn.io.impl.PortFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.stream.Collectors;

public class DataLinkMain {
  public static void main(String[] args) throws IOException {
    Configs configs = Configs.read(args[0]);

    Collection<DataLink> ports = configs.getPortConfigs().values()
            .stream()
            .filter(PortConfig::isEnabled)
            .map(config -> DataLink.create(config, PortFactory.createPortFromConfig(config)))
            .collect(Collectors.toList());

    ports.forEach(DataLink::start);
    ports.forEach(DataLink::join);
  }
}
