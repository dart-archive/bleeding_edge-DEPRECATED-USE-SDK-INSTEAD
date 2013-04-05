package com.google.dart.tools.ui.feedback;

import com.google.dart.engine.utilities.instrumentation.InstrumentationBuilder;
import com.google.dart.engine.utilities.instrumentation.InstrumentationLevel;

import java.util.HashMap;
import java.util.TreeSet;

final class MockInstrumentationBuilder implements InstrumentationBuilder {
  private final HashMap<String, String> data = new HashMap<String, String>();
  private final HashMap<String, String> metric = new HashMap<String, String>();

  @Override
  public InstrumentationBuilder data(String name, boolean value) {
    data.put(name, Boolean.toString(value));
    return this;
  }

  @Override
  public InstrumentationBuilder data(String name, long value) {
    data.put(name, Long.toString(value));
    return this;
  }

  @Override
  public InstrumentationBuilder data(String name, String value) {
    data.put(name, value);
    return this;
  }

  @Override
  public InstrumentationBuilder data(String name, String[] value) {
    for (int i = 0; i < value.length; i++) {
      data.put(name + "-" + String.valueOf(i), value[i]);
    }
    return this;
  }

  public void echoToStdOut(String name) {
    System.out.println("Instrumentation for " + name);
    System.out.println("  Metrics:");
    for (String key : new TreeSet<String>(metric.keySet())) {
      System.out.println("    " + key + " = " + metric.get(key));
    }
    System.out.println("  Data:");
    for (String key : new TreeSet<String>(data.keySet())) {
      System.out.println("    " + key + " = " + data.get(key));
    }
  }

  /**
   * Answer the "data" value associated with the specified key
   * 
   * @param key the key (not {@code null})
   * @return the value or {@code null} if none
   */
  public String getData(String key) {
    return data.get(key);
  }

  @Override
  public InstrumentationLevel getInstrumentationLevel() {
    return InstrumentationLevel.EVERYTHING;
  }

  /**
   * Answer the "metrics" value associated with the specified key
   * 
   * @param key the key (not {@code null})
   * @return the value or {@code null} if none
   */
  public String getMetric(String key) {
    return metric.get(key);
  }

  @Override
  public void log() {
    // ignored
  }

  @Override
  public InstrumentationBuilder metric(String name, boolean value) {
    metric.put(name, Boolean.toString(value));
    return this;
  }

  @Override
  public InstrumentationBuilder metric(String name, long value) {
    metric.put(name, Long.toString(value));
    return this;
  }

  @Override
  public InstrumentationBuilder metric(String name, String value) {
    metric.put(name, value);
    return this;
  }

  @Override
  public InstrumentationBuilder metric(String name, String[] value) {
    for (int i = 0; i < value.length; i++) {
      metric.put(name + "-" + String.valueOf(i), value[i]);
    }
    return this;
  }
}
