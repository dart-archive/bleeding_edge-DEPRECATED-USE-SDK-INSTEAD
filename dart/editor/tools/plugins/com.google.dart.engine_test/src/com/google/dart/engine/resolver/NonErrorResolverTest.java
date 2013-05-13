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

import com.google.dart.engine.source.Source;

public class NonErrorResolverTest extends ResolverTestCase {
  public void test_ambiguousExport() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "export 'lib1.dart';",
        "export 'lib2.dart';"));
    addSource("/lib1.dart", "class M {}");
    addSource("/lib2.dart", "class N {}");
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_ambiguousExport_combinators_hide() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "export 'lib1.dart';",
        "export 'lib2.dart' hide B;"));
    addSource("/lib1.dart", createSource(//
        "library L1;",
        "class A {}",
        "class B {}"));
    addSource("/lib2.dart", createSource(//
        "library L2;",
        "class B {}",
        "class C {}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_ambiguousExport_combinators_show() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "export 'lib1.dart';",
        "export 'lib2.dart' show C;"));
    addSource("/lib1.dart", createSource(//
        "library L1;",
        "class A {}",
        "class B {}"));
    addSource("/lib2.dart", createSource(//
        "library L2;",
        "class B {}",
        "class C {}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_argumentDefinitionTestNonParameter_formalParameter() throws Exception {
    Source source = addSource(createSource(//
        "f(var v) {",
        "  return ?v;",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_argumentDefinitionTestNonParameter_namedParameter() throws Exception {
    Source source = addSource(createSource(//
        "f({var v : 0}) {",
        "  return ?v;",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_argumentDefinitionTestNonParameter_optionalParameter() throws Exception {
    Source source = addSource(createSource(//
        "f([var v]) {",
        "  return ?v;",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_assignmentToFinals_importWithPrefix() throws Exception {
    Source source = addSource(createSource(//
        "library lib;",
        "import 'lib1.dart' as foo;",
        "main() {",
        "  foo.x = true;",
        "}"));
    addSource("/lib1.dart", createSource(//
        "library lib1;",
        "bool x = false;"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_breakWithoutLabelInSwitch() throws Exception {
    Source source = addSource(createSource(//
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
    Source source = addSource(createSource(//
        "f() {",
        "  dynamic x;",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_caseBlockNotTerminated() throws Exception {
    Source source = addSource(createSource(//
        "f(int p) {",
        "  for (int i = 0; i < 10; i++) {",
        "    switch (p) {",
        "      case 0:",
        "        break;",
        "      case 1:",
        "        continue;",
        "      case 2:",
        "        return;",
        "      case 3:",
        "        throw new Object();",
        "      case 4:",
        "      case 5:",
        "        return;",
        "      case 6:",
        "      default:",
        "        return;",
        "    }",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_caseExpressionTypeImplementsEquals_int() throws Exception {
    Source source = addSource(createSource(//
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

  public void test_caseExpressionTypeImplementsEquals_Object() throws Exception {
    Source source = addSource(createSource(//
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
    assertNoErrors();
    verify(source);
  }

  public void test_caseExpressionTypeImplementsEquals_String() throws Exception {
    Source source = addSource(createSource(//
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

  public void test_concreteClassWithAbstractMember() throws Exception {
    Source source = addSource(createSource(//
        "abstract class A {",
        "  m();",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_constConstructorWithNonFinalField_constInstanceVar() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const int x = 0;",
        "  const A() {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_constConstructorWithNonFinalField_finalInstanceVar() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final int x = 0;",
        "  const A() {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_constConstructorWithNonFinalField_static() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static int x;",
        "  const A() {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_constConstructorWithNonFinalField_syntheticField() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const A();",
        "  set x(value) {}",
        "  get x {return 0;}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_defaultValueInFunctionTypeAlias() throws Exception {
    Source source = addSource(createSource(//
    "typedef F([x]);"));
    resolve(source);
    assertErrors();
    verify(source);
  }

  public void test_duplicateDefinition_emptyName() throws Exception {
    // Note: This code has two FunctionElements '() {}' with an empty name, this tests that the
    // empty string is not put into the scope (more than once).
    Source source = addSource(createSource(//
        "Map _globalMap = {",
        "  'a' : () {},",
        "  'b' : () {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_duplicateDefinition_getter() throws Exception {
    Source source = addSource(createSource(//
    "bool get a => true;"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_exportOfNonLibrary_libraryDeclared() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "export 'lib1.dart';"));
    addSource("/lib1.dart", createSource(//
        "library lib1;"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_exportOfNonLibrary_libraryNotDeclared() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "export 'lib1.dart';"));
    addSource("/lib1.dart", createSource(//
        ""));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_fieldInitializedByMultipleInitializers() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int x;",
        "  int y;",
        "  A() : x = 0, y = 0 {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_fieldInitializedInInitializerAndDeclaration_fieldNotFinal() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int x = 0;",
        "  A() : x = 1 {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_fieldInitializedInInitializerAndDeclaration_finalFieldNotSet() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final int x;",
        "  A() : x = 1 {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_fieldInitializerOutsideConstructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int x;",
        "  A(this.x) {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_fieldInitializerOutsideConstructor_defaultParameters() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int x;",
        "  A([this.x]) {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_finalInitializedInDeclarationAndConstructor_initializer() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final x;",
        "  A() : x = 1 {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_finalInitializedInDeclarationAndConstructor_initializingFormal()
      throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final x;",
        "  A(this.x) {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_finalNotInitialized_atDeclaration() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final int x = 0;",
        "  A() {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_finalNotInitialized_fieldFormal() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final int x = 0;",
        "  A() {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_finalNotInitialized_initializer() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final int x;",
        "  A() : x = 0 {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_importDuplicatedLibraryName() throws Exception {
    Source source = addSource(createSource(//
        "library test;",
        "import 'lib.dart';",
        "import 'lib.dart';"));
    addSource("/lib.dart", "library lib;");
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_importOfNonLibrary_libraryDeclared() throws Exception {
    Source source = addSource(createSource(//
        "library lib;",
        "import 'part.dart';"));
    addSource("/part.dart", createSource(//
        "library lib1;"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_importOfNonLibrary_libraryNotDeclared() throws Exception {
    Source source = addSource(createSource(//
        "library lib;",
        "import 'part.dart';"));
    addSource("/part.dart", createSource(//
        ""));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_inconsistentCaseExpressionTypes() throws Exception {
    Source source = addSource(createSource(//
        "f(var p) {",
        "  switch (p) {",
        "    case 1:",
        "      break;",
        "    case 2:",
        "      break;",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_initializingFormalForNonExistantField() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int x;",
        "  A(this.x) {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_invalidAssignment() throws Exception {
    Source source = addSource(createSource(//
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
    Source source = addSource(createSource(//
        "f() {",
        "  var g;",
        "  g = () => 0;",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_invalidOverrideReturnType_sameType() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  int m() { return 0; }",
        "}",
        "class B extends A {",
        "  int m() { return 1; }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_invalidOverrideReturnType_subclassType() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  num m() { return 0; }",
        "}",
        "class B extends A {",
        "  int m() { return 1; }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_invalidReferenceToThis_constructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A() {",
        "    var v = this;",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_invalidReferenceToThis_instanceMethod() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m() {",
        "    var v = this;",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_invalidTypeArgumentInConstList() throws Exception {
    Source source = addSource(createSource(//
        "class A<E> {",
        "  m() {",
        "    return <E>[]",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_invalidTypeArgumentInConstMap() throws Exception {
    Source source = addSource(createSource(//
        "class A<E> {",
        "  m() {",
        "    return <String, E>{}",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_invocationOfNonFunction_dynamic() throws Exception {
    Source source = addSource(createSource(//
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
    Source source = addSource(createSource(//
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
    Source source = addSource(createSource(//
        "f() {",
        "  var g;",
        "  g();",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_memberWithClassName_setter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  set A(v) {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_mixinDeclaresConstructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m() {}",
        "}",
        "class B extends Object with A {}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_mixinDeclaresConstructor_factory() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  factory A() {}",
        "}",
        "class B extends Object with A {}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_mixinInheritsFromNotObject_classDeclaration_mixTypedef() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "typedef B = Object with A {}",
        "class C extends Object with B {}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_mixinInheritsFromNotObject_typedef_mixTypedef() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "typedef B = Object with A {}",
        "typedef C = Object with B {}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_multipleSuperInitializers_no() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B extends A {",
        "  B() {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_multipleSuperInitializers_single() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B extends A {",
        "  B() : super() {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_newWithAbstractClass_factory() throws Exception {
    Source source = addSource(createSource(//
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
    Source source = addSource(createSource(//
        "f() {",
        "  assert(true);",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_nonBoolExpression_assert_functionType() throws Exception {
    Source source = addSource(createSource(//
        "bool makeAssertion() => true;",
        "f() {",
        "  assert(makeAssertion);",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_nonConstMapAsExpressionStatement_const() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  const {'a' : 0, 'b' : 1};",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_nonConstMapAsExpressionStatement_notExpressionStatement() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  var m = {'a' : 0, 'b' : 1};",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_nonConstMapAsExpressionStatement_typeArguments() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  <String, int> {'a' : 0, 'b' : 1};",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_nonTypeInCatchClause_isClass() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  try {",
        "  } on String catch e {",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_nonTypeInCatchClause_isFunctionTypeAlias() throws Exception {
    Source source = addSource(createSource(//
        "typedef F();",
        "f() {",
        "  try {",
        "  } on F catch e {",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_nonTypeInCatchClause_isTypeVariable() throws Exception {
    Source source = addSource(createSource(//
        "class A<T> {",
        "  f() {",
        "    try {",
        "    } on T catch e {",
        "    }",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_nonTypeInCatchClause_noType() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  try {",
        "  } catch e {",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_nonVoidReturnForOperator_no() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  operator []=(a, b) {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_nonVoidReturnForOperator_void() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  void operator []=(a, b) {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_nonVoidReturnForSetter_function_no() throws Exception {
    Source source = addSource("set x(v) {}");
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_nonVoidReturnForSetter_function_void() throws Exception {
    Source source = addSource("void set x(v) {}");
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_nonVoidReturnForSetter_method_no() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  set x(v) {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_nonVoidReturnForSetter_method_void() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  void set x(v) {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_optionalParameterInOperator_required() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  operator +(p) {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_rethrowOutsideCatch() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  void m() {",
        "    try {} catch (e) {rethrow;}",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_returnInGenerativeConstructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A() { return; }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_returnOfInvalidType_dynamic() throws Exception {
    Source source = addSource(createSource(//
        "class TypeError {}",
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
    Source source = addSource(createSource(//
        "class A {}",
        "class B extends A {}",
        "A f(B b) { return b; }"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_returnOfInvalidType_supertype() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B extends A {}",
        "B f(A a) { return a; }"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_returnWithoutValue_noReturnType() throws Exception {
    Source source = addSource(createSource(//
    "f() { return; }"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_returnWithoutValue_void() throws Exception {
    Source source = addSource(createSource(//
    "void f() { return; }"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_typeArgumentNotMatchingBounds_const() throws Exception {
    Source source = addSource(createSource(//
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
    Source source = addSource(createSource(//
        "class A {}",
        "class B extends A {}",
        "class G<E extends A> {}",
        "f() { return new G<B>(); }"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_undefinedGetter_noSuchMethod_getter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  noSuchMethod(invocation) {}",
        "}",
        "f() {",
        "  (new A()).g;",
        "}"));
    resolve(source);
    assertNoErrors();
  }

  public void test_undefinedGetter_noSuchMethod_getter2() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  noSuchMethod(invocation) {}",
        "}",
        "class B {",
        "  A a = new A();",
        "  m() {",
        "    a.g;",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
  }

  public void test_undefinedGetter_typeSubstitution() throws Exception {
    Source source = addSource(createSource(//
        "class A<E> {",
        "  E element;",
        "}",
        "class B extends A<List> {",
        "  m() {",
        "    element.last;",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_undefinedIdentifier_hide() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "export 'lib1.dart' hide a;"));
    addSource("/lib1.dart", createSource(//
        "library lib1;"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_undefinedIdentifier_noSuchMethod() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  noSuchMethod(invocation) {}",
        "  f() {",
        "    var v = a;",
        "  }",
        "}"));
    resolve(source);
    assertNoErrors();
  }

  public void test_undefinedIdentifier_show() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "export 'lib1.dart' show a;"));
    addSource("/lib1.dart", createSource(//
        "library lib1;"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_undefinedMethod_noSuchMethod() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  noSuchMethod(invocation) {}",
        "}",
        "f() {",
        "  (new A()).m();",
        "}"));
    resolve(source);
    assertNoErrors();
  }

  public void test_undefinedOperator_tilde() throws Exception {
    Source source = addSource(createSource(//
        "const A = 3;",
        "const B = ~((1 << A) - 1);"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_undefinedSetter_noSuchMethod() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  noSuchMethod(invocation) {}",
        "}",
        "f() {",
        "  (new A()).s = 1;",
        "}"));
    resolve(source);
    assertNoErrors();
  }

  public void test_wrongNumberOfParametersForOperator_index() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  operator []=(a, b) {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  public void test_wrongNumberOfParametersForOperator_minus() throws Exception {
    check_wrongNumberOfParametersForOperator("-", "");
    check_wrongNumberOfParametersForOperator("-", "a");
  }

  public void test_wrongNumberOfParametersForOperator1() throws Exception {
    check_wrongNumberOfParametersForOperator1("<");
    check_wrongNumberOfParametersForOperator1(">");
    check_wrongNumberOfParametersForOperator1("<=");
    check_wrongNumberOfParametersForOperator1(">=");
    check_wrongNumberOfParametersForOperator1("+");
    check_wrongNumberOfParametersForOperator1("/");
    check_wrongNumberOfParametersForOperator1("~/");
    check_wrongNumberOfParametersForOperator1("*");
    check_wrongNumberOfParametersForOperator1("%");
    check_wrongNumberOfParametersForOperator1("|");
    check_wrongNumberOfParametersForOperator1("^");
    check_wrongNumberOfParametersForOperator1("&");
    check_wrongNumberOfParametersForOperator1("<<");
    check_wrongNumberOfParametersForOperator1(">>");
    check_wrongNumberOfParametersForOperator1("[]");
  }

  public void test_wrongNumberOfParametersForSetter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  set x(a) {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
  }

  private void check_wrongNumberOfParametersForOperator(String name, String parameters)
      throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  operator " + name + "(" + parameters + ") {}",
        "}"));
    resolve(source);
    assertNoErrors();
    verify(source);
    reset();
  }

  private void check_wrongNumberOfParametersForOperator1(String name) throws Exception {
    check_wrongNumberOfParametersForOperator(name, "a");
  }
}
