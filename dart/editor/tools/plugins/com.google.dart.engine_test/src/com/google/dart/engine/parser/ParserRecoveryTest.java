/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.engine.parser;

import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.IsExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.scanner.TokenType;

/**
 * The class {@code ParserRecoveryTest} defines parser tests that test the parsing of invalid code
 * sequences to ensure that the correct recovery steps are taken in the parser.
 */
public class ParserRecoveryTest extends ParserTestCase {

  public void fail_relationalExpression_missing_LHS_RHS() throws Exception {
    IsExpression expression = parseExpression("is", ParserErrorCode.EXPECTED_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getExpression());
    assertTrue(expression.getExpression().isSynthetic());
    assertInstanceOf(TypeName.class, expression.getType());
    assertTrue(expression.getType().isSynthetic());
  }

  public void fail_relationalExpression_missing_RHS() throws Exception {
    IsExpression expression = parseExpression("x is", ParserErrorCode.EXPECTED_IDENTIFIER);
    assertInstanceOf(TypeName.class, expression.getType());
    assertTrue(expression.getType().isSynthetic());
  }

  public void test_additiveExpression_missing_LHS() throws Exception {
    BinaryExpression expression = parseExpression("+ y", ParserErrorCode.NO_UNARY_PLUS_OPERATOR);
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
  }

  public void test_additiveExpression_missing_LHS_RHS() throws Exception {
    BinaryExpression expression = parseExpression("+", ParserErrorCode.NO_UNARY_PLUS_OPERATOR);
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_additiveExpression_missing_RHS() throws Exception {
    BinaryExpression expression = parseExpression("x +");
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_additiveExpression_missing_RHS_super() throws Exception {
    BinaryExpression expression = parseExpression("super +");
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_additiveExpression_precedence_multiplicative_left() throws Exception {
    BinaryExpression expression = parseExpression("* +", ParserErrorCode.NO_UNARY_PLUS_OPERATOR);
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_additiveExpression_precedence_multiplicative_right() throws Exception {
    BinaryExpression expression = parseExpression("+ *", ParserErrorCode.NO_UNARY_PLUS_OPERATOR);
    assertInstanceOf(BinaryExpression.class, expression.getRightOperand());
  }

  public void test_additiveExpression_super() throws Exception {
    BinaryExpression expression = parseExpression(
        "super + +",
        ParserErrorCode.NO_UNARY_PLUS_OPERATOR);
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_bitwiseAndExpression_missing_LHS() throws Exception {
    BinaryExpression expression = parseExpression("& y");
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
  }

  public void test_bitwiseAndExpression_missing_LHS_RHS() throws Exception {
    BinaryExpression expression = parseExpression("&");
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_bitwiseAndExpression_missing_RHS() throws Exception {
    BinaryExpression expression = parseExpression("x &");
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_bitwiseAndExpression_missing_RHS_super() throws Exception {
    BinaryExpression expression = parseExpression("super &");
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_bitwiseAndExpression_precedence_equality_left() throws Exception {
    BinaryExpression expression = parseExpression("== &");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_bitwiseAndExpression_precedence_equality_right() throws Exception {
    BinaryExpression expression = parseExpression("& ==");
    assertInstanceOf(BinaryExpression.class, expression.getRightOperand());
  }

  public void test_bitwiseAndExpression_super() throws Exception {
    BinaryExpression expression = parseExpression("super &  &");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_bitwiseOrExpression_missing_LHS() throws Exception {
    BinaryExpression expression = parseExpression("| y");
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
  }

  public void test_bitwiseOrExpression_missing_LHS_RHS() throws Exception {
    BinaryExpression expression = parseExpression("|");
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_bitwiseOrExpression_missing_RHS() throws Exception {
    BinaryExpression expression = parseExpression("x |");
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_bitwiseOrExpression_missing_RHS_super() throws Exception {
    BinaryExpression expression = parseExpression("super |");
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_bitwiseOrExpression_precedence_xor_left() throws Exception {
    BinaryExpression expression = parseExpression("^ |");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_bitwiseOrExpression_precedence_xor_right() throws Exception {
    BinaryExpression expression = parseExpression("| ^");
    assertInstanceOf(BinaryExpression.class, expression.getRightOperand());
  }

  public void test_bitwiseOrExpression_super() throws Exception {
    BinaryExpression expression = parseExpression("super |  |");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_bitwiseXorExpression_missing_LHS() throws Exception {
    BinaryExpression expression = parseExpression("^ y");
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
  }

  public void test_bitwiseXorExpression_missing_LHS_RHS() throws Exception {
    BinaryExpression expression = parseExpression("^");
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_bitwiseXorExpression_missing_RHS() throws Exception {
    BinaryExpression expression = parseExpression("x ^");
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_bitwiseXorExpression_missing_RHS_super() throws Exception {
    BinaryExpression expression = parseExpression("super ^");
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_bitwiseXorExpression_precedence_and_left() throws Exception {
    BinaryExpression expression = parseExpression("& ^");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_bitwiseXorExpression_precedence_and_right() throws Exception {
    BinaryExpression expression = parseExpression("^ &");
    assertInstanceOf(BinaryExpression.class, expression.getRightOperand());
  }

  public void test_bitwiseXorExpression_super() throws Exception {
    BinaryExpression expression = parseExpression("super ^  ^");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_equalityExpression_missing_LHS() throws Exception {
    BinaryExpression expression = parseExpression("== y");
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
  }

  public void test_equalityExpression_missing_LHS_RHS() throws Exception {
    BinaryExpression expression = parseExpression("==");
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_equalityExpression_missing_RHS() throws Exception {
    BinaryExpression expression = parseExpression("x ==");
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_equalityExpression_missing_RHS_super() throws Exception {
    BinaryExpression expression = parseExpression("super ==");
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_equalityExpression_precedence_relational_left() throws Exception {
    BinaryExpression expression = parseExpression("is ==", ParserErrorCode.EXPECTED_IDENTIFIER);
    assertInstanceOf(IsExpression.class, expression.getLeftOperand());
  }

  public void test_equalityExpression_precedence_relational_right() throws Exception {
    BinaryExpression expression = parseExpression("== is", ParserErrorCode.EXPECTED_IDENTIFIER);
    assertInstanceOf(IsExpression.class, expression.getRightOperand());
  }

  public void test_equalityExpression_super() throws Exception {
    BinaryExpression expression = parseExpression("super ==  ==");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_logicalAndExpression_missing_LHS() throws Exception {
    BinaryExpression expression = parseExpression("&& y");
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
  }

  public void test_logicalAndExpression_missing_LHS_RHS() throws Exception {
    BinaryExpression expression = parseExpression("&&");
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_logicalAndExpression_missing_RHS() throws Exception {
    BinaryExpression expression = parseExpression("x &&");
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_logicalAndExpression_precedence_bitwiseOr_left() throws Exception {
    BinaryExpression expression = parseExpression("| &&");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_logicalAndExpression_precedence_bitwiseOr_right() throws Exception {
    BinaryExpression expression = parseExpression("&& |");
    assertInstanceOf(BinaryExpression.class, expression.getRightOperand());
  }

  public void test_logicalOrExpression_missing_LHS() throws Exception {
    BinaryExpression expression = parseExpression("|| y");
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
  }

  public void test_logicalOrExpression_missing_LHS_RHS() throws Exception {
    BinaryExpression expression = parseExpression("||");
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_logicalOrExpression_missing_RHS() throws Exception {
    BinaryExpression expression = parseExpression("x ||");
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_logicalOrExpression_precedence_logicalAnd_left() throws Exception {
    BinaryExpression expression = parseExpression("&& ||");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_logicalOrExpression_precedence_logicalAnd_right() throws Exception {
    BinaryExpression expression = parseExpression("|| &&");
    assertInstanceOf(BinaryExpression.class, expression.getRightOperand());
  }

  public void test_multiplicativeExpression_missing_LHS() throws Exception {
    BinaryExpression expression = parseExpression("* y");
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
  }

  public void test_multiplicativeExpression_missing_LHS_RHS() throws Exception {
    BinaryExpression expression = parseExpression("*");
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_multiplicativeExpression_missing_RHS() throws Exception {
    BinaryExpression expression = parseExpression("x *");
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_multiplicativeExpression_missing_RHS_super() throws Exception {
    BinaryExpression expression = parseExpression("super *");
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_multiplicativeExpression_precedence_unary_left() throws Exception {
    BinaryExpression expression = parseExpression("-x *");
    assertInstanceOf(PrefixExpression.class, expression.getLeftOperand());
  }

  public void test_multiplicativeExpression_precedence_unary_right() throws Exception {
    BinaryExpression expression = parseExpression("* -y");
    assertInstanceOf(PrefixExpression.class, expression.getRightOperand());
  }

  public void test_multiplicativeExpression_super() throws Exception {
    BinaryExpression expression = parseExpression("super ==  ==");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_prefixExpression_missing_operand_minus() throws Exception {
    PrefixExpression expression = parseExpression("-");
    assertInstanceOf(SimpleIdentifier.class, expression.getOperand());
    assertTrue(expression.getOperand().isSynthetic());
    assertEquals(TokenType.MINUS, expression.getOperator().getType());
  }

  public void test_relationalExpression_missing_LHS() throws Exception {
    IsExpression expression = parseExpression("is y");
    assertInstanceOf(SimpleIdentifier.class, expression.getExpression());
    assertTrue(expression.getExpression().isSynthetic());
  }

  public void test_relationalExpression_precedence_shift_right() throws Exception {
    IsExpression expression = parseExpression("<< is", ParserErrorCode.EXPECTED_IDENTIFIER);
    assertInstanceOf(BinaryExpression.class, expression.getExpression());
  }

  public void test_shiftExpression_missing_LHS() throws Exception {
    BinaryExpression expression = parseExpression("<< y");
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
  }

  public void test_shiftExpression_missing_LHS_RHS() throws Exception {
    BinaryExpression expression = parseExpression("<<");
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_shiftExpression_missing_RHS() throws Exception {
    BinaryExpression expression = parseExpression("x <<");
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_shiftExpression_missing_RHS_super() throws Exception {
    BinaryExpression expression = parseExpression("super <<");
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_shiftExpression_precedence_unary_left() throws Exception {
    BinaryExpression expression = parseExpression("+ <<", ParserErrorCode.NO_UNARY_PLUS_OPERATOR);
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_shiftExpression_precedence_unary_right() throws Exception {
    BinaryExpression expression = parseExpression("<< +", ParserErrorCode.NO_UNARY_PLUS_OPERATOR);
    assertInstanceOf(BinaryExpression.class, expression.getRightOperand());
  }

  public void test_shiftExpression_super() throws Exception {
    BinaryExpression expression = parseExpression("super << <<");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }
}
