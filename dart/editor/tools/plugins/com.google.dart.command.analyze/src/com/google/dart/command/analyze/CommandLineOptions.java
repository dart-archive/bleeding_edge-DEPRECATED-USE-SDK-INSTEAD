/*
 * Copyright (c) 2012, the Dart project authors.
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
import com.google.dart.command.analyze.CommandLineErrorFormatter.ErrorFormat;

import org.kohsuke.args4j.Argument;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.util.List;

/**
 * Options that can be specified on the command line.
 */
public class CommandLineOptions {

  /**
   * Command line options accepted by the {@link AnalyzerMain} entry point.
   */
  public static class AnalyzerOptions {

    @Option(name = "--batch", aliases = {"-batch"}, usage = "Batch mode (for unit testing)")
    private boolean batch = false;

    @Option(name = "--error_format", usage = "Format errors as normal or machine")
    private String errorFormat = "";

    @Option(name = "--ignore_unrecognized_flags", aliases = {"--ignore-unrecognized-flags"}, //
    usage = "Ignore unrecognized command line flags")
    private boolean ignoreUnrecognizedFlags = false;

    @Option(name = "--help", aliases = {"-?", "-help"}, usage = "Prints this help message")
    private boolean showHelp = false;

    //TODO(pquitslund): how we specify metrics may well change (as will instrumentation)
    @Option(name = "--metrics", usage = "Print metrics")
    private boolean showMetrics = false;

    @Option(name = "--fatal_warnings", aliases = {"-Werror"}, //
    usage = "Treat non-type warnings as fatal")
    private boolean warningsAreFatal = false;

    @Option(name = "--dart_sdk", aliases = {"--dart-sdk"}, //
    usage = "Path to dart sdk.  (system property com.google.dart.sdk)")
    private File dartSdkPath = new File("Not Implemented");

    @Argument
    private final List<String> sourceFiles = Lists.newArrayList();

    /**
     * Return the path to the dart SDK.
     */
    public File dartSdkPath() {
      return dartSdkPath;
    }

    /**
     * Returns the list of files passed to the analyzer.
     */
    public List<String> getSourceFiles() {
      return sourceFiles;
    }

    /**
     * Return {@code true} if unrecognized flags should be ignored, {@code false} otherwise.
     */
    public boolean ignoreUnrecognizedFlags() {
      return ignoreUnrecognizedFlags;
    }

    /**
     * Return the format to use for printing errors.
     */
    public ErrorFormat printErrorFormat() {
      String lowerError = errorFormat.toLowerCase();
      if ("machine".equals(lowerError)) {
        return ErrorFormat.MACHINE;
      }
      return ErrorFormat.NORMAL;
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

    /**
     * Returns {@code true} to indicate that metrics should be displayed.
     */
    public boolean showMetrics() {
      return showMetrics;
    }

    /**
     * Returns whether warnings (excluding type warnings) are fatal.
     */
    public boolean warningsAreFatal() {
      return warningsAreFatal;
    }
  }

  /**
   * Parses command line options, handling the feature to ignore unrecognized flags. If one of the
   * options is 'ignore-unrecognized-flags', then any exceptions for 'not a valid option' are
   * suppressed.
   * 
   * @param args Arguments passed from main()
   * @param parsedOptions [out parameter] parsed options
   * @throws CmdLineException Thrown if there is a problem parsing the options.
   */
  public static CmdLineParser parse(String[] args, AnalyzerOptions parsedOptions)
      throws CmdLineException {
    boolean ignoreUnrecognized = false;
    for (String arg : args) {
      if (arg.equals("--ignore-unrecognized-flags") || arg.equals("--ignore_unrecognized_flags")) {
        ignoreUnrecognized = true;
        break;
      }
    }

    if (!ignoreUnrecognized) {
      CmdLineParser cmdLineParser = new CmdLineParser(parsedOptions);
      cmdLineParser.parseArgument(args);
      return cmdLineParser;
    }
    CmdLineParser cmdLineParser = new CmdLineParser(parsedOptions);
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
              System.out.println("(Ignoring unrecognized flag: " + arg + ")");
              continue;
            }
            newArgs.add(arg);
          }
          args = newArgs.toArray(new String[newArgs.size()]);
          cmdLineParser = new CmdLineParser(parsedOptions);
          continue;
        }
      }
      break;
    }
    return cmdLineParser;
  }
}
