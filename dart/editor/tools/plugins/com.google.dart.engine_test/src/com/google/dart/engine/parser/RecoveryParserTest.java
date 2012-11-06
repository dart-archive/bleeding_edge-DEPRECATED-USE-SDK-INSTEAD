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

import com.google.dart.engine.ast.ArgumentDefinitionTest;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.IsExpression;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.scanner.TokenType;

import java.util.List;

/**
 * The class {@code RecoveryParserTest} defines parser tests that test the parsing of invalid code
 * sequences to ensure that the correct recovery steps are taken in the parser.
 */
public class RecoveryParserTest extends ParserTestCase {
  public void test_additiveExpression_missing_LHS() throws Exception {
    BinaryExpression expression = parseExpression("+ y", ParserErrorCode.USE_OF_UNARY_PLUS_OPERATOR);
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
  }

  public void test_additiveExpression_missing_LHS_RHS() throws Exception {
    BinaryExpression expression = parseExpression("+", ParserErrorCode.USE_OF_UNARY_PLUS_OPERATOR);
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
    BinaryExpression expression = parseExpression("* +", ParserErrorCode.USE_OF_UNARY_PLUS_OPERATOR);
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_additiveExpression_precedence_multiplicative_right() throws Exception {
    BinaryExpression expression = parseExpression("+ *", ParserErrorCode.USE_OF_UNARY_PLUS_OPERATOR);
    assertInstanceOf(BinaryExpression.class, expression.getRightOperand());
  }

  public void test_additiveExpression_super() throws Exception {
    BinaryExpression expression = parseExpression(
        "super + +",
        ParserErrorCode.USE_OF_UNARY_PLUS_OPERATOR);
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_argumentDefinitionTest_missing_identifier() throws Exception {
    ArgumentDefinitionTest expression = parseExpression("?", ParserErrorCode.EXPECTED_IDENTIFIER);
    assertTrue(expression.getIdentifier().isSynthetic());
  }

  public void test_assignmentExpression_missing_compound1() throws Exception {
    AssignmentExpression expression = parseExpression("= y = 0");
    Expression syntheticExpression = expression.getLeftHandSide();
    assertInstanceOf(SimpleIdentifier.class, syntheticExpression);
    assertTrue(syntheticExpression.isSynthetic());
  }

  public void test_assignmentExpression_missing_compound2() throws Exception {
    AssignmentExpression expression = parseExpression("x = = 0");
    Expression syntheticExpression = ((AssignmentExpression) expression.getRightHandSide()).getLeftHandSide();
    assertInstanceOf(SimpleIdentifier.class, syntheticExpression);
    assertTrue(syntheticExpression.isSynthetic());
  }

  public void test_assignmentExpression_missing_compound3() throws Exception {
    AssignmentExpression expression = parseExpression("x = y =");
    Expression syntheticExpression = ((AssignmentExpression) expression.getRightHandSide()).getRightHandSide();
    assertInstanceOf(SimpleIdentifier.class, syntheticExpression);
    assertTrue(syntheticExpression.isSynthetic());
  }

  public void test_assignmentExpression_missing_LHS() throws Exception {
    AssignmentExpression expression = parseExpression("= 0");
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftHandSide());
    assertTrue(expression.getLeftHandSide().isSynthetic());
  }

  public void test_assignmentExpression_missing_RHS() throws Exception {
    AssignmentExpression expression = parseExpression("x =");
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftHandSide());
    assertTrue(expression.getRightHandSide().isSynthetic());
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

  public void test_conditionalExpression_missingElse() throws Exception {
    ConditionalExpression expression = parse("parseConditionalExpression", "x ? y :");
    assertInstanceOf(SimpleIdentifier.class, expression.getElseExpression());
    assertTrue(expression.getElseExpression().isSynthetic());
  }

  public void test_conditionalExpression_missingThen() throws Exception {
    ConditionalExpression expression = parse("parseConditionalExpression", "x ? : z");
    assertInstanceOf(SimpleIdentifier.class, expression.getThenExpression());
    assertTrue(expression.getThenExpression().isSynthetic());
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

  public void test_expressionList_multiple_end() throws Exception {
    List<Expression> result = parse("parseExpressionList", ", 2, 3, 4");
    assertSize(4, result);
    Expression syntheticExpression = result.get(0);
    assertInstanceOf(SimpleIdentifier.class, syntheticExpression);
    assertTrue(syntheticExpression.isSynthetic());
  }

  public void test_expressionList_multiple_middle() throws Exception {
    List<Expression> result = parse("parseExpressionList", "1, 2, , 4");
    assertSize(4, result);
    Expression syntheticExpression = result.get(2);
    assertInstanceOf(SimpleIdentifier.class, syntheticExpression);
    assertTrue(syntheticExpression.isSynthetic());
  }

  public void test_expressionList_multiple_start() throws Exception {
    List<Expression> result = parse("parseExpressionList", "1, 2, 3,");
    assertSize(4, result);
    Expression syntheticExpression = result.get(3);
    assertInstanceOf(SimpleIdentifier.class, syntheticExpression);
    assertTrue(syntheticExpression.isSynthetic());
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

  public void test_relationalExpression_missing_LHS_RHS() throws Exception {
    IsExpression expression = parseExpression("is", ParserErrorCode.EXPECTED_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getExpression());
    assertTrue(expression.getExpression().isSynthetic());
    assertInstanceOf(TypeName.class, expression.getType());
    assertTrue(expression.getType().isSynthetic());
  }

  public void test_relationalExpression_missing_RHS() throws Exception {
    IsExpression expression = parseExpression("x is", ParserErrorCode.EXPECTED_IDENTIFIER);
    assertInstanceOf(TypeName.class, expression.getType());
    assertTrue(expression.getType().isSynthetic());
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
    BinaryExpression expression = parseExpression(
        "+ <<",
        ParserErrorCode.USE_OF_UNARY_PLUS_OPERATOR);
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_shiftExpression_precedence_unary_right() throws Exception {
    BinaryExpression expression = parseExpression(
        "<< +",
        ParserErrorCode.USE_OF_UNARY_PLUS_OPERATOR);
    assertInstanceOf(BinaryExpression.class, expression.getRightOperand());
  }

  public void test_shiftExpression_super() throws Exception {
    BinaryExpression expression = parseExpression("super << <<");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_topLevelExternalFunction_extraSemicolon() throws Exception {
    CompilationUnit unit = parseCompilationUnit(
        "external void f(A a);",
        ParserErrorCode.UNEXPECTED_TOKEN);
    NodeList<CompilationUnitMember> declarations = unit.getDeclarations();
    assertSize(1, declarations);
    FunctionDeclaration declaration = (FunctionDeclaration) declarations.get(0);
    assertNotNull(declaration);
  }
}
