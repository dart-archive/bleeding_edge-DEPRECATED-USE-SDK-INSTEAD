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
package com.google.dart.engine.internal.constant;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.Expression;

import static com.google.dart.engine.ast.ASTFactory.booleanLiteral;
import static com.google.dart.engine.ast.ASTFactory.conditionalExpression;
import static com.google.dart.engine.ast.ASTFactory.identifier;
import static com.google.dart.engine.ast.ASTFactory.integer;

import java.math.BigInteger;

public class ConstantVisitorTest extends EngineTestCase {
  public void test_visitConditionalExpression_false() {
    Expression thenExpression = integer(1L);
    Expression elseExpression = integer(0L);
    ConditionalExpression expression = conditionalExpression(
        booleanLiteral(false),
        thenExpression,
        elseExpression);
    EvaluationResultImpl result = expression.accept(new ConstantVisitor());
    assertInstanceOf(ValidResult.class, result);
    Object value = ((ValidResult) result).getValue();
    assertInstanceOf(BigInteger.class, value);
    assertEquals(0L, ((BigInteger) value).longValue());
  }

  public void test_visitConditionalExpression_nonBooleanCondition() {
    Expression thenExpression = integer(1L);
    Expression elseExpression = integer(0L);
    ConditionalExpression expression = conditionalExpression(
        integer(0L),
        thenExpression,
        elseExpression);
    EvaluationResultImpl result = expression.accept(new ConstantVisitor());
    assertInstanceOf(ErrorResult.class, result);
  }

  public void test_visitConditionalExpression_nonConstantElse() {
    Expression thenExpression = integer(1L);
    Expression elseExpression = identifier("x");
    ConditionalExpression expression = conditionalExpression(
        booleanLiteral(true),
        thenExpression,
        elseExpression);
    EvaluationResultImpl result = expression.accept(new ConstantVisitor());
    assertInstanceOf(ErrorResult.class, result);
  }

  public void test_visitConditionalExpression_nonConstantThen() {
    Expression thenExpression = identifier("x");
    Expression elseExpression = integer(0L);
    ConditionalExpression expression = conditionalExpression(
        booleanLiteral(true),
        thenExpression,
        elseExpression);
    EvaluationResultImpl result = expression.accept(new ConstantVisitor());
    assertInstanceOf(ErrorResult.class, result);
  }

  public void test_visitConditionalExpression_true() {
    Expression thenExpression = integer(1L);
    Expression elseExpression = integer(0L);
    ConditionalExpression expression = conditionalExpression(
        booleanLiteral(true),
        thenExpression,
        elseExpression);
    EvaluationResultImpl result = expression.accept(new ConstantVisitor());
    assertInstanceOf(ValidResult.class, result);
    Object value = ((ValidResult) result).getValue();
    assertInstanceOf(BigInteger.class, value);
    assertEquals(1L, ((BigInteger) value).longValue());
  }
}
