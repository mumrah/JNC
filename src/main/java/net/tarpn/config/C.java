package net.tarpn.config;

import java.util.Set;

public interface C {
  String getString(String key);
  String getString(String key, String def);
  int getInt(String key);
  int getInt(String key, int def);
  boolean getBoolean(String key);
  boolean getBoolean(String key, boolean def);
  Set<String> getKeys();
}
