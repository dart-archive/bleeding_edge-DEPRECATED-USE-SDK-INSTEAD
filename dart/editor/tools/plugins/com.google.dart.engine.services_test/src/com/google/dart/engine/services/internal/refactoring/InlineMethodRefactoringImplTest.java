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

import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.change.Change;
import com.google.dart.engine.services.refactoring.ExtractLocalRefactoring;
import com.google.dart.engine.services.refactoring.InlineMethodRefactoring;
import com.google.dart.engine.services.refactoring.InlineMethodRefactoring.Mode;
import com.google.dart.engine.services.status.RefactoringStatus;
import com.google.dart.engine.services.status.RefactoringStatusSeverity;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Test for {@link InlineMethodRefactoringImpl}.
 */
public class InlineMethodRefactoringImplTest extends RefactoringImplTest {
  private InlineMethodRefactoringImpl refactoring;
  private int selection;
  private InlineMethodRefactoring.Mode currentMode = InlineMethodRefactoring.Mode.INLINE_ALL;
  private boolean deleteSource = true;
  private RefactoringStatus refactoringStatus;

  public void test_access_FunctionElement() throws Exception {
    indexTestUnit(
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
    assertEquals("Inline Function", refactoring.getRefactoringName());
    // element
    ExecutableElement element = refactoring.getElement();
    assertThat(element).isInstanceOf(FunctionElement.class);
    assertEquals("test", element.getDisplayName());
  }

  public void test_access_MethodElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test(a, b) {",
        "    return a + b;",
        "  }",
        "  main() {",
        "    var res = test(1, 2);",
        "  }",
        "}");
    selection = findOffset("test(1, 2)");
    createRefactoring();
    // name
    assertEquals("Inline Method", refactoring.getRefactoringName());
  }

  public void test_bad_cascadeInvocation() throws Exception {
    indexTestUnit(
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
    // error
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.ERROR,
        "Cannot inline cascade invocation.",
        findRange("..test()"));
  }

  public void test_bad_notExecutableElement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var test = 42;",
        "  var res = test;",
        "}");
    selection = findOffset("test;");
    createRefactoring();
    // error
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Method declaration or reference must be selected to activate this refactoring.");
  }

  public void test_bad_notSimpleIdentifier() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "}");
    selection = findOffset(") {");
    createRefactoring();
    // error
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Method declaration or reference must be selected to activate this refactoring.");
  }

  public void test_bad_operator() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  operator -(other) => this;",
        "}");
    selection = findOffset("-(other)");
    createRefactoring();
    // error
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Cannot inline operator.");
  }

  public void test_bad_severalReturns() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test() {",
        "  if (true) {",
        "    return 1;",
        "  }",
        "  return 2;",
        "}",
        "main() {",
        "  var res = test();",
        "}");
    selection = findOffset("test() {");
    createRefactoring();
    // error
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.ERROR,
        "Ambiguous return value.");
  }

  public void test_canDeleteSource() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  return a + b;",
        "}",
        "main() {",
        "  var res = test(1, 2);",
        "}");
    selection = findOffset("test(1, 2)");
    createRefactoring();
    // check
    // TODO(scheglov) currently always "true"
    assertEquals(true, refactoring.canDeleteSource());
  }

  public void test_fieldAccessor_getter() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var f;",
        "  get foo {",
        "    return f * 2;",
        "  }",
        "}",
        "main() {",
        "  A a = new A();",
        "  print(a.foo);",
        "}");
    selection = findOffset("foo {");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var f;",
        "}",
        "main() {",
        "  A a = new A();",
        "  print(a.f * 2);",
        "}");
  }

  public void test_fieldAccessor_getter_PropertyAccess() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var f;",
        "  get foo {",
        "    return f * 2;",
        "  }",
        "}",
        "class B {",
        "  A a = new A();",
        "}",
        "main() {",
        "  B b = new B();",
        "  print(b.a.foo);",
        "}");
    selection = findOffset("foo {");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var f;",
        "}",
        "class B {",
        "  A a = new A();",
        "}",
        "main() {",
        "  B b = new B();",
        "  print(b.a.f * 2);",
        "}");
  }

  public void test_fieldAccessor_setter() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var f;",
        "  set foo(x) {",
        "    f = x;",
        "  }",
        "}",
        "main() {",
        "  A a = new A();",
        "  a.foo = 0;",
        "}");
    selection = findOffset("foo(x) {");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var f;",
        "}",
        "main() {",
        "  A a = new A();",
        "  a.f = 0;",
        "}");
  }

  public void test_fieldAccessor_setter_PropertyAccess() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var f;",
        "  set foo(x) {",
        "    f = x;",
        "  }",
        "}",
        "class B {",
        "  A a = new A();",
        "}",
        "main() {",
        "  B b = new B();",
        "  b.a.foo = 0;",
        "}");
    selection = findOffset("foo(x) {");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  var f;",
        "}",
        "class B {",
        "  A a = new A();",
        "}",
        "main() {",
        "  B b = new B();",
        "  b.a.f = 0;",
        "}");
  }

  public void test_function_expressionFunctionBody() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) => a + b;",
        "main() {",
        "  print(test(1, 2));",
        "}",
        "");
    selection = findOffset("test(a, b) =>");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(1 + 2);",
        "}",
        "");
  }

  public void test_function_hasReturn_assign() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  print(a);",
        "  print(b);",
        "  return a + b;",
        "}",
        "main() {",
        "  var v;",
        "  v = test(1, 2);",
        "}");
    selection = findOffset("test(a, b)");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v;",
        "  print(1);",
        "  print(2);",
        "  v = 1 + 2;",
        "}");
  }

  public void test_function_hasReturn_hasReturnType() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "int test(a, b) {",
        "  return a + b;",
        "}",
        "main() {",
        "  var v = test(1, 2);",
        "}");
    selection = findOffset("test(a, b)");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v = 1 + 2;",
        "}");
  }

  public void test_function_hasReturn_noVars_oneUsage() throws Exception {
    indexTestUnit(
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
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(1);",
        "  print(2);",
        "  var v = 1 + 2;",
        "}");
  }

  public void test_function_hasReturn_voidReturnType() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "void test(a, b) {",
        "  print(a + b);",
        "}",
        "main() {",
        "  test(1, 2);",
        "}");
    selection = findOffset("test(a, b)");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(1 + 2);",
        "}");
  }

  public void test_function_multilineString() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  {",
        "    test();",
        "  }",
        "}",
        "test() {",
        "  print('''",
        "first line",
        "second line",
        "    ''');",
        "}",
        "");
    selection = findOffset("test() {");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  {",
        "    print('''",
        "first line",
        "second line",
        "    ''');",
        "  }",
        "}",
        "");
  }

  public void test_function_noReturn_hasVars_hasConflict_fieldSuperClass() throws Exception {
    indexTestUnit(
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
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
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
    indexTestUnit(
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
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
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
    indexTestUnit(
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
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var c2 = 1 + 2;",
        "  print(c2);",
        "  var c = 0;",
        "}");
  }

  public void test_function_noReturn_hasVars_hasConflict_localBefore() throws Exception {
    indexTestUnit(
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
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var c = 0;",
        "  var c2 = 1 + 2;",
        "  print(c2);",
        "}");
  }

  public void test_function_noReturn_hasVars_noConflict() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  var c = a + b;",
        "  print(c);",
        "}",
        "main() {",
        "  test(1, 2);",
        "}");
    selection = findOffset("test(a, b)");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var c = 1 + 2;",
        "  print(c);",
        "}");
  }

  public void test_function_noReturn_noVars_oneUsage() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  print(a);",
        "  print(b);",
        "}",
        "main() {",
        "  test(1, 2);",
        "}");
    selection = findOffset("test(a, b)");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(1);",
        "  print(2);",
        "}");
  }

  public void test_function_noReturn_noVars_useIndentation() throws Exception {
    indexTestUnit(
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
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  {",
        "    print(1);",
        "    print(2);",
        "  }",
        "}");
  }

  public void test_function_notStatement_oneStatement_assign() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(int p) {",
        "  print(p * 2);",
        "}",
        "main() {",
        "  var v;",
        "  v = test(0);",
        "}",
        "");
    selection = findOffset("test(int p");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v;",
        "  v = (int p) {",
        "    print(p * 2);",
        "  }(0);",
        "}",
        "");
  }

  public void test_function_notStatement_oneStatement_variableDeclaration() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(int p) {",
        "  print(p * 2);",
        "}",
        "main() {",
        "  var v = test(0);",
        "}",
        "");
    selection = findOffset("test(int p");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v = (int p) {",
        "    print(p * 2);",
        "  }(0);",
        "}",
        "");
  }

  public void test_function_notStatement_severalStatements() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(int p) {",
        "  print(p);",
        "  print(p * 2);",
        "}",
        "main() {",
        "  var v = test(0);",
        "}",
        "");
    selection = findOffset("test(int p");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v = (int p) {",
        "    print(p);",
        "    print(p * 2);",
        "  }(0);",
        "}",
        "");
  }

  public void test_function_notStatement_zeroStatements() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(int p) {",
        "}",
        "main() {",
        "  var v = test(0);",
        "}",
        "");
    selection = findOffset("test(int p");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var v = (int p) {",
        "  }(0);",
        "}",
        "");
  }

  public void test_function_singleStatement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var topLevelField = 0;",
        "test() {",
        "  print(topLevelField);",
        "}",
        "main() {",
        "  test();",
        "}",
        "");
    selection = findOffset("test() {");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "var topLevelField = 0;",
        "main() {",
        "  print(topLevelField);",
        "}",
        "");
  }

  public void test_getInitialMode_all() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  return a + b;",
        "}",
        "main() {",
        "  var res = test(1, 2);",
        "}");
    selection = findOffset("test(a, b) {");
    createRefactoring();
    // check
    assertSame(InlineMethodRefactoring.Mode.INLINE_ALL, refactoring.getInitialMode());
  }

  public void test_getInitialMode_single() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  return a + b;",
        "}",
        "main() {",
        "  var res = test(1, 2);",
        "}");
    selection = findOffset("test(1, 2)");
    createRefactoring();
    // check
    assertSame(InlineMethodRefactoring.Mode.INLINE_SINGLE, refactoring.getInitialMode());
  }

  public void test_method_emptyBody() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "abstract class A {",
        "  test();",
        "}",
        "main2(A a) {",
        "  print(a.test());",
        "}",
        "");
    selection = findOffset("test());");
    createRefactoring();
    // fatal error
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Cannot inline method without body.");
  }

  public void test_method_fieldInstance() throws Exception {
    indexTestUnit(
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
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
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

  public void test_method_fieldStatic() throws Exception {
    verifyNoTestUnitErrors = false;
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static var FA = 1;",
        "}",
        "class B extends A {",
        "  static var FB = 2;",
        "  test() {",
        "    print(FA);",
        "    print(FB);",
        "    print(A.FA);",
        "    print(B.FB);",
        "  }",
        "}",
        "main() {",
        "  B b = new B();",
        "  b.test();",
        "}",
        "");
    selection = findOffset("test() {");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static var FA = 1;",
        "}",
        "class B extends A {",
        "  static var FB = 2;",
        "}",
        "main() {",
        "  B b = new B();",
        "  print(A.FA);",
        "  print(B.FB);",
        "  print(A.FA);",
        "  print(B.FB);",
        "}",
        "");
  }

  public void test_method_fieldStatic_sameClass() throws Exception {
    verifyNoTestUnitErrors = false;
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static var F = 1;",
        "  foo() {",
        "    test();",
        "  }",
        "  test() {",
        "    print(A.F);",
        "  }",
        "}",
        "");
    selection = findOffset("test() {");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  static var F = 1;",
        "  foo() {",
        "    print(A.F);",
        "  }",
        "}",
        "");
  }

  public void test_method_singleStatement() throws Exception {
    indexTestUnit(
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
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  foo() {",
        "    print(0);",
        "  }",
        "}",
        "");
  }

  public void test_method_unqualifiedUnvocation() throws Exception {
    indexTestUnit(
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
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
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

  public void test_namedArgument_inBody() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "fa(pa) => fb(pb: true);",
        "fb({pb: false}) {}",
        "main() {",
        "  fa(null);",
        "}");
    selection = findOffset("fa(null)");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "fa(pa) => fb(pb: true);",
        "fb({pb: false}) {}",
        "main() {",
        "  fb(pb: true);",
        "}");
  }

  public void test_namedArguments() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test({a: 0, b: 2}) {",
        "  print(a + b);",
        "}",
        "main() {",
        "  test(a: 10, b: 20);",
        "  test(b: 20, a: 10);",
        "}",
        "");
    selection = findOffset("test({");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print(10 + 20);",
        "  print(10 + 20);",
        "}",
        "");
  }

  public void test_reference_noStatement() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  return a || b;",
        "}",
        "foo(p1, p2, p3) => p1 && test(p2, p3);",
        "bar() => {",
        "  'name' : baz(test)",
        "};",
        "baz(x) {}");
    selection = findOffset("test(a, b)");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "foo(p1, p2, p3) => p1 && (p2 || p3);",
        "bar() => {",
        "  'name' : baz((a, b) {",
        "    return a || b;",
        "  })",
        "};",
        "baz(x) {}");
  }

  public void test_reference_toClassMethod() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  test(a, b) {",
        "    print(a);",
        "    print(b);",
        "  }",
        "}",
        "main() {",
        "  print(new A().test);",
        "}");
    selection = findOffset("test(a, b)");
    createRefactoring();
    // error
    assertRefactoringStatus(
        refactoringStatus,
        RefactoringStatusSeverity.FATAL,
        "Cannot inline class method reference.",
        findRangeIdentifier("test);"));
  }

  public void test_reference_toLocal() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  test(a, b) {",
        "    print(a);",
        "    print(b);",
        "  }",
        "  print(test);",
        "}");
    selection = findOffset("test(a, b)");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print((a, b) {",
        "    print(a);",
        "    print(b);",
        "  });",
        "}");
  }

  public void test_reference_toTopLevel() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  print(a);",
        "  print(b);",
        "}",
        "main() {",
        "  print(test);",
        "}");
    selection = findOffset("test(a, b)");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  print((a, b) {",
        "    print(a);",
        "    print(b);",
        "  });",
        "}");
  }

  public void test_requiresPreview_false_literals() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b, c, d, e, f, {n: 0}) {}",
        "main() {",
        "  test(true, 1.0, 2, null, 'simple', 'adj' 'strings', n: 3);",
        "}");
    selection = findOffset("test(a, ");
    createRefactoring();
    // check
    assertFalse(refactoring.requiresPreview());
  }

  public void test_requiresPreview_false_variables() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b, c) {}",
        "var topLevelVar;",
        "main(p) {",
        "  int localVar;",
        "  test(localVar, p, topLevelVar);",
        "  test(this, null, null);",
        "}");
    selection = findOffset("test(a, ");
    createRefactoring();
    // check
    assertFalse(refactoring.requiresPreview());
  }

  public void test_requiresPreview_multipleSites_const() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a) {}",
        "var topLevelVar = 0;",
        "int func() => topLevelVar++;",
        "main() {",
        "  test(func());",
        "  test(42);",
        "}");
    selection = findOffset("test(42");
    createRefactoring();
    // single site - no preview
    assertFalse(refactoring.requiresPreview());
    // force inline all - require preview
    refactoring.setCurrentMode(Mode.INLINE_ALL);
    assertTrue(refactoring.requiresPreview());
    // single site - no preview
    refactoring.setCurrentMode(Mode.INLINE_SINGLE);
    assertFalse(refactoring.requiresPreview());
  }

  public void test_requiresPreview_multipleSites_declaration() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a) {}",
        "var topLevelVar = 0;",
        "int func() => topLevelVar++;",
        "main() {",
        "  test(func());",
        "  test(42);",
        "}");
    selection = findOffset("test(a");
    createRefactoring();
    // all sites checked - require preview
    assertTrue(refactoring.requiresPreview());
  }

  public void test_requiresPreview_multipleSites_invocation() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a) {}",
        "var topLevelVar = 0;",
        "int func() => topLevelVar++;",
        "main() {",
        "  test(func());",
        "  test(42);",
        "}");
    selection = findOffset("test(func()");
    createRefactoring();
    // single site with preview
    assertTrue(refactoring.requiresPreview());
    // force inline all, still preview required
    refactoring.setCurrentMode(Mode.INLINE_ALL);
    assertTrue(refactoring.requiresPreview());
  }

  public void test_requiresPreview_true_getter() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a) {}",
        "var topLevelVar;",
        "class A {",
        "  int f = 0;",
        "  get p => f++;",
        "  main() {",
        "  test(p);",
        "  }",
        "}");
    selection = findOffset("test(a");
    createRefactoring();
    // check
    assertTrue(refactoring.requiresPreview());
  }

  public void test_requiresPreview_true_invocation() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a) {}",
        "var topLevelVar = 0;",
        "int func() => topLevelVar++;",
        "main() {",
        "  test(func());",
        "}");
    selection = findOffset("test(a");
    createRefactoring();
    // check
    assertTrue(refactoring.requiresPreview());
  }

  public void test_singleExpression_oneUsage() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  return a + b;",
        "}",
        "main() {",
        "  var res = test(1, 2);",
        "}");
    selection = findOffset("test(a, b)");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res = 1 + 2;",
        "}");
  }

  public void test_singleExpression_oneUsage_keepMethod() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  return a + b;",
        "}",
        "main() {",
        "  var res = test(1, 2);",
        "}");
    selection = findOffset("test(a, b)");
    deleteSource = false;
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  return a + b;",
        "}",
        "main() {",
        "  var res = 1 + 2;",
        "}");
  }

  public void test_singleExpression_twoUsages() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  return a + b;",
        "}",
        "main() {",
        "  var res1 = test(1, 2);",
        "  var res2 = test(10, 20);",
        "}");
    selection = findOffset("test(a, b)");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res1 = 1 + 2;",
        "  var res2 = 10 + 20;",
        "}");
  }

  public void test_singleExpression_twoUsages_inlineOne() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  return a + b;",
        "}",
        "main() {",
        "  var res1 = test(1, 2);",
        "  var res2 = test(10, 20);",
        "}");
    selection = findOffset("test(1, 2)");
    currentMode = InlineMethodRefactoring.Mode.INLINE_SINGLE;
    // this flag should be ignored
    deleteSource = true;
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
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
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  return a * b;",
        "}",
        "main() {",
        "  var res1 = test(1, 2 + 3);",
        "  var res2 = test(1, (2 + 3));",
        "}");
    selection = findOffset("test(a, b)");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res1 = 1 * (2 + 3);",
        "  var res2 = 1 * (2 + 3);",
        "}");
  }

  public void test_singleExpression_wrapIntoParenthesized2() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(a, b) {",
        "  return a * (b);",
        "}",
        "main() {",
        "  var res = test(1, 2 + 3);",
        "}");
    selection = findOffset("test(a, b)");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main() {",
        "  var res = 1 * (2 + 3);",
        "}");
  }

  public void test_singleExpression_wrapIntoParenthesized3() throws Exception {
    indexTestUnit(
        "// filler filler filler filler filler filler filler filler filler filler",
        "test(bool a, bool b) {",
        "  return a || b;",
        "}",
        "main(bool p, bool p2, bool p3) {",
        "  var res1 = p && test(p2, p3);",
        "  var res2 = p || test(p2, p3);",
        "}");
    selection = findOffset("test(bool a, bool b)");
    createRefactoring();
    // do refactoring
    assertSuccessfulRefactoring(
        "// filler filler filler filler filler filler filler filler filler filler",
        "main(bool p, bool p2, bool p3) {",
        "  var res1 = p && (p2 || p3);",
        "  var res2 = p || p2 || p3;",
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

//  private void assert_fatalError_selection() {
//    assertRefactoringStatus(
//        refactoringStatus,
//        RefactoringStatusSeverity.FATAL,
//        "Local variable declaration or reference must be selected to activate this refactoring.");
//  }

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
    refactoring = new InlineMethodRefactoringImpl(context);
    refactoring.setCurrentMode(currentMode);
    refactoring.setDeleteSource(deleteSource);
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
