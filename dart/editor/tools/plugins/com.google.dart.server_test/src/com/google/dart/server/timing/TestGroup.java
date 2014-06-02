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

import java.io.PrintWriter;

/**
 * Instances of the class {@code TestGroup} represent objects that measure multiple ways of
 * performing the same operation.
 */
public class TestGroup {
  /**
   * The name of the group.
   */
  private String name;

  /**
   * The tests that comprise the group.
   */
  TimingTest[] tests;

  /**
   * Initialize a newly created group
   */
  TestGroup(String name, TimingTest[] tests) {
    this.name = name;
    this.tests = tests;
  }

  /**
   * Return the name of the group.
   */
  public String getName() {
    return name;
  }

  /**
   * Run all of the tests in this group, writing timing information to the given writer.
   * 
   * @param writer the writer to which results will be written.
   */
  public void runAll(PrintWriter writer) {
    writer.println(name);
    for (TimingTest test : tests) {
      try {
        TimingResult result = test.run();
        int count = test.getTimingCount();
        long time = result.getTotalTime();
        writer.print("   ");
        writer.print(test.getName());
        writer.print(" : ");
        writer.print(time);
        writer.print(" (");
        writer.print(result.getMinTime());
        writer.print(", ");
        writer.print(result.getAverageTime());
        writer.print(", ");
        writer.print(result.getMaxTime());
        writer.print(", stdDev = ");
        writer.print(Math.floor(result.getStandardDeviation() * 100.0) / 100.0);
        writer.print(") for ");
        writer.print(count);
        writer.println(" iterations");
      } catch (Exception exception) {
        writer.print("   ");
        writer.print(test.getName());
        writer.print(" : ");
        writer.println(exception.getMessage());
        exception.printStackTrace(writer);
      }
    }
    writer.println();
  }
}
