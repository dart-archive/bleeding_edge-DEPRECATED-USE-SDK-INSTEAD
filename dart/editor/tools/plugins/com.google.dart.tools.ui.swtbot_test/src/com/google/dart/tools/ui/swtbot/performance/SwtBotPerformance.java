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

import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.core.internal.perf.Metric;
import com.google.dart.tools.core.internal.perf.Performance;
import com.google.dart.tools.core.internal.perf.Result;
import com.google.dart.tools.ui.swtbot.conditions.AnalysisCompleteCondition;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;

/**
 * This class manages the performance metrics for SWTBot operations.
 * 
 * @see SwtBotMetric
 * @see Performance
 * @see Metric
 * @see Result
 * @see AbstractDartEditorTest
 */
public class SwtBotPerformance extends Performance {

  public static final SwtBotMetric ANALYZE = new SwtBotMetric("Analyze", 200);
  public static final SwtBotMetric ANALYZE_FULL = new SwtBotMetric("Analyze (Full)", 3000);
  public static final SwtBotMetric ANALYSIS_SERVER_WARMUP = new SwtBotMetric(
      "AnalysisServer Warmup",
      5000);
  public static final SwtBotMetric CODE_COMPLETION = new SwtBotMetric("Code Completion", 200);
  public static final SwtBotMetric COMPILE = new SwtBotMetric("Compile", 1000);
  public static final SwtBotMetric COMPILER_PARSE = new SwtBotMetric("Compiler Parse", 10);
  public static final SwtBotMetric COMPILER_WARMUP = new SwtBotMetric("Compiler Warmup", 5000);
  public static final SwtBotMetric LAUNCH_APP = new SwtBotMetric("Launch App", 3000);
  public static final SwtBotMetric NEW_APP = new SwtBotMetric("New App", 300);
  public static final SwtBotMetric OPEN_LIB = new SwtBotMetric("Open Library", 300);
  public static final SwtBotMetric PARSE = new SwtBotMetric("Parse", 10, false);
  public static final SwtBotMetric RESOLVE = new SwtBotMetric("Resolve", 100, false);

  static int pending = 0;

  /**
   * Wait for any timed background operations to complete
   */
  public static void waitForResults(SWTWorkbenchBot bot) {

    // Wait for the AnalysisServer to complete its background tasks

    if (DartCoreDebug.ANALYSIS_SERVER) {
      bot.waitUntil(new AnalysisCompleteCondition(), DEFAULT_TIMEOUT_MS);
    }

    // Wait for any pending operations

    int timeout;
    synchronized (allResults) {
      if (pending < 1) {
        return;
      }
      timeout = DEFAULT_TIMEOUT_MS * pending;
    }
    bot.waitUntil(new ICondition() {

      @Override
      public String getFailureMessage() {
        synchronized (Performance.allResults) {
          return "Gave up waiting for " + pending + " background operations";
        }
      }

      @Override
      public void init(SWTBot bot) {
      }

      @Override
      public boolean test() throws Exception {
        synchronized (Performance.allResults) {
          return pending == 0;
        }
      }
    }, timeout);
  }
}
