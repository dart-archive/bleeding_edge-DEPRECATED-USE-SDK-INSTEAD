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

import com.google.dart.engine.error.HintCode;
import com.google.dart.engine.error.StaticTypeWarningCode;
import com.google.dart.engine.error.StaticWarningCode;
import com.google.dart.engine.source.Source;

public class HintCodeTest extends ResolverTestCase {

  public void fail_isInt() throws Exception {
    Source source = addSource(createSource(//
    "var v = 1 is int;"));
    resolve(source);
    assertErrors(source, HintCode.IS_INT);
    verify(source);
  }

  public void fail_isNotInt() throws Exception {
    Source source = addSource(createSource(//
    "var v = 1 is! int;"));
    resolve(source);
    assertErrors(source, HintCode.IS_NOT_INT);
    verify(source);
  }

  public void fail_overrideEqualsButNotHashCode() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  bool operator ==(x) {}",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.OVERRIDE_EQUALS_BUT_NOT_HASH_CODE);
    verify(source);
  }

  public void test_deadCode_deadBlock_conditionalElse() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  true ? 1 : 2;",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEAD_CODE);
    verify(source);
  }

  public void test_deadCode_deadBlock_conditionalElse_nested() throws Exception {
    // test that a dead else-statement can't generate additional violations
    Source source = addSource(createSource(//
        "f() {",
        "  true ? true : false && false;",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEAD_CODE);
    verify(source);
  }

  public void test_deadCode_deadBlock_conditionalIf() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  false ? 1 : 2;",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEAD_CODE);
    verify(source);
  }

  public void test_deadCode_deadBlock_conditionalIf_nested() throws Exception {
    // test that a dead then-statement can't generate additional violations
    Source source = addSource(createSource(//
        "f() {",
        "  false ? false && false : true;",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEAD_CODE);
    verify(source);
  }

  public void test_deadCode_deadBlock_else() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  if(true) {} else {}",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEAD_CODE);
    verify(source);
  }

  public void test_deadCode_deadBlock_else_nested() throws Exception {
    // test that a dead else-statement can't generate additional violations
    Source source = addSource(createSource(//
        "f() {",
        "  if(true) {} else {if (false) {}}",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEAD_CODE);
    verify(source);
  }

  public void test_deadCode_deadBlock_if() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  if(false) {}",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEAD_CODE);
    verify(source);
  }

  public void test_deadCode_deadBlock_if_nested() throws Exception {
    // test that a dead then-statement can't generate additional violations
    Source source = addSource(createSource(//
        "f() {",
        "  if(false) {if(false) {}}",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEAD_CODE);
    verify(source);
  }

  public void test_deadCode_deadBlock_while() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  while(false) {}",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEAD_CODE);
    verify(source);
  }

  public void test_deadCode_deadBlock_while_nested() throws Exception {
    // test that a dead while body can't generate additional violations
    Source source = addSource(createSource(//
        "f() {",
        "  while(false) {if(false) {}}",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEAD_CODE);
    verify(source);
  }

  public void test_deadCode_deadCatch_catchFollowingCatch() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "f() {",
        "  try {} catch (e) {} catch (e) {}",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEAD_CODE_CATCH_FOLLOWING_CATCH);
    verify(source);
  }

  public void test_deadCode_deadCatch_catchFollowingCatch_nested() throws Exception {
    // test that a dead catch clause can't generate additional violations
    Source source = addSource(createSource(//
        "class A {}",
        "f() {",
        "  try {} catch (e) {} catch (e) {if(false) {}}",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEAD_CODE_CATCH_FOLLOWING_CATCH);
    verify(source);
  }

  public void test_deadCode_deadCatch_catchFollowingCatch_object() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  try {} on Object catch (e) {} catch (e) {}",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEAD_CODE_CATCH_FOLLOWING_CATCH);
    verify(source);
  }

  public void test_deadCode_deadCatch_catchFollowingCatch_object_nested() throws Exception {
    // test that a dead catch clause can't generate additional violations
    Source source = addSource(createSource(//
        "f() {",
        "  try {} on Object catch (e) {} catch (e) {if(false) {}}",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEAD_CODE_CATCH_FOLLOWING_CATCH);
    verify(source);
  }

  public void test_deadCode_deadCatch_onCatchSubtype() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B extends A {}",
        "f() {",
        "  try {} on A catch (e) {} on B catch (e) {}",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEAD_CODE_ON_CATCH_SUBTYPE);
    verify(source);
  }

  public void test_deadCode_deadCatch_onCatchSubtype_nested() throws Exception {
    // test that a dead catch clause can't generate additional violations
    Source source = addSource(createSource(//
        "class A {}",
        "class B extends A {}",
        "f() {",
        "  try {} on A catch (e) {} on B catch (e) {if(false) {}}",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEAD_CODE_ON_CATCH_SUBTYPE);
    verify(source);
  }

  public void test_deadCode_deadOperandLHS_and() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  bool b = false && false;",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEAD_CODE);
    verify(source);
  }

  public void test_deadCode_deadOperandLHS_and_nested() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  bool b = false && (false && false);",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEAD_CODE);
    verify(source);
  }

  public void test_deadCode_deadOperandLHS_or() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  bool b = true || true;",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEAD_CODE);
    verify(source);
  }

  public void test_deadCode_deadOperandLHS_or_nested() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  bool b = true || (false && false);",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEAD_CODE);
    verify(source);
  }

  public void test_deadCode_statementAfterReturn_function() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  var one = 1;",
        "  return;",
        "  var two = 2;",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEAD_CODE);
    verify(source);
  }

  public void test_deadCode_statementAfterReturn_ifStatement() throws Exception {
    Source source = addSource(createSource(//
        "f(bool b) {",
        "  if(b) {",
        "    var one = 1;",
        "    return;",
        "    var two = 2;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEAD_CODE);
    verify(source);
  }

  public void test_deadCode_statementAfterReturn_method() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  m() {",
        "    var one = 1;",
        "    return;",
        "    var two = 2;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEAD_CODE);
    verify(source);
  }

  public void test_deadCode_statementAfterReturn_nested() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  var one = 1;",
        "  return;",
        "  if(false) {}",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEAD_CODE);
    verify(source);
  }

  public void test_deadCode_statementAfterReturn_twoReturns() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  var one = 1;",
        "  return;",
        "  var two = 2;",
        "  return;",
        "  var three = 3;",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEAD_CODE);
    verify(source);
  }

  public void test_deprecatedAnnotationUse_assignment() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  @deprecated",
        "  A operator+(A a) {}",
        "}",
        "f(A a) {",
        "  A b;",
        "  a += b;",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEPRECATED_MEMBER_USE);
    verify(source);
  }

  public void test_deprecatedAnnotationUse_deprecated() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  @deprecated",
        "  m() {}",
        "  n() {m();}",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEPRECATED_MEMBER_USE);
    verify(source);
  }

  public void test_deprecatedAnnotationUse_Deprecated() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  @Deprecated('0.9')",
        "  m() {}",
        "  n() {m();}",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEPRECATED_MEMBER_USE);
    verify(source);
  }

  public void test_deprecatedAnnotationUse_export() throws Exception {
    Source source = addSource(createSource(//
    "export 'deprecated_library.dart';"));
    addSource("/deprecated_library.dart", createSource(//
        "@deprecated",
        "library deprecated_library;",
        "class A {}"));
    resolve(source);
    assertErrors(source, HintCode.DEPRECATED_MEMBER_USE);
    verify(source);
  }

  public void test_deprecatedAnnotationUse_getter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  @deprecated",
        "  get m => 1;",
        "}",
        "f(A a) {",
        "  return a.m;",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEPRECATED_MEMBER_USE);
    verify(source);
  }

  public void test_deprecatedAnnotationUse_import() throws Exception {
    Source source = addSource(createSource(//
        "import 'deprecated_library.dart';",
        "f(A a) {}"));
    addSource("/deprecated_library.dart", createSource(//
        "@deprecated",
        "library deprecated_library;",
        "class A {}"));
    resolve(source);
    assertErrors(source, HintCode.DEPRECATED_MEMBER_USE);
    verify(source);
  }

  public void test_deprecatedAnnotationUse_indexExpression() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  @deprecated",
        "  operator[](int i) {}",
        "}",
        "f(A a) {",
        "  return a[1];",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEPRECATED_MEMBER_USE);
    verify(source);
  }

  public void test_deprecatedAnnotationUse_instanceCreation() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  @deprecated",
        "  A(int i) {}",
        "}",
        "f() {",
        "  A a = new A(1);",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEPRECATED_MEMBER_USE);
    verify(source);
  }

  public void test_deprecatedAnnotationUse_instanceCreation_namedConstructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  @deprecated",
        "  A.named(int i) {}",
        "}",
        "f() {",
        "  A a = new A.named(1);",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEPRECATED_MEMBER_USE);
    verify(source);
  }

  public void test_deprecatedAnnotationUse_operator() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  @deprecated",
        "  operator+(A a) {}",
        "}",
        "f(A a) {",
        "  A b;",
        "  return a + b;",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEPRECATED_MEMBER_USE);
    verify(source);
  }

  public void test_deprecatedAnnotationUse_setter() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  @deprecated",
        "  set s(v) {}",
        "}",
        "f(A a) {",
        "  return a.s = 1;",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEPRECATED_MEMBER_USE);
    verify(source);
  }

  public void test_deprecatedAnnotationUse_superConstructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  @deprecated",
        "  A() {}",
        "}",
        "class B extends A {",
        "  B() : super() {}",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEPRECATED_MEMBER_USE);
    verify(source);
  }

  public void test_deprecatedAnnotationUse_superConstructor_namedConstructor() throws Exception {
    Source source = addSource(createSource(//
        "class A {",
        "  @deprecated",
        "  A.named() {}",
        "}",
        "class B extends A {",
        "  B() : super.named() {}",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DEPRECATED_MEMBER_USE);
    verify(source);
  }

  public void test_divisionOptimization_double() throws Exception {
    Source source = addSource(createSource(//
        "f(double x, double y) {",
        "  var v = (x / y).toInt();",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DIVISION_OPTIMIZATION);
    verify(source);
  }

  public void test_divisionOptimization_int() throws Exception {
    Source source = addSource(createSource(//
        "f(int x, int y) {",
        "  var v = (x / y).toInt();",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DIVISION_OPTIMIZATION);
    verify(source);
  }

  public void test_divisionOptimization_propagatedType() throws Exception {
    // Tests the propagated type information of the '/' method
    Source source = addSource(createSource(//
        "f(x, y) {",
        "  x = 1;",
        "  y = 1;",
        "  var v = (x / y).toInt();",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DIVISION_OPTIMIZATION);
    verify(source);
  }

  public void test_divisionOptimization_wrappedBinaryExpression() throws Exception {
    Source source = addSource(createSource(//
        "f(int x, int y) {",
        "  var v = (((x / y))).toInt();",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.DIVISION_OPTIMIZATION);
    verify(source);
  }

  public void test_duplicateImport() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "import 'lib1.dart';",
        "import 'lib1.dart';", // duplicate
        "A a;"));
    addSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {}"));
    resolve(source);
    assertErrors(source, HintCode.DUPLICATE_IMPORT);
    verify(source);
  }

  public void test_duplicateImport2() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "import 'lib1.dart';",
        "import 'lib1.dart';", // duplicate
        "import 'lib1.dart';", // duplicate
        "A a;"));
    addSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {}"));
    resolve(source);
    assertErrors(source, HintCode.DUPLICATE_IMPORT, HintCode.DUPLICATE_IMPORT);
    verify(source);
  }

  public void test_duplicateImport3() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "import 'lib1.dart' as M show A hide B;",
        "import 'lib1.dart' as M show A hide B;", // duplicate
        "M.A a;"));
    addSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {}",
        "class B {}"));
    resolve(source);
    assertErrors(source, HintCode.DUPLICATE_IMPORT, HintCode.UNUSED_IMPORT);
    verify(source);
  }

  public void test_isDouble() throws Exception {
    Source source = addSource(createSource(//
    "var v = 1 is double;"));
    resolve(source);
    assertErrors(source, HintCode.IS_DOUBLE);
    verify(source);
  }

  public void test_isNotDouble() throws Exception {
    Source source = addSource(createSource(//
    "var v = 1 is! double;"));
    resolve(source);
    assertErrors(source, HintCode.IS_NOT_DOUBLE);
    verify(source);
  }

  public void test_overriddingPrivateMember_getter() throws Exception {
    Source source = addSource(createSource(//
        "import 'lib1.dart';",
        "class B extends A {",
        "  get _g => 0;",
        "}"));
    Source source2 = addSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {",
        "  get _g => 0;",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.OVERRIDDING_PRIVATE_MEMBER);
    verify(source, source2);
  }

  public void test_overriddingPrivateMember_method() throws Exception {
    Source source = addSource(createSource(//
        "import 'lib1.dart';",
        "class B extends A {",
        "  _m(int x) => 0;",
        "}"));
    Source source2 = addSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {",
        "  _m(int x) => 0;",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.OVERRIDDING_PRIVATE_MEMBER);
    verify(source, source2);
  }

  public void test_overriddingPrivateMember_method2() throws Exception {
    Source source = addSource(createSource(//
        "import 'lib1.dart';",
        "class B extends A {}",
        "class C extends B {",
        "  _m(int x) => 0;",
        "}"));
    Source source2 = addSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {",
        "  _m(int x) => 0;",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.OVERRIDDING_PRIVATE_MEMBER);
    verify(source, source2);
  }

  public void test_overriddingPrivateMember_setter() throws Exception {
    Source source = addSource(createSource(//
        "import 'lib1.dart';",
        "class B extends A {",
        "  set _s(int x) {}",
        "}"));
    Source source2 = addSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {",
        "  set _s(int x) {}",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.OVERRIDDING_PRIVATE_MEMBER);
    verify(source, source2);
  }

  public void test_typeCheck_type_is_Null() throws Exception {
    Source source = addSource(createSource(//
        "m(i) {",
        "  bool b = i is Null;",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.TYPE_CHECK_IS_NULL);
    verify(source);
  }

  public void test_typeCheck_type_not_Null() throws Exception {
    Source source = addSource(createSource(//
        "m(i) {",
        "  bool b = i is! Null;",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.TYPE_CHECK_IS_NOT_NULL);
    verify(source);
  }

  public void test_undefinedGetter() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "f(var a) {",
        "  if(a is A) {",
        "    return a.m;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.UNDEFINED_GETTER);
  }

  // The implementation of HintCode.UNDEFINED_SETTER assumes that UNDEFINED_SETTER in
  // StaticTypeWarningCode and StaticWarningCode are the same, this verifies that assumption.
  public void test_undefinedGetter_message() throws Exception {
    assertEquals(
        StaticTypeWarningCode.UNDEFINED_GETTER.getMessage(),
        StaticWarningCode.UNDEFINED_GETTER.getMessage());
  }

  public void test_undefinedMethod() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  var a = 'str';",
        "  a.notAMethodOnString();",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.UNDEFINED_METHOD);
  }

  public void test_undefinedMethod_assignmentExpression() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "class B {",
        "  f(var a, var a2) {",
        "    a = new A();",
        "    a2 = new A();",
        "    a += a2;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.UNDEFINED_METHOD);
  }

  public void test_undefinedOperator_binaryExpression() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "f(var a) {",
        "  if(a is A) {",
        "    a + 1;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.UNDEFINED_OPERATOR);
  }

  public void test_undefinedOperator_indexBoth() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "f(var a) {",
        "  if(a is A) {",
        "    a[0]++;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.UNDEFINED_OPERATOR);
  }

  public void test_undefinedOperator_indexGetter() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "f(var a) {",
        "  if(a is A) {",
        "    a[0];",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.UNDEFINED_OPERATOR);
  }

  public void test_undefinedOperator_indexSetter() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "f(var a) {",
        "  if(a is A) {",
        "    a[0] = 1;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.UNDEFINED_OPERATOR);
  }

  public void test_undefinedOperator_postfixExpression() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "f(var a) {",
        "  if(a is A) {",
        "    a++;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.UNDEFINED_OPERATOR);
  }

  public void test_undefinedOperator_prefixExpression() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "f(var a) {",
        "  if(a is A) {",
        "    ++a;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.UNDEFINED_OPERATOR);
  }

  public void test_undefinedSetter() throws Exception {
    Source source = addSource(createSource(//
        "class A {}",
        "f(var a) {",
        "  if(a is A) {",
        "    a.m = 0;",
        "  }",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.UNDEFINED_SETTER);
  }

  // The implementation of HintCode.UNDEFINED_SETTER assumes that UNDEFINED_SETTER in
  // StaticTypeWarningCode and StaticWarningCode are the same, this verifies that assumption.
  public void test_undefinedSetter_message() throws Exception {
    assertEquals(
        StaticTypeWarningCode.UNDEFINED_SETTER.getMessage(),
        StaticWarningCode.UNDEFINED_SETTER.getMessage());
  }

  public void test_unnecessaryCast_type_supertype() throws Exception {
    Source source = addSource(createSource(//
        "m(int i) {",
        "  var b = i as Object;",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.UNNECESSARY_CAST);
    verify(source);
  }

  public void test_unnecessaryCast_type_type() throws Exception {
    Source source = addSource(createSource(//
        "m(num i) {",
        "  var b = i as num;",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.UNNECESSARY_CAST);
    verify(source);
  }

  public void test_unnecessaryTypeCheck_null_is_Null() throws Exception {
    Source source = addSource(createSource(//
    "bool b = null is Null;"));
    resolve(source);
    assertErrors(source, HintCode.UNNECESSARY_TYPE_CHECK_TRUE);
    verify(source);
  }

  public void test_unnecessaryTypeCheck_null_not_Null() throws Exception {
    Source source = addSource(createSource(//
    "bool b = null is! Null;"));
    resolve(source);
    assertErrors(source, HintCode.UNNECESSARY_TYPE_CHECK_FALSE);
    verify(source);
  }

  public void test_unnecessaryTypeCheck_type_is_dynamic() throws Exception {
    Source source = addSource(createSource(//
        "m(i) {",
        "  bool b = i is dynamic;",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.UNNECESSARY_TYPE_CHECK_TRUE);
    verify(source);
  }

  public void test_unnecessaryTypeCheck_type_is_object() throws Exception {
    Source source = addSource(createSource(//
        "m(i) {",
        "  bool b = i is Object;",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.UNNECESSARY_TYPE_CHECK_TRUE);
    verify(source);
  }

  public void test_unnecessaryTypeCheck_type_not_dynamic() throws Exception {
    Source source = addSource(createSource(//
        "m(i) {",
        "  bool b = i is! dynamic;",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.UNNECESSARY_TYPE_CHECK_FALSE);
    verify(source);
  }

  public void test_unnecessaryTypeCheck_type_not_object() throws Exception {
    Source source = addSource(createSource(//
        "m(i) {",
        "  bool b = i is! Object;",
        "}"));
    resolve(source);
    assertErrors(source, HintCode.UNNECESSARY_TYPE_CHECK_FALSE);
    verify(source);
  }

  public void test_unusedImport() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "import 'lib1.dart';"));
    Source source2 = addSource("/lib1.dart", createSource(//
        "library lib1;"));
    resolve(source);
    assertErrors(source, HintCode.UNUSED_IMPORT);
    assertNoErrors(source2);
    verify(source, source2);
  }

  public void test_unusedImport_as() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "import 'lib1.dart';", // unused
        "import 'lib1.dart' as one;",
        "one.A a;"));
    Source source2 = addSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {}"));
    resolve(source);
    assertErrors(source, HintCode.UNUSED_IMPORT);
    assertNoErrors(source2);
    verify(source, source2);
  }

  public void test_unusedImport_hide() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "import 'lib1.dart';",
        "import 'lib1.dart' hide A;", // unused
        "A a;"));
    Source source2 = addSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {}"));
    resolve(source);
    assertErrors(source, HintCode.UNUSED_IMPORT);
    assertNoErrors(source2);
    verify(source, source2);
  }

  public void test_unusedImport_show() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "import 'lib1.dart' show A;",
        "import 'lib1.dart' show B;", // unused
        "A a;"));
    Source source2 = addSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {}",
        "class B {}"));
    resolve(source);
    assertErrors(source, HintCode.UNUSED_IMPORT);
    assertNoErrors(source2);
    verify(source, source2);
  }

}
