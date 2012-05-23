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
package com.google.dart.tools.core.internal.perf;

import static com.google.dart.tools.core.utilities.general.FormattedStringBuilder.appendLong;
import static com.google.dart.tools.core.utilities.general.FormattedStringBuilder.appendText;

/**
 * Represents a named performance metric.
 * 
 * @see Performance
 * @see Result
 */
public class Metric {
  public final String name;
  public final long threshold;
  public final boolean printIndividualResults;
  private int resultCount = 0;
  private long resultHigh = 0;
  private long resultLow = 0;
  private long resultTotal = 0;

  public Metric(String name, long threshold) {
    this(name, threshold, true);
  }

  public Metric(String name, long threshold, boolean printWhileLogging) {
    this.name = name;
    this.threshold = threshold;
    this.printIndividualResults = printWhileLogging;
  }

  /**
   * Log the elapsed time
   * 
   * @param start the start time
   */
  public void log(long start, String... comments) {
    Result result = new Result(this, start, comments);
    if (printIndividualResults) {
      result.print(0);
    }
    synchronized (Performance.allResults) {
      Performance.allResults.add(result);
      resultCount++;
      resultTotal += result.elapsed;
      if (resultCount == 1) {
        resultHigh = result.elapsed;
        resultLow = result.elapsed;
      } else {
        resultHigh = Math.max(resultHigh, result.elapsed);
        resultLow = Math.min(resultLow, result.elapsed);
      }
    }
  }

  public void printAverage() {
    // compute average, being careful of division by zero
    long resultAverage = 0;
    if (resultCount != 0) {
      resultAverage = resultTotal / resultCount;
    }
    StringBuilder line = new StringBuilder();
    appendLong(line, resultCount, 5);
    line.append(' ');
    appendText(line, name, 20);
    appendLong(line, threshold, Performance.NUM_COL_WIDTH);
    line.append(" ms ");
    line.append(threshold < resultAverage ? '<' : ' ');
    line.append(' ');
    appendLong(line, resultAverage, Performance.NUM_COL_WIDTH);
    line.append(" ms ");
    appendLong(line, resultHigh, Performance.NUM_COL_WIDTH);
    line.append(" ms ");
    appendLong(line, resultLow, Performance.NUM_COL_WIDTH);
    line.append(" ms ");
    System.out.println(line);
  }

  public void printKeyValue() {
    // compute average, being careful of division by zero
    long resultAverage = 0;
    if (resultCount != 0) {
      resultAverage = resultTotal / resultCount;
    }
    StringBuilder line = new StringBuilder();
    line.append(name);
    line.append(":");
    line.append(resultAverage);
    System.out.println(line);
  }
}
