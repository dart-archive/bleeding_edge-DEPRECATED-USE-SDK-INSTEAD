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

import com.google.dart.engine.error.AuditCode;
import com.google.dart.engine.source.Source;

public class AuditCodeTest extends ResolverTestCase {

  public void test_deadCode_deadBlock_conditionalElse() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  true ? 1 : 2;",
        "}"));
    resolve(source);
    assertErrors(AuditCode.DEAD_CODE);
    verify(source);
  }

  public void test_deadCode_deadBlock_conditionalIf() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  false ? 1 : 2;",
        "}"));
    resolve(source);
    assertErrors(AuditCode.DEAD_CODE);
    verify(source);
  }

  public void test_deadCode_deadBlock_else() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  if(true) {",
        "  } else {",
        "  }",
        "}"));
    resolve(source);
    assertErrors(AuditCode.DEAD_CODE);
    verify(source);
  }

  public void test_deadCode_deadBlock_if() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  if(false) {}",
        "}"));
    resolve(source);
    assertErrors(AuditCode.DEAD_CODE);
    verify(source);
  }

  public void test_deadCode_deadBlock_while() throws Exception {
    Source source = addSource(createSource(//
        "f() {",
        "  while(false) {}",
        "}"));
    resolve(source);
    assertErrors(AuditCode.DEAD_CODE);
    verify(source);
  }

  public void test_deadCode_deadOperandLHS_and() throws Exception {
    Source source = addSource(createSource(//
        "f(bool b) {",
        "  bool c = false && b;",
        "}"));
    resolve(source);
    assertErrors(AuditCode.DEAD_CODE);
    verify(source);
  }

  public void test_deadCode_deadOperandLHS_or() throws Exception {
    Source source = addSource(createSource(//
        "f(bool b) {",
        "  bool c = true || b;",
        "}"));
    resolve(source);
    assertErrors(AuditCode.DEAD_CODE);
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
    assertErrors(AuditCode.DEAD_CODE);
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
    assertErrors(AuditCode.DEAD_CODE);
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
    assertErrors(AuditCode.DEAD_CODE);
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
    assertErrors(AuditCode.DEAD_CODE);
    verify(source);
  }
}
