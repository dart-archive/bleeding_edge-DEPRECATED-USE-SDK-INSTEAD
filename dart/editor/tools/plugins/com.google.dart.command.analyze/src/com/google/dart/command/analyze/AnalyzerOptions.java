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

package com.google.dart.command.analyze;

import com.google.common.collect.Lists;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Command line options accepted by the {@link AnalyzerMain} entry point.
 */
public class AnalyzerOptions {

  enum AnalyzerOutputFormat {
    MACHINE
  }

  /**
   * Create a new AnalyzerOptions object from the given list of command-line args.
   * 
   * @param args
   * @return
   */
  public static AnalyzerOptions createFromArgs(String[] args) {
    args = processArgs(args);

    AnalyzerOptions options = new AnalyzerOptions();

    CmdLineParser cmdLineParser = new CmdLineParser(options);

    for (int i = 0, len = args.length; i < len; i++) {
      try {
        cmdLineParser.parseArgument(args);
      } catch (CmdLineException e) {
        String msg = e.getMessage();

        if (e.getMessage().endsWith(" is not a valid option")) {
          String option = msg.substring(1);
          int closeQuote = option.indexOf('\"');
          option = option.substring(0, closeQuote);

          List<String> newArgs = Lists.newArrayList();

          for (String arg : args) {
            if (arg.equals(option)) {
              System.out.println("Ignoring unrecognized flag: " + arg);
              continue;
            }
            newArgs.add(arg);
          }

          args = newArgs.toArray(new String[newArgs.size()]);
          cmdLineParser = new CmdLineParser(options);
          continue;
        }
      }

      break;
    }

    return options;
  }

  /**
   * Print the tool usage to the given stream.
   * 
   * @param out
   */
  public static void printUsage(PrintStream out) {
    AnalyzerOptions bean = new AnalyzerOptions();
    CmdLineParser parser = new CmdLineParser(bean);
    parser.setUsageWidth(120);
    parser.printUsage(out);
  }

  private static String[] processArgs(String[] args) {
    List<String> result = new ArrayList<String>();

    for (String arg : args) {
      if (arg.isEmpty()) {
        continue;
      }
      if (arg.indexOf('=') != -1) {
        String[] strs = arg.split("=");
        for (int i = 0; i < strs.length; i++) {
          result.add(strs[i]);
        }
      } else {
        result.add(arg);
      }
    }

    return result.toArray(new String[result.size()]);
  }

  @Option(name = "--machine", // deprecated - used --format=machine instead
  usage = "Print errors in a format suitable for parsing")
  private boolean machineFormat = false;

  @Option(name = "--format", //
  usage = "Print errors in the specified format")
  private AnalyzerOutputFormat outputFormat = null;

  @Option(name = "--help", //
  aliases = {"-h"}, //
  usage = "Prints this help message")
  private boolean showHelp = false;

  @Option(name = "--version", //
  usage = "Print the analyzer version")
  private boolean showVersion = false;

  @Option(name = "--dart-sdk", //
  metaVar = "<dir>")
  // usage = "The path to the Dart SDK") // don't show in help
  private File dartSdkPath = null;

  @Option(name = "--use-dart2js-libraries")
  // usage = "Use the same resolution of dart: URI's as dart2js (defaults to the resolution used by the VM)") // don't show in help
  private boolean useDart2jsPaths = false;

  @Option(name = "--enable-async")
  private boolean enableAsync = false;

  @Option(name = "--enable-enum")
  private boolean enableEnum = false;

  @Option(name = "--verbose", //
  aliases = {"-v"})
  // TODO(devoncarew): document this flag when it is supported
  //usage = "Print verbose information while analyzing")
  private boolean enableVerbose = false;

  @Option(name = "--package-root", //
  aliases = {"-p"}, //
  metaVar = "<dir>", //
  usage = "The path to the package root")
  private File packageRootPath = null;

  @Option(name = "--use-package-map")
  private boolean usePackageMap = false;

  @Option(name = "--package-warnings", //
  aliases = {"--show-package-warnings"}, // deprecated alias
  usage = "Show warnings from package: imports")
  private boolean showPackageWarnings = false;

  @Option(name = "--batch", //
  aliases = {"-batch"})
  private boolean batch = false;

  @Option(name = "--warnings", //
  aliases = {"--show-sdk-warnings"} // deprecated alias
  )
  private boolean showSdkWarnings = false;

  @Option(name = "--fatal-warnings")
  private boolean warningsAreFatal = false;

  // TODO(devoncarew): this is unused, and is only for dartc compatibility
  @Option(name = "--fatal-type-errors")
  private boolean fatalTypeError = false;

  @Option(name = "--ignore-unrecognized-flags")
  private boolean ignoreUnrecognizedFlags;

  @Option(name = "--no-hints",//
  usage = "Do not show hint results")
  private boolean disableHints = false;

  @Option(name = "--enable_type_checks")
  // usage = "Check types in constant evaluation") // don't show in help
  private boolean enableTypeChecks = false;

  @Option(name = "--perf",//
  usage = "Print performance statistics")
  private boolean perf = false;

  @Option(name = "--diagnostic-colors")
  private boolean diagnosticColors = false; // ignored for now

  @Option(name = "--warm-perf")
  // usage = "Print both cold and warm performance statistics") // don't show in help
  private boolean warmPerf = false;

  @Argument
  private final String sourceFile = null;

  public AnalyzerOptions() {
    super();
  }

  /**
   * Return the path to the dart SDK.
   */
  public File getDartSdkPath() {
    return dartSdkPath;
  }

  /**
   * @return whether hint results (e.g. type inference based information and pub best practices)
   *         should not be reported.
   */
  public boolean getDisableHints() {
    return disableHints;
  }

  /**
   * Return {@code true} if support for async syntax should be enabled.
   */
  public boolean getEnableAsync() {
    return enableAsync;
  }

  /**
   * Return {@code true} if support for enum syntax should be enabled.
   */
  public boolean getEnableEnum() {
    return enableEnum;
  }

  /**
   * Return {@code true} if analysis should treat type mismatches found during constant evaluation
   * as errors.
   */
  public boolean getEnableTypeChecks() {
    return enableTypeChecks;
  }

  public boolean getMachineFormat() {
    return machineFormat || outputFormat == AnalyzerOutputFormat.MACHINE;
  }

  /**
   * @return the package-root path, if specified
   */
  public File getPackageRootPath() {
    return packageRootPath;
  }

  /**
   * @return whether performance statistics should be printed.
   */
  public boolean getPerf() {
    return perf;
  }

  /**
   * @return whether SDK warnings should be reported
   */
  public boolean getShowPackageWarnings() {
    return showPackageWarnings;
  }

  /**
   * @return whether SDK warnings should be reported
   */
  public boolean getShowSdkWarnings() {
    return showSdkWarnings;
  }

  /**
   * @return whether we should print out the analyzer version
   */
  public boolean getShowVersion() {
    return showVersion;
  }

  /**
   * Returns the file passed to the analyzer.
   */
  public String getSourceFile() {
    return sourceFile;
  }

  /**
   * Return {@code true} if the analyzer should use the same resolution of dart: URI's as dart2js,
   * or {@code false} if it should use the resolution used by the VM.
   * 
   * @return {@code true} if the analyzer should use the dart2js paths when they are available
   */
  public boolean getUseDart2jsPaths() {
    return useDart2jsPaths;
  }

  /**
   * Return whether package: urls should be resolved by querying pub for a package map. This uses
   * pub's list-package-dirs command.
   * 
   * @return whether to use an alternative package resolver
   */
  public boolean getUsePackageMap() {
    return usePackageMap;
  }

  /**
   * @return whether both cold and warm performance statistics should be printed
   */
  public boolean getWarmPerf() {
    return warmPerf;
  }

  /**
   * Return whether warnings are reported as fatal errors. This is only useful for batch mode.
   * 
   * @return whether warnings are reported as fatal errors
   */
  public boolean getWarningsAreFatal() {
    return warningsAreFatal;
  }

  /**
   * Initialize the SDK path.
   */
  public void initializeSdkPath() {
    // current directory, one level up, and a "sdk" directory below us
    final String[] searchPath = new String[] {".", "..", "sdk"};

    if (dartSdkPath == null) {
      try {
        File cwd = new File(".").getCanonicalFile();

        for (String path : searchPath) {
          File dir = new File(cwd, path);

          dir = dir.getCanonicalFile();

          if (isSDKPath(dir)) {
            dartSdkPath = dir;

            return;
          }
        }
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }
  }

  public void setDartSdkPath(File dartSdkPath) {
    this.dartSdkPath = dartSdkPath;
  }

  public void setWarningsAreFatal(boolean value) {
    this.warningsAreFatal = value;
  }

  /**
   * Return {@code true} if the analyzer should be run in batch mode, {@code false} otherwise.
   * <p>
   * (In batch mode, command line arguments are received through stdin and returning pass/fail
   * status through stdout. Batch mode is used in test execution.)
   */
  public boolean shouldBatch() {
    return batch;
  }

  /**
   * Returns {@code true} to indicate printing the help message.
   */
  public boolean showHelp() {
    return showHelp;
  }

  private boolean isSDKPath(File sdkDirectory) {
    if (!sdkDirectory.exists()) {
      return false;
    }

    if (!sdkDirectory.getName().equals("dart-sdk") && !sdkDirectory.getName().equals("sdk")) {
      return false;
    }

    // check for (dart-sdk|sdk)/version
    if (new File(sdkDirectory, "version").exists()) {
      return true;
    }

    // check for (dart-sdk|sdk)/lib/_internal/libraries.dart
    File libDir = new File(sdkDirectory, "lib");

    if (libDir.exists()) {
      File _internalDir = new File(libDir, "_internal");

      if (_internalDir.exists()) {
        if (new File(_internalDir, "libraries.dart").exists()) {
          return true;
        }
      }
    }
    return false;
  }
}
