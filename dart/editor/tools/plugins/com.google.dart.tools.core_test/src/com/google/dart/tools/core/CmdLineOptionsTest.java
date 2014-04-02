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

import com.google.dart.engine.utilities.io.PrintStringWriter;

import junit.framework.TestCase;

import java.io.File;

public class CmdLineOptionsTest extends TestCase {

  public void test_ignore_junit_args() throws Exception {
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {
        "-version", "3", "-port", "60695", "-testLoaderClass",
        "org.eclipse.jdt.internal.junit.runner.junit3.JUnit3TestLoader", "-loaderpluginname",
        "org.eclipse.jdt.junit.runtime", "-classNames", "com.google.dart.tools.ui.TestAll",
        "-testApplication", "com.google.dart.tools.deploy.application", "-testpluginname",
        "com.google.dart.tools.ui_test"});
    assertOptions(options, false, 0, false, 0, false, 0, 0, false, true);
  }

  public void test_no_instrumentation() throws Exception {
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {"--no-instrumentation"});
    assertOptions(options, false, 0, false, 0, false, 0, 0, false, false);
  }

  public void test_parse_autoExit() {
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {"--auto-exit"});
    assertOptions(options, false, 0, true, 0, false, 0, 0, false, true);
  }

  public void test_parse_empty() {
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {});
    assertOptions(options, false, 0, false, 0, false, 0, 0, false, true);
  }

  public void test_parse_experimental() {
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {"--experimental"});
    assertOptions(options, false, 0, false, 0, false, 0, 0, true, true);
  }

  public void test_parse_file() {
    File file1 = new File("does-not-exist.dart");
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {file1.getPath()});
    assertOptions(options, false, 0, false, 0, false, 0, 1, false, true);
  }

  public void test_parse_open() {
    File file1 = new File("does-not-exist.dart");
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {"--open", file1.getPath()});
    assertOptions(options, false, 0, false, 1, false, 0, 0, false, true);
    assertEquals(file1, options.getFiles().get(0));
  }

  public void test_parse_open2() {
    File file1 = new File("does-not-exist.dart");
    File file2 = new File("another.dart");
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {
        "--open", file1.getPath(), file2.getPath()});
    assertOptions(options, false, 0, false, 2, false, 0, 0, false, true);
    assertEquals(file1, options.getFiles().get(0));
    assertEquals(file2, options.getFiles().get(1));
  }

  public void test_parse_packageOverrideDirectory() {
    File file1 = new File("foo").getAbsoluteFile();
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {
        "--package-override-directory", "foo"});
    assertOptions(options, false, 0, false, 0, false, 0, 0, false, true);
    assertEquals(file1, options.getPackageOverrideDirectory());
  }

  public void test_parse_packageRoot() {
    File file1 = new File("foo").getAbsoluteFile();
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {"--package-root", "foo"});
    assertOptions(options, false, 0, false, 0, false, 1, 0, false, true);
    assertEquals(file1, options.getPackageRoots()[0]);
  }

  public void test_parse_packageRoots() {
    File file1 = new File("foo").getAbsoluteFile();
    File file2 = new File("bar").getAbsoluteFile();
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {
        "--package-root", "foo", "bar"});
    assertOptions(options, false, 0, false, 0, false, 2, 0, false, true);
    assertEquals(file1, options.getPackageRoots()[0]);
    assertEquals(file2, options.getPackageRoots()[1]);
  }

  public void test_parse_perf() {
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {"--perf"});
    assertOptions(options, true, 0, true, 0, false, 0, 0, false, true);
  }

  public void test_parse_perf_startTime_old() {
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {"--perf", "12345"});
    assertOptions(options, true, 12345, true, 0, false, 0, 0, false, true);
  }

  public void test_parse_test() {
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {"--test"});
    assertOptions(options, false, 0, false, 0, true, 0, 0, false, true);
  }

  public void test_test_noName_hasOtherOption() {
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {"--test", "--perf"});
    assertEquals(true, options.getRunTests());
    assertEquals(null, options.getRunTestName());
  }

  public void test_test_noName_last() {
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {"--test"});
    assertEquals(true, options.getRunTests());
    assertEquals(null, options.getRunTestName());
  }

  public void test_test_withName() {
    CmdLineOptions options = CmdLineOptions.parseCmdLine(new String[] {"--test", "my.test.Name"});
    assertEquals(true, options.getRunTests());
    assertEquals("my.test.Name", options.getRunTestName());
  }

  private void assertOptions(CmdLineOptions options, boolean perf, int startTime, boolean exitPerf,
      int fileCount, boolean runTests, int pkgRootCount, int warningCount, boolean experimental,
      boolean allowInstrumentation) {
    assertNotNull(options);
    assertEquals(perf, options.getMeasurePerformance());
    if (startTime != 0) {
      assertEquals(startTime, options.getStartTime());
    }
    assertEquals(exitPerf, options.getAutoExit());
    assertEquals(fileCount, options.getFiles().size());
    assertEquals(runTests, options.getRunTests());
    assertEquals(pkgRootCount, options.getPackageRoots().length);
    if (warningCount != options.getWarnings().size()) {
      PrintStringWriter msg = new PrintStringWriter();
      msg.print("Expected ");
      msg.print(warningCount);
      msg.print(" but found:");
      if (options.getWarnings().isEmpty()) {
        msg.print(" 0");
      } else {
        for (String warning : options.getWarnings()) {
          msg.println();
          msg.print(warning);
        }
      }
      fail(msg.toString());
      msg.close(); // suppress Eclipse 3.8 warning
    }
    assertEquals(experimental, options.getExperimental());
    assertEquals(allowInstrumentation, options.allowInstrumentation());
  }
}
