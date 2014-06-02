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
 * The class {@code ServerTimingTests} defines a main method that will run a set of timing tests.
 */
public class ServerTimingTests {
  /**
   * Run all of the timing tests.
   * 
   * @param args the command-line arguments
   */
  public static void main(String[] args) {
    ServerTimingTests tests = new ServerTimingTests();
    tests.runAll();
  }

  /**
   * The writer used to generate output.
   */
  private PrintWriter writer = new PrintWriter(System.out);

  /**
   * Run all of the timing tests.
   */
  private void runAll() {
    runGroup("Analyze engine", new TimingTest[] {
        new AnalyzeEngineInContext(), new AnalyzeEngineInServer()});
  }

  /**
   * Run a single group of timing tests.
   */
  private void runGroup(String groupName, TimingTest[] tests) {
    try {
      TestGroup group = new TestGroup(groupName, tests);
      group.runAll(writer);
    } catch (Exception exception) {
      exception.printStackTrace(writer);
    }
    writer.flush();
  }
}
