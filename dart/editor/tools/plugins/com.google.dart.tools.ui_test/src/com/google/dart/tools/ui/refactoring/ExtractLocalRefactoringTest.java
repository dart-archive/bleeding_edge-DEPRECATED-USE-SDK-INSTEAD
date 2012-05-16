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
package com.google.dart.tools.ui.refactoring;

import com.google.dart.compiler.ast.DartBinaryExpression;
import com.google.dart.tools.internal.corext.refactoring.code.ExtractLocalRefactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Test for {@link ExtractLocalRefactoring}.
 */
public final class ExtractLocalRefactoringTest extends RefactoringTest {
  private static final IProgressMonitor pm = new NullProgressMonitor();

  private int selectionStart;
  private int selectionEnd;
  private ExtractLocalRefactoring refactoring;
  private RefactoringStatus refactoringStatus;

  public void test_fragmentExpression_leadingNotWhitespace() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 + 2 + 3 + 4;",
        "}");
    selectionStart = findOffset("+ 2");
    selectionEnd = findOffset("3 + ") + 1;
    // check conditions
    createRefactoring("res");
    assert_fatalError_selection();
  }

  public void test_fragmentExpression_leadingPartialSelection() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 111 + 2 + 3 + 4;",
        "}");
    selectionStart = findOffset("11 +");
    selectionEnd = findOffset("3 +") + 3;
    // check conditions
    createRefactoring("res");
    assert_fatalError_selection();
  }

  public void test_fragmentExpression_leadingWhitespace() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 + 2 + 3 + 4;",
        "}");
    selectionStart = findOffset(" 2");
    selectionEnd = findOffset("3 + ") + 1;
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int res =  2 + 3;",
        "  int a = 1 +res + 4;",
        "}");
  }

  public void test_fragmentExpression_notAssociativeOperator() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 - 2 - 3 - 4;",
        "}");
    selectionStart = findOffset("2");
    selectionEnd = findOffset("3 - ") + 1;
    // check conditions
    createRefactoring("res");
    assert_fatalError_selection();
  }

  public void test_fragmentExpression_OK() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 + 2 + 3 + 4;",
        "}");
    selectionStart = findOffset("2");
    selectionEnd = findOffset("3 + ") + 1;
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int res = 2 + 3;",
        "  int a = 1 + res + 4;",
        "}");
  }

  public void test_fragmentExpression_trailingNotWhitespace() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 + 2 + 3 + 4;",
        "}");
    selectionStart = findOffset("2");
    selectionEnd = findOffset("3 +") + 3;
    // check conditions
    createRefactoring("res");
    assert_fatalError_selection();
  }

  public void test_fragmentExpression_trailingPartialSelection() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 + 2 + 3 + 444;",
        "}");
    selectionStart = findOffset("2");
    selectionEnd = findOffset("44;");
    // check conditions
    createRefactoring("res");
    assert_fatalError_selection();
  }

  public void test_fragmentExpression_trailingWhitespace() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 + 2 + 3 + 4;",
        "}");
    selectionStart = findOffset("2");
    selectionEnd = findOffset("3 + ") + 2;
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int res = 2 + 3 ;",
        "  int a = 1 + res+ 4;",
        "}");
  }

  public void test_singleExpression() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 + 2;",
        "}");
    selectionStart = findOffset("1");
    selectionEnd = findOffset("2;") + 1;
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int res = 1 + 2;",
        "  int a = res;",
        "}");
  }

  public void test_singleExpression_Dynamic() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {}",
        "main() {",
        "  int a = f(); // marker",
        "}");
    selectionStart = findOffset("f();");
    selectionEnd = findOffset("; // marker");
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {}",
        "main() {",
        "  var res = f();",
        "  int a = res; // marker",
        "}");
  }

  public void test_singleExpression_leadingNotWhitespace() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 12 + 345; // marker",
        "}");
    selectionStart = findOffset("345") - 2;
    selectionEnd = findOffset("; // marker");
    // check conditions
    createRefactoring("res");
    assert_fatalError_selection();
  }

  public void test_singleExpression_leadingWhitespace() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 + 2; // marker",
        "}");
    selectionStart = findOffset(" 1");
    selectionEnd = findOffset("; // marker");
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int res =  1 + 2;",
        "  int a =res; // marker",
        "}");
  }

  /**
   * We use here knowledge how exactly <code>1 + 2 + 3 + 4</code> is parsed. We know that
   * <code>1 + 2</code> will be separate and complete {@link DartBinaryExpression}, so can be
   * handled as single expression.
   */
  public void test_singleExpression_partOfBinaryExpression() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 + 2 + 3 + 4;",
        "}");
    selectionStart = findOffset("1");
    selectionEnd = findOffset("2 + ") + 1;
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int res = 1 + 2;",
        "  int a = res + 3 + 4;",
        "}");
  }

  public void test_singleExpression_trailingComment() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 12 /*abc*/ + 345;",
        "}");
    selectionStart = findOffset("12");
    selectionEnd = findOffset("*/ + 345") + 2;
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int res = 12 /*abc*/;",
        "  int a = res + 345;",
        "}");
  }

  public void test_singleExpression_trailingNotWhitespace() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 12 + 345; // marker",
        "}");
    selectionStart = findOffset("12");
    selectionEnd = findOffset(" 345");
    // check conditions
    createRefactoring("res");
    assert_fatalError_selection();
  }

  public void test_singleExpression_trailingWhitespace() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 + 2 ; // marker",
        "}");
    selectionStart = findOffset("1");
    selectionEnd = findOffset("; // marker");
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int res = 1 + 2 ;",
        "  int a = res; // marker",
        "}");
  }

  /**
   * Asserts that {@link refactoringStatus} has fatal error caused by selection.
   */
  private void assert_fatalError_selection() {
    assertTrue(refactoringStatus.hasFatalError());
    assertEquals(
        "An expression must be selected to activate this refactoring.",
        refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL));
  }

  /**
   * Creates refactoring and checks all conditions.
   */
  private void createRefactoring(String tempName) throws CoreException {
    int selectionLength = selectionEnd - selectionStart;
    refactoring = new ExtractLocalRefactoring(testUnit, selectionStart, selectionLength);
    refactoring.setLocalName(tempName);
    refactoringStatus = refactoring.checkAllConditions(pm);
  }

  private void doSuccessfullRefactoring() throws CoreException {
    // create refactoring
    createRefactoring("res");
    // OK status
    if (!refactoringStatus.isOK()) {
      fail(refactoringStatus.toString());
    }
    // perform change
    performRefactoringChange();
  }

  private void performRefactoringChange() throws CoreException {
    Change change = refactoring.createChange(pm);
    change.initializeValidationData(pm);
    new PerformChangeOperation(change).run(pm);
  }

}
