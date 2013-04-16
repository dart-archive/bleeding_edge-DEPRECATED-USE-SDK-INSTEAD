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
package com.google.dart.engine.integration;

import com.google.dart.engine.AnalysisEngine;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.FileUriResolver;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.util.ArrayList;

public class SamplesAnalysisTest extends DirectoryBasedSuiteBuilder {
  public class ReportingTest extends TestCase {
    public ReportingTest(String methodName) {
      super(methodName);
    }

    public void reportResults() throws Exception {
      System.out.print("Analyzed ");
      System.out.print(fileCount);
      System.out.print(" files in ");
      printTime(totalTime);
      System.out.println();

      System.out.print(skippedTests);
      System.out.println(" tests were skipped");

      System.out.print(errorCount);
      System.out.println(" tests failed with unexpected errors");

      System.out.print(noErrorCount);
      System.out.println(" tests failed with no errors being generated");
    }

    private void printTime(long time) {
      if (time == 0) {
        System.out.print("0 ms");
      } else {
        System.out.print(time);
        System.out.print(" ms");
        if (time > 60000) {
          long seconds = time / 1000;
          long minutes = seconds / 60;
          seconds -= minutes * 60;
          System.out.print(" (");
          System.out.print(minutes);
          System.out.print(":");
          if (seconds < 10) {
            System.out.print("0");
          }
          System.out.print(seconds);
          System.out.print(")");
        }
      }
    }
  }

  /**
   * An array containing the relative paths of sample dart files from the samples directory.
   */
  private static final String[] SAMPLES = {
      "clock/web/clock.dart", "solar/web/solar.dart", "sunflower/web/sunflower.dart",
      "swipe/web/swipe.dart", "time/time_server.dart"};

  /**
   * Build a JUnit test suite that will analyze all of the tests in the language test suite.
   * 
   * @return the test suite that was built
   */
  public static Test suite() {
    String svnRootName = System.getProperty("svnRoot");
    if (svnRootName != null) {
      File directory = new File(new File(svnRootName), "samples");
      if (directory.exists()) {
        SamplesAnalysisTest tester = new SamplesAnalysisTest();
        TestSuite suite = tester.buildSamplesSuite(directory, "Analyze sample files");
        suite.addTest(tester.new ReportingTest("reportResults"));
        return suite;
      }
    }
    return new TestSuite("Analyze sample files (no tests: directory not found)");
  }

  private long fileCount = 0L;

  private long totalTime = 0L;

  private int skippedTests = 0;

  public TestSuite buildSamplesSuite(File directory, String suiteName) {
    TestSuite suite = new TestSuite(suiteName);
    for (String sample : SAMPLES) {
      File file = new File(directory, sample);
      if (file.exists()) {
        addTestForFile(suite, file);
      } else {
        throw new IllegalStateException("Dart file does not exist at " + file.toString());
      }
    }
    return suite;
  }

  @Override
  protected void testSingleFile(File sourceFile) throws Exception {
    //
    // Create the analysis context in which the file will be analyzed.
    //
    DartSdk sdk = DirectoryBasedDartSdk.getDefaultSdk();
    SourceFactory sourceFactory = new SourceFactory(new DartUriResolver(sdk), new FileUriResolver());
    AnalysisContext context = AnalysisEngine.getInstance().createAnalysisContext();
    context.setSourceFactory(sourceFactory);
    //
    // Analyze the file.
    //
    Source source = new FileBasedSource(sourceFactory.getContentCache(), sourceFile);
    long startTime = System.currentTimeMillis();
    LibraryElement library = context.computeLibraryElement(source);
    long endTime = System.currentTimeMillis();
    if (library == null) {
      Assert.fail("Could not analyze " + sourceFile.getAbsolutePath());
    }
    //
    // Gather statistics.
    //
    fileCount++;
    totalTime += (endTime - startTime);
    //
    // Validate the results.
    //
    ElementStructureVerifier elementVerifier = new ElementStructureVerifier();
    library.accept(elementVerifier);
    elementVerifier.assertValid();

    ArrayList<AnalysisError> errorList = new ArrayList<AnalysisError>();
    addErrors(errorList, library.getDefiningCompilationUnit());
    for (CompilationUnitElement part : library.getParts()) {
      addErrors(errorList, part);
    }
    assertErrors(false, false, errorList);
  }
}
