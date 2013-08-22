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
    Source source = addSource(createSource(//
    // TODO
    ));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INACCESSIBLE_SETTER);
    verify(source);
  }

  public void fail_invocationOfNonFunction_staticInSuperclass() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static void a() {}",
        "}",
        "",
        "class B extends A {",
        "  void b() { a(); }",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION);
    verify(source);
  }

  public void test_inconsistentMethodInheritance_paramCount() throws Exception {
    Source source = addSource(createSource(//
        "abstract class A {",
        "  int x();",
        "}",
        "abstract class B {",
        "  int x(int y);",
        "}",
        "class C implements A, B {",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INCONSISTENT_METHOD_INHERITANCE);
    verify(source);
  }

  public void test_inconsistentMethodInheritance_paramType() throws Exception {
    Source source = addSource(createSource(//
        "abstract class A {",
        "  x(int i);",
        "}",
        "abstract class B {",
        "  x(String s);",
        "}",
        "abstract class C implements A, B {}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INCONSISTENT_METHOD_INHERITANCE);
    verify(source);
  }

  public void test_inconsistentMethodInheritance_returnType() throws Exception {
    Source source = addSource(createSource(//
        "abstract class A {",
        "  int x();",
        "}",
        "abstract class B {",
        "  String x();",
        "}",
        "abstract class C implements A, B {}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INCONSISTENT_METHOD_INHERITANCE);
    verify(source);
  }

  public void test_instanceAccessToStaticMember_method_invocation() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static m() {}",
        "}",
        "main(A a) {",
        "  a.m();",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INSTANCE_ACCESS_TO_STATIC_MEMBER);
    verify(source);
  }

  public void test_instanceAccessToStaticMember_method_reference() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static m() {}",
        "}",
        "main(A a) {",
        "  a.m;",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INSTANCE_ACCESS_TO_STATIC_MEMBER);
    verify(source);
  }

  public void test_instanceAccessToStaticMember_propertyAccess_field() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static var f;",
        "}",
        "main(A a) {",
        "  a.f;",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INSTANCE_ACCESS_TO_STATIC_MEMBER);
    verify(source);
  }

  public void test_instanceAccessToStaticMember_propertyAccess_getter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static get f => 42;",
        "}",
        "main(A a) {",
        "  a.f;",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INSTANCE_ACCESS_TO_STATIC_MEMBER);
    verify(source);
  }

  public void test_instanceAccessToStaticMember_propertyAccess_setter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static set f(x) {}",
        "}",
        "main(A a) {",
        "  a.f = 42;",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INSTANCE_ACCESS_TO_STATIC_MEMBER);
    verify(source);
  }

  public void test_invalidAssignment_compoundAssignment() throws Exception {
    Source source = addSource(createSource(//
        "class byte {",
        "  int _value;",
        "  byte(this._value);",
        "  int operator +(int val) {}",
        "}",
        "",
        "void main() {",
        "  byte b = new byte(52);",
        "  b += 3;",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INVALID_ASSIGNMENT);
    verify(source);
  }

  public void test_invalidAssignment_defaultValue_named() throws Exception {
    Source source = addSource(createSource(//
        "f({String x: 0}) {",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INVALID_ASSIGNMENT);
    verify(source);
  }

  public void test_invalidAssignment_defaultValue_optional() throws Exception {
    Source source = addSource(createSource(//
        "f([String x = 0]) {",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INVALID_ASSIGNMENT);
    verify(source);
  }

  public void test_invalidAssignment_instanceVariable() throws Exception {
    Source source = addSource(createSource(//
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
    Source source = addSource(createSource(//
        "f() {",
        "  int x;",
        "  x = '0';",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INVALID_ASSIGNMENT);
    verify(source);
  }

  public void test_invalidAssignment_staticVariable() throws Exception {
    Source source = addSource(createSource(//
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

  public void test_invalidAssignment_topLevelVariableDeclaration() throws Exception {
    Source source = addSource(createSource(//
    "int x = 'string';"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INVALID_ASSIGNMENT);
    verify(source);
  }

  public void test_invalidAssignment_variableDeclaration() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int x = 'string';",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INVALID_ASSIGNMENT);
    verify(source);
  }

  public void test_invocationOfNonFunction_class() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  void m() {",
        "    A();",
        "  }",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION);
  }

  public void test_invocationOfNonFunction_localVariable() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  int x;",
        "  return x();",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION);
    verify(source);
  }

  public void test_invocationOfNonFunction_ordinaryInvocation() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static int x;",
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
    Source source = addSource(createSource(//
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

  public void test_invocationOfNonFunction_superExpression() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int get g => 0;",
        "}",
        "class B extends A {",
        "  m() {",
        "    var v = super.g();",
        "  }",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION);
    verify(source);
  }

  public void test_nonBoolCondition_conditional() throws Exception {
    Source source = addSource(createSource(//
    "f() { return 3 ? 2 : 1; }"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.NON_BOOL_CONDITION);
    verify(source);
  }

  public void test_nonBoolCondition_do() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  do {} while (3);",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.NON_BOOL_CONDITION);
    verify(source);
  }

  public void test_nonBoolCondition_if() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  if (3) return 2; else return 1;",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.NON_BOOL_CONDITION);
    verify(source);
  }

  public void test_nonBoolCondition_while() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  while (3) {}",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.NON_BOOL_CONDITION);
    verify(source);
  }

  public void test_nonBoolExpression_functionType() throws Exception {
    Source source = addSource(createSource(//
        "int makeAssertion() => 1;",
        "f() {",
        "  assert(makeAssertion);",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.NON_BOOL_EXPRESSION);
    verify(source);
  }

  public void test_nonBoolExpression_interfaceType() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  assert(0);",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.NON_BOOL_EXPRESSION);
    verify(source);
  }

  public void test_nonTypeAsTypeArgument_notAType() throws Exception {
    Source source = addSource(createSource(//
        "int A;",
        "class B<E> {}",
        "f(B<A> b) {}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.NON_TYPE_AS_TYPE_ARGUMENT);
    verify(source);
  }

  public void test_nonTypeAsTypeArgument_undefinedIdentifier() throws Exception {
    Source source = addSource(createSource(//
        "class B<E> {}",
        "f(B<A> b) {}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.NON_TYPE_AS_TYPE_ARGUMENT);
    verify(source);
  }

  public void test_returnOfInvalidType_expressionFunctionBody_function() throws Exception {
    Source source = addSource(createSource(//
    "int f() => '0';"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.RETURN_OF_INVALID_TYPE);
    verify(source);
  }

  public void test_returnOfInvalidType_expressionFunctionBody_getter() throws Exception {
    Source source = addSource(createSource(//
    "int get g => '0';"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.RETURN_OF_INVALID_TYPE);
    verify(source);
  }

  public void test_returnOfInvalidType_expressionFunctionBody_localFunction() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  String m() {",
        "    int f() => '0';",
        "  }",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.RETURN_OF_INVALID_TYPE);
    verify(source);
  }

  public void test_returnOfInvalidType_expressionFunctionBody_method() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int f() => '0';",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.RETURN_OF_INVALID_TYPE);
    verify(source);
  }

  public void test_returnOfInvalidType_expressionFunctionBody_void() throws Exception {
    Source source = addSource(createSource(//
    "void f() => 42;"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.RETURN_OF_INVALID_TYPE);
    verify(source);
  }

  public void test_returnOfInvalidType_function() throws Exception {
    Source source = addSource(createSource(//
    "int f() { return '0'; }"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.RETURN_OF_INVALID_TYPE);
    verify(source);
  }

  public void test_returnOfInvalidType_getter() throws Exception {
    Source source = addSource(createSource(//
    "int get g { return '0'; }"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.RETURN_OF_INVALID_TYPE);
    verify(source);
  }

  public void test_returnOfInvalidType_localFunction() throws Exception {
    Source source = addSource(createSource(//
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
    Source source = addSource(createSource(//
        "class A {",
        "  int f() { return '0'; }",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.RETURN_OF_INVALID_TYPE);
    verify(source);
  }

  public void test_returnOfInvalidType_void() throws Exception {
    Source source = addSource(createSource(//
    "void f() { return 42; }"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.RETURN_OF_INVALID_TYPE);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_classTypeAlias() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class C {}",
        "class G<E extends A> {}",
        "typedef D = G<B> with C;"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_extends() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "class C extends G<B>{}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_fieldFormalParameter() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "class C {",
        "  var f;",
        "  C(G<B> this.f) {}",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_functionReturnType() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "G<B> f() {}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_functionTypeAlias() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "typedef G<B> f();"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_functionTypedFormalParameter() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "f(G<B> h()) {}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_implements() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "class C implements G<B>{}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_is() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "var b = 1 is G<B>;"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_methodReturnType() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "class C {",
        "  G<B> m() {}",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_new() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "f() { return new G<B>(); }"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_new_superTypeOfUpperBound() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B extends A {}",
        "class C extends B {}",
        "class G<E extends B> {}",
        "f() { return new G<A>(); }"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_parameter() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "f(G<B> g) {}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_redirectingConstructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class X<T extends A> {",
        "  X(int x, int y) {}",
        "  factory X.name(int x, int y) = X<B>;",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_typeArgumentList() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class C<E> {}",
        "class D<E extends A> {}",
        "C<D<B>> Var;"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_typeParameter() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class C {}",
        "class G<E extends A> {}",
        "class D<F extends G<B>> {}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_variableDeclaration() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "G<B> g;"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_with() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "class C extends Object with G<B>{}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_undefinedGetter() throws Exception {
    Source source = addSource(createSource(//
        "class T {}",
        "f(T e) { return e.m; }"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.UNDEFINED_GETTER);
    // A call to verify(source) fails as 'e.m' isn't resolved.
  }

  public void test_undefinedGetter_static() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "var a = A.B;"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.UNDEFINED_GETTER);
    // A call to verify(source) fails as 'A.B' isn't resolved.
  }

  public void test_undefinedMethod() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  void m() {",
        "    n();",
        "  }",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.UNDEFINED_METHOD);
  }

  public void test_undefinedMethod_ignoreTypePropagation() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B extends A {",
        "  m() {}",
        "}",
        "class C {",
        "f() {",
        "    A a = new B();",
        "    a.m();",
        "  }",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.UNDEFINED_METHOD);
  }

  public void test_undefinedOperator_indexBoth() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "f(A a) {",
        "  a[0]++;",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.UNDEFINED_OPERATOR);
    // no verify(), a[0] is not resolved
  }

  public void test_undefinedOperator_indexGetter() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "f(A a) {",
        "  a[0];",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.UNDEFINED_OPERATOR);
    // no verify(), a[0] is not resolved
  }

  public void test_undefinedOperator_indexSetter() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "f(A a) {",
        "  a[0] = 1;",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.UNDEFINED_OPERATOR);
    // no verify(), a[0] is not resolved
  }

  public void test_undefinedOperator_plus() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "f(A a) {",
        "  a + 1;",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.UNDEFINED_OPERATOR);
    // no verify(), 'a + 1' is not resolved
  }

  public void test_undefinedSetter() throws Exception {
    Source source = addSource(createSource(//
        "class T {}",
        "f(T e1) { e1.m = 0; }"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.UNDEFINED_SETTER);
    // A call to verify(source) fails as 'e.m' isn't resolved.
  }

  public void test_undefinedSetter_static() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "f() { A.B = 0;}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.UNDEFINED_SETTER);
    //A call to verify(source) fails as 'A.B' isn't resolved.
  }

  public void test_undefinedSuperMethod() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B extends A {",
        "  m() { return super.m(); }",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.UNDEFINED_SUPER_METHOD);
  }

  public void test_unqualifiedReferenceToNonLocalStaticMember_getter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static int get a => 0;",
        "}",
        "class B extends A {",
        "  int b() {",
        "    return a;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.UNQUALIFIED_REFERENCE_TO_NON_LOCAL_STATIC_MEMBER);
    verify(source);
  }

  public void test_unqualifiedReferenceToNonLocalStaticMember_method() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static void a() {}",
        "}",
        "class B extends A {",
        "  void b() {",
        "    a();",
        "  }",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.UNQUALIFIED_REFERENCE_TO_NON_LOCAL_STATIC_MEMBER);
    verify(source);
  }

  public void test_unqualifiedReferenceToNonLocalStaticMember_setter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static set a(x) {}",
        "}",
        "class B extends A {",
        "  b(y) {",
        "    a = y;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.UNQUALIFIED_REFERENCE_TO_NON_LOCAL_STATIC_MEMBER);
    verify(source);
  }

  public void test_wrongNumberOfTypeArguments_tooFew() throws Exception {
    Source source = addSource(createSource(//
        "class A<E, F> {}",
        "A<A> a = null;"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS);
    verify(source);
  }

  public void test_wrongNumberOfTypeArguments_tooMany() throws Exception {
    Source source = addSource(createSource(//
        "class A<E> {}",
        "A<A, A> a = null;"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS);
    verify(source);
  }

  public void test_wrongNumberOfTypeArguments_typeTest_tooFew() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class C<K, V> {}",
        "f(p) {",
        "  return p is C<A>;",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS);
    verify(source);
  }

  public void test_wrongNumberOfTypeArguments_typeTest_tooMany() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class C<E> {}",
        "f(p) {",
        "  return p is C<A, A>;",
        "}"));
    resolve(source);
    assertErrors(StaticTypeWarningCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS);
    verify(source);
  }
}
