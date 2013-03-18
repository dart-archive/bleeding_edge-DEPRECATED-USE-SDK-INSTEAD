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
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.ErrorSeverity;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
      showUsage(options, System.out);
      System.exit(0);
    }

    if (options.getShowVersion()) {
      showVersion(options, System.out);
      System.exit(0);
    }

    if (options.getDartSdkPath() == null) {
      System.out.println(PROGRAM_NAME + ": no Dart SDK found.");
      showUsage(options, System.out);
      System.exit(1);
    }

    if (!options.getDartSdkPath().exists()) {
      System.out.println(PROGRAM_NAME + ": invalid Dart SDK path: " + options.getDartSdkPath());
      showUsage(options, System.out);
      System.exit(1);
    }

    if (options.getSdkIndexLocation() != null) {
      AnalyzerImpl analyzer = new AnalyzerImpl(options);
      if (analyzer.createSdkIndex()) {
        System.exit(0);
      } else {
        System.exit(1);
      }
    }

    if (options.getRunTests()) {
      runTests(options);
      System.exit(0);
    }

    try {
      final AnalyzerImpl analyzer = new AnalyzerImpl(options);

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

            return runAnalyzer(analyzer, compilerOptions);
          }
        });

        if (result != ErrorSeverity.NONE) {
          System.exit(result.ordinal());
        }
      } else {
        String sourceFilePath = options.getSourceFile();

        if (sourceFilePath == null) {
          System.out.println(PROGRAM_NAME + ": no source files were specified.");
          showUsage(options, System.out);
          System.exit(1);
        }

        ErrorSeverity result = runAnalyzer(analyzer, options);

        if (result != ErrorSeverity.NONE) {
          System.exit(result.ordinal());
        }
      }
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
  protected static ErrorSeverity runAnalyzer(AnalyzerImpl analyzer, AnalyzerOptions options)
      throws IOException, AnalysisException {
    File sourceFile = new File(options.getSourceFile());

    if (!sourceFile.exists()) {
      System.out.println(PROGRAM_NAME + ": file not found: " + sourceFile);
      System.out.println();
      showUsage(options, System.out);
      return ErrorSeverity.ERROR;
    }

    ErrorFormatter formatter = new ErrorFormatter(options.getMachineFormat() ? System.err
        : System.out, options);

    List<AnalysisError> errors = new ArrayList<AnalysisError>();

    formatter.startAnalysis();

    ErrorSeverity status = analyzer.analyze(sourceFile, errors);

    formatter.formatErrors(errors);

    if (status.equals(ErrorSeverity.WARNING) && options.getWarningsAreFatal()) {
      status = ErrorSeverity.ERROR;
    }

    return status;
  }

  /**
   * If the unit tests are on our classpath, run them and exit with an appropriate status code
   * 
   * @param options
   */
  private static void runTests(AnalyzerOptions options) {
    // org.junit.runner.JUnitCore.main("com.google.dart.command.analyze.CombinedEngineTestSuite");

    try {
      // Load the main test suite using Class.forName(). This lets any jar minimization tools in the
      // build pipeline know that this class is referenced by reflection.
      Class.forName("com.google.dart.command.analyze.CombinedEngineTestSuite");

      Class<?> junitRunner = Class.forName("org.junit.runner.JUnitCore");

      Method mainMethod = junitRunner.getMethod("main", String[].class);

      System.setProperty("com.google.dart.sdk", options.getDartSdkPath().toString());

      Object mainArgs = new String[] {"com.google.dart.command.analyze.CombinedEngineTestSuite"};

      mainMethod.invoke(null, mainArgs);
    } catch (ClassNotFoundException ex) {
      System.out.println("Test classes not available in this build.");
      System.exit(1);
    } catch (SecurityException e) {
      System.out.println("Test classes not available in this build.");
      System.exit(1);
    } catch (NoSuchMethodException e) {
      System.out.println("Test classes not available in this build.");
      System.exit(1);
    } catch (IllegalArgumentException e) {
      e.printStackTrace();
      System.exit(1);
    } catch (IllegalAccessException e) {
      e.printStackTrace();
      System.exit(1);
    } catch (InvocationTargetException e) {
      e.printStackTrace();
      System.exit(1);
    }
  }

  private static void showUsage(AnalyzerOptions options, PrintStream out) {
    out.println("Usage: " + PROGRAM_NAME + " [<options>] <dart-script>");
    out.println();
    out.println("Options:");
    options.printUsage(out);
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
