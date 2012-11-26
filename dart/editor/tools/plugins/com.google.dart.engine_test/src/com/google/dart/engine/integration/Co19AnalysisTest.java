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

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.builder.CompilationUnitBuilder;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.parser.ASTValidator;
import com.google.dart.engine.parser.Parser;
import com.google.dart.engine.scanner.StringScanner;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenStreamValidator;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.utilities.io.FileUtilities;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.util.HashMap;

public class Co19AnalysisTest extends DirectoryBasedSuiteBuilder {
  public class ReportingTest extends TestCase {
    public ReportingTest(String methodName) {
      super(methodName);
    }

    public void reportResults() throws Exception {
      System.out.println("Analyzed " + charCount + " characters in " + fileCount + " files");
      printTime("  scanning: ", scannerTime);
      printTime("  parsing:  ", parserTime);
      printTime("  lexical:  ", scannerTime + parserTime);
      printTime("  building: ", builderTime);
      printTime("  total:    ", scannerTime + parserTime + builderTime);
    }

    private void printTime(String label, long time) {
      if (time == 0) {
        System.out.println(label + "0 ms ");
      } else {
        long charsPerMs = charCount / time;
        System.out.println(label + time + " ms (" + charsPerMs + " chars / ms)");
      }
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

  private long builderTime = 0L;

  @Override
  protected void testSingleFile(File sourceFile) throws Exception {
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
    // Scan the file.
    //
    Source source = new SourceFactory().forFile(sourceFile);
    GatheringErrorListener listener = new GatheringErrorListener();
    StringScanner scanner = new StringScanner(source, contents, listener);
    long scannerStartTime = System.currentTimeMillis();
    Token token = scanner.tokenize();
    long scannerEndTime = System.currentTimeMillis();
    //
    // Parse the file.
    //
    Parser parser = new Parser(source, listener);
    long parserStartTime = System.currentTimeMillis();
    final CompilationUnit unit = parser.parseCompilationUnit(token);
    long parserEndTime = System.currentTimeMillis();
    //
    // Build the element model for the compilation unit.
    //
    CompilationUnitBuilder builder = new CompilationUnitBuilder(
        new AnalysisContextImpl(),
        listener,
        new HashMap<ASTNode, Element>());
    long builderStartTime = System.currentTimeMillis();
    CompilationUnitElement element = builder.buildCompilationUnit(source);
    long builderEndTime = System.currentTimeMillis();
    //
    // Record the timing information.
    //
    fileCount++;
    charCount += contents.length();
    scannerTime += (scannerEndTime - scannerStartTime);
    parserTime += (parserEndTime - parserStartTime);
    builderTime += (builderEndTime - builderStartTime);
    //
    // Validate that the token stream was built correctly.
    //
    new TokenStreamValidator().validate(token);
    //
    // Validate the results.
    //
    if (errorExpected) {
      Assert.assertTrue("Expected errors", listener.getErrors().size() > 0);
    } else {
      // Uncomment the lines below to stop reporting failures for files containing directives or
      // declarations of the operators 'equals' and 'negate'.
//      if (listener.hasError(ParserErrorCode.UNEXPECTED_TOKEN)
//          || listener.hasError(ParserErrorCode.NON_USER_DEFINABLE_OPERATOR)) {
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
    //
    // Validate that the element model was built.
    //
    Assert.assertNotNull(element);
  }
}
