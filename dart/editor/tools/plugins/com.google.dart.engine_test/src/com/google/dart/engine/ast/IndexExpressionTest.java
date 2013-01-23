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

import static com.google.dart.engine.ast.ASTFactory.assignmentExpression;
import static com.google.dart.engine.ast.ASTFactory.binaryExpression;
import static com.google.dart.engine.ast.ASTFactory.identifier;
import static com.google.dart.engine.ast.ASTFactory.indexExpression;
import static com.google.dart.engine.ast.ASTFactory.postfixExpression;
import static com.google.dart.engine.ast.ASTFactory.prefixExpression;

public class IndexExpressionTest extends EngineTestCase {
  public void test_inGetterContext_assignment_compound_left() throws Exception {
    IndexExpression expression = indexExpression(identifier("a"), identifier("b"));
    // a[i] += ?
    assignmentExpression(expression, TokenType.PLUS_EQ, null);
    assertTrue(expression.inGetterContext());
  }

  public void test_inGetterContext_assignment_simple_left() throws Exception {
    IndexExpression expression = indexExpression(identifier("a"), identifier("b"));
    // a[i] = ?
    assignmentExpression(expression, TokenType.EQ, null);
    assertFalse(expression.inGetterContext());
  }

  public void test_inGetterContext_nonAssignment() throws Exception {
    IndexExpression expression = indexExpression(identifier("a"), identifier("b"));
    // a[i] + ?
    binaryExpression(expression, TokenType.PLUS, null);
    assertTrue(expression.inGetterContext());
  }

  public void test_inSetterContext_assignment_compound_left() throws Exception {
    IndexExpression expression = indexExpression(identifier("a"), identifier("b"));
    // a[i] += ?
    assignmentExpression(expression, TokenType.PLUS_EQ, null);
    assertTrue(expression.inSetterContext());
  }

  public void test_inSetterContext_assignment_compound_right() throws Exception {
    IndexExpression expression = indexExpression(identifier("a"), identifier("b"));
    // ? += a[i]
    assignmentExpression(null, TokenType.PLUS_EQ, expression);
    assertFalse(expression.inSetterContext());
  }

  public void test_inSetterContext_assignment_simple_left() throws Exception {
    IndexExpression expression = indexExpression(identifier("a"), identifier("b"));
    // a[i] = ?
    assignmentExpression(expression, TokenType.EQ, null);
    assertTrue(expression.inSetterContext());
  }

  public void test_inSetterContext_assignment_simple_right() throws Exception {
    IndexExpression expression = indexExpression(identifier("a"), identifier("b"));
    // ? = a[i]
    assignmentExpression(null, TokenType.EQ, expression);
    assertFalse(expression.inSetterContext());
  }

  public void test_inSetterContext_nonAssignment() throws Exception {
    IndexExpression expression = indexExpression(identifier("a"), identifier("b"));
    binaryExpression(expression, TokenType.PLUS, null);
    // a[i] + ?
    assertFalse(expression.inSetterContext());
  }

  public void test_inSetterContext_postfix() throws Exception {
    IndexExpression expression = indexExpression(identifier("a"), identifier("b"));
    postfixExpression(expression, TokenType.PLUS_PLUS);
    // a[i]++
    assertTrue(expression.inSetterContext());
  }

  public void test_inSetterContext_prefix_bang() throws Exception {
    IndexExpression expression = indexExpression(identifier("a"), identifier("b"));
    // !a[i]
    prefixExpression(TokenType.BANG, expression);
    assertFalse(expression.inSetterContext());
  }

  public void test_inSetterContext_prefix_minusMinus() throws Exception {
    IndexExpression expression = indexExpression(identifier("a"), identifier("b"));
    // --a[i]
    prefixExpression(TokenType.MINUS_MINUS, expression);
    assertTrue(expression.inSetterContext());
  }

  public void test_inSetterContext_prefix_plusPlus() throws Exception {
    IndexExpression expression = indexExpression(identifier("a"), identifier("b"));
    // ++a[i]
    prefixExpression(TokenType.PLUS_PLUS, expression);
    assertTrue(expression.inSetterContext());
  }
}
