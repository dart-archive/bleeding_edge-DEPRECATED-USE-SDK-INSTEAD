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
package com.google.dart.engine.cmdline;

import com.google.dart.engine.cmdline.BatchRunner.Invocation;
import com.google.dart.engine.cmdline.CommandLineOptions.AnalyzerOptions;
import com.google.dart.engine.metrics.Tracer;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for the Dart command line analyzer.
 */
public class AnalyzerMain {
  private static final String PROGRAM_NAME = "dart-analyzer";

  /**
   * Invoke the compiler to build all of the files passed on the command line
   * 
   * @param analyzerOptions parsed command line arguments
   * @return <code> true</code> on success, <code>false</code> on failure.
   */
  public static boolean analyzerMain(AnalyzerOptions analyzerOptions) throws IOException {
    List<String> sourceFiles = analyzerOptions.getSourceFiles();
    if (sourceFiles.size() == 0) {
      System.err.println(PROGRAM_NAME + ": no source files were specified.");
      showUsage(null, System.err);
      return false;
    }

    Analyzer analyzer = new Analyzer();

    for (String sourceFilePath : sourceFiles) {
      File sourceFile = new File(sourceFilePath);
      if (!sourceFile.exists()) {
        System.err.println(PROGRAM_NAME + ": file not found: " + sourceFile);
        showUsage(null, System.err);
        return false;
      }
      CommandLineErrorListener listener = new CommandLineErrorListener(analyzerOptions);

      AnalysisConfiguration config = new CommandLineAnalysisConfiguration(analyzerOptions);
      String errorMessage = analyzer.analyze(sourceFile, config, listener);
      if (errorMessage != null) {
        System.err.println(errorMessage);
        return false;
      }
      listener.printFormattedErrors(System.out);
      listener.clearErrors();
    }
    return true;
  }

  public static void crash() {
    // Our test scripts look for 253 to signal a "crash".
    System.exit(253);
  }

  public static void main(final String[] topArgs) {
    Tracer.init();

    AnalyzerOptions topCompilerOptions = processCommandLineOptions(topArgs);
    boolean result = false;
    try {
      if (topCompilerOptions.shouldBatch()) {
        if (topArgs.length > 1) {
          System.err.println("(Extra arguments specified with -batch ignored.)");
        }
        BatchRunner.runAsBatch(topArgs, new Invocation() {
          @Override
          public boolean invoke(String[] lineArgs) throws Throwable {
            List<String> allArgs = new ArrayList<String>();
            for (String arg : topArgs) {
              if (!arg.equals("-batch")) {
                allArgs.add(arg);
              }
            }
            for (String arg : lineArgs) {
              allArgs.add(arg);
            }

            AnalyzerOptions compilerOptions = processCommandLineOptions(allArgs.toArray(new String[allArgs.size()]));
            if (compilerOptions.shouldBatch()) {
              System.err.println("-batch ignored: Already in batch mode.");
            }
            return analyzerMain(compilerOptions);
          }
        });
      } else {
        result = analyzerMain(topCompilerOptions);
      }
    } catch (Throwable t) {
      t.printStackTrace();
      crash();
    }
    if (!result) {
      System.exit(1);
    }
  }

  private static AnalyzerOptions processCommandLineOptions(String[] args) {
    CmdLineParser cmdLineParser = null;
    AnalyzerOptions compilerOptions = null;
    try {
      compilerOptions = new AnalyzerOptions();
      cmdLineParser = CommandLineOptions.parse(args, compilerOptions);
      if (args.length == 0 || compilerOptions.showHelp()) {
        showUsage(cmdLineParser, System.err);
        System.exit(1);
      }
    } catch (CmdLineException e) {
      System.err.println(e.getLocalizedMessage());
      showUsage(cmdLineParser, System.err);
      System.exit(1);
    }

    assert compilerOptions != null;
    return compilerOptions;
  }

  private static void showUsage(CmdLineParser cmdLineParser, PrintStream out) {
    out.println("Usage: " + PROGRAM_NAME + " [<options>] <dart-script> [script-arguments]");
    out.println("Available options:");
    if (cmdLineParser == null) {
      cmdLineParser = new CmdLineParser(new AnalyzerOptions());
    }
    cmdLineParser.printUsage(out);
  }
}
