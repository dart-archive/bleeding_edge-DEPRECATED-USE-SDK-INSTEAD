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
package com.google.dart.engine.services.internal.correction;

import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.utilities.source.SourceRange;

import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartEnd;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartStart;

import static org.fest.assertions.Assertions.assertThat;

public class StatementAnalyzerTest extends AbstractDartTest {
  public void test_DoStatement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  do { // marker",
        "  } while (true);",
        "}",
        "");
    // analyze selection
    SourceRange selection = rangeStartEnd(findOffset("do {"), findEnd("true"));
    StatementAnalyzer analyzer = new StatementAnalyzer(testUnit, selection);
    testUnit.accept(analyzer);
    // FATAL
    RefactoringStatus status = analyzer.getStatus();
    assertTrue(status.hasFatalError());
    assertEquals(
        "Operation not applicable to a 'do' statement's body and expression.",
        status.getMessage());
    assertFalse(analyzer.hasSelectedNodes());
  }

  public void test_ForStatement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int i;",
        "  for (i = 0; i < 10; i++) {",
        "    print(i);",
        "  } // marker",
        "}",
        "");
    // initialization + condition
    {
      SourceRange selection = rangeStartEnd(findOffset("i = 0"), findEnd("i < 10"));
      StatementAnalyzer analyzer = new StatementAnalyzer(testUnit, selection);
      testUnit.accept(analyzer);
      // FATAL
      RefactoringStatus status = analyzer.getStatus();
      assertTrue(status.hasFatalError());
      assertEquals(
          "Operation not applicable to a 'for' statement's initializer and condition.",
          status.getMessage());
    }
    // condition + updates
    {
      SourceRange selection = rangeStartEnd(findOffset("i < 10"), findEnd("i++"));
      StatementAnalyzer analyzer = new StatementAnalyzer(testUnit, selection);
      testUnit.accept(analyzer);
      // FATAL
      RefactoringStatus status = analyzer.getStatus();
      assertTrue(status.hasFatalError());
      assertEquals(
          "Operation not applicable to a 'for' statement's condition and updaters.",
          status.getMessage());
    }
    // updates + body
    {
      SourceRange selection = rangeStartEnd(findOffset("i++"), findOffset(" // marker"));
      StatementAnalyzer analyzer = new StatementAnalyzer(testUnit, selection);
      testUnit.accept(analyzer);
      // FATAL
      RefactoringStatus status = analyzer.getStatus();
      assertTrue(status.hasFatalError());
      assertEquals(
          "Operation not applicable to a 'for' statement's updaters and body.",
          status.getMessage());
    }
  }

  public void test_ForStatement_variableInitializer() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  for (int i = 0; i < 10; i++) {",
        "    print(i);",
        "  } // marker",
        "}",
        "");
    // initialization + condition
    {
      SourceRange selection = rangeStartEnd(findOffset("int i = 0"), findEnd("i < 10"));
      StatementAnalyzer analyzer = new StatementAnalyzer(testUnit, selection);
      testUnit.accept(analyzer);
      // FATAL
      RefactoringStatus status = analyzer.getStatus();
      assertTrue(status.hasFatalError());
      assertEquals(
          "Operation not applicable to a 'for' statement's initializer and condition.",
          status.getMessage());
    }
  }

  public void test_OK() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var a;",
        "  var b;",
        "}",
        "");
    Statement statementA = findNode("var a", Statement.class);
    Statement statementB = findNode("var b", Statement.class);
    // analyze selection
    SourceRange selection = rangeStartStart(statementA, statementB);
    StatementAnalyzer analyzer = new StatementAnalyzer(testUnit, selection);
    testUnit.accept(analyzer);
    // OK
    assertTrue(analyzer.getStatus().isOK());
    assertThat(analyzer.getSelectedNodes()).containsExactly(statementA);
  }

  public void test_selectionEndsInComment() throws Exception {
    // TODO(scheglov) restore "filler" when CompilationUnit.getBeginToken() will be fixed
    parseTestUnit(
//        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "// start",
        "  print(0);",
        "/*",
        " end",
        "*/",
        "}",
        "");
    // analyze selection
    SourceRange selection = rangeStartEnd(findOffset("print(0)"), findEnd("end"));
    StatementAnalyzer analyzer = new StatementAnalyzer(testUnit, selection);
    testUnit.accept(analyzer);
    // FATAL
    RefactoringStatus status = analyzer.getStatus();
    assertTrue(status.hasFatalError());
    assertEquals("Selection ends inside a comment.", status.getMessage());
    assertFalse(analyzer.hasSelectedNodes());
  }

  public void test_selectionStartsInComment() throws Exception {
    // TODO(scheglov) restore "filler" when CompilationUnit.getBeginToken() will be fixed
    parseTestUnit(
//        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "/*",
        " start",
        "*/",
        "  print(0);",
        "// end",
        "}",
        "");
    // analyze selection
    SourceRange selection = rangeStartEnd(findOffset("start"), findEnd("print(0);"));
    StatementAnalyzer analyzer = new StatementAnalyzer(testUnit, selection);
    testUnit.accept(analyzer);
    // FATAL
    RefactoringStatus status = analyzer.getStatus();
    assertTrue(status.hasFatalError());
    assertEquals("Selection begins inside a comment.", status.getMessage());
    assertFalse(analyzer.hasSelectedNodes());
  }

  public void test_SwitchStatement() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  switch (0) {",
        "    case 0:",
        "      break;",
        "  }",
        "}",
        "");
    // analyze selection
    SourceRange selection = rangeStartEnd(findOffset("case 0:"), findEnd("break;"));
    StatementAnalyzer analyzer = new StatementAnalyzer(testUnit, selection);
    testUnit.accept(analyzer);
    // FATAL
    RefactoringStatus status = analyzer.getStatus();
    assertTrue(status.hasFatalError());
    assertEquals(
        "Selection must either cover whole switch statement or parts of a single case block.",
        status.getMessage());
    assertFalse(analyzer.hasSelectedNodes());
  }

  public void test_tokenAfterLastNode() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(42);",
        "}",
        "");
    // analyze selection
    SourceRange selection = rangeStartEnd(findOffset("42"), findEnd("42)"));
    StatementAnalyzer analyzer = new StatementAnalyzer(testUnit, selection);
    testUnit.accept(analyzer);
    // FATAL
    RefactoringStatus status = analyzer.getStatus();
    assertTrue(status.hasFatalError());
    assertEquals(
        "The end of the selection contains characters that do not belong to a statement.",
        status.getMessage());
    assertFalse(analyzer.hasSelectedNodes());
  }

  public void test_tokenBeforeFirstNode() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(42);",
        "}",
        "");
    // analyze selection
    SourceRange selection = rangeStartEnd(findOffset("(42"), findEnd("42"));
    StatementAnalyzer analyzer = new StatementAnalyzer(testUnit, selection);
    testUnit.accept(analyzer);
    // FATAL
    RefactoringStatus status = analyzer.getStatus();
    assertTrue(status.hasFatalError());
    assertEquals(
        "The beginning of the selection contains characters that do not belong to a statement.",
        status.getMessage());
    assertFalse(analyzer.hasSelectedNodes());
  }

  public void test_TryStatement_catch() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  try { // mark-1",
        "  } catch (e) { // mark-2",
        "  } // mark-3",
        "}",
        "");
    // FATAL - catch
    {
      SourceRange selection = rangeStartEnd(findOffset("catch (e)"), findOffset("} // mark-3") + 1);
      StatementAnalyzer analyzer = new StatementAnalyzer(testUnit, selection);
      testUnit.accept(analyzer);
      RefactoringStatus status = analyzer.getStatus();
      assertTrue(status.hasFatalError());
      assertEquals(
          "Selection must either cover whole try statement or parts of try, catch, or finally block.",
          status.getMessage());
      assertFalse(analyzer.hasSelectedNodes());
    }
    // FATAL - catch block
    {
      SourceRange selection = rangeStartEnd(
          findOffset("{ // mark-2"),
          findOffset("} // mark-3") + 1);
      StatementAnalyzer analyzer = new StatementAnalyzer(testUnit, selection);
      testUnit.accept(analyzer);
      RefactoringStatus status = analyzer.getStatus();
      assertTrue(status.hasFatalError());
      assertEquals(
          "Selection must either cover whole try statement or parts of try, catch, or finally block.",
          status.getMessage());
      assertFalse(analyzer.hasSelectedNodes());
    }
    // FATAL - catch parameter
    {
      SourceRange selection = rangeStartEnd(findOffset("(e)") + 1, findEnd("e)"));
      StatementAnalyzer analyzer = new StatementAnalyzer(testUnit, selection);
      testUnit.accept(analyzer);
      RefactoringStatus status = analyzer.getStatus();
      assertTrue(status.hasFatalError());
      assertEquals(
          "Selection must either cover whole try statement or parts of try, catch, or finally block.",
          status.getMessage());
      assertFalse(analyzer.hasSelectedNodes());
    }
  }

  public void test_TryStatement_finally() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  try { // mark-1",
        "  } finally { // mark-2",
        "  } // mark-3",
        "}",
        "");
    // FATAL - body
    {
      SourceRange selection = rangeStartEnd(findOffset("{ // mark-1"), findOffset("} finally") + 1);
      StatementAnalyzer analyzer = new StatementAnalyzer(testUnit, selection);
      testUnit.accept(analyzer);
      RefactoringStatus status = analyzer.getStatus();
      assertTrue(status.hasFatalError());
      assertEquals(
          "Selection must either cover whole try statement or parts of try, catch, or finally block.",
          status.getMessage());
      assertFalse(analyzer.hasSelectedNodes());
    }
    // FATAL - finally
    {
      SourceRange selection = rangeStartEnd(
          findOffset("{ // mark-2"),
          findOffset("} // mark-3") + 1);
      StatementAnalyzer analyzer = new StatementAnalyzer(testUnit, selection);
      testUnit.accept(analyzer);
      RefactoringStatus status = analyzer.getStatus();
      assertTrue(status.hasFatalError());
      assertEquals(
          "Selection must either cover whole try statement or parts of try, catch, or finally block.",
          status.getMessage());
      assertFalse(analyzer.hasSelectedNodes());
    }
  }

  public void test_WhileStatement_selectionCoveredBy() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  while (true) {",
        "  } // marker",
        "}",
        "");
    // analyze selection
    SourceRange selection = rangeStartEnd(findOffset("true"), findOffset(" // marker"));
    StatementAnalyzer analyzer = new StatementAnalyzer(testUnit, selection);
    testUnit.accept(analyzer);
    // FATAL
    RefactoringStatus status = analyzer.getStatus();
    assertTrue(status.hasFatalError());
    assertEquals(
        "Operation not applicable to a while statement's expression and body.",
        status.getMessage());
    assertFalse(analyzer.hasSelectedNodes());
  }

  public void test_WhileStatement_selectionEndsAfter() throws Exception {
    parseTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  while (true) {",
        "  }",
        "} // marker",
        "");
    // analyze selection
    SourceRange selection = rangeStartEnd(findOffset("true"), findOffset("} // marker"));
    StatementAnalyzer analyzer = new StatementAnalyzer(testUnit, selection);
    testUnit.accept(analyzer);
    // FATAL
    RefactoringStatus status = analyzer.getStatus();
    assertTrue(status.hasFatalError());
    assertEquals(
        "Operation not applicable to a while statement's expression and body.",
        status.getMessage());
    assertFalse(analyzer.hasSelectedNodes());
  }
}
