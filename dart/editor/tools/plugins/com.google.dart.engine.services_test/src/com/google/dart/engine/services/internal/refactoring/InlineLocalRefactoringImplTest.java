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
package com.google.dart.engine.services.internal.refactoring;

import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.refactoring.ExtractLocalRefactoring;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.services.status.RefactoringStatusSeverity;

/**
 * Test for {@link InlineLocalRefactoringImpl}.
 */
public class InlineLocalRefactoringImplTest extends RefactoringImplTest {
  private InlineLocalRefactoringImpl refactoring;
  private int selection;
  private RefactoringStatus refactoringStatus;

  public void test_access() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int test = 1 + 2;",
        "  print(test);",
        "}");
    selection = findOffset("test = ");
    createRefactoring();
    // validate
    assertEquals("Inline Local Variable", refactoring.getRefactoringName());
    assertEquals(1, refactoring.getReferenceCount());
    assertEquals("test", refactoring.getVariableName());
  }

  public void test_bad_selectionEmpty() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "}");
    selection = Integer.MAX_VALUE;
    createRefactoring();
    // check conditions
    assert_fatalError_selection();
  }

  public void test_bad_selectionMethod() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "}");
    selection = findOffset("main() {");
    createRefactoring();
    assert_fatalError_selection();
  }

  public void test_bad_selectionVariable_hasAssignments_1() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int test = 0;",
        "  test = 1;",
        "}");
    selection = findOffset("test = 0;");
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Local variable 'test' is assigned more than once.",
        findRangeIdentifier("test = 1"));
  }

  public void test_bad_selectionVariable_hasAssignments_2() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int test = 0;",
        "  test += 1;",
        "}");
    selection = findOffset("test = 0;");
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Local variable 'test' is assigned more than once.");
  }

  public void test_bad_selectionVariable_notInBlock() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true)",
        "    int test = 0;",
        "}");
    selection = findOffset("test =");
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Local variable declared in statement should be selected to activate this refactoring.");
  }

  public void test_bad_selectionVariable_notInitialized() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int test; // marker",
        "}");
    selection = findOffset("test;");
    createRefactoring();
    // check conditions
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Local variable 'test' is not initialized at declaration.",
        findRangeStartEnd("test;", "; // marker"));
  }

  public void test_bad_selectionVariable_parameter() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f(var test) {",
        "}");
    selection = findOffset("test) {");
    createRefactoring();
    assert_fatalError_selection();
  }

  public void test_OK_cascade_intoCascade() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {}",
        "  bar() {}",
        "}",
        "main() {",
        "  A test = new A()..foo();",
        "  test..bar();",
        "}");
    selection = findOffset("test = ");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {}",
        "  bar() {}",
        "}",
        "main() {",
        "  new A()..foo()..bar();",
        "}");
  }

  public void test_OK_cascade_intoNotCascade() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {}",
        "  bar() {}",
        "}",
        "main() {",
        "  A test = new A()..foo();",
        "  test.bar();",
        "}");
    selection = findOffset("test = ");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {}",
        "  bar() {}",
        "}",
        "main() {",
        "  (new A()..foo()).bar();",
        "}");
  }

  public void test_OK_intoStringInterpolation() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int test = 1 + 2;",
        "  print('test = $test');",
        "  print('test = ${test}');",
        "  print('test = ${process(test)}');",
        "}",
        "process(x) {}");
    selection = findOffset("test = ");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print('test = ${1 + 2}');",
        "  print('test = ${1 + 2}');",
        "  print('test = ${process(1 + 2)}');",
        "}",
        "process(x) {}");
  }

  /**
   * <p>
   * https://code.google.com/p/dart/issues/detail?id=18587
   */
  public void test_OK_keepNextCommentedLine() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int test = 1 + 2;",
        "  // foo",
        "  print(test);",
        "  // bar",
        "}");
    selection = findOffset("test = ");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  // foo",
        "  print(1 + 2);",
        "  // bar",
        "}");
  }

  public void test_OK_noUsages_1() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int test = 1 + 2;",
        "  print(0);",
        "}");
    selection = findOffset("test = ");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(0);",
        "}");
  }

  public void test_OK_noUsages_2() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int test = 1 + 2;",
        "}");
    selection = findOffset("test = ");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "}");
  }

  public void test_OK_oneUsage() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int test = 1 + 2;",
        "  print(test);",
        "}");
    selection = findOffset("test = ");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(1 + 2);",
        "}");
  }

  public void test_OK_twoUsages() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int test = 1 + 2;",
        "  print(test);",
        "  print(test);",
        "}");
    selection = findOffset("test = ");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(1 + 2);",
        "  print(1 + 2);",
        "}");
  }

  /**
   * Checks that all conditions are <code>OK</code> and applying {@link Change} to the
   * {@link #testUnit} is same source as given lines.
   */
  protected final void assertSuccessfulRefactoring(String... lines) throws Exception {
    assertRefactoringStatusOK(refactoringStatus);
    Change change = refactoring.createChange(pm);
    assertTestChangeResult(change, makeSource(lines));
  }

  @Override
  protected void tearDown() throws Exception {
    refactoring = null;
    refactoringStatus = null;
    super.tearDown();
  }

  private void assert_fatalError_selection() {
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Local variable declaration or reference must be selected to activate this refactoring.");
  }

  /**
   * Creates {@link ExtractLocalRefactoring} in {@link #refactoring}.
   */
  private void createRefactoring() throws Exception {
    AssistContext context = new AssistContext(
        searchEngine,
        analysisContext,
        null,
        testSource,
        testUnit,
        selection,
        0);
    refactoring = new InlineLocalRefactoringImpl(context);
    // prepare status
    refactoringStatus = refactoring.checkAllConditions(pm);
  }

  /**
   * Prints result of {@link #refactoring} in the way ready to parse into test expectations.
   */
  @SuppressWarnings("unused")
  private void printRefactoringResultSource() throws Exception {
    printRefactoringTestSourceResult(getAnalysisContext(), refactoring);
  }
}
