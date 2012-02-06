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
package com.google.dart.tools.ui.swtbot;

import com.google.dart.tools.core.DartCore;

import static com.google.dart.tools.ui.swtbot.util.FormattedStringBuilder.appendLong;
import static com.google.dart.tools.ui.swtbot.util.FormattedStringBuilder.appendText;

import org.eclipse.swtbot.eclipse.finder.SWTWorkbenchBot;
import org.eclipse.swtbot.swt.finder.SWTBot;
import org.eclipse.swtbot.swt.finder.waits.ICondition;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.TreeSet;

public class Performance {

  /**
   * Represents a named performance metric
   */
  public static class Metric {
    public final String name;
    public final long threshold;
    private int resultCount = 0;
    private long resultAverage = 0;
    private long resultHigh = 0;
    private long resultLow = 0;

    Metric(String name, long threshold) {
      this.name = name;
      this.threshold = threshold;
    }

    /**
     * Log the elapsed time
     * 
     * @param start the start time
     */
    public void log(long start, String... comments) {
      Result result = new Result(this, System.currentTimeMillis() - start, comments);
      result.print();
      synchronized (allResults) {
        allResults.add(result);
        resultCount++;
        if (resultCount == 1) {
          resultAverage = result.elapsed;
          resultHigh = result.elapsed;
          resultLow = result.elapsed;
        } else {
          resultAverage = (resultAverage * (resultCount - 1) + result.elapsed) / resultCount;
          resultHigh = Math.max(resultHigh, result.elapsed);
          resultLow = Math.min(resultLow, result.elapsed);
        }
      }
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
      bot.waitUntil(condition, DEFAULT_TIMEOUT_MS);
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
        Performance.pending++;
      }
      new Thread("Timing " + name) {
        @Override
        public void run() {
          long limit = System.currentTimeMillis() + DEFAULT_TIMEOUT_MS;
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
              String[] more = append(comments, anotherComment);
              log(start, more);
              break;
            }
          }
          synchronized (Performance.allResults) {
            Performance.pending--;
          }
        };
      }.start();
    }

    public void printAverage() {
      StringBuilder line = new StringBuilder();
      appendLong(line, resultCount, 2);
      line.append(' ');
      appendText(line, name, 20);
      appendLong(line, threshold, 7);
      line.append(" ms ");
      line.append(threshold < resultAverage ? '<' : ' ');
      appendLong(line, resultAverage, 7);
      line.append(" ms");
      appendLong(line, resultHigh, 7);
      line.append(" ms");
      appendLong(line, resultLow, 7);
      line.append(" ms");
      System.out.println(line);
    }
  }

  /**
   * Represents the result of executing a particular metric
   */
  private static class Result {
    private final Metric metric;
    private final long elapsed;
    private final String[] comments;

    Result(Metric metric, long elapsed, String... comments) {
      this.metric = metric;
      this.elapsed = elapsed;
      this.comments = comments;
    }

    /**
     * Log the elapsed time
     */
    void print() {
      StringBuilder line = new StringBuilder();
      appendText(line, metric.name, 20);
      appendLong(line, metric.threshold, 7);
      line.append(" ms ");
      line.append(metric.threshold < elapsed ? '<' : ' ');
      appendLong(line, elapsed, 7);
      line.append(" ms");
      for (String comment : comments) {
        line.append(", ");
        line.append(comment);
      }
      System.out.println(line.toString());
    }
  }

  public static final Metric NEW_APP = new Metric("New App", 300);
  public static final Metric OPEN_LIB = new Metric("Open Library", 300);
  public static final Metric LAUNCH_APP = new Metric("Launch App", 3000);
  public static final Metric COMPILE_FULL = new Metric("Compile (Full)", 3000);
  public static final Metric COMPILE_INCREMENTAL = new Metric("Compile (Inc)", 500);
  public static final Metric CODE_COMPLETION = new Metric("Code Completion", 200);
  public static final Metric COMPILER_WARMUP = new Metric("Compiler Warmup", 5000);

  private static final Collection<Result> allResults = new ArrayList<Performance.Result>(20);
  private static int pending = 0;

  public static final int DEFAULT_TIMEOUT_MS = 180000; // 3 minutes

  /**
   * Append the specified {@link String} to an array of {@link String}
   */
  public static String[] append(String[] comments, String anotherComment) {
    String[] result = new String[comments.length + 1];
    System.arraycopy(comments, 0, result, 0, comments.length);
    result[comments.length] = anotherComment;
    return result;
  }

  /**
   * Prepend the specified {@link String} to an array of {@link String}
   */
  public static String[] prepend(String newFirstComment, String[] comments) {
    String[] result = new String[comments.length + 1];
    System.arraycopy(comments, 0, result, 1, comments.length);
    result[0] = newFirstComment;
    return result;
  }

  /**
   * Echo the allResults to standard out.
   * 
   * @see #waitForResults(SWTWorkbenchBot)
   */
  public static void printResults() {
    System.out.println("==========================================================================");
    System.out.println("Editor Version: " + getEditorVersion());
    System.out.println("OS: " + getOsInfo());
    System.out.println();
    System.out.println("Metric               Expected    Actual    Comments");
    System.out.println("==================== ========= = ========= ===============================");
    for (Result result : allResults) {
      result.print();
    }
    System.out.println();
    System.out.println("#  Metric              Expected     Average    High       Low");
    System.out.println("== =================== ========== = ========== ========== ==========");
    for (Metric metric : getMetricsWithResults()) {
      metric.printAverage();
    }
  }

  /**
   * Wait for any timed background operations to complete
   */
  public static void waitForResults(SWTWorkbenchBot bot) {
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

  private static String getEditorVersion() {
    String version = DartCore.getBuildId();
    if (version.startsWith("@")) {
      version = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
    }
    return version;
  }

  private static Collection<Metric> getMetricsWithResults() {
    TreeSet<Metric> metrics = new TreeSet<Metric>(new Comparator<Metric>() {

      @Override
      public int compare(Metric m1, Metric m2) {
        return m1.name.compareTo(m2.name);
      }
    });
    for (Result result : allResults) {
      metrics.add(result.metric);
    }
    return metrics;
  }

  private static String getOsInfo() {
    return System.getProperty("os.name") + " - " + System.getProperty("os.arch") + " ("
        + System.getProperty("os.version") + ")";
  }
}
