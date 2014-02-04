package com.google.dart.command.analyze.test;

public class PerformanceMonitor {
  /**
   * The handle object that should be used to stop and update counter.
   */
  public class PerformanceMonitorHandle {
    private final long startTime = System.nanoTime();

    /**
     * Stops counting time and updates counter.
     */
    public void stop() {
      synchronized (PerformanceMonitor.this) {
        recordElapsedNanos(System.nanoTime() - startTime);
      }
    }
  }

  private static final int NANOS_PER_MILLI = 1000 * 1000;

  private final String name;
  private long totalElapsedTime = 0L;
  private long maxTime = 0L;
  private long minTime = Long.MAX_VALUE;
  private int totalCount = 0;

  public PerformanceMonitor(String name) {
    this.name = name;
  }

  public long getAverage() {
    if (totalCount == 0) {
      return 0;
    }
    return totalElapsedTime / (NANOS_PER_MILLI * totalCount);
  }

  public int getCount() {
    return totalCount;
  }

  public long getMax() {
    return maxTime / NANOS_PER_MILLI;
  }

  public long getMin() {
    if (totalCount == 0) {
      return 0;
    }
    return minTime / NANOS_PER_MILLI;
  }

  public String getName() {
    return name;
  }

  public void recordElapsedMillis(long milliseconds) {
    recordElapsedNanos(milliseconds * NANOS_PER_MILLI);
  }

  public void recordElapsedNanos(long delta) {
    totalElapsedTime += delta;
    totalCount++;
    minTime = Math.min(minTime, delta);
    maxTime = Math.max(maxTime, delta);
  }

  public PerformanceMonitorHandle start() {
    return new PerformanceMonitorHandle();
  }
}
