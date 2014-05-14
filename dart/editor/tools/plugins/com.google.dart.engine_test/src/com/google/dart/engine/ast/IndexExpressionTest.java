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
package com.google.dart.engine.ast;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.scanner.TokenType;

import static com.google.dart.engine.ast.AstFactory.assignmentExpression;
import static com.google.dart.engine.ast.AstFactory.binaryExpression;
import static com.google.dart.engine.ast.AstFactory.identifier;
import static com.google.dart.engine.ast.AstFactory.indexExpression;
import static com.google.dart.engine.ast.AstFactory.postfixExpression;
import static com.google.dart.engine.ast.AstFactory.prefixExpression;

public class IndexExpressionTest extends EngineTestCase {
  public void test_inGetterContext_assignment_compound_left() throws Exception {
    IndexExpression expression = indexExpression(identifier("a"), identifier("b"));
    // a[b] += c
    assignmentExpression(expression, TokenType.PLUS_EQ, identifier("c"));
    assertTrue(expression.inGetterContext());
  }

  public void test_inGetterContext_assignment_simple_left() throws Exception {
    IndexExpression expression = indexExpression(identifier("a"), identifier("b"));
    // a[b] = c
    assignmentExpression(expression, TokenType.EQ, identifier("c"));
    assertFalse(expression.inGetterContext());
  }

  public void test_inGetterContext_nonAssignment() throws Exception {
    IndexExpression expression = indexExpression(identifier("a"), identifier("b"));
    // a[b] + c
    binaryExpression(expression, TokenType.PLUS, identifier("c"));
    assertTrue(expression.inGetterContext());
  }

  public void test_inSetterContext_assignment_compound_left() throws Exception {
    IndexExpression expression = indexExpression(identifier("a"), identifier("b"));
    // a[b] += c
    assignmentExpression(expression, TokenType.PLUS_EQ, identifier("c"));
    assertTrue(expression.inSetterContext());
  }

  public void test_inSetterContext_assignment_compound_right() throws Exception {
    IndexExpression expression = indexExpression(identifier("a"), identifier("b"));
    // c += a[b]
    assignmentExpression(identifier("c"), TokenType.PLUS_EQ, expression);
    assertFalse(expression.inSetterContext());
  }

  public void test_inSetterContext_assignment_simple_left() throws Exception {
    IndexExpression expression = indexExpression(identifier("a"), identifier("b"));
    // a[b] = c
    assignmentExpression(expression, TokenType.EQ, identifier("c"));
    assertTrue(expression.inSetterContext());
  }

  public void test_inSetterContext_assignment_simple_right() throws Exception {
    IndexExpression expression = indexExpression(identifier("a"), identifier("b"));
    // c = a[b]
    assignmentExpression(identifier("c"), TokenType.EQ, expression);
    assertFalse(expression.inSetterContext());
  }

  public void test_inSetterContext_nonAssignment() throws Exception {
    IndexExpression expression = indexExpression(identifier("a"), identifier("b"));
    binaryExpression(expression, TokenType.PLUS, identifier("c"));
    // a[b] + cc
    assertFalse(expression.inSetterContext());
  }

  public void test_inSetterContext_postfix() throws Exception {
    IndexExpression expression = indexExpression(identifier("a"), identifier("b"));
    postfixExpression(expression, TokenType.PLUS_PLUS);
    // a[b]++
    assertTrue(expression.inSetterContext());
  }

  public void test_inSetterContext_prefix_bang() throws Exception {
    IndexExpression expression = indexExpression(identifier("a"), identifier("b"));
    // !a[b]
    prefixExpression(TokenType.BANG, expression);
    assertFalse(expression.inSetterContext());
  }

  public void test_inSetterContext_prefix_minusMinus() throws Exception {
    IndexExpression expression = indexExpression(identifier("a"), identifier("b"));
    // --a[b]
    prefixExpression(TokenType.MINUS_MINUS, expression);
    assertTrue(expression.inSetterContext());
  }

  public void test_inSetterContext_prefix_plusPlus() throws Exception {
    IndexExpression expression = indexExpression(identifier("a"), identifier("b"));
    // ++a[b]
    prefixExpression(TokenType.PLUS_PLUS, expression);
    assertTrue(expression.inSetterContext());
  }
}
