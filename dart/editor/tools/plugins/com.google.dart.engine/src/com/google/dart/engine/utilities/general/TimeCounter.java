/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.engine.utilities.general;

import com.google.common.collect.Lists;
import com.google.dart.engine.AnalysisEngine;

import java.util.LinkedList;

/**
 * Helper for measuring how much time is spent doing some operation. Each call to
 * {@link #recordElapsedNanos(long)} or each pair of calls to {@link #start()} and
 * {@link TimeCounterHandle#stop()} adds the specified time interval to the total recorded time.
 */
public class TimeCounter {
  /**
   * The handle object that should be used to stop and update counter.
   */
  public class TimeCounterHandle {
    final long startTime = System.nanoTime();

    /**
     * Stops counting time and calls {@link TimeCounter#recordElapsedNanos(long)} to add the elapse
     * time to the counter.
     * 
     * @return The number of elapsed nanoseconds
     */
    public long stop() {
      long elapsed = System.nanoTime() - startTime;
      recordElapsedNanos(elapsed);
      return elapsed;
    }
  }

  private static final ThreadLocal<LinkedList<TimeCounter>> stacks = new ThreadLocal<LinkedList<TimeCounter>>();
  public static final int NANOS_PER_MILLI = 1000 * 1000;

  /**
   * Returns the stack of {@link TimeCounter} started on the current {@link Thread} and not stopped
   * yet.
   */
  private static LinkedList<TimeCounter> getCountersStack() {
    LinkedList<TimeCounter> stack = stacks.get();
    if (stack == null) {
      stack = Lists.newLinkedList();
      stacks.set(stack);
    }
    return stack;
  }

  private long totalTime = 0L;
  private long correctionTime = 0L;
  private long maxInterval = 0L;
  private long minInterval = Long.MAX_VALUE;

  private int intervalCount = 0;

  /**
   * @return the average time interval in milliseconds as recorded by
   *         {@link #recordElapsedNanos(long)} or {@link #start()} and
   *         {@link TimeCounterHandle#stop()}
   */
  public long getAverage() {
    if (intervalCount == 0) {
      return 0;
    }
    return totalTime / (NANOS_PER_MILLI * intervalCount);
  }

  /**
   * @return the number of times that {@link #recordElapsedNanos(long)} and {@link #start()} and
   *         {@link TimeCounterHandle#stop()} were called
   */
  public int getCount() {
    return intervalCount;
  }

  /**
   * @return the maximum time interval in milliseconds as recorded by
   *         {@link #recordElapsedNanos(long)} or {@link #start()} and
   *         {@link TimeCounterHandle#stop()}
   */
  public long getMax() {
    return maxInterval / NANOS_PER_MILLI;
  }

  /**
   * @return the minimum time interval in milliseconds as recorded by
   *         {@link #recordElapsedNanos(long)} or {@link #start()} and
   *         {@link TimeCounterHandle#stop()}
   */
  public long getMin() {
    if (intervalCount == 0) {
      return 0;
    }
    return minInterval / NANOS_PER_MILLI;
  }

  /**
   * @return the number of milliseconds spent between {@link #start()} and {@link #stop()}.
   */
  public long getResult() {
    return totalTime / NANOS_PER_MILLI;
  }

  /**
   * Adds the specified time interval to the total time and updates the minimum, maximum, and
   * average time intervals.
   * 
   * @param delta the number of nanoseconds
   */
  public synchronized void recordElapsedNanos(long delta) {
    // apply correction to the other counters on the thread stack
    LinkedList<TimeCounter> stack = getCountersStack();
    TimeCounter removed = stack.removeFirst();
    if (removed != this) {
      AnalysisEngine.getInstance().getLogger().logInformation(
          "Unexpected TimeCounter instance stack in " + Thread.currentThread(),
          new IllegalStateException());
    }
    for (TimeCounter timeCounter : stack) {
      timeCounter.correctionTime += delta;
    }
    // update statistics
    totalTime += delta - correctionTime;
    correctionTime = 0;
    intervalCount++;
    minInterval = Math.min(minInterval, delta);
    maxInterval = Math.max(maxInterval, delta);
  }

  /**
   * Starts counting time.
   * 
   * @return the {@link TimeCounterHandle} that should be used to stop counting.
   */
  public synchronized TimeCounterHandle start() {
    getCountersStack().addFirst(this);
    return new TimeCounterHandle();
  }
}
