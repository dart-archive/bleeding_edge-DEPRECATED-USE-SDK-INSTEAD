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

import com.google.dart.command.analyze.BatchRunner.BatchRunnerInvocation;
import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorSeverity;
import com.google.dart.engine.internal.context.PerformanceStatistics;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Entry point for the Dart command line analyzer.
 */
public class AnalyzerMain {
  public static final String PROGRAM_NAME = "dartanalyzer";

  /**
   * @return the version of the dart-analyzer tool
   */
  public static String getBuildVersion() {
    final String fallbackVersion = "0.0.0";

    Package analyzerPackage = AnalyzerMain.class.getPackage();

    if (analyzerPackage.getImplementationVersion() == null) {
      return fallbackVersion;
    } else {
      return analyzerPackage.getImplementationVersion();
    }
  }

  public static void main(final String[] args) {
    final AnalyzerOptions options = AnalyzerOptions.createFromArgs(args);

    options.initializeSdkPath();

    if (args.length == 0 || options.showHelp()) {
      showVersion(options, System.out);
      System.out.println();
      showUsage(System.out);
      System.out.println("For more information, see http://www.dartlang.org/tools/analyzer.");
      System.exit(0);
    }

    if (options.getShowVersion()) {
      showVersion(options, System.out);
      System.exit(0);
    }

    if (options.getDartSdkPath() == null) {
      System.out.println(PROGRAM_NAME + ": no Dart SDK found.");
      showUsage(System.out);
      System.exit(1);
    }

    if (!options.getDartSdkPath().exists()) {
      System.out.println(PROGRAM_NAME + ": invalid Dart SDK path: " + options.getDartSdkPath());
      showUsage(System.out);
      System.exit(1);
    }

    try {

      if (options.shouldBatch()) {
        ErrorSeverity result = BatchRunner.runAsBatch(args, new BatchRunnerInvocation() {
          @Override
          public ErrorSeverity invoke(String[] lineArgs) throws Throwable {
            AnalyzerOptions compilerOptions = AnalyzerOptions.createFromArgs(lineArgs);

            if (compilerOptions.getDartSdkPath() == null) {
              compilerOptions.setDartSdkPath(options.getDartSdkPath());
            }

            if (options.getWarningsAreFatal()) {
              compilerOptions.setWarningsAreFatal(true);
            }

            return runAnalyzer(compilerOptions);
          }
        });

        if (result != ErrorSeverity.NONE) {
          System.exit(getReturnCode(result));
        }
      } else {
        String sourceFilePath = options.getSourceFile();

        if (sourceFilePath == null) {
          System.out.println(PROGRAM_NAME + ": no source files were specified.");
          showUsage(System.out);
          System.exit(1);
        }

        ErrorSeverity result = runAnalyzer(options);

        if (result != ErrorSeverity.NONE) {
          System.exit(getReturnCode(result));
        }
      }
    } catch (AnalysisException exception) {
      System.err.println("Error: " + exception.getMessage());

      crashAndExit();
    } catch (Throwable t) {
      t.printStackTrace();

      crashAndExit();
    }
  }

  protected static void crashAndExit() {
    // Our test scripts look for 253 to signal a "crash".

    System.exit(253);
  }

  /**
   * Invoke the compiler to build all of the files passed on the command line
   * 
   * @param analyzerOptions parsed command line arguments
   * @return {@code  true} on success, {@code false} on failure.
   */
  protected static ErrorSeverity runAnalyzer(AnalyzerOptions options) throws IOException,
      AnalysisException {
    File sourceFile = new File(options.getSourceFile());

    if (!sourceFile.exists()) {
      System.out.println("File not found: " + sourceFile);
      System.out.println();
      showUsage(System.out);
      return ErrorSeverity.ERROR;
    }

    // TODO: also support analyzing html files (via AnalysisEngine.isHtmlFileName())
    if (!AnalysisEngine.isDartFileName(sourceFile.getName())) {
      System.out.println(sourceFile + " is not a Dart file");
      System.out.println();
      showUsage(System.out);
      return ErrorSeverity.ERROR;
    }

    ErrorFormatter formatter = new ErrorFormatter(options.getMachineFormat() ? System.err
        : System.out, options);

    List<AnalysisError> errors = new ArrayList<AnalysisError>();

    formatter.startAnalysis();

    long startTime = System.currentTimeMillis();
    AnalyzerImpl analyzer = new AnalyzerImpl(options);
    ErrorSeverity status = analyzer.analyze(sourceFile, errors);

    formatter.formatErrors(errors);

    if (status.equals(ErrorSeverity.WARNING) && options.getWarningsAreFatal()) {
      status = ErrorSeverity.ERROR;
    }

    if (options.getPerf()) {
      long totalTime = System.currentTimeMillis() - startTime;
      System.out.println("scan:" + PerformanceStatistics.scan.getResult());
      System.out.println("parse:" + PerformanceStatistics.parse.getResult());
      System.out.println("resolve:" + PerformanceStatistics.resolve.getResult());
      System.out.println("errors:" + PerformanceStatistics.errors.getResult());
      System.out.println("hints:" + PerformanceStatistics.hints.getResult());
      System.out.println("total:" + totalTime);
    }

    return status;
  }

  /**
   * Return the return code appropriate for the given severity.
   * 
   * @param severity the severity of the most severe error that was reported
   * @return the return code that should be used returned by the analyzer
   */
  private static int getReturnCode(ErrorSeverity severity) {
    if (severity == ErrorSeverity.WARNING) {
      return 1;
    } else if (severity == ErrorSeverity.ERROR) {
      return 2;
    }
    return 0;
  }

  private static void showUsage(PrintStream out) {
    out.println("Usage: " + PROGRAM_NAME + " [<options>] <dart-script>");
    out.println();
    out.println("Options:");
    AnalyzerOptions.printUsage(out);
    out.println();
    out.println("Exit codes:");
    out.println(" 0: No analysis issues found");
    out.println(" 1: Analysis warnings encountered");
    out.println(" 2: Analysis errors encountered");
    out.println();
  }

  private static void showVersion(AnalyzerOptions options, PrintStream out) {
    out.println(PROGRAM_NAME + " version " + getBuildVersion());
  }

}
