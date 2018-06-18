package net.tarpn.io.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.tarpn.io.DataPort;
import net.tarpn.DataPortRouter;

public class SimpleDataPortRouter implements DataPortRouter {

  private final Map<String, DataPort> portMap = new HashMap<>();

  private SimpleDataPortRouter(Map<String, DataPort> portMap) {
    this.portMap.putAll(portMap);
  }

  @Override
  public DataPort findPort(String query) {
    return portMap.get(query);
  }

  public static DataPortRouter create(DataPort... ports) {
    Map<String, DataPort> portMap = Stream.of(ports)
        .collect(Collectors.toMap(DataPort::getName, Function.identity()));
    return new SimpleDataPortRouter(portMap);
  }
}
