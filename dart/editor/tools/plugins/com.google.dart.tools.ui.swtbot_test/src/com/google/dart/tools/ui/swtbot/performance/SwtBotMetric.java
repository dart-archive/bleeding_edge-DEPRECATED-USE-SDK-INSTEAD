/*
 * Copyright 2012 Dart project authors.
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
package com.google.dart.tools.ui.swtbot.performance;

import com.google.dart.tools.core.internal.perf.Metric;
import com.google.dart.tools.core.internal.perf.Performance;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;

/**
 * Represents a named, SWTBot, performance metric.
 * 
 * @see SwtBotPerformance
 */
public class SwtBotMetric extends Metric {

  public SwtBotMetric(String name, long threshold) {
    super(name, threshold);
  }

  public SwtBotMetric(String name, long threshold, boolean printWhileLogging) {
    super(name, threshold, printWhileLogging);
  }

  /**
   * Log the elapsed time for the condition to become <code>true</code>. This operation blocks the
   * current thread.
   * 
   * @param condition the condition (not <code>null</code>)
   */
  public void log(SWTWorkbenchBot bot, ICondition condition, String... comments) {
    log(bot, System.currentTimeMillis(), condition, comments);
  }

  /**
   * Log the elapsed time for the condition to become <code>true</code>. This operation blocks the
   * current thread.
   * 
   * @param start the start time
   * @param condition the condition (not <code>null</code>)
   */
  public void log(SWTWorkbenchBot bot, long start, ICondition condition, String... comments) {
    bot.waitUntil(condition, SwtBotPerformance.DEFAULT_TIMEOUT_MS);
    log(start, comments);
  }

  /**
   * Log the elapsed time for the condition to become <code>true</code>. This operation runs in a
   * background thread and does not block the current thread.
   * 
   * @param condition the condition (not <code>null</code>)
   */
  public void logInBackground(ICondition condition, String... comments) {
    logInBackground(System.currentTimeMillis(), condition, comments);
  }

  /**
   * Log the elapsed time for the condition to become <code>true</code>. This operation runs in a
   * background thread and does not block the current thread.
   * 
   * @param start the start time
   * @param condition the condition (not <code>null</code>)
   */
  public void logInBackground(final long start, final ICondition condition,
      final String... comments) {
    synchronized (Performance.allResults) {
      SwtBotPerformance.pending++;
    }
    new Thread("Timing " + name) {
      @Override
      public void run() {
        long limit = System.currentTimeMillis() + SwtBotPerformance.DEFAULT_TIMEOUT_MS;
        Throwable exception = null;
        while (true) {
          try {
            if (condition.test()) {
              log(start, comments);
              break;
            }
          } catch (Throwable e) {
            exception = e;
            //$FALL-THROUGH$
          }
          try {
            Thread.sleep(100);
          } catch (InterruptedException e) {
            //$FALL-THROUGH$
          }
          if (System.currentTimeMillis() > limit) {
            String anotherComment = exception != null ? exception.getMessage() : "<<< timed out";
            String[] more = SwtBotPerformance.append(comments, anotherComment);
            log(start, more);
            break;
          }
        }
        synchronized (SwtBotPerformance.allResults) {
          SwtBotPerformance.pending--;
        }
      };
    }.start();
  }
}
