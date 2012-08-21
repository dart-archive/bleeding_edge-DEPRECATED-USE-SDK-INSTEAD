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
package com.google.dart.engine.integration;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.parser.ASTValidator;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.parser.ParserErrorCode;
import com.google.dart.engine.scanner.StringScanner;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.utilities.io.FileUtilities;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.io.IOException;

public class Co19AnalysisTest extends DirectoryBasedSuiteBuilder {
  public class ReportingTest extends TestCase {
    public ReportingTest(String methodName) {
      super(methodName);
    }

    public void reportResults() throws Exception {
      System.out.println("Analyzed " + charCount + " characters in " + fileCount + " files");
      printTime("  combined: ", scannerTime + parserTime);
      printTime("  scanning: ", scannerTime);
      printTime("  parsing:  ", parserTime);
    }

    private void printTime(String label, long time) {
      long charsPerMs = charCount / time;
      System.out.println(label + time + " ms (" + charsPerMs + " chars / ms)");
    }
  }

  /**
   * Build a JUnit test suite that will analyze all of the tests in the co19 test suite.
   * 
   * @return the test suite that was built
   */
  public static Test suite() {
    String directoryName = System.getProperty("co19Directory");
    if (directoryName != null) {
      File directory = new File(directoryName);
      Co19AnalysisTest tester = new Co19AnalysisTest();
      TestSuite suite = tester.buildSuite(directory, "Analyze co19 files");
      suite.addTest(tester.new ReportingTest("reportResults"));
      return suite;
    }
    return new TestSuite("Analyze co19 files (no tests: directory not found)");
  }

  private long fileCount = 0L;

  private long charCount = 0L;

  private long scannerTime = 0L;

  private long parserTime = 0L;

  @Override
  protected void testSingleFile(File sourceFile) throws IOException {
    //
    // Determine whether the test is expected to pass or fail.
    //
    String contents = FileUtilities.getContents(sourceFile);
    boolean errorExpected = contents.indexOf("@compile-error") > 0
        || contents.indexOf("@static-warning") > 0;
    // Uncomment the lines below to stop reporting failures for files that are expected to contain
    // errors.
//    if (errorExpected) {
//      return;
//    }
    //
    // Scan the file, stopping if there were errors when expected.
    //
    Source source = new SourceFactory().forFile(sourceFile);
    GatheringErrorListener listener = new GatheringErrorListener();
    StringScanner scanner = new StringScanner(source, contents, listener);
    long scannerStartTime = System.currentTimeMillis();
    Token token = scanner.tokenize();
    long scannerEndTime = System.currentTimeMillis();
    if (listener.getErrors().size() > 0) {
      if (errorExpected) {
        return;
      } else {
        listener.assertNoErrors();
      }
    }
    //
    // Parse the file, stopping if there were errors when expected.
    //
    Parser parser = new Parser(source, listener);
    long parserStartTime = System.currentTimeMillis();
    CompilationUnit unit = parser.parseCompilationUnit(token);
    long parserEndTime = System.currentTimeMillis();
    //
    // Record the timing information.
    //
    fileCount++;
    charCount += contents.length();
    scannerTime += (scannerEndTime - scannerStartTime);
    parserTime += (parserEndTime - parserStartTime);
    //
    // Validate the results.
    //
    if (errorExpected) {
      Assert.assertTrue("Expected errors", listener.getErrors().size() > 0);
    } else {
      // Uncomment the lines below to stop reporting failures for files containing directives or
      // declarations of the operators 'equals' and 'negate'.
//      if (listener.hasError(ParserErrorCode.UNEXPECTED_TOKEN)
//          || listener.hasError(ParserErrorCode.OPERATOR_IS_NOT_USER_DEFINABLE)) {
//        return;
//      }
      listener.assertNoErrors();
    }
    //
    // Validate that the AST structure was built correctly.
    //
    ASTValidator validator = new ASTValidator();
    unit.accept(validator);
    validator.assertValid();
  }
}
