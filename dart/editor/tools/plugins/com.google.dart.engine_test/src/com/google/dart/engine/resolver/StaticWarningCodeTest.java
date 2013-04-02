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
  public void fail_argumentTypeNotAssignable() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(StaticWarningCode.ARGUMENT_TYPE_NOT_ASSIGNABLE);
    verify(source);
  }

  public void fail_assignmentToFinal() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "final x = 0;",
        "f() { x = 1; }"));
    resolve(source);
    assertErrors(StaticWarningCode.ASSIGNMENT_TO_FINAL);
    verify(source);
  }

  public void fail_caseBlockNotTerminated() throws Exception {
    Source source = addSource("/test.dart", createSource(//
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

  public void fail_castToNonType() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "var A = 0;",
        "f(String s) { var x = s as A; }"));
    resolve(source);
    assertErrors(StaticWarningCode.CAST_TO_NON_TYPE);
    verify(source);
  }

  public void fail_commentReferenceConstructorNotVisible() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(StaticWarningCode.COMMENT_REFERENCE_CONSTRUCTOR_NOT_VISIBLE);
    verify(source);
  }

  public void fail_commentReferenceIdentifierNotVisible() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(StaticWarningCode.COMMENT_REFERENCE_IDENTIFIER_NOT_VISIBLE);
    verify(source);
  }

  public void fail_commentReferenceUndeclaredConstructor() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(StaticWarningCode.COMMENT_REFERENCE_UNDECLARED_CONSTRUCTOR);
    verify(source);
  }

  public void fail_commentReferenceUndeclaredIdentifier() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(StaticWarningCode.COMMENT_REFERENCE_UNDECLARED_IDENTIFIER);
    verify(source);
  }

  public void fail_commentReferenceUriNotLibrary() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(StaticWarningCode.COMMENT_REFERENCE_URI_NOT_LIBRARY);
    verify(source);
  }

  public void fail_concreteClassWithAbstractMember() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  m();",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.CONCRETE_CLASS_WITH_ABSTRACT_MEMBER);
    verify(source);
  }

  public void fail_conflictingInstanceGetterAndSuperclassMember() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(StaticWarningCode.CONFLICTING_INSTANCE_GETTER_AND_SUPERCLASS_MEMBER);
    verify(source);
  }

  public void fail_conflictingInstanceSetterAndSuperclassMember() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(StaticWarningCode.CONFLICTING_INSTANCE_SETTER_AND_SUPERCLASS_MEMBER);
    verify(source);
  }

  public void fail_conflictingStaticGetterAndInstanceSetter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  static get x -> 0;",
        "  set x(int p) {}",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.CONFLICTING_STATIC_GETTER_AND_INSTANCE_SETTER);
    verify(source);
  }

  public void fail_conflictingStaticSetterAndInstanceGetter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  get x -> 0;",
        "  static set x(int p) {}",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.CONFLICTING_STATIC_SETTER_AND_INSTANCE_GETTER);
    verify(source);
  }

  public void fail_fieldInitializerWithInvalidType() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  int x;",
        "  A(String this.x) {}",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.FIELD_INITIALIZER_WITH_INVALID_TYPE);
    verify(source);
  }

  public void fail_incorrectNumberOfArguments_tooFew() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f(a, b) -> 0;",
        "g() {",
        "  f(2);",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.INCORRECT_NUMBER_OF_ARGUMENTS);
    verify(source);
  }

  public void fail_incorrectNumberOfArguments_tooMany() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f(a, b) -> 0;",
        "g() {",
        "  f(2, 3, 4);",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.INCORRECT_NUMBER_OF_ARGUMENTS);
    verify(source);
  }

  public void fail_instanceMethodNameCollidesWithSuperclassStatic() throws Exception { // superclass, interface, mixin
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  static n",
        "}",
        "class C extends A {",
        "  void n() {}",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.INSTANCE_METHOD_NAME_COLLIDES_WITH_SUPERCLASS_STATIC);
    verify(source);
  }

  public void fail_invalidFactoryName() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(StaticWarningCode.INVALID_FACTORY_NAME);
    verify(source);
  }

  public void fail_invalidOverrideGetterType() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  int get g -> 0",
        "}",
        "class B extends A {",
        "  String get g { return 'a'; }",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.INVALID_OVERRIDE_GETTER_TYPE);
    verify(source);
  }

  public void fail_invalidOverrideReturnType() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  int m() { return 0; }",
        "}",
        "class B extends A {",
        "  String m() { return 'a'; }",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.INVALID_OVERRIDE_RETURN_TYPE);
    verify(source);
  }

  public void fail_invalidOverrideSetterReturnType() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  void set s(int v) {}",
        "}",
        "class B extends A {",
        "  void set s(String v) {}",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.INVALID_OVERRIDE_SETTER_RETURN_TYPE);
    verify(source);
  }

  public void fail_invocationOfNonFunction() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(StaticWarningCode.INVOCATION_OF_NON_FUNCTION);
    verify(source);
  }

  public void fail_mismatchedGetterAndSetterTypes() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  int get g { return 0; }",
        "  set g(String v) {}",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.MISMATCHED_GETTER_AND_SETTER_TYPES);
    verify(source);
  }

  public void fail_newWithNonType() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "var A = 0;",
        "void f() {",
        "  A a = new A();",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.NEW_WITH_NON_TYPE);
    verify(source);
  }

  public void fail_newWithUndefinedConstructor() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  A(int p) {}",
        "}",
        "A f() {",
        "  return new A();",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.NEW_WITH_UNDEFINED_CONSTRUCTOR);
    verify(source);
  }

  public void fail_nonAbstractClassInheritsAbstractMember() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class I {",
        "  m(p) {}",
        "}",
        "class C implements I {",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_MEMBER);
    verify(source);
  }

  public void fail_nonAbstractClassInheritsAbstractMethod() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "abstract class A {",
        "  m(p);",
        "}",
        "class C extends A {",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.NON_ABSTRACT_CLASS_INHERITS_ABSTRACT_METHOD);
    verify(source);
  }

  public void fail_nonType() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "var A = 0;",
        "f(var p) {",
        "  if (p is A) {",
        "  }",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.NON_TYPE);
    verify(source);
  }

  public void fail_nonTypeInCatchClause() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "var T = 0;",
        "f(var p) {",
        "  try {",
        "  } on T catch e {",
        "  }",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.NON_TYPE_IN_CATCH_CLAUSE);
    verify(source);
  }

  public void fail_nonVoidReturnForOperator() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  int operator []=() {}",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.NON_VOID_RETURN_FOR_OPERATOR);
    verify(source);
  }

  public void fail_nonVoidReturnForSetter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "int set x(int v) {",
        "  var s = x;",
        "  x = v;",
        "  return s;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.NON_VOID_RETURN_FOR_SETTER);
    verify(source);
  }

  public void fail_overrideNotSubtype() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  int m() {}",
        "}",
        "class B extends A {",
        "  String m() {}",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.OVERRIDE_NOT_SUBTYPE);
    verify(source);
  }

  public void fail_overrideWithDifferentDefault() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class A {",
        "  m([int p = 0]) {}",
        "}",
        "class B extends A {",
        "  m([int p = 1]) {}",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.OVERRIDE_WITH_DIFFERENT_DEFAULT);
    verify(source);
  }

  public void fail_redirectToInvalidReturnType() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(StaticWarningCode.REDIRECT_TO_INVALID_RETURN_TYPE);
    verify(source);
  }

  public void fail_redirectToMissingConstructor() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(StaticWarningCode.REDIRECT_TO_MISSING_CONSTRUCTOR);
    verify(source);
  }

  public void fail_redirectToNonClass() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(StaticWarningCode.REDIRECT_TO_NON_CLASS);
    verify(source);
  }

  public void fail_returnWithoutValue() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "int f() { return; }"));
    resolve(source);
    assertErrors(StaticWarningCode.RETURN_WITHOUT_VALUE);
    verify(source);
  }

  public void fail_switchExpressionNotAssignable() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f(int p) {",
        "  switch (p) {",
        "    case 'a': break;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.SWITCH_EXPRESSION_NOT_ASSIGNABLE);
    verify(source);
  }

  public void fail_undefinedGetter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        // TODO
        ));
    resolve(source);
    assertErrors(StaticWarningCode.UNDEFINED_GETTER);
    verify(source);
  }

  public void fail_undefinedIdentifier_function() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "int a() -> b;"));
    resolve(source);
    assertErrors(StaticWarningCode.UNDEFINED_IDENTIFIER);
    verify(source);
  }

  public void fail_undefinedSetter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class C {}",
        "f(var p) {",
        "  C.m = 0;",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.UNDEFINED_SETTER);
    verify(source);
  }

  public void fail_undefinedStaticMethodOrGetter_getter() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class C {}",
        "f(var p) {",
        "  f(C.m);",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.UNDEFINED_STATIC_METHOD_OR_GETTER);
    verify(source);
  }

  public void fail_undefinedStaticMethodOrGetter_method() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "class C {}",
        "f(var p) {",
        "  f(C.m());",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.UNDEFINED_STATIC_METHOD_OR_GETTER);
    verify(source);
  }

  public void test_constWithAbstractClass() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "abstract class A {",
        "  const A() {}",
        "}",
        "void f() {",
        "  A a = const A();",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.CONST_WITH_ABSTRACT_CLASS);
    verify(source);
  }

  public void test_equalKeysInMap() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "var m = {'a' : 0, 'b' : 1, 'a' : 2};"));
    resolve(source);
    assertErrors(StaticWarningCode.EQUAL_KEYS_IN_MAP);
    verify(source);
  }

  public void test_newWithAbstractClass() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "abstract class A {}",
        "void f() {",
        "  A a = new A();",
        "}"));
    resolve(source);
    assertErrors(StaticWarningCode.NEW_WITH_ABSTRACT_CLASS);
    verify(source);
  }

  public void test_partOfDifferentLibrary() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "library lib;",
        "part 'part.dart';"));
    addSource("/part.dart", createSource(//
        "part of lub;"));
    resolve(source);
    assertErrors(StaticWarningCode.PART_OF_DIFFERENT_LIBRARY);
    verify(source);
  }

  public void test_undefinedClass_instanceCreation() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() { new C(); }"));
    resolve(source);
    assertErrors(StaticWarningCode.UNDEFINED_CLASS);
  }

  public void test_undefinedClass_variableDeclaration() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() { C c; }"));
    resolve(source);
    assertErrors(StaticWarningCode.UNDEFINED_CLASS);
  }

  public void test_undefinedIdentifier_initializer() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "var a = b;"));
    resolve(source);
    assertErrors(StaticWarningCode.UNDEFINED_IDENTIFIER);
  }

  public void test_undefinedIdentifier_methodInvocation() throws Exception {
    Source source = addSource("/test.dart", createSource(//
        "f() { C.m(); }"));
    resolve(source);
    assertErrors(StaticWarningCode.UNDEFINED_IDENTIFIER);
  }
}
