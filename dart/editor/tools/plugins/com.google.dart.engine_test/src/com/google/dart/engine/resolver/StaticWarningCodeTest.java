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

import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.source.Source;

public class StaticWarningCodeTest extends ResolverTestCase {
  // TODO(scheglov) requires fix for TypeVariableTypeImpl.isSubtypeOf()
  public void fail_argumentTypeNotAssignable_invocation_functionParameter_generic()
      throws Exception {
    Source source = addSource(createSource(//
        "class A<K, V> {",
        "  m(f(K k), V v) {",
        "    f(v);",
        "  }",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void fail_commentReferenceConstructorNotVisible() throws Exception {
    Source source = addSource(createSource(//
    // TODO
    ));
    resolve(source);
    assertErrors(StaticWarningCode.COMMENT_REFERENCE_CONSTRUCTOR_NOT_VISIBLE);
    verify(source);
  }

  public void fail_commentReferenceIdentifierNotVisible() throws Exception {
    Source source = addSource(createSource(//
    // TODO
    ));
    resolve(source);
    assertErrors(StaticWarningCode.COMMENT_REFERENCE_IDENTIFIER_NOT_VISIBLE);
    verify(source);
  }

  public void fail_commentReferenceUndeclaredConstructor() throws Exception {
    Source source = addSource(createSource(//
    // TODO
    ));
    resolve(source);
    assertErrors(StaticWarningCode.COMMENT_REFERENCE_UNDECLARED_CONSTRUCTOR);
    verify(source);
  }

  public void fail_commentReferenceUndeclaredIdentifier() throws Exception {
    Source source = addSource(createSource(//
    // TODO
    ));
    resolve(source);
    assertErrors(StaticWarningCode.COMMENT_REFERENCE_UNDECLARED_IDENTIFIER);
    verify(source);
  }

  public void fail_commentReferenceUriNotLibrary() throws Exception {
    Source source = addSource(createSource(//
    // TODO
    ));
    resolve(source);
    assertErrors(StaticWarningCode.COMMENT_REFERENCE_URI_NOT_LIBRARY);
    verify(source);
  }

  public void fail_finalNotInitialized_inConstructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final int x;",
        "  A() {}",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.FINAL_NOT_INITIALIZED);
    verify(source);
  }

  public void fail_incorrectNumberOfArguments_tooFew() throws Exception {
    Source source = addSource(createSource(//
        "f(a, b) => 0;",
        "g() {",
        "  f(2);",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.INCORRECT_NUMBER_OF_ARGUMENTS);
    verify(source);
  }

  public void fail_incorrectNumberOfArguments_tooMany() throws Exception {
    Source source = addSource(createSource(//
        "f(a, b) => 0;",
        "g() {",
        "  f(2, 3, 4);",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.INCORRECT_NUMBER_OF_ARGUMENTS);
    verify(source);
  }

  public void fail_invalidFactoryName() throws Exception {
    Source source = addSource(createSource(//
    // TODO
    ));
    resolve(source);
    assertErrors(StaticWarningCode.INVALID_FACTORY_NAME);
    verify(source);
  }

  public void fail_invocationOfNonFunction() throws Exception {
    Source source = addSource(createSource(//
    // TODO
    ));
    resolve(source);
    assertErrors(StaticWarningCode.INVOCATION_OF_NON_FUNCTION);
    verify(source);
  }

  public void fail_mismatchedAccessorTypes_getterAndSuperSetter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int get g { return 0; }",
        "  set g(int v) {}",
        "}",
        "class B extends A {",
        "  set g(String v) {}",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.MISMATCHED_GETTER_AND_SETTER_TYPES);
    verify(source);
  }

  public void fail_mismatchedAccessorTypes_superGetterAndSetter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int get g { return 0; }",
        "  set g(int v) {}",
        "}",
        "class B extends A {",
        "  String get g { return ''; }",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.MISMATCHED_GETTER_AND_SETTER_TYPES);
    verify(source);
  }

  public void fail_undefinedGetter() throws Exception {
    Source source = addSource(createSource(//
    // TODO
    ));
    resolve(source);
    assertErrors(StaticWarningCode.UNDEFINED_GETTER);
    verify(source);
  }

  public void fail_undefinedIdentifier_commentReference() throws Exception {
    Source source = addSource(createSource(//
        "/** [m] xxx [new B.c] */",
        "class A {",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.UNDEFINED_IDENTIFIER, StaticWarningCode.UNDEFINED_IDENTIFIER);
  }

  public void fail_undefinedIdentifier_function() throws Exception {
    Source source = addSource(createSource(//
    "int a() => b;"));
    resolve(source);
    assertErrors(StaticWarningCode.UNDEFINED_IDENTIFIER);
    verify(source);
  }

  public void fail_undefinedSetter() throws Exception {
    Source source = addSource(createSource(//
        "class C {}",
        "f(var p) {",
        "  C.m = 0;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.UNDEFINED_SETTER);
    verify(source);
  }

  public void fail_undefinedStaticMethodOrGetter_getter() throws Exception {
    Source source = addSource(createSource(//
        "class C {}",
        "f(var p) {",
        "  f(C.m);",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.UNDEFINED_STATIC_METHOD_OR_GETTER);
    verify(source);
  }

  public void fail_undefinedStaticMethodOrGetter_method() throws Exception {
    Source source = addSource(createSource(//
        "class C {}",
        "f(var p) {",
        "  f(C.m());",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.UNDEFINED_STATIC_METHOD_OR_GETTER);
    verify(source);
  }

  public void test_ambiguousImport_typeAnnotation() throws Exception {
    Source source = addSource(createSource(//
        "import 'lib1.dart';",
        "import 'lib2.dart';",
        "typedef N FT(N p);",
        "N f(N p) {",
        "  N v;",
        "}",
        "class A {",
        "  N m() {}",
        "}",
        "class B<T extends N> {}"));
    addSource("/lib1.dart", createSource(//
        "library lib1;",
        "class N {}"));
    addSource("/lib2.dart", createSource(//
        "library lib2;",
        "class N {}"));
    resolve(source);
    assertErrors(
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
        "A<N> f() {}"));
    addSource("/lib1.dart", createSource(//
        "library lib1;",
        "class N {}"));
    addSource("/lib2.dart", createSource(//
        "library lib2;",
        "class N {}"));
    resolve(source);
    assertErrors(StaticWarningCode.AMBIGUOUS_IMPORT);
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
    assertErrors(StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
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
    assertErrors(StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
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
    assertErrors(StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
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
    assertErrors(StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_invocation_functionParameter() throws Exception {
    Source source = addSource(createSource(//
        "a(b(int p)) {",
        "  b('0');",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
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
    assertErrors(StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_invocation_named() throws Exception {
    Source source = addSource(createSource(//
        "f({String p}) {}",
        "main() {",
        "  f(p: 42);",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_invocation_optional() throws Exception {
    Source source = addSource(createSource(//
        "f([String p]) {}",
        "main() {",
        "  f(42);",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_invocation_required() throws Exception {
    Source source = addSource(createSource(//
        "f(String p) {}",
        "main() {",
        "  f(42);",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_invocation_typedef_generic() throws Exception {
    Source source = addSource(createSource(//
        "typedef A<T>(T p);",
        "f(A<int> a) {",
        "  a('1');",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
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
    assertErrors(StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_argumentTypeNotAssignable_invocation_typedef_parameter() throws Exception {
    Source source = addSource(createSource(//
        "typedef A(int p);",
        "f(A a) {",
        "  a('1');",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
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
    assertErrors(StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
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
    assertErrors(StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
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
    assertErrors(StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
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
    assertErrors(StaticWarningCode.ASSIGNMENT_TO_FINAL);
    verify(source);
  }

  public void test_assignmentToFinal_localVariable() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  final x = 0;",
        "  x = 1;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.ASSIGNMENT_TO_FINAL);
    verify(source);
  }

  public void test_assignmentToFinal_prefixMinusMinus() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  final x = 0;",
        "  --x;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.ASSIGNMENT_TO_FINAL);
    verify(source);
  }

  public void test_assignmentToFinal_prefixPlusPlus() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  final x = 0;",
        "  ++x;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.ASSIGNMENT_TO_FINAL);
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
    assertErrors(StaticWarningCode.ASSIGNMENT_TO_FINAL);
    verify(source);
  }

  public void test_assignmentToFinal_suffixMinusMinus() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  final x = 0;",
        "  x--;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.ASSIGNMENT_TO_FINAL);
    verify(source);
  }

  public void test_assignmentToFinal_suffixPlusPlus() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  final x = 0;",
        "  x++;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.ASSIGNMENT_TO_FINAL);
    verify(source);
  }

  public void test_assignmentToFinal_topLevelVariable() throws Exception {
    Source source = addSource(createSource(//
        "final x = 0;",
        "f() { x = 1; }"));
    resolve(source);
    assertErrors(StaticWarningCode.ASSIGNMENT_TO_FINAL);
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
    assertErrors(StaticWarningCode.CASE_BLOCK_NOT_TERMINATED);
    verify(source);
  }

  public void test_castToNonType() throws Exception {
    Source source = addSource(createSource(//
        "var A = 0;",
        "f(String s) { var x = s as A; }"));
    resolve(source);
    assertErrors(StaticWarningCode.CAST_TO_NON_TYPE);
    verify(source);
  }

  public void test_concreteClassWithAbstractMember() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m();",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.CONCRETE_CLASS_WITH_ABSTRACT_MEMBER);
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
    assertErrors(StaticWarningCode.CONFLICTING_INSTANCE_GETTER_AND_SUPERCLASS_MEMBER);
    verify(source);
  }

  public void test_conflictingInstanceGetterAndSuperclassMember_direct_getter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static get v => 0;",
        "}",
        "class B extends A {",
        "  get v => 0;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.CONFLICTING_INSTANCE_GETTER_AND_SUPERCLASS_MEMBER);
    verify(source);
  }

  public void test_conflictingInstanceGetterAndSuperclassMember_direct_method() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static v() {}",
        "}",
        "class B extends A {",
        "  get v => 0;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.CONFLICTING_INSTANCE_GETTER_AND_SUPERCLASS_MEMBER);
    verify(source);
  }

  public void test_conflictingInstanceGetterAndSuperclassMember_direct_setter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static set v(x) {}",
        "}",
        "class B extends A {",
        "  get v => 0;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.CONFLICTING_INSTANCE_GETTER_AND_SUPERCLASS_MEMBER);
    verify(source);
  }

  public void test_conflictingInstanceGetterAndSuperclassMember_indirect() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static int v;",
        "}",
        "class B extends A {}",
        "class C extends B {",
        "  get v => 0;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.CONFLICTING_INSTANCE_GETTER_AND_SUPERCLASS_MEMBER);
    verify(source);
  }

  public void test_conflictingInstanceGetterAndSuperclassMember_mixin() throws Exception {
    Source source = addSource(createSource(//
        "class M {",
        "  static int v;",
        "}",
        "class B extends Object with M {",
        "  get v => 0;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.CONFLICTING_INSTANCE_GETTER_AND_SUPERCLASS_MEMBER);
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
    assertErrors(StaticWarningCode.CONFLICTING_INSTANCE_SETTER_AND_SUPERCLASS_MEMBER);
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
    assertErrors(StaticWarningCode.CONFLICTING_STATIC_GETTER_AND_INSTANCE_SETTER);
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
    assertErrors(StaticWarningCode.CONFLICTING_STATIC_GETTER_AND_INSTANCE_SETTER);
    verify(source);
  }

  public void test_conflictingStaticGetterAndInstanceSetter_thisClass() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static get x => 0;",
        "  set x(int p) {}",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.CONFLICTING_STATIC_GETTER_AND_INSTANCE_SETTER);
    verify(source);
  }

  public void test_conflictingStaticSetterAndInstanceMember_thisClass_getter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  get x => 0;",
        "  static set x(int p) {}",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.CONFLICTING_STATIC_SETTER_AND_INSTANCE_MEMBER);
    verify(source);
  }

  public void test_conflictingStaticSetterAndInstanceMember_thisClass_method() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  x() {}",
        "  static set x(int p) {}",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.CONFLICTING_STATIC_SETTER_AND_INSTANCE_MEMBER);
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
    assertErrors(StaticWarningCode.CONST_WITH_ABSTRACT_CLASS);
    verify(source);
  }

  public void test_equalKeysInMap() throws Exception {
    Source source = addSource(createSource(//
    "var m = {'a' : 0, 'b' : 1, 'a' : 2};"));
    resolve(source);
    assertErrors(StaticWarningCode.EQUAL_KEYS_IN_MAP);
    verify(source);
  }

  public void test_exportDuplicatedLibraryName() throws Exception {
    Source source = addSource(createSource(//
        "library test;",
        "export 'lib1.dart';",
        "export 'lib2.dart';"));
    addSource("/lib1.dart", "library lib;");
    addSource("/lib2.dart", "library lib;");
    resolve(source);
    assertErrors(StaticWarningCode.EXPORT_DUPLICATED_LIBRARY_NAME);
    verify(source);
  }

  public void test_extraPositionalArguments() throws Exception {
    Source source = addSource(createSource(//
        "f() {}",
        "main() {",
        "  f(0, 1, '2');",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.EXTRA_POSITIONAL_ARGUMENTS);
    verify(source);
  }

  public void test_fieldInitializerNotAssignable() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int x;",
        "  A() : x = '';",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.FIELD_INITIALIZER_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_fieldInitializingFormalNotAssignable() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int x;",
        "  A(String this.x) {}",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.FIELD_INITIALIZING_FORMAL_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_finalNotInitialized_instanceField_const_static() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static const F;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.FINAL_NOT_INITIALIZED);
    verify(source);
  }

  public void test_finalNotInitialized_instanceField_final() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  final F;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.FINAL_NOT_INITIALIZED);
    verify(source);
  }

  public void test_finalNotInitialized_instanceField_final_static() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  static final F;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.FINAL_NOT_INITIALIZED);
    verify(source);
  }

  public void test_finalNotInitialized_library_const() throws Exception {
    Source source = addSource(createSource(//
    "const F;"));
    resolve(source);
    assertErrors(StaticWarningCode.FINAL_NOT_INITIALIZED);
    verify(source);
  }

  public void test_finalNotInitialized_library_final() throws Exception {
    Source source = addSource(createSource(//
    "final F;"));
    resolve(source);
    assertErrors(StaticWarningCode.FINAL_NOT_INITIALIZED);
    verify(source);
  }

  public void test_finalNotInitialized_local_const() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  const int x;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.FINAL_NOT_INITIALIZED);
    verify(source);
  }

  public void test_finalNotInitialized_local_final() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  final int x;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.FINAL_NOT_INITIALIZED);
    verify(source);
  }

  public void test_importDuplicatedLibraryName() throws Exception {
    Source source = addSource(createSource(//
        "library test;",
        "import 'lib1.dart';",
        "import 'lib2.dart';"));
    addSource("/lib1.dart", "library lib;");
    addSource("/lib2.dart", "library lib;");
    resolve(source);
    assertErrors(StaticWarningCode.IMPORT_DUPLICATED_LIBRARY_NAME);
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
    assertErrors(StaticWarningCode.INCONSISTENT_METHOD_INHERITANCE_GETTER_AND_METHOD);
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
    assertErrors(StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC);
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
    assertErrors(StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC);
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
    assertErrors(StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC);
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
    assertErrors(StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC);
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
    assertErrors(StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC);
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
    assertErrors(StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC);
    verify(source);
  }

  public void test_instanceMethodNameCollidesWithSuperclassStatic_setter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int i = 0;",
        "  static set n(int x) {i = x;}",
        "}",
        "class B extends A {",
        "  void n() {}",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC);
    verify(source);
  }

  public void test_instanceMethodNameCollidesWithSuperclassStatic_setter2() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int i = 0;",
        "  static set n(int x) {i = x;}",
        "}",
        "class B extends A {",
        "}",
        "class C extends B {",
        "  void n() {}",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC);
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
    assertErrors(StaticWarningCode.INVALID_GETTER_OVERRIDE_RETURN_TYPE);
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
    assertErrors(StaticWarningCode.INVALID_METHOD_OVERRIDE_NAMED_PARAM_TYPE);
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
    assertErrors(StaticWarningCode.INVALID_METHOD_OVERRIDE_NORMAL_PARAM_TYPE);
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
    assertErrors(StaticWarningCode.INVALID_METHOD_OVERRIDE_OPTIONAL_PARAM_TYPE);
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
    assertErrors(StaticWarningCode.INVALID_METHOD_OVERRIDE_RETURN_TYPE);
    verify(source);
  }

  public void test_invalidMethodOverrideReturnType_interface2() throws Exception {
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
    assertErrors(StaticWarningCode.INVALID_METHOD_OVERRIDE_RETURN_TYPE);
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
    assertErrors(StaticWarningCode.INVALID_METHOD_OVERRIDE_RETURN_TYPE);
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
    assertErrors(StaticWarningCode.INVALID_METHOD_OVERRIDE_RETURN_TYPE);
    verify(source);
  }

  public void test_invalidMethodOverrideReturnType_superclass2() throws Exception {
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
    assertErrors(StaticWarningCode.INVALID_METHOD_OVERRIDE_RETURN_TYPE);
    verify(source);
  }

  public void test_invalidMethodOverrideReturnType_void() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int m() {}",
        "}",
        "class B extends A {",
        "  void m() {}",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.INVALID_METHOD_OVERRIDE_RETURN_TYPE);
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
    assertErrors(StaticWarningCode.INVALID_OVERRIDE_DIFFERENT_DEFAULT_VALUES_NAMED);
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
    assertErrors(StaticWarningCode.INVALID_OVERRIDE_DIFFERENT_DEFAULT_VALUES_POSITIONAL);
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
    assertErrors(StaticWarningCode.INVALID_SETTER_OVERRIDE_NORMAL_PARAM_TYPE);
    verify(source);
  }

  public void test_mismatchedAccessorTypes_class() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int get g { return 0; }",
        "  set g(String v) {}",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.MISMATCHED_GETTER_AND_SETTER_TYPES);
    verify(source);
  }

  public void test_mismatchedAccessorTypes_topLevel() throws Exception {
    Source source = addSource(createSource(//
        "int get g { return 0; }",
        "set g(String v) {}"));
    resolve(source);
    assertErrors(StaticWarningCode.MISMATCHED_GETTER_AND_SETTER_TYPES);
    verify(source);
  }

  public void test_newWithAbstractClass() throws Exception {
    Source source = addSource(createSource(//
        "abstract class A {}",
        "void f() {",
        "  A a = new A();",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.NEW_WITH_ABSTRACT_CLASS);
    verify(source);
  }

  public void test_newWithNonType() throws Exception {
    Source source = addSource(createSource(//
        "var A = 0;",
        "void f() {",
        "  var a = new A();",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.NEW_WITH_NON_TYPE);
    verify(source);
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
    assertErrors(StaticWarningCode.NEW_WITH_UNDEFINED_CONSTRUCTOR);
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
    assertErrors(StaticWarningCode.NEW_WITH_UNDEFINED_CONSTRUCTOR_DEFAULT);
    verify(source);
  }

  public void test_noDefaultSuperConstructorExplicit() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A(p);",
        "}",
        "class B extends A {",
        "  B() {}",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.NO_DEFAULT_SUPER_CONSTRUCTOR_EXPLICIT);
    verify(source);
  }

  public void test_noDefaultSuperConstructorImplicit_superHasParameters() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A(p);",
        "}",
        "class B extends A {",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.NO_DEFAULT_SUPER_CONSTRUCTOR_IMPLICIT);
    verify(source);
  }

  public void test_noDefaultSuperConstructorImplicit_superOnlyNamed() throws Exception {
    Source source = addSource(createSource(//
        "class A { A.named() {} }",
        "class B extends A {}"));
    resolve(source);
    assertErrors(StaticWarningCode.NO_DEFAULT_SUPER_CONSTRUCTOR_IMPLICIT);
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
    assertErrors(StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_FIVE_PLUS);
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
    assertErrors(StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_FOUR);
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
    assertErrors(StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE);
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
    assertErrors(StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE);
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
    assertErrors(StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE);
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
    assertErrors(StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE);
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
    assertErrors(StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE);
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
    assertErrors(StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE);
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
    assertErrors(StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE);
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
    assertErrors(StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_ONE);
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
    assertErrors(StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_THREE);
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
    assertErrors(StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER_TWO);
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
    assertErrors(StaticWarningCode.NON_TYPE_IN_CATCH_CLAUSE);
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
    assertErrors(StaticWarningCode.NON_TYPE_IN_CATCH_CLAUSE);
    verify(source);
  }

  public void test_nonVoidReturnForOperator() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  int operator []=(a, b) {}",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.NON_VOID_RETURN_FOR_OPERATOR);
    verify(source);
  }

  public void test_nonVoidReturnForSetter_function() throws Exception {
    Source source = addSource(createSource(//
        "int set x(int v) {",
        "  return 42;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.NON_VOID_RETURN_FOR_SETTER);
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
    assertErrors(StaticWarningCode.NON_VOID_RETURN_FOR_SETTER);
    verify(source);
  }

  public void test_notAType() throws Exception {
    Source source = addSource(createSource(//
        "f() {}",
        "main() {",
        "  f v = null;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.NOT_A_TYPE);
    verify(source);
  }

  public void test_notEnoughRequiredArguments() throws Exception {
    Source source = addSource(createSource(//
        "f(int a, String b) {}",
        "main() {",
        "  f();",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.NOT_ENOUGH_REQUIRED_ARGUMENTS);
    verify(source);
  }

  public void test_partOfDifferentLibrary() throws Exception {
    Source source = addSource(createSource(//
        "library lib;",
        "part 'part.dart';"));
    addSource("/part.dart", createSource(//
        "part of lub;"));
    resolve(source);
    assertErrors(StaticWarningCode.PART_OF_DIFFERENT_LIBRARY);
    verify(source);
  }

  public void test_redirectToInvalidFunctionType() throws Exception {
    Source source = addSource(createSource(//
        "class A implements B {",
        "  A(int p) {}",
        "}",
        "class B {",
        "  B() = A;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.REDIRECT_TO_INVALID_FUNCTION_TYPE);
    verify(source);
  }

  public void test_redirectToInvalidReturnType() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  A() {}",
        "}",
        "class B {",
        "  B() = A;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.REDIRECT_TO_INVALID_RETURN_TYPE);
    verify(source);
  }

  public void test_redirectToMissingConstructor_named() throws Exception {
    Source source = addSource(createSource(//
        "class A implements B{",
        "  A() {}",
        "}",
        "class B {",
        "  B() = A.name;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.REDIRECT_TO_MISSING_CONSTRUCTOR);
  }

  public void test_redirectToMissingConstructor_unnamed() throws Exception {
    Source source = addSource(createSource(//
        "class A implements B{",
        "  A.name() {}",
        "}",
        "class B {",
        "  B() = A;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.REDIRECT_TO_MISSING_CONSTRUCTOR);
  }

  public void test_redirectToNonClass_notAType() throws Exception {
    Source source = addSource(createSource(//
        "class B {",
        "  int A;",
        "  B() = A;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.REDIRECT_TO_NON_CLASS);
    verify(source);
  }

  public void test_redirectToNonClass_undefinedIdentifier() throws Exception {
    Source source = addSource(createSource(//
        "class B {",
        "  B() = A;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.REDIRECT_TO_NON_CLASS);
    verify(source);
  }

  public void test_returnWithoutValue() throws Exception {
    Source source = addSource(createSource(//
    "int f() { return; }"));
    resolve(source);
    assertErrors(StaticWarningCode.RETURN_WITHOUT_VALUE);
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
    assertErrors(StaticWarningCode.STATIC_ACCESS_TO_INSTANCE_MEMBER);
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
    assertErrors(StaticWarningCode.STATIC_ACCESS_TO_INSTANCE_MEMBER);
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
    assertErrors(StaticWarningCode.STATIC_ACCESS_TO_INSTANCE_MEMBER);
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
    assertErrors(StaticWarningCode.STATIC_ACCESS_TO_INSTANCE_MEMBER);
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
    assertErrors(StaticWarningCode.STATIC_ACCESS_TO_INSTANCE_MEMBER);
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
    assertErrors(StaticWarningCode.SWITCH_EXPRESSION_NOT_ASSIGNABLE);
    verify(source);
  }

  public void test_typeTestNonType() throws Exception {
    Source source = addSource(createSource(//
        "var A = 0;",
        "f(var p) {",
        "  if (p is A) {",
        "  }",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.TYPE_TEST_NON_TYPE);
    verify(source);
  }

  public void test_undefinedClass_instanceCreation() throws Exception {
    Source source = addSource(createSource(//
    "f() { new C(); }"));
    resolve(source);
    assertErrors(StaticWarningCode.UNDEFINED_CLASS);
  }

  public void test_undefinedClass_variableDeclaration() throws Exception {
    Source source = addSource(createSource(//
    "f() { C c; }"));
    resolve(source);
    assertErrors(StaticWarningCode.UNDEFINED_CLASS);
  }

  public void test_undefinedClassBoolean_variableDeclaration() throws Exception {
    Source source = addSource(createSource(//
    "f() { boolean v; }"));
    resolve(source);
    assertErrors(StaticWarningCode.UNDEFINED_CLASS_BOOLEAN);
  }

  public void test_undefinedIdentifier_initializer() throws Exception {
    Source source = addSource(createSource(//
    "var a = b;"));
    resolve(source);
    assertErrors(StaticWarningCode.UNDEFINED_IDENTIFIER);
  }

  public void test_undefinedIdentifier_metadata() throws Exception {
    Source source = addSource(createSource(//
    "@undefined class A {}"));
    resolve(source);
    assertErrors(StaticWarningCode.UNDEFINED_IDENTIFIER);
  }

  public void test_undefinedIdentifier_methodInvocation() throws Exception {
    Source source = addSource(createSource(//
    "f() { C.m(); }"));
    resolve(source);
    assertErrors(StaticWarningCode.UNDEFINED_IDENTIFIER);
  }

  public void test_undefinedNamedParameter() throws Exception {
    Source source = addSource(createSource(//
        "f({a, b}) {}",
        "main() {",
        "  f(c: 1);",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.UNDEFINED_NAMED_PARAMETER);
    // no verify(), 'c' is not resolved
  }
}
