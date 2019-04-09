package net.tarpn.main;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;
import java.util.stream.Collectors;

import net.tarpn.config.impl.Configs;
import net.tarpn.config.PortConfig;
import net.tarpn.datalink.DataLink;
import net.tarpn.datalink.DataLinkManager;
import net.tarpn.datalink.DataLinkPrimitive;
import net.tarpn.io.DataPort;
import net.tarpn.io.impl.PortFactory;

public class DataLinkMain {
  public static void main(String[] args) throws IOException {
    Configs configs = Configs.read(args[0]);

    Collection<DataLink> ports = configs.getPortConfigs().values()
            .stream()
            .map(config -> DataLink.create(config, PortFactory.createPortFromConfig(config)))
            .collect(Collectors.toList());

    ports.forEach(DataLink::start);
    ports.forEach(DataLink::join);
  }
}
