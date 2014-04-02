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

import com.google.dart.engine.error.CompileTimeErrorCode;
import com.google.dart.engine.error.HintCode;
import com.google.dart.engine.error.StaticTypeWarningCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.source.Source;

public class StaticWarningCodeTest extends ResolverTestCase {
  public void fail_undefinedGetter() throws Exception {
    Source source = addSource(createSource(//
    // TODO
    ));
    resolve(source);
    assertErrors(source, StaticWarningCode.UNDEFINED_GETTER);
    verify(source);
  }

  public void fail_undefinedIdentifier_commentReference() throws Exception {
    Source source = addSource(createSource(//
        "/** [m] xxx [new B.c] */",
        "class A {",
        "}"));
    resolve(source);
    assertErrors(
        source,
        StaticWarningCode.UNDEFINED_IDENTIFIER,
        StaticWarningCode.UNDEFINED_IDENTIFIER);
  }

  public void fail_undefinedSetter() throws Exception {
    Source source = addSource(createSource(//
        "class C {}",
        "f(var p) {",
        "  C.m = 0;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.UNDEFINED_SETTER);
    verify(source);
  }

  public void test_ambiguousImport_as() throws Exception {
    Source source = addSource(createSource(//
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "f(p) {p as N;}"));
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class N {}"));
    addNamedSource("/lib2.dart", createSource(//
        "library lib2;",
        "class N {}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.AMBIGUOUS_IMPORT);
  }

  public void test_ambiguousImport_extends() throws Exception {
    Source source = addSource(createSource(//
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "class A extends N {}"));
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class N {}"));
    addNamedSource("/lib2.dart", createSource(//
        "library lib2;",
        "class N {}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.AMBIGUOUS_IMPORT, CompileTimeErrorCode.EXTENDS_NON_CLASS);
  }

  public void test_ambiguousImport_implements() throws Exception {
    Source source = addSource(createSource(//
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "class A implements N {}"));
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class N {}"));
    addNamedSource("/lib2.dart", createSource(//
        "library lib2;",
        "class N {}"));
    resolve(source);
    assertErrors(
        source,
        StaticWarningCode.AMBIGUOUS_IMPORT,
        CompileTimeErrorCode.IMPLEMENTS_NON_CLASS);
  }

  public void test_ambiguousImport_inPart() throws Exception {
    Source source = addSource(createSource(//
        "library lib;",
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "part 'part.dart';"));
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class N {}"));
    addNamedSource("/lib2.dart", createSource(//
        "library lib2;",
        "class N {}"));
    Source partSource = addNamedSource("/part.dart", createSource(//
        "part of lib;",
        "class A extends N {}"));
    resolve(source);
    assertErrors(
        partSource,
        StaticWarningCode.AMBIGUOUS_IMPORT,
        CompileTimeErrorCode.EXTENDS_NON_CLASS);
  }

  public void test_ambiguousImport_instanceCreation() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "f() {new N();}"));
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class N {}"));
    addNamedSource("/lib2.dart", createSource(//
        "library lib2;",
        "class N {}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.AMBIGUOUS_IMPORT);
  }

  public void test_ambiguousImport_is() throws Exception {
    Source source = addSource(createSource(//
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "f(p) {p is N;}"));
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class N {}"));
    addNamedSource("/lib2.dart", createSource(//
        "library lib2;",
        "class N {}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.AMBIGUOUS_IMPORT);
  }

  public void test_ambiguousImport_qualifier() throws Exception {
    Source source = addSource(createSource(//
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "g() { N.FOO; }"));
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class N {}"));
    addNamedSource("/lib2.dart", createSource(//
        "library lib2;",
        "class N {}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.AMBIGUOUS_IMPORT);
  }

  public void test_ambiguousImport_typeAnnotation() throws Exception {
    Source source = addSource(createSource(//
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "typedef N FT(N p);",
        "N f(N p) {",
        "  N v;",
        "  return null;",
        "}",
        "class A {",
        "  N m() { return null; }",
        "}",
        "class B<T extends N> {}"));
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class N {}"));
    addNamedSource("/lib2.dart", createSource(//
        "library lib2;",
        "class N {}"));
    resolve(source);
    assertErrors(
        source,
        StaticWarningCode.AMBIGUOUS_IMPORT,
        StaticWarningCode.AMBIGUOUS_IMPORT,
        StaticWarningCode.AMBIGUOUS_IMPORT,
        StaticWarningCode.AMBIGUOUS_IMPORT,
        StaticWarningCode.AMBIGUOUS_IMPORT,
        StaticWarningCode.AMBIGUOUS_IMPORT,
        StaticWarningCode.AMBIGUOUS_IMPORT);
  }

  public void test_ambiguousImport_typeArgument_annotation() throws Exception {
    Source source = addSource(createSource(//
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "class A<T> {}",
        "A<N> f() { return null; }"));
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class N {}"));
    addNamedSource("/lib2.dart", createSource(//
        "library lib2;",
        "class N {}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.AMBIGUOUS_IMPORT);
  }

  public void test_ambiguousImport_typeArgument_instanceCreation() throws Exception {
    Source source = addSource(createSource(//
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "class A<T> {}",
        "f() {new A<N>();}"));
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "class N {}"));
    addNamedSource("/lib2.dart", createSource(//
        "library lib2;",
        "class N {}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.AMBIGUOUS_IMPORT);
  }

  public void test_ambiguousImport_varRead() throws Exception {
    Source source = addSource(createSource(//
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "f() { g(v); }",
        "g(p) {}"));
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "var v;"));
    addNamedSource("/lib2.dart", createSource(//
        "library lib2;",
        "var v;"));
    resolve(source);
    assertErrors(source, StaticWarningCode.AMBIGUOUS_IMPORT);
  }

  public void test_ambiguousImport_varWrite() throws Exception {
    Source source = addSource(createSource(//
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "f() { v = 0; }"));
    addNamedSource("/lib1.dart", createSource(//
        "library lib1;",
        "var v;"));
    addNamedSource("/lib2.dart", createSource(//
        "library lib2;",
        "var v;"));
    resolve(source);
    assertErrors(source, StaticWarningCode.AMBIGUOUS_IMPORT);
  }

  public void test_argumentTypeNotAssignable_annotation_namedConstructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const A.fromInt(int p);",
        "}",
        "@A.fromInt('0')",
        "main() {",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_annotation_unnamedConstructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const A(int p);",
        "}",
        "@A('0')",
        "main() {",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_binary() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  operator +(int p) {}",
        "}",
        "f(A a) {",
        "  a + '0';",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_cascadeSecond() throws Exception {
    Source source = addSource(createSource(//
        "// filler filler filler filler filler filler filler filler filler filler",
        "class A {",
        "  B ma() { return new B(); }",
        "}",
        "class B {",
        "  mb(String p) {}",
        "}",
        "",
        "main() {",
        "  A a = new A();",
        "  a..  ma().mb(0);",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_const() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const A(String p);",
        "}",
        "main() {",
        "  const A(42);",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_const_super() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  const A(String p);",
        "}",
        "class B extends A {",
        "  const B() : super(42);",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_functionExpressionInvocation_required()
      throws Exception {
    Source source = addSource(createSource(//
        "main() {",
        "  (int x) {} ('');",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_index() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  operator [](int index) {}",
        "}",
        "f(A a) {",
        "  a['0'];",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_invocation_callParameter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  call(int p) {}",
        "}",
        "f(A a) {",
        "  a('0');",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_invocation_callVariable() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  call(int p) {}",
        "}",
        "main() {",
        "  A a = new A();",
        "  a('0');",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_invocation_functionParameter() throws Exception {
    Source source = addSource(createSource(//
        "a(b(int p)) {",
        "  b('0');",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_invocation_functionParameter_generic()
      throws Exception {
    Source source = addSource(createSource(//
        "class A<K, V> {",
        "  m(f(K k), V v) {",
        "    f(v);",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_invocation_functionTypes_optional() throws Exception {
    Source source = addSource(createSource(//
        "void acceptFunNumOptBool(void funNumOptBool([bool b])) {}",
        "void funNumBool(bool b) {}",
        "main() {",
        "  acceptFunNumOptBool(funNumBool);",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_invocation_generic() throws Exception {
    Source source = addSource(createSource(//
        "class A<T> {",
        "  m(T t) {}",
        "}",
        "f(A<String> a) {",
        "  a.m(1);",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_invocation_named() throws Exception {
    Source source = addSource(createSource(//
        "f({String p}) {}",
        "main() {",
        "  f(p: 42);",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_invocation_optional() throws Exception {
    Source source = addSource(createSource(//
        "f([String p]) {}",
        "main() {",
        "  f(42);",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_invocation_required() throws Exception {
    Source source = addSource(createSource(//
        "f(String p) {}",
        "main() {",
        "  f(42);",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_invocation_typedef_generic() throws Exception {
    Source source = addSource(createSource(//
        "typedef A<T>(T p);",
        "f(A<int> a) {",
        "  a('1');",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_invocation_typedef_local() throws Exception {
    Source source = addSource(createSource(//
        "typedef A(int p);",
        "A getA() => null;",
        "main() {",
        "  A a = getA();",
        "  a('1');",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_invocation_typedef_parameter() throws Exception {
    Source source = addSource(createSource(//
        "typedef A(int p);",
        "f(A a) {",
        "  a('1');",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_new_generic() throws Exception {
    Source source = addSource(createSource(//
        "class A<T> {",
        "  A(T p) {}",
        "}",
        "main() {",
        "  new A<String>(42);",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_new_optional() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A([String p]) {}",
        "}",
        "main() {",
        "  new A(42);",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_new_required() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A(String p) {}",
        "}",
        "main() {",
        "  new A(42);",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_assignmentToConst_instanceVariable() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static const v = 0;",
        "}",
        "f() {",
        "  A.v = 1;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ASSIGNMENT_TO_CONST);
    verify(source);
  }

  public void test_assignmentToConst_instanceVariable_plusEq() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static const v = 0;",
        "}",
        "f() {",
        "  A.v += 1;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ASSIGNMENT_TO_CONST);
    verify(source);
  }

  public void test_assignmentToConst_localVariable() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  const x = 0;",
        "  x = 1;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ASSIGNMENT_TO_CONST);
    verify(source);
  }

  public void test_assignmentToConst_localVariable_plusEq() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  const x = 0;",
        "  x += 1;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ASSIGNMENT_TO_CONST);
    verify(source);
  }

  public void test_assignmentToFinal_instanceVariable() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final v = 0;",
        "}",
        "f() {",
        "  A a = new A();",
        "  a.v = 1;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ASSIGNMENT_TO_FINAL);
    verify(source);
  }

  public void test_assignmentToFinal_instanceVariable_plusEq() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final v = 0;",
        "}",
        "f() {",
        "  A a = new A();",
        "  a.v += 1;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ASSIGNMENT_TO_FINAL);
    verify(source);
  }

  public void test_assignmentToFinal_localVariable() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  final x = 0;",
        "  x = 1;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ASSIGNMENT_TO_FINAL);
    verify(source);
  }

  public void test_assignmentToFinal_localVariable_plusEq() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  final x = 0;",
        "  x += 1;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ASSIGNMENT_TO_FINAL);
    verify(source);
  }

  public void test_assignmentToFinal_prefixMinusMinus() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  final x = 0;",
        "  --x;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ASSIGNMENT_TO_FINAL);
    verify(source);
  }

  public void test_assignmentToFinal_prefixPlusPlus() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  final x = 0;",
        "  ++x;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ASSIGNMENT_TO_FINAL);
    verify(source);
  }

  public void test_assignmentToFinal_propertyAccess() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int get x => 0;",
        "}",
        "class B {",
        "  static A a;",
        "}",
        "main() {",
        "  B.a.x = 0;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ASSIGNMENT_TO_FINAL);
    verify(source);
  }

  public void test_assignmentToFinal_suffixMinusMinus() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  final x = 0;",
        "  x--;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ASSIGNMENT_TO_FINAL);
    verify(source);
  }

  public void test_assignmentToFinal_suffixPlusPlus() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  final x = 0;",
        "  x++;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ASSIGNMENT_TO_FINAL);
    verify(source);
  }

  public void test_assignmentToFinal_topLevelVariable() throws Exception {
    Source source = addSource(createSource(//
        "final x = 0;",
        "f() { x = 1; }"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ASSIGNMENT_TO_FINAL);
    verify(source);
  }

  public void test_assignmentToMethod() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m() {}",
        "}",
        "f(A a) {",
        "  a.m = () {};",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ASSIGNMENT_TO_METHOD);
    verify(source);
  }

  public void test_caseBlockNotTerminated() throws Exception {
    Source source = addSource(createSource(//
        "f(int p) {",
        "  switch (p) {",
        "    case 0:",
        "      f(p);",
        "    case 1:",
        "      break;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.CASE_BLOCK_NOT_TERMINATED);
    verify(source);
  }

  public void test_castToNonType() throws Exception {
    Source source = addSource(createSource(//
        "var A = 0;",
        "f(String s) { var x = s as A; }"));
    resolve(source);
    assertErrors(source, StaticWarningCode.CAST_TO_NON_TYPE);
    verify(source);
  }

  public void test_concreteClassWithAbstractMember() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m();",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.CONCRETE_CLASS_WITH_ABSTRACT_MEMBER);
    verify(source);
  }

  public void test_conflictingDartImport() throws Exception {
    Source source = addSource(createSource(//
        "import 'lib.dart';",
        "import 'dart:async';",
        "Future f = null;",
        "Stream s;"));
    addNamedSource("/lib.dart", createSource(//
        "library lib;",
        "class Future {}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.CONFLICTING_DART_IMPORT);
  }

  public void test_conflictingInstanceGetterAndSuperclassMember_declField_direct_setter()
      throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static set v(x) {}",
        "}",
        "class B extends A {",
        "  var v;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.CONFLICTING_INSTANCE_GETTER_AND_SUPERCLASS_MEMBER);
    verify(source);
  }

  public void test_conflictingInstanceGetterAndSuperclassMember_declGetter_direct_getter()
      throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static get v => 0;",
        "}",
        "class B extends A {",
        "  get v => 0;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.CONFLICTING_INSTANCE_GETTER_AND_SUPERCLASS_MEMBER);
    verify(source);
  }

  public void test_conflictingInstanceGetterAndSuperclassMember_declGetter_direct_method()
      throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static v() {}",
        "}",
        "class B extends A {",
        "  get v => 0;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.CONFLICTING_INSTANCE_GETTER_AND_SUPERCLASS_MEMBER);
    verify(source);
  }

  public void test_conflictingInstanceGetterAndSuperclassMember_declGetter_direct_setter()
      throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static set v(x) {}",
        "}",
        "class B extends A {",
        "  get v => 0;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.CONFLICTING_INSTANCE_GETTER_AND_SUPERCLASS_MEMBER);
    verify(source);
  }

  public void test_conflictingInstanceGetterAndSuperclassMember_declGetter_indirect()
      throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static int v;",
        "}",
        "class B extends A {}",
        "class C extends B {",
        "  get v => 0;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.CONFLICTING_INSTANCE_GETTER_AND_SUPERCLASS_MEMBER);
    verify(source);
  }

  public void test_conflictingInstanceGetterAndSuperclassMember_declGetter_mixin() throws Exception {
    Source source = addSource(createSource(//
        "class M {",
        "  static int v;",
        "}",
        "class B extends Object with M {",
        "  get v => 0;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.CONFLICTING_INSTANCE_GETTER_AND_SUPERCLASS_MEMBER);
    verify(source);
  }

  public void test_conflictingInstanceGetterAndSuperclassMember_direct_field() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static int v;",
        "}",
        "class B extends A {",
        "  get v => 0;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.CONFLICTING_INSTANCE_GETTER_AND_SUPERCLASS_MEMBER);
    verify(source);
  }

  public void test_conflictingInstanceMethodSetter_sameClass() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  set foo(a) {}",
        "  foo() {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.CONFLICTING_INSTANCE_METHOD_SETTER);
    verify(source);
  }

  public void test_conflictingInstanceMethodSetter_setterInInterface() throws Exception {
    Source source = addSource(createSource(//
        "abstract class A {",
        "  set foo(a);",
        "}",
        "abstract class B implements A {",
        "  foo() {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.CONFLICTING_INSTANCE_METHOD_SETTER);
    verify(source);
  }

  public void test_conflictingInstanceMethodSetter_setterInSuper() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  set foo(a) {}",
        "}",
        "class B extends A {",
        "  foo() {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.CONFLICTING_INSTANCE_METHOD_SETTER);
    verify(source);
  }

  public void test_conflictingInstanceMethodSetter2() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  foo() {}",
        "  set foo(a) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.CONFLICTING_INSTANCE_METHOD_SETTER2);
    verify(source);
  }

  public void test_conflictingInstanceSetterAndSuperclassMember() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static int v;",
        "}",
        "class B extends A {",
        "  set v(x) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.CONFLICTING_INSTANCE_SETTER_AND_SUPERCLASS_MEMBER);
    verify(source);
  }

  public void test_conflictingStaticGetterAndInstanceSetter_mixin() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  set x(int p) {}",
        "}",
        "class B extends Object with A {",
        "  static get x => 0;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.CONFLICTING_STATIC_GETTER_AND_INSTANCE_SETTER);
    verify(source);
  }

  public void test_conflictingStaticGetterAndInstanceSetter_superClass() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  set x(int p) {}",
        "}",
        "class B extends A {",
        "  static get x => 0;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.CONFLICTING_STATIC_GETTER_AND_INSTANCE_SETTER);
    verify(source);
  }

  public void test_conflictingStaticGetterAndInstanceSetter_thisClass() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static get x => 0;",
        "  set x(int p) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.CONFLICTING_STATIC_GETTER_AND_INSTANCE_SETTER);
    verify(source);
  }

  public void test_conflictingStaticSetterAndInstanceMember_thisClass_getter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  get x => 0;",
        "  static set x(int p) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.CONFLICTING_STATIC_SETTER_AND_INSTANCE_MEMBER);
    verify(source);
  }

  public void test_conflictingStaticSetterAndInstanceMember_thisClass_method() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  x() {}",
        "  static set x(int p) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.CONFLICTING_STATIC_SETTER_AND_INSTANCE_MEMBER);
    verify(source);
  }

  public void test_constWithAbstractClass() throws Exception {
    Source source = addSource(createSource(//
        "abstract class A {",
        "  const A();",
        "}",
        "void f() {",
        "  A a = const A();",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.CONST_WITH_ABSTRACT_CLASS);
    verify(source);
  }

  public void test_equalKeysInMap() throws Exception {
    Source source = addSource(createSource(//
    "var m = {'a' : 0, 'b' : 1, 'a' : 2};"));
    resolve(source);
    assertErrors(source, StaticWarningCode.EQUAL_KEYS_IN_MAP);
    verify(source);
  }

  public void test_exportDuplicatedLibraryName() throws Exception {
    Source source = addSource(createSource(//
        "library test;",
        "export 'lib1.dart';",
        "export 'lib2.dart';"));
    addNamedSource("/lib1.dart", "library lib;");
    addNamedSource("/lib2.dart", "library lib;");
    resolve(source);
    assertErrors(source, StaticWarningCode.EXPORT_DUPLICATED_LIBRARY_NAME);
    verify(source);
  }

  public void test_extraPositionalArguments() throws Exception {
    Source source = addSource(createSource(//
        "f() {}",
        "main() {",
        "  f(0, 1, '2');",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.EXTRA_POSITIONAL_ARGUMENTS);
    verify(source);
  }

  public void test_extraPositionalArguments_functionExpression() throws Exception {
    Source source = addSource(createSource(//
        "main() {",
        "  (int x) {} (0, 1);",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.EXTRA_POSITIONAL_ARGUMENTS);
    verify(source);
  }

  public void test_fieldInitializedInInitializerAndDeclaration_final() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final int x = 0;",
        "  A() : x = 1 {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.FIELD_INITIALIZED_IN_INITIALIZER_AND_DECLARATION);
    verify(source);
  }

  public void test_fieldInitializerNotAssignable() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int x;",
        "  A() : x = '';",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.FIELD_INITIALIZER_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_fieldInitializingFormalNotAssignable() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int x;",
        "  A(String this.x) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.FIELD_INITIALIZING_FORMAL_NOT_ASSIGNABLE);
    verify(source);
  }

  /**
   * This test doesn't test the FINAL_INITIALIZED_IN_DECLARATION_AND_CONSTRUCTOR code, but tests the
   * FIELD_INITIALIZED_IN_INITIALIZER_AND_DECLARATION code instead. It is provided here to show
   * coverage over all of the permutations of initializers in constructor declarations.
   * <p>
   * Note: FIELD_INITIALIZED_IN_INITIALIZER_AND_DECLARATION covers a subset of
   * FINAL_INITIALIZED_IN_DECLARATION_AND_CONSTRUCTOR, since it more specific, we use it instead of
   * the broader code
   */
  public void test_finalInitializedInDeclarationAndConstructor_initializers() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final x = 0;",
        "  A() : x = 0 {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.FIELD_INITIALIZED_IN_INITIALIZER_AND_DECLARATION);
    verify(source);
  }

  public void test_finalInitializedInDeclarationAndConstructor_initializingFormal()
      throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final x = 0;",
        "  A(this.x) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.FINAL_INITIALIZED_IN_DECLARATION_AND_CONSTRUCTOR);
    verify(source);
  }

  public void test_finalNotInitialized_inConstructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final int x;",
        "  A() {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.FINAL_NOT_INITIALIZED);
    verify(source);
  }

  public void test_finalNotInitialized_instanceField_final() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final F;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.FINAL_NOT_INITIALIZED);
    verify(source);
  }

  public void test_finalNotInitialized_instanceField_final_static() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static final F;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.FINAL_NOT_INITIALIZED);
    verify(source);
  }

  public void test_finalNotInitialized_library_final() throws Exception {
    Source source = addSource(createSource(//
    "final F;"));
    resolve(source);
    assertErrors(source, StaticWarningCode.FINAL_NOT_INITIALIZED);
    verify(source);
  }

  public void test_finalNotInitialized_local_final() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  final int x;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.FINAL_NOT_INITIALIZED);
    verify(source);
  }

  public void test_functionWithoutCall_direct() throws Exception {
    Source source = addSource(createSource(//
        "class A implements Function {",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.FUNCTION_WITHOUT_CALL);
    verify(source);
  }

  public void test_functionWithoutCall_indirect_extends() throws Exception {
    Source source = addSource(createSource(//
        "abstract class A implements Function {",
        "}",
        "class B extends A {",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.FUNCTION_WITHOUT_CALL);
    verify(source);
  }

  public void test_functionWithoutCall_indirect_implements() throws Exception {
    Source source = addSource(createSource(//
        "abstract class A implements Function {",
        "}",
        "class B implements A {",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.FUNCTION_WITHOUT_CALL);
    verify(source);
  }

  public void test_importDuplicatedLibraryName() throws Exception {
    Source source = addSource(createSource(//
        "library test;",
        "import 'lib1.dart';",
        "import 'lib2.dart';"));
    addNamedSource("/lib1.dart", "library lib;");
    addNamedSource("/lib2.dart", "library lib;");
    resolve(source);
    assertErrors(
        source,
        StaticWarningCode.IMPORT_DUPLICATED_LIBRARY_NAME,
        HintCode.UNUSED_IMPORT,
        HintCode.UNUSED_IMPORT);
    verify(source);
  }

  public void test_inconsistentMethodInheritanceGetterAndMethod() throws Exception {
    Source source = addSource(createSource(//
        "abstract class A {",
        "  int x();",
        "}",
        "abstract class B {",
        "  int get x;",
        "}",
        "class C implements A, B {",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INCONSISTENT_METHOD_INHERITANCE_GETTER_AND_METHOD);
    verify(source);
  }

  public void test_instanceMethodNameCollidesWithSuperclassStatic_field() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static var n;",
        "}",
        "class B extends A {",
        "  void n() {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC);
    verify(source);
  }

  public void test_instanceMethodNameCollidesWithSuperclassStatic_field2() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static var n;",
        "}",
        "class B extends A {",
        "}",
        "class C extends B {",
        "  void n() {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC);
    verify(source);
  }

  public void test_instanceMethodNameCollidesWithSuperclassStatic_getter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static get n {return 0;}",
        "}",
        "class B extends A {",
        "  void n() {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC);
    verify(source);
  }

  public void test_instanceMethodNameCollidesWithSuperclassStatic_getter2() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static get n {return 0;}",
        "}",
        "class B extends A {",
        "}",
        "class C extends B {",
        "  void n() {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC);
    verify(source);
  }

  public void test_instanceMethodNameCollidesWithSuperclassStatic_method() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static n () {}",
        "}",
        "class B extends A {",
        "  void n() {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC);
    verify(source);
  }

  public void test_instanceMethodNameCollidesWithSuperclassStatic_method2() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static n () {}",
        "}",
        "class B extends A {",
        "}",
        "class C extends B {",
        "  void n() {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC);
    verify(source);
  }

  public void test_instanceMethodNameCollidesWithSuperclassStatic_setter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static set n(int x) {}",
        "}",
        "class B extends A {",
        "  void n() {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC);
    verify(source);
  }

  public void test_instanceMethodNameCollidesWithSuperclassStatic_setter2() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static set n(int x) {}",
        "}",
        "class B extends A {",
        "}",
        "class C extends B {",
        "  void n() {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC);
    verify(source);
  }

  public void test_invalidGetterOverrideReturnType() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int get g { return 0; }",
        "}",
        "class B extends A {",
        "  String get g { return 'a'; }",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INVALID_GETTER_OVERRIDE_RETURN_TYPE);
    verify(source);
  }

  public void test_invalidGetterOverrideReturnType_implicit() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  String f;",
        "}",
        "class B extends A {",
        "  int f;",
        "}"));
    resolve(source);
    assertErrors(
        source,
        StaticWarningCode.INVALID_GETTER_OVERRIDE_RETURN_TYPE,
        StaticWarningCode.INVALID_SETTER_OVERRIDE_NORMAL_PARAM_TYPE);
    verify(source);
  }

  public void test_invalidGetterOverrideReturnType_twoInterfaces() throws Exception {
    // test from language/override_inheritance_field_test_11.dart
    Source source = addSource(createSource(//
        "abstract class I {",
        "  int get getter => null;",
        "}",
        "abstract class J {",
        "  num get getter => null;",
        "}",
        "abstract class A implements I, J {}",
        "class B extends A {",
        "  String get getter => null;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INVALID_GETTER_OVERRIDE_RETURN_TYPE);
    verify(source);
  }

  public void test_invalidMethodOverrideNamedParamType() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m({int a}) {}",
        "}",
        "class B implements A {",
        "  m({String a}) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INVALID_METHOD_OVERRIDE_NAMED_PARAM_TYPE);
    verify(source);
  }

  public void test_invalidMethodOverrideNormalParamType() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m(int a) {}",
        "}",
        "class B implements A {",
        "  m(String a) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INVALID_METHOD_OVERRIDE_NORMAL_PARAM_TYPE);
    verify(source);
  }

  public void test_invalidMethodOverrideNormalParamType_twoInterfaces() throws Exception {
    Source source = addSource(createSource(//
        "abstract class I {",
        "  m(int n);",
        "}",
        "abstract class J {",
        "  m(num n);",
        "}",
        "abstract class A implements I, J {}",
        "class B extends A {",
        "  m(String n) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INVALID_METHOD_OVERRIDE_NORMAL_PARAM_TYPE);
    verify(source);
  }

  public void test_invalidMethodOverrideOptionalParamType() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m([int a]) {}",
        "}",
        "class B implements A {",
        "  m([String a]) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INVALID_METHOD_OVERRIDE_OPTIONAL_PARAM_TYPE);
    verify(source);
  }

  public void test_invalidMethodOverrideOptionalParamType_twoInterfaces() throws Exception {
    Source source = addSource(createSource(//
        "abstract class I {",
        "  m([int n]);",
        "}",
        "abstract class J {",
        "  m([num n]);",
        "}",
        "abstract class A implements I, J {}",
        "class B extends A {",
        "  m([String n]) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INVALID_METHOD_OVERRIDE_OPTIONAL_PARAM_TYPE);
    verify(source);
  }

  public void test_invalidMethodOverrideReturnType_interface() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int m() { return 0; }",
        "}",
        "class B implements A {",
        "  String m() { return 'a'; }",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INVALID_METHOD_OVERRIDE_RETURN_TYPE);
    verify(source);
  }

  public void test_invalidMethodOverrideReturnType_interface_grandparent() throws Exception {
    Source source = addSource(createSource(//
        "abstract class A {",
        "  int m();",
        "}",
        "abstract class B implements A {",
        "}",
        "class C implements B {",
        "  String m() { return 'a'; }",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INVALID_METHOD_OVERRIDE_RETURN_TYPE);
    verify(source);
  }

  public void test_invalidMethodOverrideReturnType_mixin() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int m() { return 0; }",
        "}",
        "class B extends Object with A {",
        "  String m() { return 'a'; }",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INVALID_METHOD_OVERRIDE_RETURN_TYPE);
    verify(source);
  }

  public void test_invalidMethodOverrideReturnType_superclass() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int m() { return 0; }",
        "}",
        "class B extends A {",
        "  String m() { return 'a'; }",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INVALID_METHOD_OVERRIDE_RETURN_TYPE);
    verify(source);
  }

  public void test_invalidMethodOverrideReturnType_superclass_grandparent() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int m() { return 0; }",
        "}",
        "class B extends A {",
        "}",
        "class C extends B {",
        "  String m() { return 'a'; }",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INVALID_METHOD_OVERRIDE_RETURN_TYPE);
    verify(source);
  }

  public void test_invalidMethodOverrideReturnType_twoInterfaces() throws Exception {
    Source source = addSource(createSource(//
        "abstract class I {",
        "  int m();",
        "}",
        "abstract class J {",
        "  num m();",
        "}",
        "abstract class A implements I, J {}",
        "class B extends A {",
        "  String m() => '';",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INVALID_METHOD_OVERRIDE_RETURN_TYPE);
    verify(source);
  }

  public void test_invalidMethodOverrideReturnType_void() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int m() { return 0; }",
        "}",
        "class B extends A {",
        "  void m() {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INVALID_METHOD_OVERRIDE_RETURN_TYPE);
    verify(source);
  }

  public void test_invalidOverrideDifferentDefaultValues_named() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m({int p : 0}) {}",
        "}",
        "class B extends A {",
        "  m({int p : 1}) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INVALID_OVERRIDE_DIFFERENT_DEFAULT_VALUES_NAMED);
    verify(source);
  }

  public void test_invalidOverrideDifferentDefaultValues_positional() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m([int p = 0]) {}",
        "}",
        "class B extends A {",
        "  m([int p = 1]) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INVALID_OVERRIDE_DIFFERENT_DEFAULT_VALUES_POSITIONAL);
    verify(source);
  }

  public void test_invalidOverrideNamed_fewerNamedParameters() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m({a, b}) {}",
        "}",
        "class B extends A {",
        "  m({a}) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INVALID_OVERRIDE_NAMED);
    verify(source);
  }

  public void test_invalidOverrideNamed_missingNamedParameter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m({a, b}) {}",
        "}",
        "class B extends A {",
        "  m({a, c}) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INVALID_OVERRIDE_NAMED);
    verify(source);
  }

  public void test_invalidOverridePositional_optional() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m([a, b]) {}",
        "}",
        "class B extends A {",
        "  m([a]) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INVALID_OVERRIDE_POSITIONAL);
    verify(source);
  }

  public void test_invalidOverridePositional_optionalAndRequired() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m(a, b, [c, d]) {}",
        "}",
        "class B extends A {",
        "  m(a, b, [c]) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INVALID_OVERRIDE_POSITIONAL);
    verify(source);
  }

  public void test_invalidOverridePositional_optionalAndRequired2() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m(a, b, [c, d]) {}",
        "}",
        "class B extends A {",
        "  m(a, [c, d]) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INVALID_OVERRIDE_POSITIONAL);
    verify(source);
  }

  public void test_invalidOverrideRequired() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m(a) {}",
        "}",
        "class B extends A {",
        "  m(a, b) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INVALID_OVERRIDE_REQUIRED);
    verify(source);
  }

  public void test_invalidSetterOverrideNormalParamType() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  void set s(int v) {}",
        "}",
        "class B extends A {",
        "  void set s(String v) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INVALID_SETTER_OVERRIDE_NORMAL_PARAM_TYPE);
    verify(source);
  }

  public void test_invalidSetterOverrideNormalParamType_twoInterfaces() throws Exception {
    // test from language/override_inheritance_field_test_34.dart
    Source source = addSource(createSource(//
        "abstract class I {",
        "  set setter14(int _) => null;",
        "}",
        "abstract class J {",
        "  set setter14(num _) => null;",
        "}",
        "abstract class A implements I, J {}",
        "class B extends A {",
        "  set setter14(String _) => null;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.INVALID_SETTER_OVERRIDE_NORMAL_PARAM_TYPE);
    verify(source);
  }

  public void test_listElementTypeNotAssignable() throws Exception {
    Source source = addSource(createSource(//
    "var v = <String> [42];"));
    resolve(source);
    assertErrors(source, StaticWarningCode.LIST_ELEMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_mapKeyTypeNotAssignable() throws Exception {
    Source source = addSource(createSource(//
    "var v = <String, int > {1 : 2};"));
    resolve(source);
    assertErrors(source, StaticWarningCode.MAP_KEY_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_mapValueTypeNotAssignable() throws Exception {
    Source source = addSource(createSource(//
    "var v = <String, String> {'a' : 2};"));
    resolve(source);
    assertErrors(source, StaticWarningCode.MAP_VALUE_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_mismatchedAccessorTypes_class() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int get g { return 0; }",
        "  set g(String v) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.MISMATCHED_GETTER_AND_SETTER_TYPES);
    verify(source);
  }

  public void test_mismatchedAccessorTypes_getterAndSuperSetter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int get g { return 0; }",
        "}",
        "class B extends A {",
        "  set g(String v) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.MISMATCHED_GETTER_AND_SETTER_TYPES_FROM_SUPERTYPE);
    verify(source);
  }

  public void test_mismatchedAccessorTypes_setterAndSuperGetter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  set g(int v) {}",
        "}",
        "class B extends A {",
        "  String get g { return ''; }",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.MISMATCHED_GETTER_AND_SETTER_TYPES_FROM_SUPERTYPE);
    verify(source);
  }

  public void test_mismatchedAccessorTypes_topLevel() throws Exception {
    Source source = addSource(createSource(//
        "int get g { return 0; }",
        "set g(String v) {}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.MISMATCHED_GETTER_AND_SETTER_TYPES);
    verify(source);
  }

  public void test_mixedReturnTypes_localFunction() throws Exception {
    Source source = addSource(createSource(//
        "class C {",
        "  m(int x) {",
        "    return (int y) {",
        "      if (y < 0) {",
        "        return;",
        "      }",
        "      return 0;",
        "    };",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.MIXED_RETURN_TYPES, StaticWarningCode.MIXED_RETURN_TYPES);
    verify(source);
  }

  public void test_mixedReturnTypes_method() throws Exception {
    Source source = addSource(createSource(//
        "class C {",
        "  m(int x) {",
        "    if (x < 0) {",
        "      return;",
        "    }",
        "    return 0;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.MIXED_RETURN_TYPES, StaticWarningCode.MIXED_RETURN_TYPES);
    verify(source);
  }

  public void test_mixedReturnTypes_topLevelFunction() throws Exception {
    Source source = addSource(createSource(//
        "f(int x) {",
        "  if (x < 0) {",
        "    return;",
        "  }",
        "  return 0;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.MIXED_RETURN_TYPES, StaticWarningCode.MIXED_RETURN_TYPES);
    verify(source);
  }

  public void test_newWithAbstractClass() throws Exception {
    Source source = addSource(createSource(//
        "abstract class A {}",
        "void f() {",
        "  A a = new A();",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NEW_WITH_ABSTRACT_CLASS);
    verify(source);
  }

  public void test_newWithInvalidTypeParameters() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "f() { return new A<A>(); }"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NEW_WITH_INVALID_TYPE_PARAMETERS);
    verify(source);
  }

  public void test_newWithInvalidTypeParameters_tooFew() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class C<K, V> {}",
        "f(p) {",
        "  return new C<A>();",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NEW_WITH_INVALID_TYPE_PARAMETERS);
    verify(source);
  }

  public void test_newWithInvalidTypeParameters_tooMany() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class C<E> {}",
        "f(p) {",
        "  return new C<A, A>();",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NEW_WITH_INVALID_TYPE_PARAMETERS);
    verify(source);
  }

  public void test_newWithNonType() throws Exception {
    Source source = addSource(createSource(//
        "var A = 0;",
        "void f() {",
        "  var a = new A();",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NEW_WITH_NON_TYPE);
    verify(source);
  }

  public void test_newWithNonType_fromLibrary() throws Exception {
    Source source1 = addNamedSource("lib.dart", "class B {}");
    Source source2 = addNamedSource("lib2.dart", createSource(//
        "import 'lib.dart' as lib;",
        "void f() {",
        "  var a = new lib.A();",
        "}",
        "lib.B b;"));
    resolve(source1);
    resolve(source2);
    assertErrors(source2, StaticWarningCode.NEW_WITH_NON_TYPE);
    verify(source1);
  }

  public void test_newWithUndefinedConstructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A() {}",
        "}",
        "f() {",
        "  new A.name();",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NEW_WITH_UNDEFINED_CONSTRUCTOR);
    // no verify(), 'name' is not resolved
  }

  public void test_newWithUndefinedConstructorDefault() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A.name() {}",
        "}",
        "f() {",
        "  new A();",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NEW_WITH_UNDEFINED_CONSTRUCTOR_DEFAULT);
    verify(source);
  }

  public void test_nonAbstractClassInheritsAbstractMemberFivePlus() throws Exception {
    Source source = addSource(createSource(//
        "abstract class A {",
        "  m();",
        "  n();",
        "  o();",
        "  p();",
        "  q();",
        "}",
        "class C extends A {",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_FIVE_PLUS);
    verify(source);
  }

  public void test_nonAbstractClassInheritsAbstractMemberFour() throws Exception {
    Source source = addSource(createSource(//
        "abstract class A {",
        "  m();",
        "  n();",
        "  o();",
        "  p();",
        "}",
        "class C extends A {",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_FOUR);
    verify(source);
  }

  public void test_nonAbstractClassInheritsAbstractMemberOne_ensureCorrectFunctionSubtypeIsUsedInImplementation()
      throws Exception {
    // 15028
    Source source = addSource(createSource(//
        "class C {",
        "  foo(int x) => x;",
        "}",
        "abstract class D {",
        "  foo(x, [y]);",
        "}",
        "class E extends C implements D {}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE);
    verify(source);
  }

  public void test_nonAbstractClassInheritsAbstractMemberOne_getter_fromInterface()
      throws Exception {
    Source source = addSource(createSource(//
        "class I {",
        "  int get g {return 1;}",
        "}",
        "class C implements I {",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE);
    verify(source);
  }

  public void test_nonAbstractClassInheritsAbstractMemberOne_getter_fromSuperclass()
      throws Exception {
    Source source = addSource(createSource(//
        "abstract class A {",
        "  int get g;",
        "}",
        "class C extends A {",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE);
    verify(source);
  }

  public void test_nonAbstractClassInheritsAbstractMemberOne_method_fromInterface()
      throws Exception {
    Source source = addSource(createSource(//
        "class I {",
        "  m(p) {}",
        "}",
        "class C implements I {",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE);
    verify(source);
  }

  public void test_nonAbstractClassInheritsAbstractMemberOne_method_fromSuperclass()
      throws Exception {
    Source source = addSource(createSource(//
        "abstract class A {",
        "  m(p);",
        "}",
        "class C extends A {",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE);
    verify(source);
  }

  public void test_nonAbstractClassInheritsAbstractMemberOne_method_optionalParamCount()
      throws Exception {
    // 7640
    Source source = addSource(createSource(//
        "abstract class A {",
        "  int x(int a);",
        "}",
        "abstract class B {",
        "  int x(int a, [int b]);",
        "}",
        "class C implements A, B {",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE);
    verify(source);
  }

  public void test_nonAbstractClassInheritsAbstractMemberOne_mixinInherits_getter()
      throws Exception {
    // 15001
    Source source = addSource(createSource(//
        "abstract class A { get g1; get g2; }",
        "abstract class B implements A { get g1 => 1; }",
        "class C extends Object with B {}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE);
  }

  public void test_nonAbstractClassInheritsAbstractMemberOne_mixinInherits_method()
      throws Exception {
    // 15001
    Source source = addSource(createSource(//
        "abstract class A { m1(); m2(); }",
        "abstract class B implements A { m1() => 1; }",
        "class C extends Object with B {}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE);
  }

  public void test_nonAbstractClassInheritsAbstractMemberOne_mixinInherits_setter()
      throws Exception {
    // 15001
    Source source = addSource(createSource(//
        "abstract class A { set s1(v); set s2(v); }",
        "abstract class B implements A { set s1(v) {} }",
        "class C extends Object with B {}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE);
  }

  public void test_nonAbstractClassInheritsAbstractMemberOne_setter_and_implicitSetter()
      throws Exception {
    // test from language/override_inheritance_abstract_test_14.dart
    Source source = addSource(createSource(//
        "abstract class A {",
        "  set field(_);",
        "}",
        "abstract class I {",
        "  var field;",
        "}",
        "class B extends A implements I {",
        "  get field => 0;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE);
    verify(source);
  }

  public void test_nonAbstractClassInheritsAbstractMemberOne_setter_fromInterface()
      throws Exception {
    Source source = addSource(createSource(//
        "class I {",
        "  set s(int i) {}",
        "}",
        "class C implements I {",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE);
    verify(source);
  }

  public void test_nonAbstractClassInheritsAbstractMemberOne_setter_fromSuperclass()
      throws Exception {
    Source source = addSource(createSource(//
        "abstract class A {",
        "  set s(int i);",
        "}",
        "class C extends A {",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE);
    verify(source);
  }

  public void test_nonAbstractClassInheritsAbstractMemberOne_superclasses_interface()
      throws Exception {
    // bug 11154
    Source source = addSource(createSource(//
        "class A {",
        "  get a => 'a';",
        "}",
        "abstract class B implements A {",
        "  get b => 'b';",
        "}",
        "class C extends B {",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE);
    verify(source);
  }

  public void test_nonAbstractClassInheritsAbstractMemberOne_variable_fromInterface_missingGetter()
      throws Exception {
    // 16133
    Source source = addSource(createSource(//
        "class I {",
        "  var v;",
        "}",
        "class C implements I {",
        "  set v(_) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE);
    verify(source);
  }

  public void test_nonAbstractClassInheritsAbstractMemberOne_variable_fromInterface_missingSetter()
      throws Exception {
    // 16133
    Source source = addSource(createSource(//
        "class I {",
        "  var v;",
        "}",
        "class C implements I {",
        "  get v => 1;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE);
    verify(source);
  }

  public void test_nonAbstractClassInheritsAbstractMemberThree() throws Exception {
    Source source = addSource(createSource(//
        "abstract class A {",
        "  m();",
        "  n();",
        "  o();",
        "}",
        "class C extends A {",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_THREE);
    verify(source);
  }

  public void test_nonAbstractClassInheritsAbstractMemberTwo() throws Exception {
    Source source = addSource(createSource(//
        "abstract class A {",
        "  m();",
        "  n();",
        "}",
        "class C extends A {",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_TWO);
    verify(source);
  }

  public void test_nonAbstractClassInheritsAbstractMemberTwo_variable_fromInterface_missingBoth()
      throws Exception {
    // 16133
    Source source = addSource(createSource(//
        "class I {",
        "  var v;",
        "}",
        "class C implements I {",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_TWO);
    verify(source);
  }

  public void test_nonTypeInCatchClause_noElement() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  try {",
        "  } on T catch (e) {",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NON_TYPE_IN_CATCH_CLAUSE);
    verify(source);
  }

  public void test_nonTypeInCatchClause_notType() throws Exception {
    Source source = addSource(createSource(//
        "var T = 0;",
        "f() {",
        "  try {",
        "  } on T catch (e) {",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NON_TYPE_IN_CATCH_CLAUSE);
    verify(source);
  }

  public void test_nonVoidReturnForOperator() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int operator []=(a, b) { return a; }",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NON_VOID_RETURN_FOR_OPERATOR);
    verify(source);
  }

  public void test_nonVoidReturnForSetter_function() throws Exception {
    Source source = addSource(createSource(//
        "int set x(int v) {",
        "  return 42;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NON_VOID_RETURN_FOR_SETTER);
    verify(source);
  }

  public void test_nonVoidReturnForSetter_method() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int set x(int v) {",
        "    return 42;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NON_VOID_RETURN_FOR_SETTER);
    verify(source);
  }

  public void test_notAType() throws Exception {
    Source source = addSource(createSource(//
        "f() {}",
        "main() {",
        "  f v = null;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NOT_A_TYPE);
    verify(source);
  }

  public void test_notEnoughRequiredArguments() throws Exception {
    Source source = addSource(createSource(//
        "f(int a, String b) {}",
        "main() {",
        "  f();",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NOT_ENOUGH_REQUIRED_ARGUMENTS);
    verify(source);
  }

  public void test_notEnoughRequiredArguments_functionExpression() throws Exception {
    Source source = addSource(createSource(//
        "main() {",
        "  (int x) {} ();",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.NOT_ENOUGH_REQUIRED_ARGUMENTS);
    verify(source);
  }

  public void test_partOfDifferentLibrary() throws Exception {
    Source source = addSource(createSource(//
        "library lib;",
        "part 'part.dart';"));
    addNamedSource("/part.dart", createSource(//
        "part of lub;"));
    resolve(source);
    assertErrors(source, StaticWarningCode.PART_OF_DIFFERENT_LIBRARY);
    verify(source);
  }

  public void test_redirectToInvalidFunctionType() throws Exception {
    Source source = addSource(createSource(//
        "class A implements B {",
        "  A(int p) {}",
        "}",
        "class B {",
        "  factory B() = A;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.REDIRECT_TO_INVALID_FUNCTION_TYPE);
    verify(source);
  }

  public void test_redirectToInvalidReturnType() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A() {}",
        "}",
        "class B {",
        "  factory B() = A;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.REDIRECT_TO_INVALID_RETURN_TYPE);
    verify(source);
  }

  public void test_redirectToMissingConstructor_named() throws Exception {
    Source source = addSource(createSource(//
        "class A implements B{",
        "  A() {}",
        "}",
        "class B {",
        "  factory B() = A.name;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.REDIRECT_TO_MISSING_CONSTRUCTOR);
  }

  public void test_redirectToMissingConstructor_unnamed() throws Exception {
    Source source = addSource(createSource(//
        "class A implements B{",
        "  A.name() {}",
        "}",
        "class B {",
        "  factory B() = A;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.REDIRECT_TO_MISSING_CONSTRUCTOR);
  }

  public void test_redirectToNonClass_notAType() throws Exception {
    Source source = addSource(createSource(//
        "class B {",
        "  int A;",
        "  factory B() = A;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.REDIRECT_TO_NON_CLASS);
    verify(source);
  }

  public void test_redirectToNonClass_undefinedIdentifier() throws Exception {
    Source source = addSource(createSource(//
        "class B {",
        "  factory B() = A;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.REDIRECT_TO_NON_CLASS);
    verify(source);
  }

  public void test_returnWithoutValue_factoryConstructor() throws Exception {
    Source source = addSource(createSource(//
    "class A { factory A() { return; } }"));
    resolve(source);
    assertErrors(source, StaticWarningCode.RETURN_WITHOUT_VALUE);
    verify(source);
  }

  public void test_returnWithoutValue_function() throws Exception {
    Source source = addSource(createSource(//
    "int f() { return; }"));
    resolve(source);
    assertErrors(source, StaticWarningCode.RETURN_WITHOUT_VALUE);
    verify(source);
  }

  public void test_returnWithoutValue_method() throws Exception {
    Source source = addSource(createSource(//
    "class A { int m() { return; } }"));
    resolve(source);
    assertErrors(source, StaticWarningCode.RETURN_WITHOUT_VALUE);
    verify(source);
  }

  public void test_returnWithoutValue_mixedReturnTypes_function() throws Exception {
    // Tests that only the RETURN_WITHOUT_VALUE warning is created, and no MIXED_RETURN_TYPES are
    // created.
    Source source = addSource(createSource(//
        "int f(int x) {",
        "  if (x < 0) {",
        "    return 1;",
        "  }",
        "  return;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.RETURN_WITHOUT_VALUE);
    verify(source);
  }

  public void test_staticAccessToInstanceMember_method_invocation() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m() {}",
        "}",
        "main() {",
        "  A.m();",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.STATIC_ACCESS_TO_INSTANCE_MEMBER);
    verify(source);
  }

  public void test_staticAccessToInstanceMember_method_reference() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m() {}",
        "}",
        "main() {",
        "  A.m;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.STATIC_ACCESS_TO_INSTANCE_MEMBER);
    verify(source);
  }

  public void test_staticAccessToInstanceMember_propertyAccess_field() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  var f;",
        "}",
        "main() {",
        "  A.f;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.STATIC_ACCESS_TO_INSTANCE_MEMBER);
    verify(source);
  }

  public void test_staticAccessToInstanceMember_propertyAccess_getter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  get f => 42;",
        "}",
        "main() {",
        "  A.f;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.STATIC_ACCESS_TO_INSTANCE_MEMBER);
    verify(source);
  }

  public void test_staticAccessToInstanceMember_propertyAccess_setter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  set f(x) {}",
        "}",
        "main() {",
        "  A.f = 42;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.STATIC_ACCESS_TO_INSTANCE_MEMBER);
    verify(source);
  }

  public void test_switchExpressionNotAssignable() throws Exception {
    Source source = addSource(createSource(//
        "f(int p) {",
        "  switch (p) {",
        "    case 'a': break;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.SWITCH_EXPRESSION_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_typeParameterReferencedByStatic_field() throws Exception {
    Source source = addSource(createSource(//
        "class A<K> {",
        "  static K k;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.TYPE_PARAMETER_REFERENCED_BY_STATIC);
    verify(source);
  }

  public void test_typeParameterReferencedByStatic_getter() throws Exception {
    Source source = addSource(createSource(//
        "class A<K> {",
        "  static K get k => 0;",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.TYPE_PARAMETER_REFERENCED_BY_STATIC);
    verify(source);
  }

  public void test_typeParameterReferencedByStatic_methodBodyReference() throws Exception {
    Source source = addSource(createSource(//
        "class A<K> {",
        "  static m() {",
        "    K k;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.TYPE_PARAMETER_REFERENCED_BY_STATIC);
    verify(source);
  }

  public void test_typeParameterReferencedByStatic_methodParameter() throws Exception {
    Source source = addSource(createSource(//
        "class A<K> {",
        "  static m(K k) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.TYPE_PARAMETER_REFERENCED_BY_STATIC);
    verify(source);
  }

  public void test_typeParameterReferencedByStatic_methodReturn() throws Exception {
    Source source = addSource(createSource(//
        "class A<K> {",
        "  static K m() { return null; }",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.TYPE_PARAMETER_REFERENCED_BY_STATIC);
    verify(source);
  }

  public void test_typeParameterReferencedByStatic_setter() throws Exception {
    Source source = addSource(createSource(//
        "class A<K> {",
        "  static set s(K k) {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.TYPE_PARAMETER_REFERENCED_BY_STATIC);
    verify(source);
  }

  public void test_typePromotion_functionType_arg_InterToDyn() throws Exception {
    Source source = addSource(createSource(//
        "typedef FuncDyn(x);",
        "typedef FuncA(A a);",
        "class A {}",
        "class B {}",
        "main(FuncA f) {",
        "  if (f is FuncDyn) {", // ignored: dynamic !<< A
        "    f(new B());",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
  }

  public void test_typeTestNonType() throws Exception {
    Source source = addSource(createSource(//
        "var A = 0;",
        "f(var p) {",
        "  if (p is A) {",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.TYPE_TEST_NON_TYPE);
    verify(source);
  }

  public void test_undefinedClass_instanceCreation() throws Exception {
    Source source = addSource(createSource(//
    "f() { new C(); }"));
    resolve(source);
    assertErrors(source, StaticWarningCode.UNDEFINED_CLASS);
  }

  public void test_undefinedClass_variableDeclaration() throws Exception {
    Source source = addSource(createSource(//
    "f() { C c; }"));
    resolve(source);
    assertErrors(source, StaticWarningCode.UNDEFINED_CLASS);
  }

  public void test_undefinedClassBoolean_variableDeclaration() throws Exception {
    Source source = addSource(createSource(//
    "f() { boolean v; }"));
    resolve(source);
    assertErrors(source, StaticWarningCode.UNDEFINED_CLASS_BOOLEAN);
  }

  public void test_undefinedGetter_fromLibrary() throws Exception {
    Source source1 = addNamedSource("lib.dart", "");
    Source source2 = addNamedSource("lib2.dart", createSource(//
        "import 'lib.dart' as lib;",
        "void f() {",
        "  var g = lib.gg;",
        "}"));
    resolve(source1);
    resolve(source2);
    assertErrors(source2, StaticWarningCode.UNDEFINED_GETTER);
    verify(source1);
  }

  public void test_undefinedIdentifier_for() throws Exception {
    Source source = addSource(createSource(//
        "f(var l) {",
        "  for (e in l) {",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.UNDEFINED_IDENTIFIER);
  }

  public void test_undefinedIdentifier_function() throws Exception {
    Source source = addSource(createSource(//
    "int a() => b;"));
    resolve(source);
    assertErrors(source, StaticWarningCode.UNDEFINED_IDENTIFIER);
  }

  public void test_undefinedIdentifier_function_prefix() throws Exception {
    addNamedSource("/lib.dart", createSource(//
        "library lib;",
        "class C {}"));
    Source source = addSource(createSource(//
        "import 'lib.dart' as b;",
        "",
        "int a() => b;",
        "b.C c;"));
    resolve(source);
    assertErrors(source, StaticWarningCode.UNDEFINED_IDENTIFIER);
    verify(source);
  }

  public void test_undefinedIdentifier_initializer() throws Exception {
    Source source = addSource(createSource(//
    "var a = b;"));
    resolve(source);
    assertErrors(source, StaticWarningCode.UNDEFINED_IDENTIFIER);
  }

  public void test_undefinedIdentifier_initializer_prefix() throws Exception {
    addNamedSource("/lib.dart", createSource(//
        "library lib;",
        "class C {}"));
    Source source = addSource(createSource(//
        "import 'lib.dart' as b;",
        "",
        "var a = b;",
        "b.C c;"));
    resolve(source);
    assertErrors(source, StaticWarningCode.UNDEFINED_IDENTIFIER);
  }

  public void test_undefinedIdentifier_methodInvocation() throws Exception {
    Source source = addSource(createSource(//
    "f() { C.m(); }"));
    resolve(source);
    assertErrors(source, StaticWarningCode.UNDEFINED_IDENTIFIER);
  }

  public void test_undefinedIdentifier_private_getter() throws Exception {
    addNamedSource("/lib.dart", createSource(//
        "library lib;",
        "class A {",
        "  var _foo;",
        "}"));
    Source source = addSource(createSource(//
        "import 'lib.dart';",
        "class B extends A {",
        "  test() {",
        "    var v = _foo;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.UNDEFINED_IDENTIFIER);
  }

  public void test_undefinedIdentifier_private_setter() throws Exception {
    addNamedSource("/lib.dart", createSource(//
        "library lib;",
        "class A {",
        "  var _foo;",
        "}"));
    Source source = addSource(createSource(//
        "import 'lib.dart';",
        "class B extends A {",
        "  test() {",
        "    _foo = 42;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.UNDEFINED_IDENTIFIER);
  }

  public void test_undefinedNamedParameter() throws Exception {
    Source source = addSource(createSource(//
        "f({a, b}) {}",
        "main() {",
        "  f(c: 1);",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.UNDEFINED_NAMED_PARAMETER);
    // no verify(), 'c' is not resolved
  }

  public void test_undefinedSetter() throws Exception {
    Source source1 = addNamedSource("lib.dart", "");
    Source source2 = addNamedSource("lib2.dart", createSource(//
        "import 'lib.dart' as lib;",
        "void f() {",
        "  lib.gg = null;",
        "}"));
    resolve(source1);
    resolve(source2);
    assertErrors(source2, StaticWarningCode.UNDEFINED_SETTER);
  }

  // See comment on StaticWarningCode.UNDEFINED_STATIC_METHOD_OR_GETTER
  public void test_undefinedStaticMethodOrGetter_getter() throws Exception {
    Source source = addSource(createSource(//
        "class C {}",
        "f(var p) {",
        "  f(C.m);",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_GETTER);
  }

  // See comment on StaticWarningCode.UNDEFINED_STATIC_METHOD_OR_GETTER
  public void test_undefinedStaticMethodOrGetter_getter_inSuperclass() throws Exception {
    Source source = addSource(createSource(//
        "class S {",
        "  static int get g => 0;",
        "}",
        "class C extends S {}",
        "f(var p) {",
        "  f(C.g);",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_GETTER);
  }

  // See comment on StaticWarningCode.UNDEFINED_STATIC_METHOD_OR_GETTER
  public void test_undefinedStaticMethodOrGetter_method() throws Exception {
    Source source = addSource(createSource(//
        "class C {}",
        "f(var p) {",
        "  f(C.m());",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_METHOD);
  }

  // See comment on StaticWarningCode.UNDEFINED_STATIC_METHOD_OR_GETTER
  public void test_undefinedStaticMethodOrGetter_method_inSuperclass() throws Exception {
    Source source = addSource(createSource(//
        "class S {",
        "  static m() {}",
        "}",
        "class C extends S {}",
        "f(var p) {",
        "  f(C.m());",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_METHOD);
  }

  // See comment on StaticWarningCode.UNDEFINED_STATIC_METHOD_OR_GETTER
  public void test_undefinedStaticMethodOrGetter_setter_inSuperclass() throws Exception {
    Source source = addSource(createSource(//
        "class S {",
        "  static set s(int i) {}",
        "}",
        "class C extends S {}",
        "f(var p) {",
        "  f(C.s = 1);",
        "}"));
    resolve(source);
    assertErrors(source, StaticTypeWarningCode.UNDEFINED_SETTER);
  }

  public void test_voidReturnForGetter() throws Exception {
    Source source = addSource(createSource(//
        "class S {",
        "  void get value {}",
        "}"));
    resolve(source);
    assertErrors(source, StaticWarningCode.VOID_RETURN_FOR_GETTER);
  }
}
