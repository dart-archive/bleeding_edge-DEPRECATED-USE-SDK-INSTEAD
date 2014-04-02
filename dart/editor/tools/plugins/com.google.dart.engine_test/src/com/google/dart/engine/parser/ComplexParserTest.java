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

import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.CascadeExpression;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FunctionExpressionInvocation;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.IntegerLiteral;
import com.google.dart.engine.ast.IsExpression;
import com.google.dart.engine.ast.LabeledStatement;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.SimpleIdentifier;

/**
 * The class {@code ComplexParserTest} defines parser tests that test the parsing of more complex
 * code fragments or the interactions between multiple parsing methods. For example, tests to ensure
 * that the precedence of operations is being handled correctly should be defined in this class.
 * <p>
 * Simpler tests should be defined in the class {@link SimpleParserTest}.
 */
public class ComplexParserTest extends ParserTestCase {
  public void test_additiveExpression_normal() throws Exception {
    BinaryExpression expression = parseExpression("x + y - z");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_additiveExpression_noSpaces() throws Exception {
    BinaryExpression expression = parseExpression("i+1");
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertInstanceOf(IntegerLiteral.class, expression.getRightOperand());
  }

  public void test_additiveExpression_precedence_multiplicative_left() throws Exception {
    BinaryExpression expression = parseExpression("x * y + z");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_additiveExpression_precedence_multiplicative_left_withSuper() throws Exception {
    BinaryExpression expression = parseExpression("super * y - z");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_additiveExpression_precedence_multiplicative_right() throws Exception {
    BinaryExpression expression = parseExpression("x + y * z");
    assertInstanceOf(BinaryExpression.class, expression.getRightOperand());
  }

  public void test_additiveExpression_super() throws Exception {
    BinaryExpression expression = parseExpression("super + y - z");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_assignableExpression_arguments_normal_chain() throws Exception {
    PropertyAccess propertyAccess1 = parseExpression("a(b)(c).d(e).f");
    assertEquals("f", propertyAccess1.getPropertyName().getName());
    //
    // a(b)(c).d(e)
    //
    MethodInvocation invocation2 = assertInstanceOf(
        MethodInvocation.class,
        propertyAccess1.getTarget());
    assertEquals("d", invocation2.getMethodName().getName());
    ArgumentList argumentList2 = invocation2.getArgumentList();
    assertNotNull(argumentList2);
    assertSizeOfList(1, argumentList2.getArguments());
    //
    // a(b)(c)
    //
    FunctionExpressionInvocation invocation3 = assertInstanceOf(
        FunctionExpressionInvocation.class,
        invocation2.getTarget());
    ArgumentList argumentList3 = invocation3.getArgumentList();
    assertNotNull(argumentList3);
    assertSizeOfList(1, argumentList3.getArguments());
    //
    // a(b)
    //
    MethodInvocation invocation4 = assertInstanceOf(
        MethodInvocation.class,
        invocation3.getFunction());
    assertEquals("a", invocation4.getMethodName().getName());
    ArgumentList argumentList4 = invocation4.getArgumentList();
    assertNotNull(argumentList4);
    assertSizeOfList(1, argumentList4.getArguments());
  }

  public void test_assignmentExpression_compound() throws Exception {
    AssignmentExpression expression = parseExpression("x = y = 0");
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftHandSide());
    assertInstanceOf(AssignmentExpression.class, expression.getRightHandSide());
  }

  public void test_assignmentExpression_indexExpression() throws Exception {
    AssignmentExpression expression = parseExpression("x[1] = 0");
    assertInstanceOf(IndexExpression.class, expression.getLeftHandSide());
    assertInstanceOf(IntegerLiteral.class, expression.getRightHandSide());
  }

  public void test_assignmentExpression_prefixedIdentifier() throws Exception {
    AssignmentExpression expression = parseExpression("x.y = 0");
    assertInstanceOf(PrefixedIdentifier.class, expression.getLeftHandSide());
    assertInstanceOf(IntegerLiteral.class, expression.getRightHandSide());
  }

  public void test_assignmentExpression_propertyAccess() throws Exception {
    AssignmentExpression expression = parseExpression("super.y = 0");
    assertInstanceOf(PropertyAccess.class, expression.getLeftHandSide());
    assertInstanceOf(IntegerLiteral.class, expression.getRightHandSide());
  }

  public void test_bitwiseAndExpression_normal() throws Exception {
    BinaryExpression expression = parseExpression("x & y & z");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_bitwiseAndExpression_precedence_equality_left() throws Exception {
    BinaryExpression expression = parseExpression("x == y && z");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_bitwiseAndExpression_precedence_equality_right() throws Exception {
    BinaryExpression expression = parseExpression("x && y == z");
    assertInstanceOf(BinaryExpression.class, expression.getRightOperand());
  }

  public void test_bitwiseAndExpression_super() throws Exception {
    BinaryExpression expression = parseExpression("super & y & z");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_bitwiseOrExpression_normal() throws Exception {
    BinaryExpression expression = parseExpression("x | y | z");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_bitwiseOrExpression_precedence_xor_left() throws Exception {
    BinaryExpression expression = parseExpression("x ^ y | z");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_bitwiseOrExpression_precedence_xor_right() throws Exception {
    BinaryExpression expression = parseExpression("x | y ^ z");
    assertInstanceOf(BinaryExpression.class, expression.getRightOperand());
  }

  public void test_bitwiseOrExpression_super() throws Exception {
    BinaryExpression expression = parseExpression("super | y | z");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_bitwiseXorExpression_normal() throws Exception {
    BinaryExpression expression = parseExpression("x ^ y ^ z");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_bitwiseXorExpression_precedence_and_left() throws Exception {
    BinaryExpression expression = parseExpression("x & y ^ z");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_bitwiseXorExpression_precedence_and_right() throws Exception {
    BinaryExpression expression = parseExpression("x ^ y & z");
    assertInstanceOf(BinaryExpression.class, expression.getRightOperand());
  }

  public void test_bitwiseXorExpression_super() throws Exception {
    BinaryExpression expression = parseExpression("super ^ y ^ z");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_cascade_withAssignment() throws Exception {
    CascadeExpression cascade = parseExpression("new Map()..[3] = 4 ..[0] = 11;");
    Expression target = cascade.getTarget();
    for (Expression section : cascade.getCascadeSections()) {
      assertInstanceOf(AssignmentExpression.class, section);
      Expression lhs = ((AssignmentExpression) section).getLeftHandSide();
      assertInstanceOf(IndexExpression.class, lhs);
      IndexExpression index = (IndexExpression) lhs;
      assertTrue(index.isCascaded());
      assertSame(target, index.getRealTarget());
    }
  }

  public void test_conditionalExpression_precedence_logicalOrExpression() throws Exception {
    ConditionalExpression expression = parseExpression("a | b ? y : z");
    assertInstanceOf(BinaryExpression.class, expression.getCondition());
  }

  public void test_constructor_initializer_withParenthesizedExpression() throws Exception {
    CompilationUnit unit = parseCompilationUnit(createSource(
        "class C {",
        "  C() :",
        "    this.a = (b == null ? c : d) {",
        "  }",
        "}"));
    NodeList<CompilationUnitMember> declarations = unit.getDeclarations();
    assertSizeOfList(1, declarations);
  }

  public void test_equalityExpression_normal() throws Exception {
    BinaryExpression expression = parseExpression(
        "x == y != z",
        ParserErrorCode.EQUALITY_CANNOT_BE_EQUALITY_OPERAND);
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_equalityExpression_precedence_relational_left() throws Exception {
    BinaryExpression expression = parseExpression("x is y == z");
    assertInstanceOf(IsExpression.class, expression.getLeftOperand());
  }

  public void test_equalityExpression_precedence_relational_right() throws Exception {
    BinaryExpression expression = parseExpression("x == y is z");
    assertInstanceOf(IsExpression.class, expression.getRightOperand());
  }

  public void test_equalityExpression_super() throws Exception {
    BinaryExpression expression = parseExpression(
        "super == y != z",
        ParserErrorCode.EQUALITY_CANNOT_BE_EQUALITY_OPERAND);
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_logicalAndExpression() throws Exception {
    BinaryExpression expression = parseExpression("x && y && z");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_logicalAndExpression_precedence_bitwiseOr_left() throws Exception {
    BinaryExpression expression = parseExpression("x | y < z");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_logicalAndExpression_precedence_bitwiseOr_right() throws Exception {
    BinaryExpression expression = parseExpression("x < y | z");
    assertInstanceOf(BinaryExpression.class, expression.getRightOperand());
  }

  public void test_logicalOrExpression() throws Exception {
    BinaryExpression expression = parseExpression("x || y || z");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_logicalOrExpression_precedence_logicalAnd_left() throws Exception {
    BinaryExpression expression = parseExpression("x && y || z");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_logicalOrExpression_precedence_logicalAnd_right() throws Exception {
    BinaryExpression expression = parseExpression("x || y && z");
    assertInstanceOf(BinaryExpression.class, expression.getRightOperand());
  }

  public void test_multipleLabels_statement() throws Exception {
    LabeledStatement statement = parseStatement("a: b: c: return x;");
    assertSizeOfList(3, statement.getLabels());
    assertInstanceOf(ReturnStatement.class, statement.getStatement());
  }

  public void test_multiplicativeExpression_normal() throws Exception {
    BinaryExpression expression = parseExpression("x * y / z");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_multiplicativeExpression_precedence_unary_left() throws Exception {
    BinaryExpression expression = parseExpression("-x * y");
    assertInstanceOf(PrefixExpression.class, expression.getLeftOperand());
  }

  public void test_multiplicativeExpression_precedence_unary_right() throws Exception {
    BinaryExpression expression = parseExpression("x * -y");
    assertInstanceOf(PrefixExpression.class, expression.getRightOperand());
  }

  public void test_multiplicativeExpression_super() throws Exception {
    BinaryExpression expression = parseExpression("super * y / z");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_relationalExpression_precedence_shift_right() throws Exception {
    IsExpression expression = parseExpression("x << y is z");
    assertInstanceOf(BinaryExpression.class, expression.getExpression());
  }

  public void test_shiftExpression_normal() throws Exception {
    BinaryExpression expression = parseExpression("x >> 4 << 3");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_shiftExpression_precedence_additive_left() throws Exception {
    BinaryExpression expression = parseExpression("x + y << z");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_shiftExpression_precedence_additive_right() throws Exception {
    BinaryExpression expression = parseExpression("x << y + z");
    assertInstanceOf(BinaryExpression.class, expression.getRightOperand());
  }

  public void test_shiftExpression_super() throws Exception {
    BinaryExpression expression = parseExpression("super >> 4 << 3");
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_topLevelVariable_withMetadata() throws Exception {
    parseCompilationUnit("String @A string;");
  }
}
