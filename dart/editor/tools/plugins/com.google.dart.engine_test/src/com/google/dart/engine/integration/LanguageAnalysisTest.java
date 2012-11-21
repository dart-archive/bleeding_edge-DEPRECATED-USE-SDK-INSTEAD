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
import com.google.dart.engine.utilities.io.PrintStringWriter;

import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

public class LanguageAnalysisTest extends DirectoryBasedSuiteBuilder {
  public class AnalysisTestWithSource extends AnalysisTest {
    private int index;

    private String contents;

    public AnalysisTestWithSource(File sourceFile, int index, String contents) {
      super(sourceFile);
      this.index = index;
      this.contents = contents;
    }

    @Override
    public void testFile() throws Exception {
      testSingleFile(getSourceFile(), contents);
    }

    @Override
    protected String getTestName() {
      if (index >= 0) {
        return getSourceFile().getName() + ":" + index;
      }
      return getSourceFile().getName();
    }
  }

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
    String directoryName = System.getProperty("languageDirectory");
    if (directoryName != null) {
      File directory = new File(directoryName);
      LanguageAnalysisTest tester = new LanguageAnalysisTest();
      TestSuite suite = tester.buildSuite(directory, "Analyze language files");
      suite.addTest(tester.new ReportingTest("reportResults"));
      return suite;
    }
    return new TestSuite("Analyze language files (no tests: directory not found)");
  }

  private long fileCount = 0L;

  private long charCount = 0L;

  private long scannerTime = 0L;

  private long parserTime = 0L;

  @Override
  protected void addTestForFile(TestSuite suite, File file) {
    //
    // Determine how many tests to create from this one file.
    //
    try {
      String contents = FileUtilities.getContents(file);
      if (contents.indexOf("///") < 0) {
        suite.addTest(new AnalysisTestWithSource(file, -1, contents));
        return;
      }
      String[] lines = toLines(contents);
      int count = getTestCount(lines);
      for (int i = 0; i < count; i++) {
        String testSource = getTestSource(i, lines);
        suite.addTest(new AnalysisTestWithSource(file, i, testSource));
      }
    } catch (IOException exception) {
      suite.addTest(new TestSuite("Analyze " + file.getAbsolutePath() + " (could not read file)"));
    }
  }

  @Override
  protected void testSingleFile(File sourceFile) throws IOException {
    // This method should never be called.
    throw new InternalError("Wrong test method invoked for file " + sourceFile.getAbsolutePath());
  }

  protected void testSingleFile(File sourceFile, String contents) throws Exception {
    //
    // Uncomment the lines below to stop reporting failures for files containing directives or
    // interface declarations.
    //
    if (contents.indexOf("#library") >= 0 || contents.indexOf("#import") >= 0
        || contents.indexOf("#source") >= 0 || contents.indexOf("interface") >= 0) {
      return;
    }
    //
    // Determine whether the test is expected to pass or fail.
    //
    boolean errorExpected = contents.indexOf("compile-time error") > 0
        || contents.indexOf("static type warning") > 0 || contents.indexOf("static warning") > 0;
    // Uncomment the lines below to stop reporting failures for files that are expected to contain
    // errors.
    if (errorExpected) {
      return;
    }
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
    // Record the timing information.
    //
    fileCount++;
    charCount += contents.length();
    scannerTime += (scannerEndTime - scannerStartTime);
    parserTime += (parserEndTime - parserStartTime);
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
      listener.assertNoErrors();
    }
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

  private int getTestCount(String[] lines) {
    int maxIndex = 0;
    for (int i = 0; i < lines.length; i++) {
      int testIndex = parseTestIndex(lines[i]);
      maxIndex = Math.max(maxIndex, testIndex);
    }
    return maxIndex + 1;
  }

  private String getTestSource(int testIndex, String[] lines) {
    PrintStringWriter writer = new PrintStringWriter();
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      int index = parseTestIndex(line);
      if (index < 0 || index == testIndex) {
        writer.println(line);
      }
    }
    return writer.toString();
  }

  private int parseTestIndex(String line) {
    int index = line.indexOf("///");
    if (index < 0) {
      return -1;
    }
    int length = line.length();
    while (index < length && Character.isWhitespace(line.charAt(index))) {
      index++;
    }
    int testIndex = 0;
    while (index < length && Character.isDigit(line.charAt(index))) {
      testIndex = (testIndex * 10) + Character.digit(line.charAt(index++), 10);
    }
    return testIndex;
  }

  private String[] toLines(String source) {
    ArrayList<String> lines = new ArrayList<String>();
    BufferedReader reader = new BufferedReader(new StringReader(source));
    String line;
    try {
      line = reader.readLine();
      while (line != null) {
        lines.add(line);
        line = reader.readLine();
      }
    } catch (IOException exception) {
      // This cannot happen because we are reading from a string, not from an external source.
    }
    return lines.toArray(new String[lines.size()]);
  }
}
