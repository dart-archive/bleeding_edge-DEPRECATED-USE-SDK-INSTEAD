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
package com.google.dart.engine.resolver;

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.source.Source;

public class SimpleResolverTest extends ResolverTestCase {
  public void fail_caseExpressionTypeImplementsEquals_Object() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class IntWrapper {",
        "  final int value;",
        "  const IntWrapper(this.value);",
        "}",
        "",
        "f(IntWrapper intWrapper) {",
        "  switch(intWrapper) {",
        "    case(const IntWrapper(1)) : return 1;",
        "    default: return 0;",
        "  }",
        "}"));
    resolve(source);
    // Fails since "const IntWrapper(1)" is not considered constant by the constant evaluator yet,
    // thus, NON_CONSTANT_CASE_EXPRESSION is generated on the switch expression.
    assertNoErrors();
    verify(source);
  }

  public void fail_staticInvocation() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  static int get g => (a,b) => 0;",
        "}",
        "class B {",
        "  f() {",
        "    A.g(1,0);",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_argumentDefinitionTestNonParameter_formalParameter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f(var v) {",
        "  return ?v;",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_argumentDefinitionTestNonParameter_namedParameter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f({var v : 0}) {",
        "  return ?v;",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_argumentDefinitionTestNonParameter_optionalParameter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f([var v]) {",
        "  return ?v;",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_breakWithoutLabelInSwitch() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  void m(int i) {",
        "    switch (i) {",
        "      case 0:",
        "        break;",
        "    }",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_builtInIdentifierAsType_dynamic() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  dynamic x;",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_caseExpressionTypeImplementsEquals_int() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f(int i) {",
        "  switch(i) {",
        "    case(1) : return 1;",
        "    default: return 0;",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_caseExpressionTypeImplementsEquals_String() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f(String s) {",
        "  switch(s) {",
        "    case('1') : return 1;",
        "    default: return 0;",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_class_extends_implements() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A extends B implements C {}",
        "class B {}",
        "class C {}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_constConstructorWithNonFinalField_const() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  const int x;",
        "  const A() {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_constConstructorWithNonFinalField_final() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  final int x;",
        "  const A() {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_constConstructorWithNonFinalField_syntheticField() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  const A();",
        "  set x(value) {}",
        "  get x {return 0;}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_duplicateDefinition_getter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "bool get a => true;"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_empty() throws Exception {
    Source source = addSource("/test.dart", "");
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_forEachLoops_nonConflicting() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  List list = [1,2,3];",
        "  for (int x in list) {}",
        "  for (int x in list) {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_forLoops_nonConflicting() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  for (int i = 0; i < 3; i++) {",
        "  }",
        "  for (int i = 0; i < 3; i++) {",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_functionTypeAlias() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "typedef bool P(e);",
        "class A {",
        "  P p;",
        "  m(e) {",
        "    if (p(e)) {}",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_getterAndSetterWithDifferentTypes() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  int get f => 0;",
        "  void set f(String s) {}",
        "}",
        "g (A a) {",
        "  a.f = a.f.toString();",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_invalidAssignment() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  var x;",
        "  var y;",
        "  x = y;",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_invalidAssignment_toDynamic() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  var g;",
        "  g = () => 0;",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_invocationOfNonFunction_dynamic() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  var f;",
        "}",
        "class B extends A {",
        "  g() {",
        "    f();",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_invocationOfNonFunction_getter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  var g;",
        "}",
        "f() {",
        "  A a;",
        "  a.g();",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_invocationOfNonFunction_localVariable() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  var g;",
        "  g();",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_isValidMixin_badSuperclass() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A extends B {}",
        "class B {}"));
    LibraryElement library = resolve(source);
    assertNotNull(library);
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    assertNotNull(unit);
    ClassElement[] classes = unit.getTypes();
    assertLength(2, classes);
    assertFalse(classes[0].isValidMixin());
    assertNoErrors();
    verify(source);
  }

  public void test_isValidMixin_constructor() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  A() {}",
        "}"));
    LibraryElement library = resolve(source);
    assertNotNull(library);
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    assertNotNull(unit);
    ClassElement[] classes = unit.getTypes();
    assertLength(1, classes);
    assertFalse(classes[0].isValidMixin());
    assertNoErrors();
    verify(source);
  }

  public void test_isValidMixin_super() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  toString() {",
        "    return super.toString();",
        "  }",
        "}"));
    LibraryElement library = resolve(source);
    assertNotNull(library);
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    assertNotNull(unit);
    ClassElement[] classes = unit.getTypes();
    assertLength(1, classes);
    assertFalse(classes[0].isValidMixin());
    assertNoErrors();
    verify(source);
  }

  public void test_isValidMixin_valid() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}"));
    LibraryElement library = resolve(source);
    assertNotNull(library);
    CompilationUnitElement unit = library.getDefiningCompilationUnit();
    assertNotNull(unit);
    ClassElement[] classes = unit.getTypes();
    assertLength(1, classes);
    assertTrue(classes[0].isValidMixin());
    assertNoErrors();
    verify(source);
  }

  public void test_methodCascades() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  void m1() {}",
        "  void m2() {}",
        "  void m() {",
        "    A a = new A();",
        "    a..m1()",
        "     ..m2();",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_methodCascades_withSetter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  String name;",
        "  void m1() {}",
        "  void m2() {}",
        "  void m() {",
        "    A a = new A();",
        "    a..m1()",
        "     ..name = 'name'",
        "     ..m2();",
        "  }",
        "}"));
    resolve(source);
    // failing with error code: INVOCATION_OF_NON_FUNCTION
    assertNoErrors();
    verify(source);
  }

  public void test_newWithAbstractClass_factory() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "abstract class A {",
        "  factory A() { return new B(); }",
        "}",
        "class B implements A {",
        "  B() {}",
        "}",
        "A f() {",
        "  return new A();",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_nonBoolExpression_assert_bool() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() {",
        "  assert(true);",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_nonBoolExpression_assert_functionType() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "bool makeAssertion() => true;",
        "f() {",
        "  assert(makeAssertion);",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_resolveAgainstNull() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f(var p) {",
        "  return null == p;",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_returnOfInvalidType_dynamic() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  static void testLogicalOp() {",
        "    testOr(a, b, onTypeError) {",
        "      try {",
        "        return a || b;",
        "      } on TypeError catch (t) {",
        "        return onTypeError;",
        "      }",
        "    }",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_returnOfInvalidType_subtype() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "class B extends A {}",
        "A f(B b) { return b; }"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_returnOfInvalidType_supertype() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "class B extends A {}",
        "B f(A a) { return a; }"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_const() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "class B extends A {}",
        "class G<E extends A> {",
        "  const G() {}",
        "}",
        "f() { return const G<B>(); }"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_new() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {}",
        "class B extends A {}",
        "class G<E extends A> {}",
        "f() { return new G<B>(); }"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }
}
