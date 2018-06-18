package net.tarpn;

import java.util.List;
import net.tarpn.io.DataPort;

public interface PortProvider {
  List<DataPort> getPorts();
}
