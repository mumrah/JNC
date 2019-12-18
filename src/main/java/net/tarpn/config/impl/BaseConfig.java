package net.tarpn.config.impl;

import net.tarpn.config.Configuration;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class BaseConfig implements Configuration {

  private final org.apache.commons.configuration2.Configuration delegate;

  BaseConfig(org.apache.commons.configuration2.Configuration delegate) {
    this.delegate = delegate;
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

  @Override
  public List<String> getStrings(String key) {
    return Arrays.asList(delegate.getStringArray(key));
  }


}