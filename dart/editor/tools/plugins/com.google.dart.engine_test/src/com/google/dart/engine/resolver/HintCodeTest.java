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
import com.google.dart.engine.source.Source;

public class HintCodeTest extends ResolverTestCase {
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

  public void test_unusedImport() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "import 'lib1.dart';"));
    addSource("/lib1.dart", createSource(//
        "library lib1;"));
    resolve(source);
    assertErrors(source, HintCode.UNUSED_IMPORT);
    verify(source);
  }

  public void test_unusedImport_as() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "import 'lib1.dart';", // unused
        "import 'lib1.dart' as one;",
        "one.A a;"));
    addSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {}"));
    resolve(source);
    assertErrors(source, HintCode.UNUSED_IMPORT);
    verify(source);
  }

  public void test_unusedImport_hide() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "import 'lib1.dart';",
        "import 'lib1.dart' hide A;", // unused
        "A a;"));
    addSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {}"));
    resolve(source);
    assertErrors(source, HintCode.UNUSED_IMPORT);
    verify(source);
  }

  public void test_unusedImport_show() throws Exception {
    Source source = addSource(createSource(//
        "library L;",
        "import 'lib1.dart' show A;",
        "import 'lib1.dart' show B;", // unused
        "A a;"));
    addSource("/lib1.dart", createSource(//
        "library lib1;",
        "class A {}",
        "class B {}"));
    resolve(source);
    assertErrors(source, HintCode.UNUSED_IMPORT);
    verify(source);
  }
}
