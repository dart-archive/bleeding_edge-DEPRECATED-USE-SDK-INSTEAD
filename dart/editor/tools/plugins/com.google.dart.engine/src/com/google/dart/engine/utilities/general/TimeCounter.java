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

/**
 * Helper for measuring how much time is spent doing some operation.
 */
public class TimeCounter {
  /**
   * The handle object that should be used to stop and update counter.
   */
  public class TimeCounterHandle {
    final long startTime = System.currentTimeMillis();

    /**
     * Stops counting time and updates counter.
     */
    public void stop() {
      synchronized (TimeCounter.this) {
        result += (System.currentTimeMillis() - startTime);
      }
    }
  }

  private long result;

  /**
   * @return the number of milliseconds spent between {@link #start()} and {@link #stop()}.
   */
  public long getResult() {
    return result;
  }

  /**
   * Starts counting time.
   * 
   * @return the {@link TimeCounterHandle} that should be used to stop counting.
   */
  public TimeCounterHandle start() {
    return new TimeCounterHandle();
  }
}
