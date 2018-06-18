package net.tarpn;

import net.tarpn.io.DataPort;

public interface DataPortRouter {
  DataPort findPort(String query);
}
