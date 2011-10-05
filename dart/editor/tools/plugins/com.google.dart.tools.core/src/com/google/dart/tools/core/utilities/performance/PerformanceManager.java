/*
 * Copyright (c) 2011, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.core.utilities.performance;

import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * Instances of the class <code>PerformanceManager</code> provide support for measuring the
 * performance of a piece of code.
 * <p>
 * The expected code pattern for using this class is as follows:
 * 
 * <pre>
 *   PerformanceManager.Timer timer = PerformanceManager.getInstance().start(OPERATION_IDENTIFIER);
 *   try {
 *     // code to be measured
 *   } finally {
 *     timer.end();
 *   }
 * </pre>
 */
public class PerformanceManager {
  /**
   * Instances of the class <code>Timer</code> implement an object that maintains information about
   * a single measurement that is in progress.
   */
  public class Timer {
    /**
     * The name of the operation being measured by this timer.
     */
    private String name;

    /**
     * The time at the beginning of the operation.
     */
    private long startTime;

    /**
     * Initialize a newly created timer to time an operation with the given name.
     * 
     * @param name the name of the operation being measured
     */
    public Timer(String name) {
      this.name = name;
      startTime = System.currentTimeMillis();
    }

    /**
     * Record the end of the operation being measured by this timer.
     */
    public void end() {
      recordMetric(System.currentTimeMillis() - startTime, name, 1);
    }

    /**
     * Record the end of the operation being measured by this timer.
     * 
     * @param work the amount of work completed by the operation
     */
    public void end(int work) {
      recordMetric(System.currentTimeMillis() - startTime, name, work);
    }
  }

  /**
   * Instances of the class <code>Metric</code> implement an object that maintains information about
   * some number of measurements of a single piece of code.
   */
  private static class Metric {
    /**
     * The name of the metric.
     */
    private String name;

    /**
     * The number of times the metric has been measured.
     */
    private int count;

    /**
     * The total amount of work performed by the operation.
     */
    private int totalWork;

    /**
     * The total amount of time taken by the operation.
     */
    private long totalTime;

    /**
     * The minimum amount of time (per unit of work) taken by a single measurement.
     */
    private long minTime;

    /**
     * The maximum amount of time (per unit of work) taken by a single measurement.
     */
    private long maxTime;

    /**
     * Initialize a newly created metric with the given name to have a single measurement of the
     * given duration as a result of performing the given amount of work.
     * 
     * @param name the name of the metric
     * @param work the amount of work done for the first measurement
     * @param time the duration of the first measurement
     */
    public Metric(String name, int work, long time) {
      this.name = name;
      count = 1;
      totalWork = work;
      totalTime = time;
      minTime = time / work;
      maxTime = time / work;
    }

    /**
     * Add a measurement of the give duration.
     * 
     * @param work the amount of work done for the measurement being added
     * @param elapsedTime the duration of the measurement being added
     */
    public void addTime(int work, long elapsedTime) {
      count++;
      totalWork += work;
      totalTime += elapsedTime;
      long timePerUnit = elapsedTime / work;
      minTime = Math.min(minTime, timePerUnit);
      maxTime = Math.max(maxTime, timePerUnit);
    }

    /**
     * Print this metric to the given print writer.
     * 
     * @param writer the writer to which this metric is to be printed
     */
    public void printOn(PrintWriter writer) {
      writer.print(name);
      writer.print(" = ");
      writer.print(minTime);
      writer.print(", ");
      writer.print(totalTime / totalWork);
      writer.print(", ");
      writer.print(maxTime);
      writer.print(" ms/unit of work [");
      writer.print(count);
      writer.print("]");
    }
  }

  /**
   * A table mapping metric names to metrics.
   */
  private Map<String, Metric> metricMap = new HashMap<String, Metric>();

  /**
   * The unique instance of this class.
   */
  private static final PerformanceManager UNIQUE_INSTANCE = new PerformanceManager();

  /**
   * Return the unique instance of this class.
   * 
   * @return the unique instance of this class
   */
  public static PerformanceManager getInstance() {
    return UNIQUE_INSTANCE;
  }

  /**
   * Prevent the creation of new instances of this class.
   */
  private PerformanceManager() {
    super();
  }

  /**
   * Print all of the metrics to the given print writer.
   * 
   * @param writer the writer to which all of the metrics are to be printed
   */
  public void printMetrics(PrintWriter writer) {
    synchronized (metricMap) {
      for (Metric metric : metricMap.values()) {
        metric.printOn(writer);
        writer.println();
      }
    }
    writer.flush();
  }

  /**
   * Start the measurement of the operation with the given name.
   * 
   * @return the timer used to perform the measurement
   */
  public Timer start(String name) {
    return new Timer(name);
  }

  /**
   * Record the end of the most recently started measurement.
   * 
   * @param name the name of the measurement that just completed
   */
  private void recordMetric(long elapsedTime, String name, int work) {
    synchronized (metricMap) {
      Metric metric = metricMap.get(name);
      if (metric == null) {
        metric = new Metric(name, work, elapsedTime);
        metricMap.put(name, metric);
      } else {
        metric.addTime(work, elapsedTime);
      }
    }
  }
}
