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
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.source.Source;

public class StaticTypeWarningCodeTest extends ResolverTestCase {
  public void fail_inaccessibleSetter() throws Exception {
    Source source = addSource(createSource(//
    // TODO
    ));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.INACCESSIBLE_SETTER);
    verify(source);
  }

  public void fail_undefinedEnumConstant() throws Exception {
    // We need a way to set the parseEnum flag in the parser to true.
    Source source = addSource(createSource(//
        "enum E { ONE }",
        "E e() {",
        "  return E.TWO;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_ENUM_CONSTANT);
    verify(source);
  }

  public void test_ambiguousImport_function() throws Exception {
    Source source = addSource(createSource(//
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "g() { return f(); }"));
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "f() {}"));
    addNamedSource("/lib2.dart", createSource(//
        "library lib2;",
        "f() {}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.AMBIGUOUS_IMPORT);
  }

  public void test_expectedOneListTypeArgument() throws Exception {
    Source source = addSource(createSource(//
        "main() {",
        "  <int, int> [];",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.EXPECTED_ONE_LIST_TYPE_ARGUMENTS);
    verify(source);
  }

  public void test_expectedTwoMapTypeArguments_one() throws Exception {
    Source source = addSource(createSource(//
        "main() {",
        "  <int> {};",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.EXPECTED_TWO_MAP_TYPE_ARGUMENTS);
    verify(source);
  }

  public void test_expectedTwoMapTypeArguments_three() throws Exception {
    Source source = addSource(createSource(//
        "main() {",
        "  <int, int, int> {};",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.EXPECTED_TWO_MAP_TYPE_ARGUMENTS);
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
    assertErrors(source, StaticTypeWarningCode.INCONSISTENT_METHOD_INHERITANCE);
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
    assertErrors(source, StaticTypeWarningCode.INCONSISTENT_METHOD_INHERITANCE);
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
    assertErrors(source, StaticTypeWarningCode.INCONSISTENT_METHOD_INHERITANCE);
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
    assertErrors(source, StaticTypeWarningCode.INSTANCE_ACCESS_TO_STATIC_MEMBER);
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
    assertErrors(source, StaticTypeWarningCode.INSTANCE_ACCESS_TO_STATIC_MEMBER);
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
    assertErrors(source, StaticTypeWarningCode.INSTANCE_ACCESS_TO_STATIC_MEMBER);
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
    assertErrors(source, StaticTypeWarningCode.INSTANCE_ACCESS_TO_STATIC_MEMBER);
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
    assertErrors(source, StaticTypeWarningCode.INSTANCE_ACCESS_TO_STATIC_MEMBER);
    verify(source);
  }

  public void test_invalidAssignment_compoundAssignment() throws Exception {
    Source source = addSource(createSource(//
        "class byte {",
        "  int _value;",
        "  byte(this._value);",
        "  int operator +(int val) { return 0; }",
        "}",
        "",
        "void main() {",
        "  byte b = new byte(52);",
        "  b += 3;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.INVALID_ASSIGNMENT);
    verify(source);
  }

  public void test_invalidAssignment_defaultValue_named() throws Exception {
    Source source = addSource(createSource(//
        "f({String x: 0}) {",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.INVALID_ASSIGNMENT);
    verify(source);
  }

  public void test_invalidAssignment_defaultValue_optional() throws Exception {
    Source source = addSource(createSource(//
        "f([String x = 0]) {",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.INVALID_ASSIGNMENT);
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
    assertErrors(source, StaticTypeWarningCode.INVALID_ASSIGNMENT);
    verify(source);
  }

  public void test_invalidAssignment_localVariable() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  int x;",
        "  x = '0';",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.INVALID_ASSIGNMENT);
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
    assertErrors(source, StaticTypeWarningCode.INVALID_ASSIGNMENT);
    verify(source);
  }

  public void test_invalidAssignment_topLevelVariableDeclaration() throws Exception {
    Source source = addSource(createSource(//
    "int x = 'string';"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.INVALID_ASSIGNMENT);
    verify(source);
  }

  public void test_invalidAssignment_typeParameter() throws Exception {
    // 14221
    Source source = addSource(createSource(//
        "class B<T> {",
        "  T value;",
        "  void test(num n) {",
        "    value = n;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.INVALID_ASSIGNMENT);
    verify(source);
  }

  public void test_invalidAssignment_variableDeclaration() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int x = 'string';",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.INVALID_ASSIGNMENT);
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
    assertErrors(source, StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION);
  }

  public void test_invocationOfNonFunction_localVariable() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  int x;",
        "  return x();",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION);
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
    assertErrors(source, StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION);
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
    assertErrors(source, StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION);
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
    assertErrors(source, StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION);
    verify(source);
  }

  public void test_invocationOfNonFunctionExpression_literal() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  3(5);",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.INVOCATION_OF_NON_FUNCTION_EXPRESSION);
    verify(source);
  }

  public void test_nonBoolCondition_conditional() throws Exception {
    Source source = addSource(createSource(//
    "f() { return 3 ? 2 : 1; }"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.NON_BOOL_CONDITION);
    verify(source);
  }

  public void test_nonBoolCondition_do() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  do {} while (3);",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.NON_BOOL_CONDITION);
    verify(source);
  }

  public void test_nonBoolCondition_if() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  if (3) return 2; else return 1;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.NON_BOOL_CONDITION);
    verify(source);
  }

  public void test_nonBoolCondition_while() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  while (3) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.NON_BOOL_CONDITION);
    verify(source);
  }

  public void test_nonBoolExpression_functionType() throws Exception {
    Source source = addSource(createSource(//
        "int makeAssertion() => 1;",
        "f() {",
        "  assert(makeAssertion);",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.NON_BOOL_EXPRESSION);
    verify(source);
  }

  public void test_nonBoolExpression_interfaceType() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  assert(0);",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.NON_BOOL_EXPRESSION);
    verify(source);
  }

  public void test_nonBoolNegationExpression() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  !42;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.NON_BOOL_NEGATION_EXPRESSION);
    verify(source);
  }

  public void test_nonBoolOperand_and_left() throws Exception {
    Source source = addSource(createSource(//
        "bool f(int left, bool right) {",
        "  return left && right;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.NON_BOOL_OPERAND);
    verify(source);
  }

  public void test_nonBoolOperand_and_right() throws Exception {
    Source source = addSource(createSource(//
        "bool f(bool left, String right) {",
        "  return left && right;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.NON_BOOL_OPERAND);
    verify(source);
  }

  public void test_nonBoolOperand_or_left() throws Exception {
    Source source = addSource(createSource(//
        "bool f(List<int> left, bool right) {",
        "  return left || right;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.NON_BOOL_OPERAND);
    verify(source);
  }

  public void test_nonBoolOperand_or_right() throws Exception {
    Source source = addSource(createSource(//
        "bool f(bool left, double right) {",
        "  return left || right;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.NON_BOOL_OPERAND);
    verify(source);
  }

  public void test_nonTypeAsTypeArgument_notAType() throws Exception {
    Source source = addSource(createSource(//
        "int A;",
        "class B<E> {}",
        "f(B<A> b) {}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.NON_TYPE_AS_TYPE_ARGUMENT);
    verify(source);
  }

  public void test_nonTypeAsTypeArgument_undefinedIdentifier() throws Exception {
    Source source = addSource(createSource(//
        "class B<E> {}",
        "f(B<A> b) {}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.NON_TYPE_AS_TYPE_ARGUMENT);
    verify(source);
  }

  public void test_returnOfInvalidType_expressionFunctionBody_function() throws Exception {
    Source source = addSource(createSource(//
    "int f() => '0';"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.RETURN_OF_INVALID_TYPE);
    verify(source);
  }

  public void test_returnOfInvalidType_expressionFunctionBody_getter() throws Exception {
    Source source = addSource(createSource(//
    "int get g => '0';"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.RETURN_OF_INVALID_TYPE);
    verify(source);
  }

  public void test_returnOfInvalidType_expressionFunctionBody_localFunction() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  String m() {",
        "    int f() => '0';",
        "    return '0';",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.RETURN_OF_INVALID_TYPE);
    verify(source);
  }

  public void test_returnOfInvalidType_expressionFunctionBody_method() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int f() => '0';",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.RETURN_OF_INVALID_TYPE);
    verify(source);
  }

  public void test_returnOfInvalidType_expressionFunctionBody_void() throws Exception {
    Source source = addSource(createSource(//
    "void f() => 42;"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.RETURN_OF_INVALID_TYPE);
    verify(source);
  }

  public void test_returnOfInvalidType_function() throws Exception {
    Source source = addSource(createSource(//
    "int f() { return '0'; }"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.RETURN_OF_INVALID_TYPE);
    verify(source);
  }

  public void test_returnOfInvalidType_getter() throws Exception {
    Source source = addSource(createSource(//
    "int get g { return '0'; }"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.RETURN_OF_INVALID_TYPE);
    verify(source);
  }

  public void test_returnOfInvalidType_localFunction() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  String m() {",
        "    int f() { return '0'; }",
        "    return '0';",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.RETURN_OF_INVALID_TYPE);
    verify(source);
  }

  public void test_returnOfInvalidType_method() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int f() { return '0'; }",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.RETURN_OF_INVALID_TYPE);
    verify(source);
  }

  public void test_returnOfInvalidType_void() throws Exception {
    Source source = addSource(createSource(//
    "void f() { return 42; }"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.RETURN_OF_INVALID_TYPE);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_classTypeAlias() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class C {}",
        "class G<E extends A> {}",
        "class D = G<B> with C;"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_extends() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "class C extends G<B>{}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
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
    assertErrors(source, StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_functionReturnType() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "G<B> f() { return null; }"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_functionTypeAlias() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "typedef G<B> f();"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_functionTypedFormalParameter() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "f(G<B> h()) {}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_implements() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "class C implements G<B>{}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_is() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "var b = 1 is G<B>;"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_methodReturnType() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "class C {",
        "  G<B> m() { return null; }",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_new() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "f() { return new G<B>(); }"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
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
    assertErrors(source, StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_parameter() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "f(G<B> g) {}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
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
    assertErrors(
        source,
        StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS,
        StaticWarningCode.REDIRECT_TO_INVALID_RETURN_TYPE);
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
    assertErrors(source, StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
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
    assertErrors(source, StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_variableDeclaration() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "G<B> g;"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_with() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {}",
        "class G<E extends A> {}",
        "class C extends Object with G<B>{}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.TYPE_ARGUMENT_NOT_MATCHING_BOUNDS);
    verify(source);
  }

  public void test_typeParameterSupertypeOfItsBound() throws Exception {
    Source source = addSource(createSource(//
        "class A<T extends T> {",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.TYPE_PARAMETER_SUPERTYPE_OF_ITS_BOUND);
    verify(source);
  }

  public void test_typePromotion_booleanAnd_useInRight_accessedInClosureRight_mutated()
      throws Exception {
    Source source = addSource(createSource(//
        "callMe(f()) { f(); }",
        "main(Object p) {",
        "  (p is String) && callMe(() { p.length; });",
        "  p = 0;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_GETTER);
  }

  public void test_typePromotion_booleanAnd_useInRight_mutatedInLeft() throws Exception {
    Source source = addSource(createSource(//
        "main(Object p) {",
        "  ((p is String) && ((p = 42) == 42)) && p.length != 0;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_GETTER);
  }

  public void test_typePromotion_booleanAnd_useInRight_mutatedInRight() throws Exception {
    Source source = addSource(createSource(//
        "main(Object p) {",
        "  (p is String) && (((p = 42) == 42) && p.length != 0);",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_GETTER);
  }

  public void test_typePromotion_conditional_useInThen_accessedInClosure_hasAssignment_after()
      throws Exception {
    Source source = addSource(createSource(//
        "callMe(f()) { f(); }",
        "main(Object p) {",
        "  p is String ? callMe(() { p.length; }) : 0;",
        "  p = 42;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_GETTER);
  }

  public void test_typePromotion_conditional_useInThen_accessedInClosure_hasAssignment_before()
      throws Exception {
    Source source = addSource(createSource(//
        "callMe(f()) { f(); }",
        "main(Object p) {",
        "  p = 42;",
        "  p is String ? callMe(() { p.length; }) : 0;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_GETTER);
  }

  public void test_typePromotion_conditional_useInThen_hasAssignment() throws Exception {
    Source source = addSource(createSource(//
        "main(Object p) {",
        "  p is String ? (p.length + (p = 42)) : 0;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_GETTER);
  }

  public void test_typePromotion_if_accessedInClosure_hasAssignment() throws Exception {
    Source source = addSource(createSource(//
        "callMe(f()) { f(); }",
        "main(Object p) {",
        "  if (p is String) {",
        "    callMe(() {",
        "      p.length;",
        "    });",
        "  }",
        "  p = 0;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_GETTER);
  }

  public void test_typePromotion_if_and_right_hasAssignment() throws Exception {
    Source source = addSource(createSource(//
        "main(Object p) {",
        "  if (p is String && (p = null) == null) {",
        "    p.length;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_GETTER);
  }

  public void test_typePromotion_if_extends_notMoreSpecific_dynamic() throws Exception {
    Source source = addSource(createSource(//
        "class V {}",
        "class A<T> {}",
        "class B<S> extends A<S> {",
        "  var b;",
        "}",
        "",
        "main(A<V> p) {",
        "  if (p is B) {",
        "    p.b;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_GETTER);
  }

  public void test_typePromotion_if_extends_notMoreSpecific_notMoreSpecificTypeArg()
      throws Exception {
    Source source = addSource(createSource(//
        "class V {}",
        "class A<T> {}",
        "class B<S> extends A<S> {",
        "  var b;",
        "}",
        "",
        "main(A<V> p) {",
        "  if (p is B<int>) {",
        "    p.b;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_GETTER);
  }

  public void test_typePromotion_if_hasAssignment_after() throws Exception {
    Source source = addSource(createSource(//
        "main(Object p) {",
        "  if (p is String) {",
        "    p.length;",
        "    p = 0;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_GETTER);
  }

  public void test_typePromotion_if_hasAssignment_before() throws Exception {
    Source source = addSource(createSource(//
        "main(Object p) {",
        "  if (p is String) {",
        "    p = 0;",
        "    p.length;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_GETTER);
  }

  public void test_typePromotion_if_hasAssignment_inClosure_anonymous_after() throws Exception {
    Source source = addSource(createSource(//
        "main(Object p) {",
        "  if (p is String) {",
        "    p.length;",
        "  }",
        "  () {p = 0;};",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_GETTER);
  }

  public void test_typePromotion_if_hasAssignment_inClosure_anonymous_before() throws Exception {
    Source source = addSource(createSource(//
        "main(Object p) {",
        "  () {p = 0;};",
        "  if (p is String) {",
        "    p.length;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_GETTER);
  }

  public void test_typePromotion_if_hasAssignment_inClosure_function_after() throws Exception {
    Source source = addSource(createSource(//
        "main(Object p) {",
        "  if (p is String) {",
        "    p.length;",
        "  }",
        "  f() {p = 0;};",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_GETTER);
  }

  public void test_typePromotion_if_hasAssignment_inClosure_function_before() throws Exception {
    Source source = addSource(createSource(//
        "main(Object p) {",
        "  f() {p = 0;};",
        "  if (p is String) {",
        "    p.length;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_GETTER);
  }

  public void test_typePromotion_if_implements_notMoreSpecific_dynamic() throws Exception {
    Source source = addSource(createSource(//
        "class V {}",
        "class A<T> {}",
        "class B<S> implements A<S> {",
        "  var b;",
        "}",
        "",
        "main(A<V> p) {",
        "  if (p is B) {",
        "    p.b;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_GETTER);
  }

  public void test_typePromotion_if_with_notMoreSpecific_dynamic() throws Exception {
    Source source = addSource(createSource(//
        "class V {}",
        "class A<T> {}",
        "class B<S> extends Object with A<S> {",
        "  var b;",
        "}",
        "",
        "main(A<V> p) {",
        "  if (p is B) {",
        "    p.b;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_GETTER);
  }

  public void test_undefinedFunction() throws Exception {
    Source source = addSource(createSource(//
        "void f() {",
        "  g();",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_FUNCTION);
  }

  public void test_undefinedFunction_hasImportPrefix() throws Exception {
    Source source = addSource(createSource(//
        "import 'lib.dart' as f;",
        "main() { return f(); }"));
    addNamedSource("/lib.dart", "library lib;");
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_FUNCTION);
  }

  public void test_undefinedFunction_inCatch() throws Exception {
    Source source = addSource(createSource(//
        "void f() {",
        "  try {",
        "  } on Object {",
        "    g();",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_FUNCTION);
  }

  public void test_undefinedFunction_inImportedLib() throws Exception {
    Source source = addSource(createSource(//
        "import 'lib.dart' as f;",
        "main() { return f.g(); }"));
    addNamedSource("/lib.dart", createSource(//
        "library lib;",
        "h() {}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_FUNCTION);
  }

  public void test_undefinedGetter() throws Exception {
    Source source = addSource(createSource(//
        "class T {}",
        "f(T e) { return e.m; }"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_GETTER);
  }

  public void test_undefinedGetter_proxy_annotation_fakeProxy() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "class Fake {",
        "  const Fake();",
        "}",
        "const proxy = const Fake();",
        "@proxy class PrefixProxy {}",
        "main() {",
        "  new PrefixProxy().foo;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_GETTER);
  }

  public void test_undefinedGetter_static() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "var a = A.B;"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_GETTER);
  }

  public void test_undefinedGetter_void() throws Exception {
    Source source = addSource(createSource(//
        "class T {",
        "  void m() {}",
        "}",
        "f(T e) { return e.m().f; }"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_GETTER);
  }

  public void test_undefinedGetter_wrongNumberOfTypeArguments_tooLittle() throws Exception {
    Source source = addSource(createSource(//
        "class A<K, V> {",
        "  K element;",
        "}",
        "main(A<int> a) {",
        "  a.element.anyGetterExistsInDynamic;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS);
    verify(source);
  }

  public void test_undefinedGetter_wrongNumberOfTypeArguments_tooMany() throws Exception {
    Source source = addSource(createSource(//
        "class A<E> {",
        "  E element;",
        "}",
        "main(A<int,int> a) {",
        "  a.element.anyGetterExistsInDynamic;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS);
    verify(source);
  }

  public void test_undefinedGetter_wrongOfTypeArgument() throws Exception {
    Source source = addSource(createSource(//
        "class A<E> {",
        "  E element;",
        "}",
        "main(A<NoSuchType> a) {",
        "  a.element.anyGetterExistsInDynamic;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.NON_TYPE_AS_TYPE_ARGUMENT);
    verify(source);
  }

  public void test_undefinedMethod() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  void m() {",
        "    n();",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_METHOD);
  }

  public void test_undefinedMethod_assignmentExpression() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {",
        "  f(A a) {",
        "    A a2 = new A();",
        "    a += a2;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_METHOD);
  }

  public void test_undefinedMethod_ignoreTypePropagation() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B extends A {",
        "  m() {}",
        "}",
        "class C {",
        "  f() {",
        "    A a = new B();",
        "    a.m();",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_METHOD);
  }

  public void test_undefinedMethod_private() throws Exception {
    addNamedSource("/lib.dart", createSource(//
        "library lib;",
        "class A {",
        "  _foo() {}",
        "}"));
    Source source = addSource(createSource(//
        "import 'lib.dart';",
        "class B extends A {",
        "  test() {",
        "    _foo();",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_METHOD);
  }

  public void test_undefinedMethod_proxy_annotation_fakeProxy() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "class Fake {",
        "  const Fake();",
        "}",
        "const proxy = const Fake();",
        "@proxy class PrefixProxy {}",
        "main() {",
        "  new PrefixProxy().foo();",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_METHOD);
  }

  public void test_undefinedOperator_indexBoth() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "f(A a) {",
        "  a[0]++;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_OPERATOR);
  }

  public void test_undefinedOperator_indexGetter() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "f(A a) {",
        "  a[0];",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_OPERATOR);
  }

  public void test_undefinedOperator_indexSetter() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "f(A a) {",
        "  a[0] = 1;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_OPERATOR);
  }

  public void test_undefinedOperator_plus() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "f(A a) {",
        "  a + 1;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_OPERATOR);
  }

  public void test_undefinedOperator_postfixExpression() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "f(A a) {",
        "  a++;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_OPERATOR);
  }

  public void test_undefinedOperator_prefixExpression() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "f(A a) {",
        "  ++a;",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_OPERATOR);
  }

  public void test_undefinedSetter() throws Exception {
    Source source = addSource(createSource(//
        "class T {}",
        "f(T e1) { e1.m = 0; }"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_SETTER);
  }

  public void test_undefinedSetter_static() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "f() { A.B = 0;}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_SETTER);
  }

  public void test_undefinedSetter_void() throws Exception {
    Source source = addSource(createSource(//
        "class T {",
        "  void m() {}",
        "}",
        "f(T e) { e.m().f = 0; }"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_SETTER);
  }

  public void test_undefinedSuperMethod() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B extends A {",
        "  m() { return super.m(); }",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_SUPER_METHOD);
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
    assertErrors(source, StaticTypeWarningCode.UNQUALIFIED_REFERENCE_TO_NON_LOCAL_STATIC_MEMBER);
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
    assertErrors(source, StaticTypeWarningCode.UNQUALIFIED_REFERENCE_TO_NON_LOCAL_STATIC_MEMBER);
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
    assertErrors(source, StaticTypeWarningCode.UNQUALIFIED_REFERENCE_TO_NON_LOCAL_STATIC_MEMBER);
    verify(source);
  }

  public void test_wrongNumberOfTypeArguments_classAlias() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class M {}",
        "class B<F extends num> = A<F> with M;"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS);
    verify(source);
  }

  public void test_wrongNumberOfTypeArguments_tooFew() throws Exception {
    Source source = addSource(createSource(//
        "class A<E, F> {}",
        "A<A> a = null;"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS);
    verify(source);
  }

  public void test_wrongNumberOfTypeArguments_tooMany() throws Exception {
    Source source = addSource(createSource(//
        "class A<E> {}",
        "A<A, A> a = null;"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS);
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
    assertErrors(source, StaticTypeWarningCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS);
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
    assertErrors(source, StaticTypeWarningCode.WRONG_NUMBER_OF_TYPE_ARGUMENTS);
    verify(source);
  }
}
