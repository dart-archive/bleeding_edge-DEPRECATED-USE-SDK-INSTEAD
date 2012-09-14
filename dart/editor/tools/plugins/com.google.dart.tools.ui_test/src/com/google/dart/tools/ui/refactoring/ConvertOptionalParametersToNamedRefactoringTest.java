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

import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.internal.corext.refactoring.code.ConvertOptionalParametersToNamedRefactoring;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Test for {@link ConvertOptionalParametersToNamedRefactoring}.
 */
public final class ConvertOptionalParametersToNamedRefactoringTest extends RefactoringTest {
  private static final IProgressMonitor pm = new NullProgressMonitor();

  private int selection;
  private ConvertOptionalParametersToNamedRefactoring refactoring;
  private RefactoringStatus refactoringStatus;

  public void test_access() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test([a]) => 42;",
        "");
    selection = findOffset("test([a]) =>");
    createRefactoring();
    // name
    assertEquals("Convert Optional Positional Parameters to Named", refactoring.getName());
  }

  public void test_OK_constructor() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A(a, [b = 2]) {}",
        "main() {",
        "  new A(10, 20);",
        "}");
    selection = findOffset("A(a");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  A(a, {b: 2}) {}",
        "main() {",
        "  new A(10, b: 20);",
        "}");
  }

  public void test_OK_onePositionalArgument() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, [b = 2, c]) {}",
        "main() {",
        "  var v = test(10, 20);",
        "}");
    selection = findOffset("test(a");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, {b: 2, c}) {}",
        "main() {",
        "  var v = test(10, b: 20);",
        "}");
  }

  public void test_OK_processHierarchy() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test(a, [b = 2]) {}",
        "}",
        "class B extends A {",
        "  test(a, [b = 2, c = 3]) {}",
        "}",
        "class C extends B {",
        "  test(a, [b = 2, c = 3, d = 4]) {}",
        "}",
        "",
        "main() {",
        "  C t = new C();",
        "  t.test(10, 20, 30, 40);",
        "}");
    selection = findOffset("test(a, [b = 2, c = 3]) {}");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test(a, {b: 2}) {}",
        "}",
        "class B extends A {",
        "  test(a, {b: 2, c: 3}) {}",
        "}",
        "class C extends B {",
        "  test(a, {b: 2, c: 3, d: 4}) {}",
        "}",
        "",
        "main() {",
        "  C t = new C();",
        "  t.test(10, b: 20, c: 30, d: 40);",
        "}");
  }

  public void test_OKL_allPositionalArguments() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, [b = 2, c]) {}",
        "main() {",
        "  var v = test(10, 20, 30);",
        "}");
    selection = findOffset("test(a");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, {b: 2, c}) {}",
        "main() {",
        "  var v = test(10, b: 20, c: 30);",
        "}");
  }

//  public void test_withReturnType() throws Exception {
//    setTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "int test() => 42;",
//        "main() {",
//        "  var v = test();",
//        "}");
//    selection = findOffset("test() =>");
//    // do refactoring
//    doSuccessfullRefactoring();
//    assertTestUnitContent(
//        "// filler filler filler filler filler filler filler filler filler filler",
//        "int get test => 42;",
//        "main() {",
//        "  var v = test;",
//        "}");
//  }

  /**
   * Creates refactoring and checks all conditions.
   */
  private void createRefactoring() throws Exception {
    DartFunction method = findElement(selection);
    refactoring = new ConvertOptionalParametersToNamedRefactoring(method);
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
