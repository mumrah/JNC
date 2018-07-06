package net.tarpn.network;

import net.tarpn.io.DataPort;

public interface DataPortRouter {
  DataPort findPort(String query);
}
