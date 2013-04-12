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
package com.google.dart.engine.resolver;

import com.google.dart.engine.error.StaticTypeWarningCode;
import com.google.dart.engine.source.Source;

public class StaticTypeWarningCodeTest extends ResolverTestCase {
  public void fail_inaccessibleSetter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INACCESSIBLE_SETTER);
    verify(source);
  }

  public void fail_inconsistentMethodInheritance() throws Exception { // This probably wants to be multiple messages.
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INCONSISTENT_METHOD_INHERITANCE);
    verify(source);
  }

  public void fail_nonTypeAsTypeArgument() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "int A;",
        "class B<E> {}",
        "f(B<A> b) {}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.NON_TYPE_AS_TYPE_ARGUMENT);
    verify(source);
  }

  public void fail_redirectWithInvalidTypeParameters() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(StaticTypeWarningCode.REDIRECT_WITH_INVALID_TYPE_PARAMETERS);
    verify(source);
  }

  public void fail_typeArgumentViolatesBounds() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(StaticTypeWarningCode.TYPE_ARGUMENT_VIOLATES_BOUNDS);
    verify(source);
  }

  public void test_invalidAssignment_instanceVariable() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  int x;",
        "}",
        "f() {",
        "  A a;",
        "  a.x = '0';",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INVALID_ASSIGNMENT);
    verify(source);
  }

  public void test_invalidAssignment_localVariable() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  int x;",
        "  x = '0';",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INVALID_ASSIGNMENT);
    verify(source);
  }

  public void test_invalidAssignment_staticVariable() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  static int x;",
        "}",
        "f() {",
        "  A.x = '0';",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INVALID_ASSIGNMENT);
    verify(source);
  }

  public void test_invocationOfNonFunction_class() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  void m() {",
        "    A();",
        "  }",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION);
  }

  public void test_invocationOfNonFunction_localVariable() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  int x;",
        "  return x();",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION);
    verify(source);
  }

  public void test_invocationOfNonFunction_ordinaryInvocation() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  int x;",
        "}",
        "class B {",
        "  m() {",
        "    A.x();",
        "  }",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION);
    // A call to verify(source) fails as A.x() cannot be resolved.
  }

  public void test_invocationOfNonFunction_staticInvocation() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  static int get g => 0;",
        "  f() {",
        "    A.g();",
        "  }",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION);
    // A call to verify(source) fails as g() cannot be resolved.
  }

  public void test_nonBoolCondition_conditional() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() { return 3 ? 2 : 1; }"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.NON_BOOL_CONDITION);
    verify(source);
  }

  public void test_nonBoolCondition_do() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  do {} while (3);",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.NON_BOOL_CONDITION);
    verify(source);
  }

  public void test_nonBoolCondition_if() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  if (3) return 2; else return 1;",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.NON_BOOL_CONDITION);
    verify(source);
  }

  public void test_nonBoolCondition_while() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  while (3) {}",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.NON_BOOL_CONDITION);
    verify(source);
  }

  public void test_nonBoolExpression() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  assert(0);",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.NON_BOOL_EXPRESSION);
    verify(source);
  }

  public void test_returnOfInvalidType_function() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "int f() { return '0'; }"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.RETURN_OF_INVALID_TYPE);
    verify(source);
  }

  public void test_returnOfInvalidType_localFunction() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  String m() {",
        "    int f() { return '0'; }",
        "  }",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.RETURN_OF_INVALID_TYPE);
    verify(source);
  }

  public void test_returnOfInvalidType_method() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  int f() { return '0'; }",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.RETURN_OF_INVALID_TYPE);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_const() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {",
        "  const G() {}",
        "}",
        "f() { return const G<B>(); }"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_new() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "f() { return new G<B>(); }"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_undefinedFunction() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "void f() {",
        "  g();",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.UNDEFINED_FUNCTION);
  }

  public void test_undefinedGetter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class T {}",
        "f(T e) { return e.m; }"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.UNDEFINED_GETTER);
    // A call to verify(source) fails as 'e.m' isn't resolved.
  }

  public void test_undefinedGetter_static() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "var a = A.B;"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.UNDEFINED_GETTER);
    // A call to verify(source) fails as 'A.B' isn't resolved.
  }

  public void test_undefinedMethod() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  void m() {",
        "    n();",
        "  }",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.UNDEFINED_METHOD);
  }

  public void test_undefinedSetter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class T {}",
        "f(T e1) { e1.m = 0; }"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.UNDEFINED_SETTER);
    // A call to verify(source) fails as 'e.m' isn't resolved.
  }

  public void test_undefinedSetter_static() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "f() { A.B = 0;}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.UNDEFINED_SETTER);
    //A call to verify(source) fails as 'A.B' isn't resolved.
  }

  public void test_undefinedSuperMethod() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "class B extends A {",
        "  m() { return super.m(); }",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.UNDEFINED_SUPER_METHOD);
  }

  public void test_wrongNumberOfTypeArguments_tooFew() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A<E, F> {}",
        "A<A> a = null;"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS);
    verify(source);
  }

  public void test_wrongNumberOfTypeArguments_tooMany() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A<E> {}",
        "A<A, A> a = null;"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS);
    verify(source);
  }
}
