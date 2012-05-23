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

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.utilities.performance.PerformanceManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

/**
 * This class manages a set of metrics for Dart Editor operations.
 * <p>
 * TODO(jwren) This functionality should be merged with the functionality in
 * {@link PerformanceManager}, the reason that this was not done initially, or up to this point, is
 * because these performance classes are super classes for the performance classes in the SWTBot
 * plug-in which were preserved in the refactoring, also, these performance classes, over
 * {@link PerformanceManager} were closer to what we need for the support we want for performance to
 * be printed to the command line.
 * 
 * @see Metric
 * @see Result
 * @see DartEditorCommandLineManager
 */
public class Performance {

  public static enum PrintStyle {
    ALL,
    KEY_VALUE
  }

  public static final List<Result> allResults = new ArrayList<Result>(20);
  public static final int DEFAULT_TIMEOUT_MS = 180000; // 3 minutes
  public static final int NUM_COL_WIDTH = 7;

  public static final Metric TIME_TO_START_APP = new Metric("Time_to_start_DartIDEApp", 100);
  public static final Metric TIME_TO_STARTUP = new Metric("Time_to_startup", 100);
  public static final Metric TIME_TO_STARTUP_PLUS_ANALYSIS = new Metric(
      "Time_to_startup_plus_analysis",
      100);

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
   * Echo the allResults to standard out, using the passed {@link PrintStyle}.
   */
  public static void printResults_all() {
    System.out.println("==========================================================================");
    System.out.println("Editor Version: " + DartCore.getBuildIdOrDate());
    System.out.println("OS: " + getOsInfo());
    System.out.println();
    System.out.println("Metric                     Expected    Actual    Comments");
    System.out.println("========================== ========= = ========= ===============================");
    // Calculate depth to display metrics within other metrics
    int[] depth = new int[allResults.size()];
    for (int i = allResults.size() - 1; i >= 0; i--) {
      long start = allResults.get(i).getStart();
      int j = i - 1;
      while (j >= 0 && allResults.get(j).getStart() > start) {
        depth[j--]++;
      }
    }
    for (int i = 0; i < allResults.size(); i++) {
      Result result = allResults.get(i);
      if (result.getMetric().printIndividualResults) {
        result.print(depth[i]);
      }
    }
    System.out.println();
    System.out.println("#     Metric              Expected     Average    High       Low");
    System.out.println("===== =================== ========== = ========== ========== ==========");
    for (Metric metric : getMetricsWithResults()) {
      metric.printAverage();
    }
  }

  /**
   * Echo all the results in the form of key:value pairs.
   */
  public static void printResults_keyValue() {
    System.out.println("==========================================================================");
    for (Metric metric : getMetricsWithResults()) {
      metric.printKeyValue();
    }
    System.out.println("==========================================================================");
  }

  protected static Collection<Metric> getMetricsWithResults() {
    TreeSet<Metric> metrics = new TreeSet<Metric>(new Comparator<Metric>() {

      @Override
      public int compare(Metric m1, Metric m2) {
        return m1.name.compareTo(m2.name);
      }
    });
    for (Result result : allResults) {
      metrics.add(result.getMetric());
    }
    return metrics;
  }

  protected static String getOsInfo() {
    return System.getProperty("os.name") + " - " + System.getProperty("os.arch") + " ("
        + System.getProperty("os.version") + ")";
  }

}
