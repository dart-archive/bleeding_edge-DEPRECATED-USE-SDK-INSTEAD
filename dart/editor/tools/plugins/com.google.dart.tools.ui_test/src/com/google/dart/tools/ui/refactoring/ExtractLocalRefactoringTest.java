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

import com.google.dart.tools.internal.corext.refactoring.code.ExtractLocalRefactoring;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for {@link ExtractLocalRefactoring}.
 */
public final class ExtractLocalRefactoringTest extends RefactoringTest {
  private static final IProgressMonitor pm = new NullProgressMonitor();

  private int selectionStart;
  private int selectionEnd;
  private ExtractLocalRefactoring refactoring;
  private RefactoringStatus refactoringStatus;

  public void test_singleExpression() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 + 2; // marker",
        "}");
    selectionStart = findPattern("1", 0);
    selectionEnd = findPattern("; // marker", 0);
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int res = 1 + 2;",
        "  int a = res; // marker",
        "}");
  }

  public void test_singleExpression_Dynamic() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {}",
        "main() {",
        "  int a = f(); // marker",
        "}");
    selectionStart = findPattern("f();", 0);
    selectionEnd = findPattern("; // marker", 0);
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {}",
        "main() {",
        "  var res = f();",
        "  int a = res; // marker",
        "}");
  }

  public void test_singleExpression_leadingSpace() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 + 2; // marker",
        "}");
    selectionStart = findPattern(" 1", 0);
    selectionEnd = findPattern("; // marker", 0);
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int res =  1 + 2;",
        "  int a =res; // marker",
        "}");
  }

  public void test_singleExpression_trailingSpace() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int a = 1 + 2 ; // marker",
        "}");
    selectionStart = findPattern("1", 0);
    selectionEnd = findPattern("; // marker", 0);
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f() {",
        "  int res = 1 + 2 ;",
        "  int a = res; // marker",
        "}");
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
    assertTrue(refactoringStatus.isOK());
    // perform change
    performRefactoringChange();
  }

  /**
   * @return the offset of the given "pattern" in {@link #testUnit}, adjusted to the given "delta".
   */
  private int findPattern(String pattern, int delta) throws Exception {
    int offset = testUnit.getSource().indexOf(pattern);
    assertThat(offset).as(pattern).isNotEqualTo(-1);
    return offset + delta;
  }

  private void performRefactoringChange() throws CoreException {
    Change change = refactoring.createChange(pm);
    change.initializeValidationData(pm);
    new PerformChangeOperation(change).run(pm);
  }

}
