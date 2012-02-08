/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.debug.core.util;

import com.google.dart.tools.debug.core.DartDebugCorePlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple class to measure timings in the debugger.
 */
public class LogTimer {
  public static interface LogListener {
    /**
     * Handle a log event w/ the given action name and duraction (in milliseconds).
     * 
     * @param actionName
     * @param durationMs
     */
    public void timerLog(String actionName, long durationMs);
  }

  private static List<LogListener> listeners = new ArrayList<LogTimer.LogListener>();

  public static void addLogListener(LogListener listener) {
    listeners.add(listener);
  }

  public static void removeLogListener(LogListener listener) {
    listeners.remove(listener);
  }

  private String name;

  private long startTime;

  /**
   * Create a new LogTimer.
   * 
   * @param name
   */
  public LogTimer(String name) {
    this.name = name;
    this.startTime = System.currentTimeMillis();
  }

  /**
   * Stop the timer, and log the duration to the Eclipse .log.
   */
  public void endTimer() {
    long duration = System.currentTimeMillis() - startTime;

    DartDebugCorePlugin.logInfo(name + " time: " + duration + "ms");

    for (LogListener listener : listeners) {
      listener.timerLog(name, duration);
    }
  }

}
