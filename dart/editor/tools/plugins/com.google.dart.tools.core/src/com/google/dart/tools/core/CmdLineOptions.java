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

import java.io.File;
import java.util.ArrayList;

/**
 * Instances of {@code CmdLineOptions} parse and cache options specified on the command line via the
 * {@link #parseCmdLine(String[])} method. Command line arguments can include:
 * <ul>
 * <li>{@value #PERF_FLAG} [start-time-in-milliseconds]</li>
 * <li>{@value #AUTO_EXIT_FLAG}</li>
 * <li>/path/to/file.dart</li>
 * </ul>
 * Regardless of the other flags, the editor should open an editor tag for each *.dart file
 * specified on the command line, and open a project for each directory specified on the command
 * line.
 */
public class CmdLineOptions {

  private static final String AUTO_EXIT_FLAG = "--auto-exit";
  private static final String KILL_AFTER_PERF_FLAG_OLD = "-kill-after-perf"; // deprecated
  private static final String PACKAGE_ROOT_FLAG = "--package-root";
  private static final String PERF_FLAG = "--perf";
  private static final String PERF_FLAG_OLD = "-perf"; // deprecated
  private static final String TEST_FLAG = "--test";

  private static CmdLineOptions globalOptions;

  /**
   * Answer the options specified on the command line.
   * 
   * @return the options or {@code null} if {@link #setOptions(CmdLineOptions)} has not been called
   */
  public static CmdLineOptions getOptions() {
    return globalOptions;
  }

  /**
   * Parse the specified command line arguments and return an object representing the options
   * specified on the command line.
   * 
   * @param args the command line arguments (not {@code null}, contains no {@code null}s)
   * @return the options (not {@code null})
   */
  public static CmdLineOptions parseCmdLine(String[] args) {
    CmdLineOptions options = new CmdLineOptions();
    options.startTime = System.currentTimeMillis();

    for (int index = 0; index < args.length; index++) {
      String arg = args[index];

      if (arg.equals(PERF_FLAG) || arg.equals(PERF_FLAG_OLD)) {
        // process --perf <startTime-in-milliseconds>
        options.measurePerformance = true;
        if (index + 1 < args.length) {
          try {
            options.startTime = Long.valueOf(args[index + 1]);
            index++;
          } catch (NumberFormatException e) {
            // fall through to set start time to current time
          }
        }

      } else if (arg.equals(AUTO_EXIT_FLAG) || arg.equals(KILL_AFTER_PERF_FLAG_OLD)) {
        options.autoExit = true;

      } else if (arg.equals(TEST_FLAG)) {
        options.runTests = true;
        if (index + 1 < args.length) {
          String nextArg = args[index + 1];
          if (nextArg.length() > 0 && nextArg.charAt(0) != '-') {
            options.runTestName = nextArg;
            index++;
          }
        }
      } else if (arg.equals(PACKAGE_ROOT_FLAG)) {
        if (index + 1 < args.length) {
          String nextArg = args[index + 1];
          if (nextArg.length() > 0 && nextArg.charAt(0) != '-') {
            options.packageRootString = nextArg.trim();
            index++;
          } else {
            System.err.println("Expected path after " + PACKAGE_ROOT_FLAG + " but found " + nextArg);
          }
        } else {
          System.err.println("Expected path after " + PACKAGE_ROOT_FLAG);
        }

      } else if (arg.equals("-version") || arg.equals("-port") || arg.equals("-testLoaderClass")
          || arg.equals("-loaderpluginname") || arg.equals("-classNames")
          || arg.equals("-testApplication") || arg.equals("-testpluginname") || arg.equals("-test")) {
        options.junitTestsAreRunning = true;
        // Ignore jUnit arguments
        if (index + 1 < args.length) {
          String nextArg = args[index + 1];
          if (nextArg.length() > 0 && nextArg.charAt(0) != '-') {
            index++;
          }
        }

      } else if (arg.length() > 0) {
        if (arg.charAt(0) != '-') {
          // files to be opened by the editor regardless of whether performance is measured
          options.files.add(new File(arg));
        } else {
          System.err.println("Unknown option: " + arg);
        }
      }
    }

    return options;
  }

  /**
   * Set the options specified on the command line.
   * 
   * @param options the options (not {@code null})
   */
  public static void setOptions(CmdLineOptions options) {
    // Sanity check
    if (CmdLineOptions.globalOptions != null) {
      throw new RuntimeException("Options have already been set");
    }
    CmdLineOptions.globalOptions = options;
  }

  // options
  private final ArrayList<File> files = new ArrayList<File>();
  private boolean autoExit = false;
  private boolean measurePerformance = false;
  private boolean runTests = false;
  private String runTestName = null;
  private boolean junitTestsAreRunning = false;
  private long startTime = 0;
  private String packageRootString = "";
  private File[] packageRoots = null;

  // use parseCmdLine(...) to construct new options
  private CmdLineOptions() {
  }

  /**
   * Answer {@code true} if the {@value CmdLineOptions#AUTO_EXIT_FLAG} (or the deprecated
   * {@value #KILL_AFTER_PERF_FLAG_OLD}) is specified on the command line indicating that the Editor
   * should exit after all the performance numbers and test results have been echoed to standard
   * out.
   */
  public boolean getAutoExit() {
    return autoExit;
  }

  /**
   * Answer the files specified on the command line.
   * 
   * @return an array of files (not {@code null}, contains no {@code null}s)
   */
  public ArrayList<File> getFiles() {
    return files;
  }

  /**
   * Answer {@code true} if the {@value CmdLineOptions#PERF_FLAG} is specified on the command line
   * indicating that performance should be measured and echoed to standard out.
   */
  public boolean getMeasurePerformance() {
    return measurePerformance;
  }

  /**
   * Answer the package roots specified on the command line or an empty list if none.
   * 
   * @return the package roots (not {@code null}, contains no {@code null}s)
   */
  public File[] getPackageRoots() {
    if (packageRoots == null) {
      if (packageRootString.length() == 0) {
        packageRoots = new File[0];
      } else {
        String[] rootPaths = packageRootString.split(";");
        packageRoots = new File[rootPaths.length];
        for (int index = 0; index < packageRoots.length; index++) {
          packageRoots[index] = new File(rootPaths[index]);
        }
      }
    }
    return packageRoots;
  }

  /**
   * Answer a string representing the package roots specified on the command line.
   * 
   * @return the package roots or {@code null} if none specified
   */
  public String getPackageRootString() {
    return packageRootString.length() > 0 ? packageRootString : null;
  }

  /**
   * Answer name of the test to run if {@link #getRunTests()} is {@code true}.
   */
  public String getRunTestName() {
    return runTestName;
  }

  /**
   * Answer {@code true} if the {@value #TEST_FLAG} is specified on the command line indicating that
   * the Editor should automatically run the Editor's suite of tests.
   */
  public boolean getRunTests() {
    return runTests && !junitTestsAreRunning;
  }

  /**
   * Answer the start time in milliseconds (see {@link System#currentTimeMillis()} of Dart Editor
   * for use during performance measurements. This is the numeric value specified on the command
   * line immediately after the {@value CmdLineOptions#PERF_FLAG} flag, or the time at which the
   * command line arguments are parsed if the {@value CmdLineOptions#PERF_FLAG} flag is not present
   * or is not followed by a number.
   */
  public long getStartTime() {
    return startTime;
  }
}
