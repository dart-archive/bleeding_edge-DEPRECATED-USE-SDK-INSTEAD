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
package com.google.dart.command.analyze.test;

import com.google.dart.command.analyze.AnalyzerImpl;
import com.google.dart.command.analyze.AnalyzerMain;
import com.google.dart.command.analyze.AnalyzerOptions;
import com.google.dart.engine.context.AnalysisContext;

import java.util.Collection;
import java.util.Comparator;
import java.util.TreeSet;

/**
 * A cmdline analyzer that uses {@link AnalysisContext#performAnalysisTask()} to analyze an
 * application and tracks various performance measurements such as the time to execute that method.
 */
public class BackgroundAnalyzerPerformanceTest extends AnalyzerMain {

  public static void main(String[] args) {
    new BackgroundAnalyzerPerformanceTest().run(args);
  }

  private BackgroundAnalyzerImpl analyzer;

  @Override
  protected String getProgramName() {
    return BackgroundAnalyzerPerformanceTest.class.getSimpleName();
  }

  @Override
  protected AnalyzerImpl newAnalyzer(AnalyzerOptions options) {
    analyzer = new BackgroundAnalyzerImpl(options);
    return analyzer;
  }

  @Override
  protected void showPerformanceResults(long startTime, String suffix) {
    super.showPerformanceResults(startTime, suffix);
    Collection<PerformanceMonitor> allMonitors = analyzer.getPerformanceMonitors();
    TreeSet<PerformanceMonitor> sortedMonitors = new TreeSet<PerformanceMonitor>(
        new Comparator<PerformanceMonitor>() {
          @Override
          public int compare(PerformanceMonitor o1, PerformanceMonitor o2) {
            return o1.getName().compareTo(o2.getName());
          }
        });
    sortedMonitors.addAll(allMonitors);
    for (PerformanceMonitor m : sortedMonitors) {
      System.out.println();
      System.out.println(m.getName() + " Max: " + m.getMax());
      System.out.println(m.getName() + " Average: " + m.getAverage());
      System.out.println(m.getName() + " Min: " + m.getMin());
      System.out.println(m.getName() + " Count: " + m.getCount());
    }
  }
}
