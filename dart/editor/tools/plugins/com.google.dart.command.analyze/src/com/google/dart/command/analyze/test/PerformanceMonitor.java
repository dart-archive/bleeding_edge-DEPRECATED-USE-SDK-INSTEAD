package com.google.dart.command.analyze.test;

import com.google.dart.engine.utilities.general.TimeCounter;

public class PerformanceMonitor extends TimeCounter {

  private final String name;

  public PerformanceMonitor(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void recordElapsedMillis(long milliseconds) {
    recordElapsedNanos(milliseconds * NANOS_PER_MILLI);
  }
}
