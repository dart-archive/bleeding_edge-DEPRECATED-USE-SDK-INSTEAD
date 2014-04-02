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
import java.util.List;

/**
 * Instances of {@code CmdLineOptions} parse and cache options specified on the command line via the
 * {@link #parseCmdLine(String[])} method. Command line arguments can include:
 * <ul>
 * <li>{@value #PERF} [start-time-in-milliseconds]</li>
 * <li>{@value #AUTO_EXIT}</li>
 * <li>/path/to/file.dart</li>
 * </ul>
 * Regardless of the other flags, the editor should open an editor tag for each *.dart file
 * specified on the command line, and open a project for each directory specified on the command
 * line.
 * 
 * @coverage dart.tools.core
 */
public class CmdLineOptions {
  public static final String PACKAGE_ROOT = "--package-root";
  public static final String PACKAGE_OVERRIDE = "--package-override-directory";

  private static final String AUTO_EXIT = "--auto-exit";

  private static final String OPEN = "--open";
  private static final String PERF = "--perf";
  private static final String TEST = "--test";

  private static final String EXPERIMENTAL = "--experimental";

  private static final String NO_INSTRUMENTATION = "--no-instrumentation";

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

    ArrayList<File> roots = new ArrayList<File>();

    int index = 0;
    while (index < args.length) {
      String arg = args[index++];

      if (arg.equals(AUTO_EXIT)) {
        options.autoExit = true;
      } else if (arg.equals(OPEN)) {
        while (isOptionValue(args, index)) {
          options.files.add(new File(args[index]));
          index++;
        }
      } else if (arg.equals(EXPERIMENTAL)) {
        options.experimental = true;
      } else if (arg.equals(NO_INSTRUMENTATION)) {
        options.allowInstrumentation = false;
      } else if (arg.equals(PERF)) {
        // --perf [<startTime-in-milliseconds>]
        options.measurePerformance = true;
        options.startTime = System.currentTimeMillis();
        options.autoExit = true;

        if (isOptionValue(args, index)) {
          try {
            options.startTime = Long.valueOf(args[index]);
            index++;
          } catch (NumberFormatException e) {

          }
        }
      } else if (arg.equals(PACKAGE_ROOT)) {
        while (isOptionValue(args, index)) {
          roots.add(new File(args[index]).getAbsoluteFile());
          index++;
        }
        if (roots.size() == 0) {
          options.warning("Expected path after " + PACKAGE_ROOT);
        }
      } else if (arg.equals(PACKAGE_OVERRIDE)) {
        if (isOptionValue(args, index)) {
          options.packageOverrideDirectory = new File(args[index]).getAbsoluteFile();
          index++;
        } else {
          options.warning("Expected path after " + PACKAGE_OVERRIDE);
        }
      } else if (arg.equals(TEST)) {
        options.runTests = true;
        if (isOptionValue(args, index)) {
          options.runTestName = args[index];
          index++;
        }
      } else if (arg.equals("-version") || arg.equals("-port") || arg.equals("-testLoaderClass")
          || arg.equals("-loaderpluginname") || arg.equals("-classNames")
          || arg.equals("-testApplication") || arg.equals("-testpluginname") || arg.equals("-test")) {
        options.junitTestsAreRunning = true;
        // Ignore jUnit arguments
        if (isOptionValue(args, index)) {
          index++;
        }
      } else if (arg.length() > 0) {
        options.warning("Unknown option: " + arg);
      }
    }

    options.packageRoots = roots.toArray(new File[roots.size()]);

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

  /**
   * Answer {@code true} if the specified argument does not start with a "-"
   * 
   * @param arg the command line argument being tested
   * @return {@code true} if the argument is a value
   */
  private static boolean isOptionValue(String value) {
    return value.length() <= 0 || value.charAt(0) != '-';
  }

  /**
   * Answer {@code true} if the specified argument exists and does not start with a "-"
   * 
   * @param args the command line arguments
   * @param index the index of the argument being tested
   * @return {@code true} if the argument is a value
   */
  private static boolean isOptionValue(String[] args, int index) {
    if (index >= args.length) {
      return false;
    }
    return isOptionValue(args[index]);
  }

  // options
  private final ArrayList<File> files = new ArrayList<File>();

  private boolean autoExit = false;

  private boolean measurePerformance = false;

  private boolean runTests = false;

  private String runTestName = null;
  private boolean junitTestsAreRunning = false;
  private long startTime;
  private File[] packageRoots = null;
  private File packageOverrideDirectory = null;
  private ArrayList<String> warnings = new ArrayList<String>();
  private boolean experimental = false;
  private boolean allowInstrumentation = true;

  // use parseCmdLine(...) to construct new options
  private CmdLineOptions() {
  }

  /**
   * Determine if instrumentation should be allowed.
   * Specifying the --no-instrumentation flag on the command line will prevent instrumentation
   * from running regardless of whether instrumentation is installed.
   *
   * @return {@code true} if instrumentation should be allowed, else {@code false}
   */
  public boolean allowInstrumentation() {
    return allowInstrumentation;
  }

  /**
   * Answer {@code true} if the {@value CmdLineOptions#AUTO_EXIT} (or the deprecated
   * {@value #KILL_AFTER_PERF_OLD}) is specified on the command line indicating that the Editor
   * should exit after all the performance numbers and test results have been echoed to standard
   * out.
   */
  public boolean getAutoExit() {
    return autoExit;
  }

  /**
   * Answer <code>true</code> if {@value #EXPERIMENTAL} is specified on the command line indicating
   * that the Editor should use experimental code during execution.
   */
  public boolean getExperimental() {
    return experimental;
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
   * Answer {@code true} if the {@value CmdLineOptions#PERF} is specified on the command line
   * indicating that performance should be measured and echoed to standard out.
   */
  public boolean getMeasurePerformance() {
    return measurePerformance;
  }

  /**
   * Answer a string representing the package override directory specified on the command line.
   * 
   * @return the package override directory or {@code null} if none specified
   */
  public File getPackageOverrideDirectory() {
    return packageOverrideDirectory;
  }

  /**
   * Answer the package roots specified on the command line or an empty list if none.
   * 
   * @return the package roots (not {@code null}, contains no {@code null}s)
   */
  public File[] getPackageRoots() {
    return packageRoots;
  }

  /**
   * Answer name of the test to run if {@link #getRunTests()} is {@code true}.
   */
  public String getRunTestName() {
    return runTestName;
  }

  /**
   * Answer {@code true} if {@value #TEST} is specified on the command line indicating that the
   * Editor should automatically run the Editor's suite of tests.
   */
  public boolean getRunTests() {
    return runTests && !junitTestsAreRunning;
  }

  /**
   * Answer the start time in milliseconds (see {@link System#currentTimeMillis()} of Dart Editor
   * for use during performance measurements. This is the numeric value specified on the command
   * line immediately after the {@value CmdLineOptions#PERF} flag, or the time at which the command
   * line arguments are parsed if the {@value CmdLineOptions#PERF} flag is not present or is not
   * followed by a number.
   */
  public long getStartTime() {
    return startTime;
  }

  /**
   * Answer the warnings generated when parsing options.
   * 
   * @return a list of warnings (not {@code null}, contains no {@code null}s)
   */
  public List<String> getWarnings() {
    return warnings;
  }

  /**
   * Echo warnings to {@link System#err}.
   */
  public void printWarnings() {
    for (String message : warnings) {
      System.err.println(message);
    }
  }

  /**
   * Report a warning about the options being parsed
   * 
   * @param message the warning message
   */
  private void warning(String message) {
    warnings.add(message);
  }
}
