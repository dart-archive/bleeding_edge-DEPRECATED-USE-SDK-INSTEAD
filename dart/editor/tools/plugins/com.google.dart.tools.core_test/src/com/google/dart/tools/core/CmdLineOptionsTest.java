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
package com.google.dart.tools.core;

import junit.framework.TestCase;

public class CmdLineOptionsTest extends TestCase {

  public void test_parse_autoExit() {
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {"--auto-exit"});
    assertOptions(options, false, 0, true, 0, false);
  }

  public void test_parse_empty() {
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {});
    assertOptions(options, false, 0, false, 0, false);
  }

  public void test_parse_file() {
    String filePath = "does-not-exist.dart";
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {filePath});
    assertOptions(options, false, 0, false, 1, false);
    assertEquals(filePath, options.getFiles().get(0).getPath());
  }

  public void test_parse_killAfterPerf_old() {
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {"-kill-after-perf"});
    assertOptions(options, false, 0, true, 0, false);
  }

  public void test_parse_perf_noStartTime() {
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {"--perf"});
    assertOptions(options, true, 0, false, 0, false);
  }

  public void test_parse_perf_startTime() {
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {"--perf", "12345"});
    assertOptions(options, true, 12345, false, 0, false);
  }

  public void test_parse_perf_startTime_old() {
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {"-perf", "12345"});
    assertOptions(options, true, 12345, false, 0, false);
  }

  public void test_parse_test() {
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {"--test"});
    assertOptions(options, false, 0, false, 0, true);
  }

  private void assertOptions(CmdLineOptions options, boolean perf, int startTime, boolean exitPerf,
      int fileCount, boolean runTests) {
    assertNotNull(options);
    assertEquals(perf, options.getMeasurePerformance());
    if (startTime == 0) {
      long delta = System.currentTimeMillis() - options.getStartTime();
      assertTrue("Expected getStartTime() to be within 10 ms", 0 <= delta && delta <= 10);
    } else {
      assertEquals(startTime, options.getStartTime());
    }
    assertEquals(exitPerf, options.getAutoExit());
    assertEquals(fileCount, options.getFiles().size());
    assertEquals(runTests, options.getRunTests());
  }
}
