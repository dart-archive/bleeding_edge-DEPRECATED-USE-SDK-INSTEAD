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

import com.google.dart.tools.internal.corext.refactoring.RefactoringCoreMessages;
import com.google.dart.tools.internal.corext.refactoring.code.InlineLocalRefactoring;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import java.text.MessageFormat;

/**
 * Test for {@link InlineLocalRefactoring}.
 */
public final class InlineLocalRefactoringTest extends RefactoringTest {
  private static final IProgressMonitor pm = new NullProgressMonitor();

  private int selection;
  private InlineLocalRefactoring refactoring;
  private RefactoringStatus refactoringStatus;

  public void test_access() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int test = 1 + 2;",
        "  print(0);",
        "}");
    selection = findOffset("test = ");
    // prepare refactoring
    createRefactoring();
    assertEquals("Inline Local Variable", refactoring.getName());
    assertEquals("test", refactoring.getVariableElement().getName());
  }

  public void test_bad_selectionEmpty() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "}");
    selection = Integer.MAX_VALUE;
    createRefactoring();
    // check conditions
    assert_fatalError_selection();
  }

  public void test_bad_selectionMethod() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "}");
    selection = findOffset("main() {");
    createRefactoring();
    // check conditions
    assert_fatalError_selection();
  }

  public void test_bad_selectionVariable_hasAssignments_1() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int test = 0;",
        "  test = 1;",
        "}");
    selection = findOffset("test = 0;");
    createRefactoring();
    // check conditions
    assert_fatalError(MessageFormat.format(
        RefactoringCoreMessages.InlineLocalRefactoring_assigned_more_once,
        "test"));
  }

  public void test_bad_selectionVariable_hasAssignments_2() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int test = 0;",
        "  test += 1;",
        "}");
    selection = findOffset("test = 0;");
    createRefactoring();
    // check conditions
    assert_fatalError(MessageFormat.format(
        RefactoringCoreMessages.InlineLocalRefactoring_assigned_more_once,
        "test"));
  }

  public void test_bad_selectionVariable_notInBlock() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  if (true)",
        "    int test = 0;",
        "}");
    selection = findOffset("test =");
    createRefactoring();
    // check conditions
    assert_fatalError(RefactoringCoreMessages.InlineLocalRefactoring_declaration_inStatement);
  }

  public void test_bad_selectionVariable_notInitialized() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int test;",
        "}");
    selection = findOffset("test;");
    createRefactoring();
    // check conditions
    assert_fatalError(MessageFormat.format(
        RefactoringCoreMessages.InlineLocalRefactoring_not_initialized,
        "test"));
  }

  public void test_bad_selectionVariable_parameter() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "f(var test) {",
        "}");
    selection = findOffset("test) {");
    createRefactoring();
    // check conditions
    assert_fatalError_selection();
  }

  public void test_OK_noUsages_1() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int test = 1 + 2;",
        "  print(0);",
        "}");
    selection = findOffset("test = ");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(0);",
        "}");
  }

  public void test_OK_noUsages_2() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int test = 1 + 2;",
        "}");
    selection = findOffset("test = ");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "}");
  }

  public void test_OK_oneUsage() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int test = 1 + 2;",
        "  print(test);",
        "}");
    selection = findOffset("test = ");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(1 + 2);",
        "}");
  }

  public void test_OK_twoUsages() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  int test = 1 + 2;",
        "  print(test);",
        "  print(test);",
        "}");
    selection = findOffset("test = ");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(1 + 2);",
        "  print(1 + 2);",
        "}");
  }

  /**
   * Asserts that {@link refactoringStatus} has fatal error with given message.
   */
  private void assert_fatalError(String msg) {
    assertTrue(refactoringStatus.hasFatalError());
    assertEquals(msg, refactoringStatus.getMessageMatchingSeverity(RefactoringStatus.FATAL));
  }

  /**
   * Asserts that {@link refactoringStatus} has fatal error caused by wrong selection.
   */
  private void assert_fatalError_selection() {
    assert_fatalError(RefactoringCoreMessages.InlineLocalRefactoring_select_temp);
  }

  /**
   * Creates refactoring and checks all conditions.
   */
  private void createRefactoring() throws Exception {
    refactoring = new InlineLocalRefactoring(testUnit, selection, 0);
    refactoringStatus = refactoring.checkAllConditions(pm);
  }

  private void doSuccessfullRefactoring() throws Exception {
    // create refactoring
    createRefactoring();
    // OK status
    if (!refactoringStatus.isOK()) {
      fail(refactoringStatus.toString());
    }
    // perform change
    performRefactoringChange();
  }

  private void performRefactoringChange() throws Exception {
    ResourcesPlugin.getWorkspace().run(new IWorkspaceRunnable() {
      @Override
      public void run(IProgressMonitor monitor) throws CoreException {
        Change change = refactoring.createChange(pm);
        change.initializeValidationData(pm);
        new PerformChangeOperation(change).run(pm);
      }
    }, null);
  }
}
