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
import com.google.dart.tools.internal.corext.refactoring.code.ConvertMethodToGetterRefactoring;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Test for {@link ConvertMethodToGetterRefactoring}.
 */
public final class ConvertMethodToGetterRefactoringTest extends RefactoringTest {
  private static final IProgressMonitor pm = new NullProgressMonitor();

  private int selection;
  private ConvertMethodToGetterRefactoring refactoring;
  private RefactoringStatus refactoringStatus;

  public void test_access() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() => 42;",
        "");
    selection = findOffset("test() =>");
    createRefactoring();
    // name
    assertEquals("Convert Method to Getter", refactoring.getName());
  }

  public void test_noReturnType() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() => 42;",
        "main() {",
        "  var v = test();",
        "}");
    selection = findOffset("test() =>");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "get test() => 42;",
        "main() {",
        "  var v = test;",
        "}");
  }

  public void test_processHierarchy() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() => 1;",
        "}",
        "class B extends A {",
        "  test() => 2;",
        "}",
        "class C extends B {",
        "  test() => 3;",
        "}",
        "class D extends C {",
        "  test() => 4;",
        "}",
        "class E extends D {",
        "  test() => 5;",
        "}",
        "",
        "main() {",
        "  A a = new E();",
        "  var v = a.test();",
        "}");
    selection = findOffset("test() => 3;");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  get test() => 1;",
        "}",
        "class B extends A {",
        "  get test() => 2;",
        "}",
        "class C extends B {",
        "  get test() => 3;",
        "}",
        "class D extends C {",
        "  get test() => 4;",
        "}",
        "class E extends D {",
        "  get test() => 5;",
        "}",
        "",
        "main() {",
        "  A a = new E();",
        "  var v = a.test;",
        "}");
  }

  public void test_withReturnType() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int test() => 42;",
        "main() {",
        "  var v = test();",
        "}");
    selection = findOffset("test() =>");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int get test() => 42;",
        "main() {",
        "  var v = test;",
        "}");
  }

  /**
   * Creates refactoring and checks all conditions.
   */
  private void createRefactoring() throws Exception {
    DartFunction method = findElement(selection);
    refactoring = new ConvertMethodToGetterRefactoring(method);
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
