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
package com.google.dart.engine.parser;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.scanner.CharSequenceReader;
import com.google.dart.engine.scanner.IncrementalScanner;
import com.google.dart.engine.scanner.Scanner;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.TestSource;
import com.google.dart.engine.utilities.ast.AstComparator;

public class IncrementalParserTest extends EngineTestCase {
  public void test_delete_everything() {
    // "f() => a + b;"
    // ""
    assertParse("", "f() => a + b;", "", "");
  }

  public void test_delete_identifier_beginning() {
    // "f() => abs + b;"
    // "f() => s + b;"
    assertParse("f() => ", "ab", "", "s + b;");
  }

  public void test_delete_identifier_end() {
    // "f() => abs + b;"
    // "f() => a + b;"
    assertParse("f() => a", "bs", "", " + b;");
  }

  public void test_delete_identifier_middle() {
    // "f() => abs + b;"
    // "f() => as + b;"
    assertParse("f() => a", "b", "", "s + b;");
  }

  public void test_delete_mergeTokens() {
    // "f() => a + b + c;"
    // "f() => ac;"
    assertParse("f() => a", " + b + ", "", "c;");
  }

  public void test_insert_afterIdentifier1() {
    // "f() => a + b;"
    // "f() => abs + b;"
    assertParse("f() => a", "", "bs", " + b;");
  }

  public void test_insert_afterIdentifier2() {
    // "f() => a + b;"
    // "f() => a + bar;"
    assertParse("f() => a + b", "", "ar", ";");
  }

  public void test_insert_beforeIdentifier1() {
    // "f() => a + b;"
    // "f() => xa + b;"
    assertParse("f() => ", "", "x", "a + b;");
  }

  public void test_insert_beforeIdentifier2() {
    // "f() => a + b;"
    // "f() => a + xb;"
    assertParse("f() => a + ", "", "x", "b;");
  }

  public void test_insert_convertOneFunctionToTwo() {
    // "f() {}"
    // "f() => 0; g() {}"
    assertParse("f()", "", " => 0; g()", " {}");
  }

  public void test_insert_end() {
    // "class A {}"
    // "class A {} class B {}"
    assertParse("class A {}", "", " class B {}", "");
  }

  public void test_insert_insideClassBody() {
    // "class C {C(); }"
    // "class C { C(); }"
    assertParse("class C {", "", " ", "C(); }");
  }

  public void test_insert_insideIdentifier() {
    // "f() => cob;"
    // "f() => cow.b;"
    assertParse("f() => co", "", "w.", "b;");
  }

  public void test_insert_newIdentifier1() {
    // "f() => a; c;"
    // "f() => a; b c;"
    assertParse("f() => a;", "", " b", " c;");
  }

  public void test_insert_newIdentifier2() {
    // "f() => a;  c;"
    // "f() => a;b  c;"
    assertParse("f() => a;", "", "b", "  c;");
  }

  public void test_insert_newIdentifier3() {
    // "/** A simple function. */ f() => a; c;"
    // "/** A simple function. */ f() => a; b c;"
    assertParse("/** A simple function. */ f() => a;", "", " b", " c;");
  }

  public void test_insert_newIdentifier4() {
    // "/** An [A]. */ class A {} class B { m() { return 1; } }"
    // "/** An [A]. */ class A {} class B { m() { return 1 + 2; } }"
    assertParse("/** An [A]. */ class A {} class B { m() { return 1", "", " + 2", "; } }");
  }

  public void test_insert_period() {
    // "f() => a + b;"
    // "f() => a + b.;"
    assertParse("f() => a + b", "", ".", ";");
  }

  public void test_insert_period_betweenIdentifiers1() {
    // "f() => a b;"
    // "f() => a. b;"
    assertParse("f() => a", "", ".", " b;");
  }

  public void test_insert_period_betweenIdentifiers2() {
    // "f() => a b;"
    // "f() => a .b;"
    assertParse("f() => a ", "", ".", "b;");
  }

  public void test_insert_period_betweenIdentifiers3() {
    // "f() => a  b;"
    // "f() => a . b;"
    assertParse("f() => a ", "", ".", " b;");
  }

  public void test_insert_period_insideExistingIdentifier() {
    // "f() => ab;"
    // "f() => a.b;"
    assertParse("f() => a", "", ".", "b;");
  }

  public void test_insert_periodAndIdentifier() {
    // "f() => a + b;"
    // "f() => a + b.x;"
    assertParse("f() => a + b", "", ".x", ";");
  }

  public void test_insert_simpleToComplexExression() {
    // "/** An [A]. */ class A {} class B { m() => 1; }"
    // "/** An [A]. */ class A {} class B { m() => 1 + 2; }"
    assertParse("/** An [A]. */ class A {} class B { m() => 1", "", " + 2", "; }");
  }

  public void test_insert_whitespace_end() {
    // "f() => a + b;"
    // "f() => a + b; "
    assertParse("f() => a + b;", "", " ", "");
  }

  public void test_insert_whitespace_end_multiple() {
    // "f() => a + b;"
    // "f() => a + b;  "
    assertParse("f() => a + b;", "", "  ", "");
  }

  public void test_insert_whitespace_middle() {
    // "f() => a + b;"
    // "f() => a  + b;"
    assertParse("f() => a", "", " ", " + b;");
  }

  public void test_replace_identifier_beginning() {
    // "f() => bell + b;"
    // "f() => fell + b;"
    assertParse("f() => ", "b", "f", "ell + b;");
  }

  public void test_replace_identifier_end() {
    // "f() => bell + b;"
    // "f() => belt + b;"
    assertParse("f() => bel", "l", "t", " + b;");
  }

  public void test_replace_identifier_middle() {
    // "f() => first + b;"
    // "f() => frost + b;"
    assertParse("f() => f", "ir", "ro", "st + b;");
  }

  public void test_replace_multiple_partialFirstAndLast() {
    // "f() => aa + bb;"
    // "f() => ab * ab;"
    assertParse("f() => a", "a + b", "b * a", "b;");
  }

  public void test_replace_operator_oneForMany() {
    // "f() => a + b;"
    // "f() => a * c - b;"
    assertParse("f() => a ", "+", "* c -", " b;");
  }

  public void test_replace_operator_oneForOne() {
    // "f() => a + b;"
    // "f() => a * b;"
    assertParse("f() => a ", "+", "*", " b;");
  }

  /**
   * Given a description of the original and modified contents, perform an incremental scan of the
   * two pieces of text.
   * 
   * @param prefix the unchanged text before the edit region
   * @param removed the text that was removed from the original contents
   * @param added the text that was added to the modified contents
   * @param suffix the unchanged text after the edit region
   */
  private void assertParse(String prefix, String removed, String added, String suffix) {
    //
    // Compute the information needed to perform the test.
    //
    String originalContents = prefix + removed + suffix;
    String modifiedContents = prefix + added + suffix;
    int replaceStart = prefix.length();
    Source source = new TestSource();
    //
    // Parse the original contents.
    //
    GatheringErrorListener originalListener = new GatheringErrorListener();
    Scanner originalScanner = new Scanner(
        source,
        new CharSequenceReader(originalContents),
        originalListener);
    Token originalTokens = originalScanner.tokenize();
    assertNotNull(originalTokens);
    Parser originalParser = new Parser(source, originalListener);
    CompilationUnit originalUnit = originalParser.parseCompilationUnit(originalTokens);
    assertNotNull(originalUnit);
    //
    // Parse the modified contents.
    //
    GatheringErrorListener modifiedListener = new GatheringErrorListener();
    Scanner modifiedScanner = new Scanner(
        source,
        new CharSequenceReader(modifiedContents),
        modifiedListener);
    Token modifiedTokens = modifiedScanner.tokenize();
    assertNotNull(modifiedTokens);
    Parser modifiedParser = new Parser(source, modifiedListener);
    CompilationUnit modifiedUnit = modifiedParser.parseCompilationUnit(modifiedTokens);
    assertNotNull(modifiedUnit);
    //
    // Incrementally parse the modified contents.
    //
    GatheringErrorListener incrementalListener = new GatheringErrorListener();
    IncrementalScanner incrementalScanner = new IncrementalScanner(source, new CharSequenceReader(
        modifiedContents), incrementalListener);
    Token incrementalTokens = incrementalScanner.rescan(
        originalTokens,
        replaceStart,
        removed.length(),
        added.length());
    assertNotNull(incrementalTokens);
    IncrementalParser incrementalParser = new IncrementalParser(
        source,
        incrementalScanner.getTokenMap(),
        incrementalListener);
    CompilationUnit incrementalUnit = incrementalParser.reparse(
        originalUnit,
        incrementalScanner.getLeftToken(),
        incrementalScanner.getRightToken(),
        replaceStart,
        prefix.length() + removed.length());
    assertNotNull(incrementalUnit);
    //
    // Validate that the results of the incremental parse are the same as the full parse of the
    // modified source.
    //
    assertTrue(AstComparator.equalUnits(modifiedUnit, incrementalUnit));
    // TODO(brianwilkerson) Verify that the errors are correct?
  }
}
