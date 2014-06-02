/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.server.timing;

/**
 * Instances of the class {@code TimingResult} represent the timing information gathered while
 * executing a given timing test.
 */
public class TimingResult {
  /**
   * The amount of time spent executing each test, in nanoseconds.
   */
  private long[] times;

  /**
   * The number of nanoseconds in a millisecond.
   */
  private static long NANOSECONDS_PER_MILLISECOND = 1000000;

  /**
   * Initialize a newly created timing result.
   */
  public TimingResult(long[] times) {
    this.times = times;
  }

  /**
   * Return the average amount of time spent executing a single iteration, in milliseconds.
   */
  public long getAverageTime() {
    return getTotalTime() / times.length;
  }

  /**
   * Return the maximum amount of time spent executing a single iteration, in milliseconds.
   */
  public long getMaxTime() {
    long maxTime = 0L;
    int count = times.length;
    for (int i = 0; i < count; i++) {
      maxTime = Math.max(maxTime, times[i]);
    }
    return maxTime / NANOSECONDS_PER_MILLISECOND;
  }

  /**
   * Return the maximum amount of time spent executing a single iteration, in milliseconds.
   */
  public long getMinTime() {
    long minTime = Long.MAX_VALUE;
    int count = times.length;
    for (int i = 0; i < count; i++) {
      minTime = Math.min(minTime, times[i]);
    }
    return minTime / NANOSECONDS_PER_MILLISECOND;
  }

  /**
   * Return the standard deviation of the times.
   * 
   * @return the standard deviation of the times
   */
  public double getStandardDeviation() {
    return computeStandardDeviation(toMilliseconds(times));
  }

  /**
   * Return the total amount of time spent executing the test, in milliseconds.
   */
  public long getTotalTime() {
    long totalTime = 0L;
    int count = times.length;
    for (int i = 0; i < count; i++) {
      totalTime += times[i];
    }
    return totalTime / NANOSECONDS_PER_MILLISECOND;
  }

  /**
   * Compute the standard deviation of the given set of values.
   * 
   * @param values the values for which a standard deviation is to be computed
   * @return the standard deviation that was computed
   */
  private double computeStandardDeviation(long[] values) {
    int count = values.length;
    double sumOfValues = 0.0;
    for (int i = 0; i < count; i++) {
      sumOfValues += values[i];
    }
    double average = sumOfValues / count;
    double sumOfDiffSquared = 0.0;
    for (int i = 0; i < count; i++) {
      double diff = values[i] - average;
      sumOfDiffSquared += diff * diff;
    }
    // If this were a sample we would divide by (count - 1).
    return Math.sqrt(sumOfDiffSquared / count);
  }

  /**
   * Convert the given times, expressed in nanoseconds, to times expressed in milliseconds.
   * 
   * @param values the times in nanoseconds
   * @return the times in milliseconds
   */
  private long[] toMilliseconds(long[] values) {
    int count = values.length;
    long[] convertedValues = new long[count];
    for (int i = 0; i < count; i++) {
      convertedValues[i] = values[i] / NANOSECONDS_PER_MILLISECOND;
    }
    return convertedValues;
  }
}
