package net.tarpn.config;

import java.util.Set;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.commons.configuration2.Configuration;

public class BaseConfig implements C {

  private final Configuration delegate;

  BaseConfig(Configuration delegate) {
    this.delegate = delegate;
  }

  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  public boolean containsKey(String key) {
    return delegate.containsKey(key);
  }

  @Override
  public Set<String> getKeys() {
    return StreamSupport.stream(
        Spliterators.spliteratorUnknownSize(delegate.getKeys(), Spliterator.ORDERED), false)
        .collect(Collectors.toSet());
  }

  @Override
  public boolean getBoolean(String key) {
    return delegate.getBoolean(key);
  }

  @Override
  public boolean getBoolean(String key, boolean defaultValue) {
    return delegate.getBoolean(key, defaultValue);
  }

  @Override
  public int getInt(String key) {
    return delegate.getInt(key);
  }

  @Override
  public int getInt(String key, int defaultValue) {
    return delegate.getInt(key, defaultValue);
  }

  @Override
  public String getString(String key) {
    return delegate.getString(key);
  }

  @Override
  public String getString(String key, String defaultValue) {
    return delegate.getString(key, defaultValue);
  }

}