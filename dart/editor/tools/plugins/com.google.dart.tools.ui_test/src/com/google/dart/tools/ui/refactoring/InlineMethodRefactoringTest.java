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

import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartFunction;
import com.google.dart.tools.internal.corext.refactoring.code.InlineMethodRefactoring;

import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.PerformChangeOperation;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

/**
 * Test for {@link InlineMethodRefactoring}.
 */
public final class InlineMethodRefactoringTest extends RefactoringTest {
  private static final IProgressMonitor pm = new NullProgressMonitor();

  private int selection;
  private InlineMethodRefactoring refactoring;
  private RefactoringStatus refactoringStatus;
  private InlineMethodRefactoring.Mode mode = InlineMethodRefactoring.Mode.INLINE_ALL;
  private boolean deleteSource = true;

  public void test_access() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  return a + b;",
        "}",
        "main() {",
        "  var res = test(1, 2);",
        "}");
    selection = findOffset("test(1, 2)");
    createRefactoring();
    // name
    assertEquals("Inline Method", refactoring.getName());
    // method
    {
      DartFunction method = refactoring.getMethod();
      assertNotNull(method);
      assertEquals("test", method.getElementName());
    }
  }

  public void test_bad_cascadeInvocation() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {}",
        "  bar() {}",
        "  test() {}",
        "}",
        "main() {",
        " A a = new A();",
        " a..foo()..test()..bar();",
        "}");
    selection = findOffset("test() {");
    createRefactoring();
    // fatal error
    assertTrue(refactoringStatus.hasFatalError());
  }

  public void test_bad_severalReturns() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {",
        "  if (true) {",
        "    return 1;",
        "  }",
        "  return 2;",
        "}",
        "main() {",
        "  var res = test(1, 2);",
        "}");
    selection = findOffset("test() {");
    createRefactoring();
    // error
    assertTrue(refactoringStatus.hasError());
  }

  /**
   * Test for {@link InlineMethodRefactoring#canEnableDeleteSource()}
   */
  @SuppressWarnings("deprecation")
  public void test_canEnableDeleteSource_false() throws Exception {
    CompilationUnit functionsUnit = setUnitContent("Functions.dart", new String[] {
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  return a + b;",
        "}",
        ""});
    functionsUnit.getResource().setReadOnly(true);
    try {
      setTestUnitContent(
          "// filler filler filler filler filler filler filler filler filler filler",
          "#source('Functions.dart');",
          "main() {",
          "  var res = test(1, 2);",
          "}");
      selection = findOffset("test(1, 2)");
      createRefactoring();
      assertEquals(false, refactoring.canEnableDeleteSource());
    } finally {
      functionsUnit.getResource().setReadOnly(false);
    }
  }

  /**
   * Test for {@link InlineMethodRefactoring#canEnableDeleteSource()}
   */
  public void test_canEnableDeleteSource_true() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  return a + b;",
        "}",
        "main() {",
        "  var res = test(1, 2);",
        "}");
    selection = findOffset("test(a, b) {");
    createRefactoring();
    assertEquals(true, refactoring.canEnableDeleteSource());
  }

  public void test_function_hasReturn_noVars_oneUsage() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  print(a);",
        "  print(b);",
        "  return a + b;",
        "}",
        "main() {",
        "  var v = test(1, 2);",
        "}");
    selection = findOffset("test(a, b)");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(1);",
        "  print(2);",
        "  var v = 1 + 2;",
        "}");
  }

  public void test_function_noReturn_hasVars_hasConflict_fieldSuperClass() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var c;",
        "}",
        "class B extends A {",
        "  foo() {",
        "    test(1, 2);",
        "  }",
        "}",
        "test(a, b) {",
        "  var c = a + b;",
        "  print(c);",
        "}");
    selection = findOffset("test(a, b)");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var c;",
        "}",
        "class B extends A {",
        "  foo() {",
        "    var c2 = 1 + 2;",
        "    print(c2);",
        "  }",
        "}",
        "");
  }

  public void test_function_noReturn_hasVars_hasConflict_fieldThisClass() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var c;",
        "  foo() {",
        "    test(1, 2);",
        "  }",
        "}",
        "test(a, b) {",
        "  var c = a + b;",
        "  print(c);",
        "}");
    selection = findOffset("test(a, b)");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var c;",
        "  foo() {",
        "    var c2 = 1 + 2;",
        "    print(c2);",
        "  }",
        "}",
        "");
  }

  public void test_function_noReturn_hasVars_hasConflict_localAfter() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  var c = a + b;",
        "  print(c);",
        "}",
        "main() {",
        "  test(1, 2);",
        "  var c = 0;",
        "}");
    selection = findOffset("test(a, b)");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var c2 = 1 + 2;",
        "  print(c2);",
        "  var c = 0;",
        "}");
  }

  public void test_function_noReturn_hasVars_hasConflict_localBefore() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  var c = a + b;",
        "  print(c);",
        "}",
        "main() {",
        "  var c = 0;",
        "  test(1, 2);",
        "}");
    selection = findOffset("test(a, b)");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var c = 0;",
        "  var c2 = 1 + 2;",
        "  print(c2);",
        "}");
  }

  public void test_function_noReturn_hasVars_noConflict() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  var c = a + b;",
        "  print(c);",
        "}",
        "main() {",
        "  test(1, 2);",
        "}");
    selection = findOffset("test(a, b)");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var c = 1 + 2;",
        "  print(c);",
        "}");
  }

  public void test_function_noReturn_noVars_oneUsage() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  print(a);",
        "  print(b);",
        "}",
        "main() {",
        "  test(1, 2);",
        "}");
    selection = findOffset("test(a, b)");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(1);",
        "  print(2);",
        "}");
  }

  public void test_function_noReturn_noVars_useIndentation() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  print(a);",
        "  print(b);",
        "}",
        "main() {",
        "  {",
        "    test(1, 2);",
        "  }",
        "}");
    selection = findOffset("test(a, b)");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  {",
        "    print(1);",
        "    print(2);",
        "  }",
        "}");
  }

  /**
   * Test for {@link InlineMethodRefactoring#getInitialMode()}
   */
  public void test_getInitialMode_all() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  return a + b;",
        "}",
        "main() {",
        "  var res = test(1, 2);",
        "}");
    selection = findOffset("test(a, b) {");
    createRefactoring();
    assertSame(InlineMethodRefactoring.Mode.INLINE_ALL, refactoring.getInitialMode());
  }

  /**
   * Test for {@link InlineMethodRefactoring#getInitialMode()}
   */
  public void test_getInitialMode_single() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  return a + b;",
        "}",
        "main() {",
        "  var res = test(1, 2);",
        "}");
    selection = findOffset("test(1, 2)");
    createRefactoring();
    assertSame(InlineMethodRefactoring.Mode.INLINE_SINGLE, refactoring.getInitialMode());
  }

  public void test_method_qualifiedUnvocation_instanceField() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var fA;",
        "}",
        "class B extends A {",
        "  var fB;",
        "  test() {",
        "    print(fA);",
        "    print(fB);",
        "    print(this.fA);",
        "    print(this.fB);",
        "  }",
        "}",
        "main() {",
        "  B b = new B();",
        "  b.test();",
        "}",
        "");
    selection = findOffset("test() {");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var fA;",
        "}",
        "class B extends A {",
        "  var fB;",
        "}",
        "main() {",
        "  B b = new B();",
        "  print(b.fA);",
        "  print(b.fB);",
        "  print(b.fA);",
        "  print(b.fB);",
        "}",
        "");
  }

  public void test_method_qualifiedUnvocation_staticField() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static FA = 1;",
        "}",
        "class B extends A {",
        "  static FB = 2;",
        "  test() {",
        "    print(FA);",
        "    print(FB);",
        "  }",
        "}",
        "main() {",
        "  B b = new B();",
        "  b.test();",
        "}",
        "");
    selection = findOffset("test() {");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static FA = 1;",
        "}",
        "class B extends A {",
        "  static FB = 2;",
        "}",
        "main() {",
        "  B b = new B();",
        "  print(A.FA);",
        "  print(B.FB);",
        "}",
        "");
  }

  public void test_method_singleStatement() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test() {",
        "    print(0);",
        "  }",
        "  foo() {",
        "    test();",
        "  }",
        "}",
        "");
    selection = findOffset("test() {");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {",
        "    print(0);",
        "  }",
        "}",
        "");
  }

  public void test_method_unqualifiedUnvocation() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test(a, b) {",
        "    print(a);",
        "    print(b);",
        "    return a + b;",
        "  }",
        "  foo() {",
        "    var v = test(1, 2);",
        "  }",
        "}",
        "");
    selection = findOffset("test(a, b)");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {",
        "    print(1);",
        "    print(2);",
        "    var v = 1 + 2;",
        "  }",
        "}",
        "");
  }

  public void test_singleExpression_oneUsage() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  return a + b;",
        "}",
        "main() {",
        "  var res = test(1, 2);",
        "}");
    selection = findOffset("test(a, b)");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res = 1 + 2;",
        "}");
  }

  public void test_singleExpression_oneUsage_keepMethod() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  return a + b;",
        "}",
        "main() {",
        "  var res = test(1, 2);",
        "}");
    selection = findOffset("test(a, b)");
    deleteSource = false;
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  return a + b;",
        "}",
        "main() {",
        "  var res = 1 + 2;",
        "}");
  }

  public void test_singleExpression_twoUsages() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  return a + b;",
        "}",
        "main() {",
        "  var res1 = test(1, 2);",
        "  var res2 = test(10, 20);",
        "}");
    selection = findOffset("test(a, b)");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res1 = 1 + 2;",
        "  var res2 = 10 + 20;",
        "}");
  }

  public void test_singleExpression_twoUsages_inlineOne() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  return a + b;",
        "}",
        "main() {",
        "  var res1 = test(1, 2);",
        "  var res2 = test(10, 20);",
        "}");
    selection = findOffset("test(1, 2)");
    mode = InlineMethodRefactoring.Mode.INLINE_SINGLE;
    // this flag should be ignored
    deleteSource = true;
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  return a + b;",
        "}",
        "main() {",
        "  var res1 = 1 + 2;",
        "  var res2 = test(10, 20);",
        "}");
  }

  public void test_singleExpression_wrapIntoParenthesized() throws Exception {
    setTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  return a * b;",
        "}",
        "main() {",
        "  var res = test(1, 2 + 3);",
        "}");
    selection = findOffset("test(a, b)");
    // do refactoring
    doSuccessfullRefactoring();
    assertTestUnitContent(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res = 1 * (2 + 3);",
        "}");
  }

  /**
   * Creates refactoring and checks all conditions.
   */
  private void createRefactoring() throws Exception {
    DartFunction method = findElement(selection);
    refactoring = new InlineMethodRefactoring(method, testUnit, selection);
    refactoring.setCurrentMode(mode);
    refactoring.setDeleteSource(deleteSource);
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
