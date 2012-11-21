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
import com.google.dart.engine.scanner.CharBufferScanner;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenStreamValidator;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceFactory;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.File;
import java.nio.CharBuffer;
import java.util.HashMap;

public class SDKAnalysisTest extends DirectoryBasedSuiteBuilder {
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
   * Build a JUnit test suite that will analyze all of the files in the SDK.
   * 
   * @return the test suite that was built
   */
  public static Test suite() {
    File directory = DartSdk.getDefaultSdkDirectory();
    SDKAnalysisTest tester = new SDKAnalysisTest();
    TestSuite suite = tester.buildSuite(directory, "Analyze SDK files");
    suite.addTest(tester.new ReportingTest("reportResults"));
    return suite;
  }

  private long fileCount = 0L;

  private long charCount = 0L;

  private long scannerTime = 0L;

  private long parserTime = 0L;

  @Override
  protected void testSingleFile(File sourceFile) throws Exception {
    //
    // Scan the file.
    //
    final CharBuffer[] buffer = new CharBuffer[1];
    Source source = new SourceFactory().forFile(sourceFile);
    source.getContents(new Source.ContentReceiver() {
      @Override
      public void accept(CharBuffer contents) {
        buffer[0] = contents;
      }

      @Override
      public void accept(String contents) {
      }
    });
    Assert.assertNotNull("Could not read file", buffer[0]);
    GatheringErrorListener listener = new GatheringErrorListener();
    CharBufferScanner scanner = new CharBufferScanner(source, buffer[0], listener);
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
    // Record the timing information.
    //
    fileCount++;
    charCount += buffer[0].length();
    scannerTime += (scannerEndTime - scannerStartTime);
    parserTime += (parserEndTime - parserStartTime);
    //
    // Validate that the token stream was built correctly.
    //
    new TokenStreamValidator().validate(token);
    //
    // Uncomment the lines below to stop reporting failures for files containing directives and for
    // files containing an interface.
    //
//    String contents = buffer.toString();
//    if (contents.indexOf("#") >= 0 || contents.indexOf("interface") >= 0) {
//      return;
//    }
    //
    // Validate the results.
    //
    listener.assertNoErrors();
    //
    // Validate that the AST structure was built correctly.
    //
    ASTValidator validator = new ASTValidator();
    unit.accept(validator);
    validator.assertValid();
    //
    // Build the element model for the compilation unit.
    //
    CompilationUnitBuilder builder = new CompilationUnitBuilder(
        new AnalysisContextImpl(),
        listener,
        new HashMap<ASTNode, Element>());
    CompilationUnitElement element = builder.buildCompilationUnit(source);
    Assert.assertNotNull(element);
  }
}
