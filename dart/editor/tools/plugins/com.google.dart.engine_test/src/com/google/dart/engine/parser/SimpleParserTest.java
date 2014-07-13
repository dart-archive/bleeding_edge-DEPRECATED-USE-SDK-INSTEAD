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

import com.google.dart.engine.ast.*;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.parser.CommentAndMetadata;
import com.google.dart.engine.internal.parser.FinalConstVarOrType;
import com.google.dart.engine.internal.parser.Modifiers;
import com.google.dart.engine.scanner.CharSequenceReader;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.Scanner;
import com.google.dart.engine.scanner.StringToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.source.TestSource;
import com.google.dart.engine.utilities.dart.ParameterKind;

import static com.google.dart.engine.ast.AstFactory.typeArgumentList;
import static com.google.dart.engine.ast.AstFactory.typeName;
import static com.google.dart.engine.scanner.TokenFactory.tokenFromKeyword;

import junit.framework.AssertionFailedError;

import java.math.BigInteger;
import java.util.List;

/**
 * The class {@code SimpleParserTest} defines parser tests that test individual parsing method. The
 * code fragments should be as minimal as possible in order to test the method, but should not test
 * the interactions between the method under test and other methods.
 * <p>
 * More complex tests should be defined in the class {@link ComplexParserTest}.
 */
public class SimpleParserTest extends ParserTestCase {
  public void fail_parseCommentReference_this() throws Exception {
    // This fails because we are returning null from the method and asserting that the return value
    // is not null.
    CommentReference reference = parse("parseCommentReference", new Object[] {"this", 5}, "");
    SimpleIdentifier identifier = assertInstanceOf(
        SimpleIdentifier.class,
        reference.getIdentifier());
    assertNotNull(identifier.getToken());
    assertEquals("a", identifier.getName());
    assertEquals(5, identifier.getOffset());
  }

  public void test_computeStringValue_emptyInterpolationPrefix() throws Exception {
    assertEquals("", computeStringValue("'''", true, false));
  }

  public void test_computeStringValue_escape_b() throws Exception {
    assertEquals("\b", computeStringValue("'\\b'", true, true));
  }

  public void test_computeStringValue_escape_f() throws Exception {
    assertEquals("\f", computeStringValue("'\\f'", true, true));
  }

  public void test_computeStringValue_escape_n() throws Exception {
    assertEquals("\n", computeStringValue("'\\n'", true, true));
  }

  public void test_computeStringValue_escape_notSpecial() throws Exception {
    assertEquals(":", computeStringValue("'\\:'", true, true));
  }

  public void test_computeStringValue_escape_r() throws Exception {
    assertEquals("\r", computeStringValue("'\\r'", true, true));
  }

  public void test_computeStringValue_escape_t() throws Exception {
    assertEquals("\t", computeStringValue("'\\t'", true, true));
  }

  public void test_computeStringValue_escape_u_fixed() throws Exception {
    assertEquals("\u4321", computeStringValue("'\\u4321'", true, true));
  }

  public void test_computeStringValue_escape_u_variable() throws Exception {
    assertEquals("\u0123", computeStringValue("'\\u{123}'", true, true));
  }

  public void test_computeStringValue_escape_v() throws Exception {
    assertEquals("\u000B", computeStringValue("'\\v'", true, true));
  }

  public void test_computeStringValue_escape_x() throws Exception {
    assertEquals("\u00FF", computeStringValue("'\\xFF'", true, true));
  }

  public void test_computeStringValue_noEscape_single() throws Exception {
    assertEquals("text", computeStringValue("'text'", true, true));
  }

  public void test_computeStringValue_noEscape_triple() throws Exception {
    assertEquals("text", computeStringValue("'''text'''", true, true));
  }

  public void test_computeStringValue_raw_single() throws Exception {
    assertEquals("text", computeStringValue("r'text'", true, true));
  }

  public void test_computeStringValue_raw_triple() throws Exception {
    assertEquals("text", computeStringValue("r'''text'''", true, true));
  }

  public void test_computeStringValue_raw_withEscape() throws Exception {
    assertEquals("two\\nlines", computeStringValue("r'two\\nlines'", true, true));
  }

  public void test_computeStringValue_triple_internalQuote_first_empty() throws Exception {
    assertEquals("'", computeStringValue("''''", true, false));
  }

  public void test_computeStringValue_triple_internalQuote_first_nonEmpty() throws Exception {
    assertEquals("'text", computeStringValue("''''text", true, false));
  }

  public void test_computeStringValue_triple_internalQuote_last_empty() throws Exception {
    assertEquals("", computeStringValue("'''", false, true));
  }

  public void test_computeStringValue_triple_internalQuote_last_nonEmpty() throws Exception {
    assertEquals("text", computeStringValue("text'''", false, true));
  }

  public void test_constFactory() throws Exception {
    parse("parseClassMember", new Object[] {"C"}, "const factory C() = A;");
  }

  public void test_createSyntheticIdentifier() throws Exception {
    SimpleIdentifier identifier = createSyntheticIdentifier();
    assertTrue(identifier.isSynthetic());
  }

  public void test_createSyntheticStringLiteral() throws Exception {
    SimpleStringLiteral literal = createSyntheticStringLiteral();
    assertTrue(literal.isSynthetic());
  }

  public void test_function_literal_allowed_at_toplevel() throws Exception {
    parseCompilationUnit("var x = () {};");
  }

  public void test_function_literal_allowed_in_ArgumentList_in_ConstructorFieldInitializer()
      throws Exception {
    parseCompilationUnit("class C { C() : a = f(() {}); }");
  }

  public void test_function_literal_allowed_in_IndexExpression_in_ConstructorFieldInitializer()
      throws Exception {
    parseCompilationUnit("class C { C() : a = x[() {}]; }");
  }

  public void test_function_literal_allowed_in_ListLiteral_in_ConstructorFieldInitializer()
      throws Exception {
    parseCompilationUnit("class C { C() : a = [() {}]; }");
  }

  public void test_function_literal_allowed_in_MapLiteral_in_ConstructorFieldInitializer()
      throws Exception {
    parseCompilationUnit("class C { C() : a = {'key': () {}}; }");
  }

  public void test_function_literal_allowed_in_ParenthesizedExpression_in_ConstructorFieldInitializer()
      throws Exception {
    parseCompilationUnit("class C { C() : a = (() {}); }");
  }

  public void test_function_literal_allowed_in_StringInterpolation_in_ConstructorFieldInitializer()
      throws Exception {
    parseCompilationUnit("class C { C() : a = \"${(){}}\"; }");
  }

  public void test_isFunctionDeclaration_nameButNoReturn_block() throws Exception {
    assertTrue(isFunctionDeclaration("f() {}"));
  }

  public void test_isFunctionDeclaration_nameButNoReturn_expression() throws Exception {
    assertTrue(isFunctionDeclaration("f() => e"));
  }

  public void test_isFunctionDeclaration_normalReturn_block() throws Exception {
    assertTrue(isFunctionDeclaration("C f() {}"));
  }

  public void test_isFunctionDeclaration_normalReturn_expression() throws Exception {
    assertTrue(isFunctionDeclaration("C f() => e"));
  }

  public void test_isFunctionDeclaration_voidReturn_block() throws Exception {
    assertTrue(isFunctionDeclaration("void f() {}"));
  }

  public void test_isFunctionDeclaration_voidReturn_expression() throws Exception {
    assertTrue(isFunctionDeclaration("void f() => e"));
  }

  public void test_isFunctionExpression_false_noBody() throws Exception {
    assertFalse(isFunctionExpression("f();"));
  }

  public void test_isFunctionExpression_false_notParameters() throws Exception {
    assertFalse(isFunctionExpression("(a + b) {"));
  }

  public void test_isFunctionExpression_noName_block() throws Exception {
    assertTrue(isFunctionExpression("() {}"));
  }

  public void test_isFunctionExpression_noName_expression() throws Exception {
    assertTrue(isFunctionExpression("() => e"));
  }

  public void test_isFunctionExpression_parameter_final() throws Exception {
    assertTrue(isFunctionExpression("(final a) {}"));
    assertTrue(isFunctionExpression("(final a, b) {}"));
    assertTrue(isFunctionExpression("(final a, final b) {}"));
  }

  public void test_isFunctionExpression_parameter_final_typed() throws Exception {
    assertTrue(isFunctionExpression("(final int a) {}"));
    assertTrue(isFunctionExpression("(final prefix.List a) {}"));
    assertTrue(isFunctionExpression("(final List<int> a) {}"));
    assertTrue(isFunctionExpression("(final prefix.List<int> a) {}"));
  }

  public void test_isFunctionExpression_parameter_multiple() throws Exception {
    assertTrue(isFunctionExpression("(a, b) {}"));
  }

  public void test_isFunctionExpression_parameter_named() throws Exception {
    assertTrue(isFunctionExpression("({a}) {}"));
  }

  public void test_isFunctionExpression_parameter_optional() throws Exception {
    assertTrue(isFunctionExpression("([a]) {}"));
  }

  public void test_isFunctionExpression_parameter_single() throws Exception {
    assertTrue(isFunctionExpression("(a) {}"));
  }

  public void test_isFunctionExpression_parameter_typed() throws Exception {
    assertTrue(isFunctionExpression("(int a, int b) {}"));
  }

  public void test_isInitializedVariableDeclaration_assignment() throws Exception {
    assertFalse(isInitializedVariableDeclaration("a = null;"));
  }

  public void test_isInitializedVariableDeclaration_comparison() throws Exception {
    assertFalse(isInitializedVariableDeclaration("a < 0;"));
  }

  public void test_isInitializedVariableDeclaration_conditional() throws Exception {
    assertFalse(isInitializedVariableDeclaration("a == null ? init() : update();"));
  }

  public void test_isInitializedVariableDeclaration_const_noType_initialized() throws Exception {
    assertTrue(isInitializedVariableDeclaration("const a = 0;"));
  }

  public void test_isInitializedVariableDeclaration_const_noType_uninitialized() throws Exception {
    assertTrue(isInitializedVariableDeclaration("const a;"));
  }

  public void test_isInitializedVariableDeclaration_const_simpleType_uninitialized()
      throws Exception {
    assertTrue(isInitializedVariableDeclaration("const A a;"));
  }

  public void test_isInitializedVariableDeclaration_final_noType_initialized() throws Exception {
    assertTrue(isInitializedVariableDeclaration("final a = 0;"));
  }

  public void test_isInitializedVariableDeclaration_final_noType_uninitialized() throws Exception {
    assertTrue(isInitializedVariableDeclaration("final a;"));
  }

  public void test_isInitializedVariableDeclaration_final_simpleType_initialized() throws Exception {
    assertTrue(isInitializedVariableDeclaration("final A a = 0;"));
  }

  public void test_isInitializedVariableDeclaration_functionDeclaration_typed() throws Exception {
    assertFalse(isInitializedVariableDeclaration("A f() {};"));
  }

  public void test_isInitializedVariableDeclaration_functionDeclaration_untyped() throws Exception {
    assertFalse(isInitializedVariableDeclaration("f() {};"));
  }

  public void test_isInitializedVariableDeclaration_noType_initialized() throws Exception {
    assertTrue(isInitializedVariableDeclaration("var a = 0;"));
  }

  public void test_isInitializedVariableDeclaration_noType_uninitialized() throws Exception {
    assertTrue(isInitializedVariableDeclaration("var a;"));
  }

  public void test_isInitializedVariableDeclaration_parameterizedType_initialized()
      throws Exception {
    assertTrue(isInitializedVariableDeclaration("List<int> a = null;"));
  }

  public void test_isInitializedVariableDeclaration_parameterizedType_uninitialized()
      throws Exception {
    assertTrue(isInitializedVariableDeclaration("List<int> a;"));
  }

  public void test_isInitializedVariableDeclaration_simpleType_initialized() throws Exception {
    assertTrue(isInitializedVariableDeclaration("A a = 0;"));
  }

  public void test_isInitializedVariableDeclaration_simpleType_uninitialized() throws Exception {
    assertTrue(isInitializedVariableDeclaration("A a;"));
  }

  public void test_isSwitchMember_case_labeled() throws Exception {
    assertTrue(isSwitchMember("l1: l2: case"));
  }

  public void test_isSwitchMember_case_unlabeled() throws Exception {
    assertTrue(isSwitchMember("case"));
  }

  public void test_isSwitchMember_default_labeled() throws Exception {
    assertTrue(isSwitchMember("l1: l2: default"));
  }

  public void test_isSwitchMember_default_unlabeled() throws Exception {
    assertTrue(isSwitchMember("default"));
  }

  public void test_isSwitchMember_false() throws Exception {
    assertFalse(isSwitchMember("break;"));
  }

  public void test_parseAdditiveExpression_normal() throws Exception {
    BinaryExpression expression = parse("parseAdditiveExpression", "x + y");
    assertNotNull(expression.getLeftOperand());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.PLUS, expression.getOperator().getType());
    assertNotNull(expression.getRightOperand());
  }

  public void test_parseAdditiveExpression_super() throws Exception {
    BinaryExpression expression = parse("parseAdditiveExpression", "super + y");
    assertInstanceOf(SuperExpression.class, expression.getLeftOperand());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.PLUS, expression.getOperator().getType());
    assertNotNull(expression.getRightOperand());
  }

  public void test_parseAnnotation_n1() throws Exception {
    Annotation annotation = parse("parseAnnotation", "@A");
    assertNotNull(annotation.getAtSign());
    assertNotNull(annotation.getName());
    assertNull(annotation.getPeriod());
    assertNull(annotation.getConstructorName());
    assertNull(annotation.getArguments());
  }

  public void test_parseAnnotation_n1_a() throws Exception {
    Annotation annotation = parse("parseAnnotation", "@A(x,y)");
    assertNotNull(annotation.getAtSign());
    assertNotNull(annotation.getName());
    assertNull(annotation.getPeriod());
    assertNull(annotation.getConstructorName());
    assertNotNull(annotation.getArguments());
  }

  public void test_parseAnnotation_n2() throws Exception {
    Annotation annotation = parse("parseAnnotation", "@A.B");
    assertNotNull(annotation.getAtSign());
    assertNotNull(annotation.getName());
    assertNull(annotation.getPeriod());
    assertNull(annotation.getConstructorName());
    assertNull(annotation.getArguments());
  }

  public void test_parseAnnotation_n2_a() throws Exception {
    Annotation annotation = parse("parseAnnotation", "@A.B(x,y)");
    assertNotNull(annotation.getAtSign());
    assertNotNull(annotation.getName());
    assertNull(annotation.getPeriod());
    assertNull(annotation.getConstructorName());
    assertNotNull(annotation.getArguments());
  }

  public void test_parseAnnotation_n3() throws Exception {
    Annotation annotation = parse("parseAnnotation", "@A.B.C");
    assertNotNull(annotation.getAtSign());
    assertNotNull(annotation.getName());
    assertNotNull(annotation.getPeriod());
    assertNotNull(annotation.getConstructorName());
    assertNull(annotation.getArguments());
  }

  public void test_parseAnnotation_n3_a() throws Exception {
    Annotation annotation = parse("parseAnnotation", "@A.B.C(x,y)");
    assertNotNull(annotation.getAtSign());
    assertNotNull(annotation.getName());
    assertNotNull(annotation.getPeriod());
    assertNotNull(annotation.getConstructorName());
    assertNotNull(annotation.getArguments());
  }

  public void test_parseArgument_named() throws Exception {
    NamedExpression expression = parse("parseArgument", "n: x");
    Label name = expression.getName();
    assertNotNull(name);
    assertNotNull(name.getLabel());
    assertNotNull(name.getColon());
    assertNotNull(expression.getExpression());
  }

  public void test_parseArgument_unnamed() throws Exception {
    String lexeme = "x";
    SimpleIdentifier identifier = parse("parseArgument", lexeme);
    assertEquals(lexeme, identifier.getName());
  }

  public void test_parseArgumentList_empty() throws Exception {
    ArgumentList argumentList = parse("parseArgumentList", "()");
    NodeList<Expression> arguments = argumentList.getArguments();
    assertSizeOfList(0, arguments);
  }

  public void test_parseArgumentList_mixed() throws Exception {
    ArgumentList argumentList = parse("parseArgumentList", "(w, x, y: y, z: z)");
    NodeList<Expression> arguments = argumentList.getArguments();
    assertSizeOfList(4, arguments);
  }

  public void test_parseArgumentList_noNamed() throws Exception {
    ArgumentList argumentList = parse("parseArgumentList", "(x, y, z)");
    NodeList<Expression> arguments = argumentList.getArguments();
    assertSizeOfList(3, arguments);
  }

  public void test_parseArgumentList_onlyNamed() throws Exception {
    ArgumentList argumentList = parse("parseArgumentList", "(x: x, y: y)");
    NodeList<Expression> arguments = argumentList.getArguments();
    assertSizeOfList(2, arguments);
  }

  public void test_parseAssertStatement() throws Exception {
    AssertStatement statement = parse("parseAssertStatement", "assert (x);");
    assertNotNull(statement.getKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNotNull(statement.getCondition());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getSemicolon());
  }

  public void test_parseAssignableExpression_expression_args_dot() throws Exception {
    PropertyAccess propertyAccess = parse(
        "parseAssignableExpression",
        new Object[] {false},
        "(x)(y).z");
    FunctionExpressionInvocation invocation = (FunctionExpressionInvocation) propertyAccess.getTarget();
    assertNotNull(invocation.getFunction());
    ArgumentList argumentList = invocation.getArgumentList();
    assertNotNull(argumentList);
    assertSizeOfList(1, argumentList.getArguments());
    assertNotNull(propertyAccess.getOperator());
    assertNotNull(propertyAccess.getPropertyName());
  }

  public void test_parseAssignableExpression_expression_dot() throws Exception {
    PropertyAccess propertyAccess = parse(
        "parseAssignableExpression",
        new Object[] {false},
        "(x).y");
    assertNotNull(propertyAccess.getTarget());
    assertNotNull(propertyAccess.getOperator());
    assertNotNull(propertyAccess.getPropertyName());
  }

  public void test_parseAssignableExpression_expression_index() throws Exception {
    IndexExpression expression = parse("parseAssignableExpression", new Object[] {false}, "(x)[y]");
    assertNotNull(expression.getTarget());
    assertNotNull(expression.getLeftBracket());
    assertNotNull(expression.getIndex());
    assertNotNull(expression.getRightBracket());
  }

  public void test_parseAssignableExpression_identifier() throws Exception {
    SimpleIdentifier identifier = parse("parseAssignableExpression", new Object[] {false}, "x");
    assertNotNull(identifier);
  }

  public void test_parseAssignableExpression_identifier_args_dot() throws Exception {
    PropertyAccess propertyAccess = parse(
        "parseAssignableExpression",
        new Object[] {false},
        "x(y).z");
    MethodInvocation invocation = (MethodInvocation) propertyAccess.getTarget();
    assertEquals("x", invocation.getMethodName().getName());
    ArgumentList argumentList = invocation.getArgumentList();
    assertNotNull(argumentList);
    assertSizeOfList(1, argumentList.getArguments());
    assertNotNull(propertyAccess.getOperator());
    assertNotNull(propertyAccess.getPropertyName());
  }

  public void test_parseAssignableExpression_identifier_dot() throws Exception {
    PropertyAccess propertyAccess = parse("parseAssignableExpression", new Object[] {false}, "x.y");
    assertNotNull(propertyAccess.getTarget());
    assertNotNull(propertyAccess.getOperator());
    assertNotNull(propertyAccess.getPropertyName());
  }

  public void test_parseAssignableExpression_identifier_index() throws Exception {
    IndexExpression expression = parse("parseAssignableExpression", new Object[] {false}, "x[y]");
    assertNotNull(expression.getTarget());
    assertNotNull(expression.getLeftBracket());
    assertNotNull(expression.getIndex());
    assertNotNull(expression.getRightBracket());
  }

  public void test_parseAssignableExpression_super_dot() throws Exception {
    PropertyAccess propertyAccess = parse(
        "parseAssignableExpression",
        new Object[] {false},
        "super.y");
    assertInstanceOf(SuperExpression.class, propertyAccess.getTarget());
    assertNotNull(propertyAccess.getOperator());
    assertNotNull(propertyAccess.getPropertyName());
  }

  public void test_parseAssignableExpression_super_index() throws Exception {
    IndexExpression expression = parse(
        "parseAssignableExpression",
        new Object[] {false},
        "super[y]");
    assertInstanceOf(SuperExpression.class, expression.getTarget());
    assertNotNull(expression.getLeftBracket());
    assertNotNull(expression.getIndex());
    assertNotNull(expression.getRightBracket());
  }

  public void test_parseAssignableSelector_dot() throws Exception {
    PropertyAccess selector = parse("parseAssignableSelector", new Object[] {null, true}, ".x");
    assertNotNull(selector.getOperator());
    assertNotNull(selector.getPropertyName());
  }

  public void test_parseAssignableSelector_index() throws Exception {
    IndexExpression selector = parse("parseAssignableSelector", new Object[] {null, true}, "[x]");
    assertNotNull(selector.getLeftBracket());
    assertNotNull(selector.getIndex());
    assertNotNull(selector.getRightBracket());
  }

  public void test_parseAssignableSelector_none() throws Exception {
    SimpleIdentifier selector = parse("parseAssignableSelector", new Object[] {
        new SimpleIdentifier(null), true}, ";");
    assertNotNull(selector);
  }

  public void test_parseAwaitExpression() throws Exception {
    AwaitExpression expression = parse("parseAwaitExpression", "await x;");
    assertNotNull(expression.getAwaitKeyword());
    assertNotNull(expression.getExpression());
    assertNotNull(expression.getSemicolon());
  }

  public void test_parseBitwiseAndExpression_normal() throws Exception {
    BinaryExpression expression = parse("parseBitwiseAndExpression", "x & y");
    assertNotNull(expression.getLeftOperand());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.AMPERSAND, expression.getOperator().getType());
    assertNotNull(expression.getRightOperand());
  }

  public void test_parseBitwiseAndExpression_super() throws Exception {
    BinaryExpression expression = parse("parseBitwiseAndExpression", "super & y");
    assertInstanceOf(SuperExpression.class, expression.getLeftOperand());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.AMPERSAND, expression.getOperator().getType());
    assertNotNull(expression.getRightOperand());
  }

  public void test_parseBitwiseOrExpression_normal() throws Exception {
    BinaryExpression expression = parse("parseBitwiseOrExpression", "x | y");
    assertNotNull(expression.getLeftOperand());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.BAR, expression.getOperator().getType());
    assertNotNull(expression.getRightOperand());
  }

  public void test_parseBitwiseOrExpression_super() throws Exception {
    BinaryExpression expression = parse("parseBitwiseOrExpression", "super | y");
    assertInstanceOf(SuperExpression.class, expression.getLeftOperand());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.BAR, expression.getOperator().getType());
    assertNotNull(expression.getRightOperand());
  }

  public void test_parseBitwiseXorExpression_normal() throws Exception {
    BinaryExpression expression = parse("parseBitwiseXorExpression", "x ^ y");
    assertNotNull(expression.getLeftOperand());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.CARET, expression.getOperator().getType());
    assertNotNull(expression.getRightOperand());
  }

  public void test_parseBitwiseXorExpression_super() throws Exception {
    BinaryExpression expression = parse("parseBitwiseXorExpression", "super ^ y");
    assertInstanceOf(SuperExpression.class, expression.getLeftOperand());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.CARET, expression.getOperator().getType());
    assertNotNull(expression.getRightOperand());
  }

  public void test_parseBlock_empty() throws Exception {
    Block block = parse("parseBlock", "{}");
    assertNotNull(block.getLeftBracket());
    assertSizeOfList(0, block.getStatements());
    assertNotNull(block.getRightBracket());
  }

  public void test_parseBlock_nonEmpty() throws Exception {
    Block block = parse("parseBlock", "{;}");
    assertNotNull(block.getLeftBracket());
    assertSizeOfList(1, block.getStatements());
    assertNotNull(block.getRightBracket());
  }

  public void test_parseBreakStatement_label() throws Exception {
    BreakStatement statement = parse("parseBreakStatement", "break foo;");
    assertNotNull(statement.getKeyword());
    assertNotNull(statement.getLabel());
    assertNotNull(statement.getSemicolon());
  }

  public void test_parseBreakStatement_noLabel() throws Exception {
    BreakStatement statement = parse(
        "parseBreakStatement",
        "break;",
        ParserErrorCode.BREAK_OUTSIDE_OF_LOOP);
    assertNotNull(statement.getKeyword());
    assertNull(statement.getLabel());
    assertNotNull(statement.getSemicolon());
  }

  public void test_parseCascadeSection_i() throws Exception {
    IndexExpression section = parse("parseCascadeSection", "..[i]");
    assertNull(section.getTarget());
    assertNotNull(section.getLeftBracket());
    assertNotNull(section.getIndex());
    assertNotNull(section.getRightBracket());
  }

  public void test_parseCascadeSection_ia() throws Exception {
    FunctionExpressionInvocation section = parse("parseCascadeSection", "..[i](b)");
    assertInstanceOf(IndexExpression.class, section.getFunction());
    assertNotNull(section.getArgumentList());
  }

  public void test_parseCascadeSection_ii() throws Exception {
    MethodInvocation section = parse("parseCascadeSection", "..a(b).c(d)");
    assertInstanceOf(MethodInvocation.class, section.getTarget());
    assertNotNull(section.getPeriod());
    assertNotNull(section.getMethodName());
    assertNotNull(section.getArgumentList());
    assertSizeOfList(1, section.getArgumentList().getArguments());
  }

  public void test_parseCascadeSection_p() throws Exception {
    PropertyAccess section = parse("parseCascadeSection", "..a");
    assertNull(section.getTarget());
    assertNotNull(section.getOperator());
    assertNotNull(section.getPropertyName());
  }

  public void test_parseCascadeSection_p_assign() throws Exception {
    AssignmentExpression section = parse("parseCascadeSection", "..a = 3");
    assertNotNull(section.getLeftHandSide());
    assertNotNull(section.getOperator());
    Expression rhs = section.getRightHandSide();
    assertNotNull(rhs);
  }

  public void test_parseCascadeSection_p_assign_withCascade() throws Exception {
    AssignmentExpression section = parse("parseCascadeSection", "..a = 3..m()");
    assertNotNull(section.getLeftHandSide());
    assertNotNull(section.getOperator());
    Expression rhs = section.getRightHandSide();
    assertInstanceOf(IntegerLiteral.class, rhs);
  }

  public void test_parseCascadeSection_p_builtIn() throws Exception {
    PropertyAccess section = parse("parseCascadeSection", "..as");
    assertNull(section.getTarget());
    assertNotNull(section.getOperator());
    assertNotNull(section.getPropertyName());
  }

  public void test_parseCascadeSection_pa() throws Exception {
    MethodInvocation section = parse("parseCascadeSection", "..a(b)");
    assertNull(section.getTarget());
    assertNotNull(section.getPeriod());
    assertNotNull(section.getMethodName());
    assertNotNull(section.getArgumentList());
    assertSizeOfList(1, section.getArgumentList().getArguments());
  }

  public void test_parseCascadeSection_paa() throws Exception {
    FunctionExpressionInvocation section = parse("parseCascadeSection", "..a(b)(c)");
    assertInstanceOf(MethodInvocation.class, section.getFunction());
    assertNotNull(section.getArgumentList());
    assertSizeOfList(1, section.getArgumentList().getArguments());
  }

  public void test_parseCascadeSection_paapaa() throws Exception {
    FunctionExpressionInvocation section = parse("parseCascadeSection", "..a(b)(c).d(e)(f)");
    assertInstanceOf(MethodInvocation.class, section.getFunction());
    assertNotNull(section.getArgumentList());
    assertSizeOfList(1, section.getArgumentList().getArguments());
  }

  public void test_parseCascadeSection_pap() throws Exception {
    PropertyAccess section = parse("parseCascadeSection", "..a(b).c");
    assertNotNull(section.getTarget());
    assertNotNull(section.getOperator());
    assertNotNull(section.getPropertyName());
  }

  public void test_parseClassDeclaration_abstract() throws Exception {
    ClassDeclaration declaration = parse("parseClassDeclaration", new Object[] {
        emptyCommentAndMetadata(), tokenFromKeyword(Keyword.ABSTRACT)}, "class A {}");
    assertNull(declaration.getDocumentationComment());
    assertNotNull(declaration.getAbstractKeyword());
    assertNull(declaration.getExtendsClause());
    assertNull(declaration.getImplementsClause());
    assertNotNull(declaration.getClassKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    assertSizeOfList(0, declaration.getMembers());
    assertNotNull(declaration.getRightBracket());
    assertNull(declaration.getTypeParameters());
  }

  public void test_parseClassDeclaration_empty() throws Exception {
    ClassDeclaration declaration = parse("parseClassDeclaration", new Object[] {
        emptyCommentAndMetadata(), null}, "class A {}");
    assertNull(declaration.getDocumentationComment());
    assertNull(declaration.getAbstractKeyword());
    assertNull(declaration.getExtendsClause());
    assertNull(declaration.getImplementsClause());
    assertNotNull(declaration.getClassKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    assertSizeOfList(0, declaration.getMembers());
    assertNotNull(declaration.getRightBracket());
    assertNull(declaration.getTypeParameters());
  }

  public void test_parseClassDeclaration_extends() throws Exception {
    ClassDeclaration declaration = parse("parseClassDeclaration", new Object[] {
        emptyCommentAndMetadata(), null}, "class A extends B {}");
    assertNull(declaration.getDocumentationComment());
    assertNull(declaration.getAbstractKeyword());
    assertNotNull(declaration.getExtendsClause());
    assertNull(declaration.getImplementsClause());
    assertNotNull(declaration.getClassKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    assertSizeOfList(0, declaration.getMembers());
    assertNotNull(declaration.getRightBracket());
    assertNull(declaration.getTypeParameters());
  }

  public void test_parseClassDeclaration_extendsAndImplements() throws Exception {
    ClassDeclaration declaration = parse("parseClassDeclaration", new Object[] {
        emptyCommentAndMetadata(), null}, "class A extends B implements C {}");
    assertNull(declaration.getDocumentationComment());
    assertNull(declaration.getAbstractKeyword());
    assertNotNull(declaration.getExtendsClause());
    assertNotNull(declaration.getImplementsClause());
    assertNotNull(declaration.getClassKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    assertSizeOfList(0, declaration.getMembers());
    assertNotNull(declaration.getRightBracket());
    assertNull(declaration.getTypeParameters());
  }

  public void test_parseClassDeclaration_extendsAndWith() throws Exception {
    ClassDeclaration declaration = parse("parseClassDeclaration", new Object[] {
        emptyCommentAndMetadata(), null}, "class A extends B with C {}");
    assertNull(declaration.getDocumentationComment());
    assertNull(declaration.getAbstractKeyword());
    assertNotNull(declaration.getClassKeyword());
    assertNotNull(declaration.getName());
    assertNull(declaration.getTypeParameters());
    assertNotNull(declaration.getExtendsClause());
    assertNotNull(declaration.getWithClause());
    assertNull(declaration.getImplementsClause());
    assertNotNull(declaration.getLeftBracket());
    assertSizeOfList(0, declaration.getMembers());
    assertNotNull(declaration.getRightBracket());
  }

  public void test_parseClassDeclaration_extendsAndWithAndImplements() throws Exception {
    ClassDeclaration declaration = parse("parseClassDeclaration", new Object[] {
        emptyCommentAndMetadata(), null}, "class A extends B with C implements D {}");
    assertNull(declaration.getDocumentationComment());
    assertNull(declaration.getAbstractKeyword());
    assertNotNull(declaration.getClassKeyword());
    assertNotNull(declaration.getName());
    assertNull(declaration.getTypeParameters());
    assertNotNull(declaration.getExtendsClause());
    assertNotNull(declaration.getWithClause());
    assertNotNull(declaration.getImplementsClause());
    assertNotNull(declaration.getLeftBracket());
    assertSizeOfList(0, declaration.getMembers());
    assertNotNull(declaration.getRightBracket());
  }

  public void test_parseClassDeclaration_implements() throws Exception {
    ClassDeclaration declaration = parse("parseClassDeclaration", new Object[] {
        emptyCommentAndMetadata(), null}, "class A implements C {}");
    assertNull(declaration.getDocumentationComment());
    assertNull(declaration.getAbstractKeyword());
    assertNull(declaration.getExtendsClause());
    assertNotNull(declaration.getImplementsClause());
    assertNotNull(declaration.getClassKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    assertSizeOfList(0, declaration.getMembers());
    assertNotNull(declaration.getRightBracket());
    assertNull(declaration.getTypeParameters());
  }

  public void test_parseClassDeclaration_native() throws Exception {
    ClassDeclaration declaration = parse("parseClassDeclaration", new Object[] {
        emptyCommentAndMetadata(), null}, "class A native 'nativeValue' {}");
    NativeClause nativeClause = declaration.getNativeClause();
    assertNotNull(nativeClause);
    assertNotNull(nativeClause.getKeyword());
    assertEquals("nativeValue", nativeClause.getName().getStringValue());
    assertSame(nativeClause.getKeyword(), nativeClause.getBeginToken());
    assertSame(nativeClause.getName().getEndToken(), nativeClause.getEndToken());
  }

  public void test_parseClassDeclaration_nonEmpty() throws Exception {
    ClassDeclaration declaration = parse("parseClassDeclaration", new Object[] {
        emptyCommentAndMetadata(), null}, "class A {var f;}");
    assertNull(declaration.getDocumentationComment());
    assertNull(declaration.getAbstractKeyword());
    assertNull(declaration.getExtendsClause());
    assertNull(declaration.getImplementsClause());
    assertNotNull(declaration.getClassKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    assertSizeOfList(1, declaration.getMembers());
    assertNotNull(declaration.getRightBracket());
    assertNull(declaration.getTypeParameters());
  }

  public void test_parseClassDeclaration_typeAlias_implementsC() throws Exception {
    ClassTypeAlias typeAlias = parse("parseClassDeclaration", new Object[] {
        emptyCommentAndMetadata(), null}, "class A = Object with B implements C;");
    assertNotNull(typeAlias.getKeyword());
    assertNotNull(typeAlias.getName());
    assertNull(typeAlias.getTypeParameters());
    assertNotNull(typeAlias.getWithClause());
    assertNotNull(typeAlias.getImplementsClause());
    assertNotNull(typeAlias.getImplementsClause().getKeyword());
    assertEquals(1, typeAlias.getImplementsClause().getInterfaces().size());
    assertNotNull(typeAlias.getSemicolon());
  }

  public void test_parseClassDeclaration_typeAlias_withB() throws Exception {
    ClassTypeAlias typeAlias = parse("parseClassDeclaration", new Object[] {
        emptyCommentAndMetadata(), null}, "class A = Object with B;");
    assertNotNull(typeAlias.getKeyword());
    assertNotNull(typeAlias.getName());
    assertNull(typeAlias.getTypeParameters());
    assertNotNull(typeAlias.getWithClause());
    assertNotNull(typeAlias.getWithClause().getWithKeyword());
    assertEquals(1, typeAlias.getWithClause().getMixinTypes().size());
    assertNull(typeAlias.getImplementsClause());
    assertNotNull(typeAlias.getSemicolon());
  }

  public void test_parseClassDeclaration_typeParameters() throws Exception {
    ClassDeclaration declaration = parse("parseClassDeclaration", new Object[] {
        emptyCommentAndMetadata(), null}, "class A<B> {}");
    assertNull(declaration.getDocumentationComment());
    assertNull(declaration.getAbstractKeyword());
    assertNull(declaration.getExtendsClause());
    assertNull(declaration.getImplementsClause());
    assertNotNull(declaration.getClassKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    assertSizeOfList(0, declaration.getMembers());
    assertNotNull(declaration.getRightBracket());
    assertNotNull(declaration.getTypeParameters());
    assertSizeOfList(1, declaration.getTypeParameters().getTypeParameters());
  }

  public void test_parseClassMember_constructor_withInitializers() throws Exception {
    // TODO(brianwilkerson) Test other kinds of class members: fields, getters and setters.
    ConstructorDeclaration constructor = parse(
        "parseClassMember",
        new Object[] {"C"},
        "C(_, _$, this.__) : _a = _ + _$ {}");
    assertNotNull(constructor.getBody());
    assertNotNull(constructor.getSeparator());
    assertNull(constructor.getExternalKeyword());
    assertNull(constructor.getConstKeyword());
    assertNull(constructor.getFactoryKeyword());
    assertNull(constructor.getName());
    assertNotNull(constructor.getParameters());
    assertNull(constructor.getPeriod());
    assertNotNull(constructor.getReturnType());
    assertSizeOfList(1, constructor.getInitializers());
  }

  public void test_parseClassMember_field_instance_prefixedType() throws Exception {
    FieldDeclaration field = parse("parseClassMember", new Object[] {"C"}, "p.A f;");
    assertNull(field.getDocumentationComment());
    assertSizeOfList(0, field.getMetadata());
    assertNull(field.getStaticKeyword());
    VariableDeclarationList list = field.getFields();
    assertNotNull(list);
    NodeList<VariableDeclaration> variables = list.getVariables();
    assertSizeOfList(1, variables);
    VariableDeclaration variable = variables.get(0);
    assertNotNull(variable.getName());
  }

  public void test_parseClassMember_field_namedGet() throws Exception {
    FieldDeclaration field = parse("parseClassMember", new Object[] {"C"}, "var get;");
    assertNull(field.getDocumentationComment());
    assertSizeOfList(0, field.getMetadata());
    assertNull(field.getStaticKeyword());
    VariableDeclarationList list = field.getFields();
    assertNotNull(list);
    NodeList<VariableDeclaration> variables = list.getVariables();
    assertSizeOfList(1, variables);
    VariableDeclaration variable = variables.get(0);
    assertNotNull(variable.getName());
  }

  public void test_parseClassMember_field_namedOperator() throws Exception {
    FieldDeclaration field = parse("parseClassMember", new Object[] {"C"}, "var operator;");
    assertNull(field.getDocumentationComment());
    assertSizeOfList(0, field.getMetadata());
    assertNull(field.getStaticKeyword());
    VariableDeclarationList list = field.getFields();
    assertNotNull(list);
    NodeList<VariableDeclaration> variables = list.getVariables();
    assertSizeOfList(1, variables);
    VariableDeclaration variable = variables.get(0);
    assertNotNull(variable.getName());
  }

  public void test_parseClassMember_field_namedOperator_withAssignment() throws Exception {
    FieldDeclaration field = parse("parseClassMember", new Object[] {"C"}, "var operator = (5);");
    assertNull(field.getDocumentationComment());
    assertSizeOfList(0, field.getMetadata());
    assertNull(field.getStaticKeyword());
    VariableDeclarationList list = field.getFields();
    assertNotNull(list);
    NodeList<VariableDeclaration> variables = list.getVariables();
    assertSizeOfList(1, variables);
    VariableDeclaration variable = variables.get(0);
    assertNotNull(variable.getName());
    assertNotNull(variable.getInitializer());
  }

  public void test_parseClassMember_field_namedSet() throws Exception {
    FieldDeclaration field = parse("parseClassMember", new Object[] {"C"}, "var set;");
    assertNull(field.getDocumentationComment());
    assertSizeOfList(0, field.getMetadata());
    assertNull(field.getStaticKeyword());
    VariableDeclarationList list = field.getFields();
    assertNotNull(list);
    NodeList<VariableDeclaration> variables = list.getVariables();
    assertSizeOfList(1, variables);
    VariableDeclaration variable = variables.get(0);
    assertNotNull(variable.getName());
  }

  public void test_parseClassMember_getter_void() throws Exception {
    MethodDeclaration method = parse("parseClassMember", new Object[] {"C"}, "void get g {}");
    assertNull(method.getDocumentationComment());
    assertNull(method.getExternalKeyword());
    assertNull(method.getModifierKeyword());
    assertNotNull(method.getPropertyKeyword());
    assertNotNull(method.getReturnType());
    assertNotNull(method.getName());
    assertNull(method.getOperatorKeyword());
    assertNotNull(method.getBody());
  }

  public void test_parseClassMember_method_external() throws Exception {
    MethodDeclaration method = parse("parseClassMember", new Object[] {"C"}, "external m();");
    assertNotNull(method.getBody());
    assertNull(method.getDocumentationComment());
    assertNotNull(method.getExternalKeyword());
    assertNull(method.getModifierKeyword());
    assertNotNull(method.getName());
    assertNull(method.getOperatorKeyword());
    assertNotNull(method.getParameters());
    assertNull(method.getPropertyKeyword());
    assertNull(method.getReturnType());
  }

  public void test_parseClassMember_method_external_withTypeAndArgs() throws Exception {
    MethodDeclaration method = parse(
        "parseClassMember",
        new Object[] {"C"},
        "external int m(int a);");
    assertNotNull(method.getBody());
    assertNull(method.getDocumentationComment());
    assertNotNull(method.getExternalKeyword());
    assertNull(method.getModifierKeyword());
    assertNotNull(method.getName());
    assertNull(method.getOperatorKeyword());
    assertNotNull(method.getParameters());
    assertNull(method.getPropertyKeyword());
    assertNotNull(method.getReturnType());
  }

  public void test_parseClassMember_method_get_noType() throws Exception {
    MethodDeclaration method = parse("parseClassMember", new Object[] {"C"}, "get() {}");
    assertNull(method.getDocumentationComment());
    assertNull(method.getExternalKeyword());
    assertNull(method.getModifierKeyword());
    assertNull(method.getPropertyKeyword());
    assertNull(method.getReturnType());
    assertNotNull(method.getName());
    assertNull(method.getOperatorKeyword());
    assertNotNull(method.getParameters());
    assertNotNull(method.getBody());
  }

  public void test_parseClassMember_method_get_type() throws Exception {
    MethodDeclaration method = parse("parseClassMember", new Object[] {"C"}, "int get() {}");
    assertNull(method.getDocumentationComment());
    assertNull(method.getExternalKeyword());
    assertNull(method.getModifierKeyword());
    assertNull(method.getPropertyKeyword());
    assertNotNull(method.getReturnType());
    assertNotNull(method.getName());
    assertNull(method.getOperatorKeyword());
    assertNotNull(method.getParameters());
    assertNotNull(method.getBody());
  }

  public void test_parseClassMember_method_get_void() throws Exception {
    MethodDeclaration method = parse("parseClassMember", new Object[] {"C"}, "void get() {}");
    assertNull(method.getDocumentationComment());
    assertNull(method.getExternalKeyword());
    assertNull(method.getModifierKeyword());
    assertNull(method.getPropertyKeyword());
    assertNotNull(method.getReturnType());
    assertNotNull(method.getName());
    assertNull(method.getOperatorKeyword());
    assertNotNull(method.getParameters());
    assertNotNull(method.getBody());
  }

  public void test_parseClassMember_method_operator_noType() throws Exception {
    MethodDeclaration method = parse("parseClassMember", new Object[] {"C"}, "operator() {}");
    assertNull(method.getDocumentationComment());
    assertNull(method.getExternalKeyword());
    assertNull(method.getModifierKeyword());
    assertNull(method.getPropertyKeyword());
    assertNull(method.getReturnType());
    assertNotNull(method.getName());
    assertNull(method.getOperatorKeyword());
    assertNotNull(method.getParameters());
    assertNotNull(method.getBody());
  }

  public void test_parseClassMember_method_operator_type() throws Exception {
    MethodDeclaration method = parse("parseClassMember", new Object[] {"C"}, "int operator() {}");
    assertNull(method.getDocumentationComment());
    assertNull(method.getExternalKeyword());
    assertNull(method.getModifierKeyword());
    assertNull(method.getPropertyKeyword());
    assertNotNull(method.getReturnType());
    assertNotNull(method.getName());
    assertNull(method.getOperatorKeyword());
    assertNotNull(method.getParameters());
    assertNotNull(method.getBody());
  }

  public void test_parseClassMember_method_operator_void() throws Exception {
    MethodDeclaration method = parse("parseClassMember", new Object[] {"C"}, "void operator() {}");
    assertNull(method.getDocumentationComment());
    assertNull(method.getExternalKeyword());
    assertNull(method.getModifierKeyword());
    assertNull(method.getPropertyKeyword());
    assertNotNull(method.getReturnType());
    assertNotNull(method.getName());
    assertNull(method.getOperatorKeyword());
    assertNotNull(method.getParameters());
    assertNotNull(method.getBody());
  }

  public void test_parseClassMember_method_returnType_parameterized() throws Exception {
    MethodDeclaration method = parse("parseClassMember", new Object[] {"C"}, "p.A m() {}");
    assertNull(method.getDocumentationComment());
    assertNull(method.getExternalKeyword());
    assertNull(method.getModifierKeyword());
    assertNull(method.getPropertyKeyword());
    assertNotNull(method.getReturnType());
    assertNotNull(method.getName());
    assertNull(method.getOperatorKeyword());
    assertNotNull(method.getParameters());
    assertNotNull(method.getBody());
  }

  public void test_parseClassMember_method_set_noType() throws Exception {
    MethodDeclaration method = parse("parseClassMember", new Object[] {"C"}, "set() {}");
    assertNull(method.getDocumentationComment());
    assertNull(method.getExternalKeyword());
    assertNull(method.getModifierKeyword());
    assertNull(method.getPropertyKeyword());
    assertNull(method.getReturnType());
    assertNotNull(method.getName());
    assertNull(method.getOperatorKeyword());
    assertNotNull(method.getParameters());
    assertNotNull(method.getBody());
  }

  public void test_parseClassMember_method_set_type() throws Exception {
    MethodDeclaration method = parse("parseClassMember", new Object[] {"C"}, "int set() {}");
    assertNull(method.getDocumentationComment());
    assertNull(method.getExternalKeyword());
    assertNull(method.getModifierKeyword());
    assertNull(method.getPropertyKeyword());
    assertNotNull(method.getReturnType());
    assertNotNull(method.getName());
    assertNull(method.getOperatorKeyword());
    assertNotNull(method.getParameters());
    assertNotNull(method.getBody());
  }

  public void test_parseClassMember_method_set_void() throws Exception {
    MethodDeclaration method = parse("parseClassMember", new Object[] {"C"}, "void set() {}");
    assertNull(method.getDocumentationComment());
    assertNull(method.getExternalKeyword());
    assertNull(method.getModifierKeyword());
    assertNull(method.getPropertyKeyword());
    assertNotNull(method.getReturnType());
    assertNotNull(method.getName());
    assertNull(method.getOperatorKeyword());
    assertNotNull(method.getParameters());
    assertNotNull(method.getBody());
  }

  public void test_parseClassMember_operator_index() throws Exception {
    MethodDeclaration method = parse(
        "parseClassMember",
        new Object[] {"C"},
        "int operator [](int i) {}");
    assertNull(method.getDocumentationComment());
    assertNull(method.getExternalKeyword());
    assertNull(method.getModifierKeyword());
    assertNull(method.getPropertyKeyword());
    assertNotNull(method.getReturnType());
    assertNotNull(method.getName());
    assertNotNull(method.getOperatorKeyword());
    assertNotNull(method.getParameters());
    assertNotNull(method.getBody());
  }

  public void test_parseClassMember_operator_indexAssign() throws Exception {
    MethodDeclaration method = parse(
        "parseClassMember",
        new Object[] {"C"},
        "int operator []=(int i) {}");
    assertNull(method.getDocumentationComment());
    assertNull(method.getExternalKeyword());
    assertNull(method.getModifierKeyword());
    assertNull(method.getPropertyKeyword());
    assertNotNull(method.getReturnType());
    assertNotNull(method.getName());
    assertNotNull(method.getOperatorKeyword());
    assertNotNull(method.getParameters());
    assertNotNull(method.getBody());
  }

  public void test_parseClassMember_redirectingFactory_const() throws Exception {
    ConstructorDeclaration constructor = parse(
        "parseClassMember",
        new Object[] {"C"},
        "const factory C() = B;");
    assertNull(constructor.getExternalKeyword());
    assertNotNull(constructor.getConstKeyword());
    assertNotNull(constructor.getFactoryKeyword());
    assertNotNull(constructor.getReturnType());
    assertNull(constructor.getPeriod());
    assertNull(constructor.getName());
    assertNotNull(constructor.getParameters());
    assertNotNull(constructor.getSeparator());
    assertSizeOfList(0, constructor.getInitializers());
    assertNotNull(constructor.getRedirectedConstructor());
    assertNotNull(constructor.getBody());
  }

  public void test_parseClassMember_redirectingFactory_nonConst() throws Exception {
    ConstructorDeclaration constructor = parse(
        "parseClassMember",
        new Object[] {"C"},
        "factory C() = B;");
    assertNull(constructor.getExternalKeyword());
    assertNull(constructor.getConstKeyword());
    assertNotNull(constructor.getFactoryKeyword());
    assertNotNull(constructor.getReturnType());
    assertNull(constructor.getPeriod());
    assertNull(constructor.getName());
    assertNotNull(constructor.getParameters());
    assertNotNull(constructor.getSeparator());
    assertSizeOfList(0, constructor.getInitializers());
    assertNotNull(constructor.getRedirectedConstructor());
    assertNotNull(constructor.getBody());
  }

  public void test_parseClassTypeAlias_abstract() throws Exception {
    Token classToken = tokenFromKeyword(Keyword.CLASS);
    Token abstractToken = tokenFromKeyword(Keyword.ABSTRACT);
    ClassTypeAlias classTypeAlias = parse("parseClassTypeAlias", new Object[] {
        emptyCommentAndMetadata(), abstractToken, classToken}, "A = B with C;");
    assertNotNull(classTypeAlias.getKeyword());
    assertEquals("A", classTypeAlias.getName().getName());
    assertNotNull(classTypeAlias.getEquals());
    assertNotNull(classTypeAlias.getAbstractKeyword());
    assertNotNull("B", classTypeAlias.getSuperclass().getName().getName());
    assertNotNull(classTypeAlias.getWithClause());
    assertNull(classTypeAlias.getImplementsClause());
    assertNotNull(classTypeAlias.getSemicolon());
  }

  public void test_parseClassTypeAlias_implements() throws Exception {
    Token token = tokenFromKeyword(Keyword.CLASS);
    ClassTypeAlias classTypeAlias = parse("parseClassTypeAlias", new Object[] {
        emptyCommentAndMetadata(), null, token}, "A = B with C implements D;");
    assertNotNull(classTypeAlias.getKeyword());
    assertEquals("A", classTypeAlias.getName().getName());
    assertNotNull(classTypeAlias.getEquals());
    assertNull(classTypeAlias.getAbstractKeyword());
    assertNotNull("B", classTypeAlias.getSuperclass().getName().getName());
    assertNotNull(classTypeAlias.getWithClause());
    assertNotNull(classTypeAlias.getImplementsClause());
    assertNotNull(classTypeAlias.getSemicolon());
  }

  public void test_parseClassTypeAlias_with() throws Exception {
    Token token = tokenFromKeyword(Keyword.CLASS);
    ClassTypeAlias classTypeAlias = parse("parseClassTypeAlias", new Object[] {
        emptyCommentAndMetadata(), null, token}, "A = B with C;");
    assertNotNull(classTypeAlias.getKeyword());
    assertEquals("A", classTypeAlias.getName().getName());
    assertNotNull(classTypeAlias.getEquals());
    assertNull(classTypeAlias.getAbstractKeyword());
    assertNotNull("B", classTypeAlias.getSuperclass().getName().getName());
    assertNotNull(classTypeAlias.getWithClause());
    assertNull(classTypeAlias.getImplementsClause());
    assertNotNull(classTypeAlias.getSemicolon());
  }

  public void test_parseClassTypeAlias_with_implements() throws Exception {
    Token token = tokenFromKeyword(Keyword.CLASS);
    ClassTypeAlias classTypeAlias = parse("parseClassTypeAlias", new Object[] {
        emptyCommentAndMetadata(), null, token}, "A = B with C implements D;");
    assertNotNull(classTypeAlias.getKeyword());
    assertEquals("A", classTypeAlias.getName().getName());
    assertNotNull(classTypeAlias.getEquals());
    assertNull(classTypeAlias.getAbstractKeyword());
    assertNotNull("B", classTypeAlias.getSuperclass().getName().getName());
    assertNotNull(classTypeAlias.getWithClause());
    assertNotNull(classTypeAlias.getImplementsClause());
    assertNotNull(classTypeAlias.getSemicolon());
  }

  public void test_parseCombinators_h() throws Exception {
    List<Combinator> combinators = parse("parseCombinators", "hide a;");
    assertSizeOfList(1, combinators);
    HideCombinator combinator = (HideCombinator) combinators.get(0);
    assertNotNull(combinator);
    assertNotNull(combinator.getKeyword());
    assertSizeOfList(1, combinator.getHiddenNames());
  }

  public void test_parseCombinators_hs() throws Exception {
    List<Combinator> combinators = parse("parseCombinators", "hide a show b;");
    assertSizeOfList(2, combinators);
    HideCombinator hideCombinator = (HideCombinator) combinators.get(0);
    assertNotNull(hideCombinator);
    assertNotNull(hideCombinator.getKeyword());
    assertSizeOfList(1, hideCombinator.getHiddenNames());
    ShowCombinator showCombinator = (ShowCombinator) combinators.get(1);
    assertNotNull(showCombinator);
    assertNotNull(showCombinator.getKeyword());
    assertSizeOfList(1, showCombinator.getShownNames());
  }

  public void test_parseCombinators_hshs() throws Exception {
    List<Combinator> combinators = parse("parseCombinators", "hide a show b hide c show d;");
    assertSizeOfList(4, combinators);
  }

  public void test_parseCombinators_s() throws Exception {
    List<Combinator> combinators = parse("parseCombinators", "show a;");
    assertSizeOfList(1, combinators);
    ShowCombinator combinator = (ShowCombinator) combinators.get(0);
    assertNotNull(combinator);
    assertNotNull(combinator.getKeyword());
    assertSizeOfList(1, combinator.getShownNames());
  }

  public void test_parseCommentAndMetadata_c() throws Exception {
    CommentAndMetadata commentAndMetadata = parse("parseCommentAndMetadata", "/** 1 */ void");
    assertNotNull(commentAndMetadata.getComment());
    assertSizeOfList(0, commentAndMetadata.getMetadata());
  }

  public void test_parseCommentAndMetadata_cmc() throws Exception {
    CommentAndMetadata commentAndMetadata = parse(
        "parseCommentAndMetadata",
        "/** 1 */ @A /** 2 */ void");
    assertNotNull(commentAndMetadata.getComment());
    assertSizeOfList(1, commentAndMetadata.getMetadata());
  }

  public void test_parseCommentAndMetadata_cmcm() throws Exception {
    CommentAndMetadata commentAndMetadata = parse(
        "parseCommentAndMetadata",
        "/** 1 */ @A /** 2 */ @B void");
    assertNotNull(commentAndMetadata.getComment());
    assertSizeOfList(2, commentAndMetadata.getMetadata());
  }

  public void test_parseCommentAndMetadata_cmm() throws Exception {
    CommentAndMetadata commentAndMetadata = parse("parseCommentAndMetadata", "/** 1 */ @A @B void");
    assertNotNull(commentAndMetadata.getComment());
    assertSizeOfList(2, commentAndMetadata.getMetadata());
  }

  public void test_parseCommentAndMetadata_m() throws Exception {
    CommentAndMetadata commentAndMetadata = parse("parseCommentAndMetadata", "@A void");
    assertNull(commentAndMetadata.getComment());
    assertSizeOfList(1, commentAndMetadata.getMetadata());
  }

  public void test_parseCommentAndMetadata_mcm() throws Exception {
    CommentAndMetadata commentAndMetadata = parse("parseCommentAndMetadata", "@A /** 1 */ @B void");
    assertNotNull(commentAndMetadata.getComment());
    assertSizeOfList(2, commentAndMetadata.getMetadata());
  }

  public void test_parseCommentAndMetadata_mcmc() throws Exception {
    CommentAndMetadata commentAndMetadata = parse(
        "parseCommentAndMetadata",
        "@A /** 1 */ @B /** 2 */ void");
    assertNotNull(commentAndMetadata.getComment());
    assertSizeOfList(2, commentAndMetadata.getMetadata());
  }

  public void test_parseCommentAndMetadata_mm() throws Exception {
    CommentAndMetadata commentAndMetadata = parse("parseCommentAndMetadata", "@A @B(x) void");
    assertNull(commentAndMetadata.getComment());
    assertSizeOfList(2, commentAndMetadata.getMetadata());
  }

  public void test_parseCommentAndMetadata_none() throws Exception {
    CommentAndMetadata commentAndMetadata = parse("parseCommentAndMetadata", "void");
    assertNull(commentAndMetadata.getComment());
    assertSizeOfList(0, commentAndMetadata.getMetadata());
  }

  public void test_parseCommentAndMetadata_singleLine() throws Exception {
    CommentAndMetadata commentAndMetadata = parse("parseCommentAndMetadata", createSource(//
        "/// 1",
        "/// 2",
        "void"));
    assertNotNull(commentAndMetadata.getComment());
    assertSizeOfList(0, commentAndMetadata.getMetadata());
  }

  public void test_parseCommentReference_new_prefixed() throws Exception {
    CommentReference reference = parse("parseCommentReference", new Object[] {"new a.b", 7}, "");
    PrefixedIdentifier prefixedIdentifier = assertInstanceOf(
        PrefixedIdentifier.class,
        reference.getIdentifier());
    SimpleIdentifier prefix = prefixedIdentifier.getPrefix();
    assertNotNull(prefix.getToken());
    assertEquals("a", prefix.getName());
    assertEquals(11, prefix.getOffset());
    assertNotNull(prefixedIdentifier.getPeriod());
    SimpleIdentifier identifier = prefixedIdentifier.getIdentifier();
    assertNotNull(identifier.getToken());
    assertEquals("b", identifier.getName());
    assertEquals(13, identifier.getOffset());
  }

  public void test_parseCommentReference_new_simple() throws Exception {
    CommentReference reference = parse("parseCommentReference", new Object[] {"new a", 5}, "");
    SimpleIdentifier identifier = assertInstanceOf(
        SimpleIdentifier.class,
        reference.getIdentifier());
    assertNotNull(identifier.getToken());
    assertEquals("a", identifier.getName());
    assertEquals(9, identifier.getOffset());
  }

  public void test_parseCommentReference_prefixed() throws Exception {
    CommentReference reference = parse("parseCommentReference", new Object[] {"a.b", 7}, "");
    PrefixedIdentifier prefixedIdentifier = assertInstanceOf(
        PrefixedIdentifier.class,
        reference.getIdentifier());
    SimpleIdentifier prefix = prefixedIdentifier.getPrefix();
    assertNotNull(prefix.getToken());
    assertEquals("a", prefix.getName());
    assertEquals(7, prefix.getOffset());
    assertNotNull(prefixedIdentifier.getPeriod());
    SimpleIdentifier identifier = prefixedIdentifier.getIdentifier();
    assertNotNull(identifier.getToken());
    assertEquals("b", identifier.getName());
    assertEquals(9, identifier.getOffset());
  }

  public void test_parseCommentReference_simple() throws Exception {
    CommentReference reference = parse("parseCommentReference", new Object[] {"a", 5}, "");
    SimpleIdentifier identifier = assertInstanceOf(
        SimpleIdentifier.class,
        reference.getIdentifier());
    assertNotNull(identifier.getToken());
    assertEquals("a", identifier.getName());
    assertEquals(5, identifier.getOffset());
  }

  public void test_parseCommentReference_synthetic() throws Exception {
    CommentReference reference = parse("parseCommentReference", new Object[] {"", 5}, "");
    SimpleIdentifier identifier = assertInstanceOf(
        SimpleIdentifier.class,
        reference.getIdentifier());
    assertNotNull(identifier);
    assertTrue(identifier.isSynthetic());
    assertNotNull(identifier.getToken());
    assertEquals("", identifier.getName());
    assertEquals(5, identifier.getOffset());
  }

  public void test_parseCommentReferences_multiLine() throws Exception {
    Token[] tokens = new Token[] {new StringToken(
        TokenType.MULTI_LINE_COMMENT,
        "/** xxx [a] yyy [b] zzz */",
        3),};
    List<CommentReference> references = parse("parseCommentReferences", new Object[] {tokens}, "");
    assertSizeOfList(2, references);
    CommentReference reference = references.get(0);
    assertNotNull(reference);
    assertNotNull(reference.getIdentifier());
    assertEquals(12, reference.getOffset());

    reference = references.get(1);
    assertNotNull(reference);
    assertNotNull(reference.getIdentifier());
    assertEquals(20, reference.getOffset());
  }

  public void test_parseCommentReferences_notClosed_noIdentifier() throws Exception {
    Token[] tokens = new Token[] {new StringToken(
        TokenType.MULTI_LINE_COMMENT,
        "/** [ some text",
        5),};
    List<CommentReference> references = parse("parseCommentReferences", new Object[] {tokens}, "");
    assertSizeOfList(1, references);
    CommentReference reference = references.get(0);
    assertNotNull(reference);
    assertNotNull(reference.getIdentifier());
    assertTrue(reference.getIdentifier().isSynthetic());
    assertEquals("", reference.getIdentifier().getName());
  }

  public void test_parseCommentReferences_notClosed_withIdentifier() throws Exception {
    Token[] tokens = new Token[] {new StringToken(
        TokenType.MULTI_LINE_COMMENT,
        "/** [namePrefix some text",
        5),};
    List<CommentReference> references = parse("parseCommentReferences", new Object[] {tokens}, "");
    assertSizeOfList(1, references);
    CommentReference reference = references.get(0);
    assertNotNull(reference);
    assertNotNull(reference.getIdentifier());
    assertFalse(reference.getIdentifier().isSynthetic());
    assertEquals("namePrefix", reference.getIdentifier().getName());
  }

  public void test_parseCommentReferences_singleLine() throws Exception {
    Token[] tokens = new Token[] {
        new StringToken(TokenType.SINGLE_LINE_COMMENT, "/// xxx [a] yyy [b] zzz", 3),
        new StringToken(TokenType.SINGLE_LINE_COMMENT, "/// x [c]", 28),};
    List<CommentReference> references = parse("parseCommentReferences", new Object[] {tokens}, "");
    assertSizeOfList(3, references);
    CommentReference reference = references.get(0);
    assertNotNull(reference);
    assertNotNull(reference.getIdentifier());
    assertEquals(12, reference.getOffset());

    reference = references.get(1);
    assertNotNull(reference);
    assertNotNull(reference.getIdentifier());
    assertEquals(20, reference.getOffset());

    reference = references.get(2);
    assertNotNull(reference);
    assertNotNull(reference.getIdentifier());
    assertEquals(35, reference.getOffset());
  }

  public void test_parseCommentReferences_skipCodeBlock_bracketed() throws Exception {
    Token[] tokens = new Token[] {new StringToken(
        TokenType.MULTI_LINE_COMMENT,
        "/** [:xxx [a] yyy:] [b] zzz */",
        3),};
    List<CommentReference> references = parse("parseCommentReferences", new Object[] {tokens}, "");
    assertSizeOfList(1, references);
    CommentReference reference = references.get(0);
    assertNotNull(reference);
    assertNotNull(reference.getIdentifier());
    assertEquals(24, reference.getOffset());
  }

  public void test_parseCommentReferences_skipCodeBlock_spaces() throws Exception {
    Token[] tokens = new Token[] {new StringToken(
        TokenType.MULTI_LINE_COMMENT,
        "/**\n *     a[i]\n * xxx [i] zzz\n */",
        3),};
    List<CommentReference> references = parse("parseCommentReferences", new Object[] {tokens}, "");
    assertSizeOfList(1, references);
    CommentReference reference = references.get(0);
    assertNotNull(reference);
    assertNotNull(reference.getIdentifier());
    assertEquals(27, reference.getOffset());
  }

  public void test_parseCommentReferences_skipLinkDefinition() throws Exception {
    Token[] tokens = new Token[] {new StringToken(
        TokenType.MULTI_LINE_COMMENT,
        "/** [a]: http://www.google.com (Google) [b] zzz */",
        3),};
    List<CommentReference> references = parse("parseCommentReferences", new Object[] {tokens}, "");
    assertSizeOfList(1, references);
    CommentReference reference = references.get(0);
    assertNotNull(reference);
    assertNotNull(reference.getIdentifier());
    assertEquals(44, reference.getOffset());
  }

  public void test_parseCommentReferences_skipLinked() throws Exception {
    Token[] tokens = new Token[] {new StringToken(
        TokenType.MULTI_LINE_COMMENT,
        "/** [a](http://www.google.com) [b] zzz */",
        3),};
    List<CommentReference> references = parse("parseCommentReferences", new Object[] {tokens}, "");
    assertSizeOfList(1, references);
    CommentReference reference = references.get(0);
    assertNotNull(reference);
    assertNotNull(reference.getIdentifier());
    assertEquals(35, reference.getOffset());
  }

  public void test_parseCommentReferences_skipReferenceLink() throws Exception {
    Token[] tokens = new Token[] {new StringToken(
        TokenType.MULTI_LINE_COMMENT,
        "/** [a][c] [b] zzz */",
        3),};
    List<CommentReference> references = parse("parseCommentReferences", new Object[] {tokens}, "");
    assertSizeOfList(1, references);
    CommentReference reference = references.get(0);
    assertNotNull(reference);
    assertNotNull(reference.getIdentifier());
    assertEquals(15, reference.getOffset());
  }

  public void test_parseCompilationUnit_abstractAsPrefix_parameterized() throws Exception {
    CompilationUnit unit = parse(
        "parseCompilationUnit",
        "abstract<dynamic> _abstract = new abstract.A();");
    assertNull(unit.getScriptTag());
    assertSizeOfList(0, unit.getDirectives());
    assertSizeOfList(1, unit.getDeclarations());
  }

  public void test_parseCompilationUnit_builtIn_asFunctionName() throws Exception {
    parse("parseCompilationUnit", "abstract(x) => 0;");
    parse("parseCompilationUnit", "as(x) => 0;");
    parse("parseCompilationUnit", "dynamic(x) => 0;");
    parse("parseCompilationUnit", "export(x) => 0;");
    parse("parseCompilationUnit", "external(x) => 0;");
    parse("parseCompilationUnit", "factory(x) => 0;");
    parse("parseCompilationUnit", "get(x) => 0;");
    parse("parseCompilationUnit", "implements(x) => 0;");
    parse("parseCompilationUnit", "import(x) => 0;");
    parse("parseCompilationUnit", "library(x) => 0;");
    parse("parseCompilationUnit", "operator(x) => 0;");
    parse("parseCompilationUnit", "part(x) => 0;");
    parse("parseCompilationUnit", "set(x) => 0;");
    parse("parseCompilationUnit", "static(x) => 0;");
    parse("parseCompilationUnit", "typedef(x) => 0;");
  }

  public void test_parseCompilationUnit_directives_multiple() throws Exception {
    CompilationUnit unit = parse("parseCompilationUnit", "library l;\npart 'a.dart';");
    assertNull(unit.getScriptTag());
    assertSizeOfList(2, unit.getDirectives());
    assertSizeOfList(0, unit.getDeclarations());
  }

  public void test_parseCompilationUnit_directives_single() throws Exception {
    CompilationUnit unit = parse("parseCompilationUnit", "library l;");
    assertNull(unit.getScriptTag());
    assertSizeOfList(1, unit.getDirectives());
    assertSizeOfList(0, unit.getDeclarations());
  }

  public void test_parseCompilationUnit_empty() throws Exception {
    CompilationUnit unit = parse("parseCompilationUnit", "");
    assertNull(unit.getScriptTag());
    assertSizeOfList(0, unit.getDirectives());
    assertSizeOfList(0, unit.getDeclarations());
  }

  public void test_parseCompilationUnit_exportAsPrefix() throws Exception {
    CompilationUnit unit = parse("parseCompilationUnit", "export.A _export = new export.A();");
    assertNull(unit.getScriptTag());
    assertSizeOfList(0, unit.getDirectives());
    assertSizeOfList(1, unit.getDeclarations());
  }

  public void test_parseCompilationUnit_exportAsPrefix_parameterized() throws Exception {
    CompilationUnit unit = parse(
        "parseCompilationUnit",
        "export<dynamic> _export = new export.A();");
    assertNull(unit.getScriptTag());
    assertSizeOfList(0, unit.getDirectives());
    assertSizeOfList(1, unit.getDeclarations());
  }

  public void test_parseCompilationUnit_operatorAsPrefix_parameterized() throws Exception {
    CompilationUnit unit = parse(
        "parseCompilationUnit",
        "operator<dynamic> _operator = new operator.A();");
    assertNull(unit.getScriptTag());
    assertSizeOfList(0, unit.getDirectives());
    assertSizeOfList(1, unit.getDeclarations());
  }

  public void test_parseCompilationUnit_script() throws Exception {
    CompilationUnit unit = parse("parseCompilationUnit", "#! /bin/dart");
    assertNotNull(unit.getScriptTag());
    assertSizeOfList(0, unit.getDirectives());
    assertSizeOfList(0, unit.getDeclarations());
  }

  public void test_parseCompilationUnit_skipFunctionBody_withInterpolation() throws Exception {
    ParserTestCase.parseFunctionBodies = false;
    CompilationUnit unit = parse("parseCompilationUnit", "f() { '${n}'; }");
    assertNull(unit.getScriptTag());
    assertSizeOfList(1, unit.getDeclarations());
  }

  public void test_parseCompilationUnit_topLevelDeclaration() throws Exception {
    CompilationUnit unit = parse("parseCompilationUnit", "class A {}");
    assertNull(unit.getScriptTag());
    assertSizeOfList(0, unit.getDirectives());
    assertSizeOfList(1, unit.getDeclarations());
  }

  public void test_parseCompilationUnit_typedefAsPrefix() throws Exception {
    CompilationUnit unit = parse("parseCompilationUnit", "typedef.A _typedef = new typedef.A();");
    assertNull(unit.getScriptTag());
    assertSizeOfList(0, unit.getDirectives());
    assertSizeOfList(1, unit.getDeclarations());
  }

  public void test_parseCompilationUnitMember_abstractAsPrefix() throws Exception {
    TopLevelVariableDeclaration declaration = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "abstract.A _abstract = new abstract.A();");
    assertNotNull(declaration.getSemicolon());
    assertNotNull(declaration.getVariables());
  }

  public void test_parseCompilationUnitMember_class() throws Exception {
    ClassDeclaration declaration = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "class A {}");
    assertEquals("A", declaration.getName().getName());
    assertSizeOfList(0, declaration.getMembers());
  }

  public void test_parseCompilationUnitMember_classTypeAlias() throws Exception {
    ClassTypeAlias alias = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "abstract class A = B with C;");
    assertEquals("A", alias.getName().getName());
    assertNotNull(alias.getAbstractKeyword());
  }

  public void test_parseCompilationUnitMember_constVariable() throws Exception {
    TopLevelVariableDeclaration declaration = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "const int x = 0;");
    assertNotNull(declaration.getSemicolon());
    assertNotNull(declaration.getVariables());
  }

  public void test_parseCompilationUnitMember_finalVariable() throws Exception {
    TopLevelVariableDeclaration declaration = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "final x = 0;");
    assertNotNull(declaration.getSemicolon());
    assertNotNull(declaration.getVariables());
  }

  public void test_parseCompilationUnitMember_function_external_noType() throws Exception {
    FunctionDeclaration declaration = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "external f();");
    assertNotNull(declaration.getExternalKeyword());
    assertNotNull(declaration.getFunctionExpression());
    assertNull(declaration.getPropertyKeyword());
  }

  public void test_parseCompilationUnitMember_function_external_type() throws Exception {
    FunctionDeclaration declaration = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "external int f();");
    assertNotNull(declaration.getExternalKeyword());
    assertNotNull(declaration.getFunctionExpression());
    assertNull(declaration.getPropertyKeyword());
  }

  public void test_parseCompilationUnitMember_function_noType() throws Exception {
    FunctionDeclaration declaration = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "f() {}");
    assertNotNull(declaration.getFunctionExpression());
    assertNull(declaration.getPropertyKeyword());
  }

  public void test_parseCompilationUnitMember_function_type() throws Exception {
    FunctionDeclaration declaration = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "int f() {}");
    assertNotNull(declaration.getFunctionExpression());
    assertNull(declaration.getPropertyKeyword());
  }

  public void test_parseCompilationUnitMember_function_void() throws Exception {
    FunctionDeclaration declaration = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "void f() {}");
    assertNotNull(declaration.getReturnType());
  }

  public void test_parseCompilationUnitMember_getter_external_noType() throws Exception {
    FunctionDeclaration declaration = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "external get p;");
    assertNotNull(declaration.getExternalKeyword());
    assertNotNull(declaration.getFunctionExpression());
    assertNotNull(declaration.getPropertyKeyword());
  }

  public void test_parseCompilationUnitMember_getter_external_type() throws Exception {
    FunctionDeclaration declaration = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "external int get p;");
    assertNotNull(declaration.getExternalKeyword());
    assertNotNull(declaration.getFunctionExpression());
    assertNotNull(declaration.getPropertyKeyword());
  }

  public void test_parseCompilationUnitMember_getter_noType() throws Exception {
    FunctionDeclaration declaration = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "get p => 0;");
    assertNotNull(declaration.getFunctionExpression());
    assertNotNull(declaration.getPropertyKeyword());
  }

  public void test_parseCompilationUnitMember_getter_type() throws Exception {
    FunctionDeclaration declaration = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "int get p => 0;");
    assertNotNull(declaration.getFunctionExpression());
    assertNotNull(declaration.getPropertyKeyword());
  }

  public void test_parseCompilationUnitMember_setter_external_noType() throws Exception {
    FunctionDeclaration declaration = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "external set p(v);");
    assertNotNull(declaration.getExternalKeyword());
    assertNotNull(declaration.getFunctionExpression());
    assertNotNull(declaration.getPropertyKeyword());
  }

  public void test_parseCompilationUnitMember_setter_external_type() throws Exception {
    FunctionDeclaration declaration = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "external void set p(int v);");
    assertNotNull(declaration.getExternalKeyword());
    assertNotNull(declaration.getFunctionExpression());
    assertNotNull(declaration.getPropertyKeyword());
  }

  public void test_parseCompilationUnitMember_setter_noType() throws Exception {
    FunctionDeclaration declaration = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "set p(v) {}");
    assertNotNull(declaration.getFunctionExpression());
    assertNotNull(declaration.getPropertyKeyword());
  }

  public void test_parseCompilationUnitMember_setter_type() throws Exception {
    FunctionDeclaration declaration = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "void set p(int v) {}");
    assertNotNull(declaration.getFunctionExpression());
    assertNotNull(declaration.getPropertyKeyword());
    assertNotNull(declaration.getReturnType());
  }

  public void test_parseCompilationUnitMember_typeAlias_abstract() throws Exception {
    ClassTypeAlias typeAlias = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "abstract class C = S with M;");
    assertNotNull(typeAlias.getKeyword());
    assertEquals("C", typeAlias.getName().getName());
    assertNull(typeAlias.getTypeParameters());
    assertNotNull(typeAlias.getEquals());
    assertNotNull(typeAlias.getAbstractKeyword());
    assertEquals("S", typeAlias.getSuperclass().getName().getName());
    assertNotNull(typeAlias.getWithClause());
    assertNull(typeAlias.getImplementsClause());
    assertNotNull(typeAlias.getSemicolon());
  }

  public void test_parseCompilationUnitMember_typeAlias_generic() throws Exception {
    ClassTypeAlias typeAlias = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "class C<E> = S<E> with M<E> implements I<E>;");
    assertNotNull(typeAlias.getKeyword());
    assertEquals("C", typeAlias.getName().getName());
    assertSizeOfList(1, typeAlias.getTypeParameters().getTypeParameters());
    assertNotNull(typeAlias.getEquals());
    assertNull(typeAlias.getAbstractKeyword());
    assertEquals("S", typeAlias.getSuperclass().getName().getName());
    assertNotNull(typeAlias.getWithClause());
    assertNotNull(typeAlias.getImplementsClause());
    assertNotNull(typeAlias.getSemicolon());
  }

  public void test_parseCompilationUnitMember_typeAlias_implements() throws Exception {
    ClassTypeAlias typeAlias = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "class C = S with M implements I;");
    assertNotNull(typeAlias.getKeyword());
    assertEquals("C", typeAlias.getName().getName());
    assertNull(typeAlias.getTypeParameters());
    assertNotNull(typeAlias.getEquals());
    assertNull(typeAlias.getAbstractKeyword());
    assertEquals("S", typeAlias.getSuperclass().getName().getName());
    assertNotNull(typeAlias.getWithClause());
    assertNotNull(typeAlias.getImplementsClause());
    assertNotNull(typeAlias.getSemicolon());
  }

  public void test_parseCompilationUnitMember_typeAlias_noImplements() throws Exception {
    ClassTypeAlias typeAlias = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "class C = S with M;");
    assertNotNull(typeAlias.getKeyword());
    assertEquals("C", typeAlias.getName().getName());
    assertNull(typeAlias.getTypeParameters());
    assertNotNull(typeAlias.getEquals());
    assertNull(typeAlias.getAbstractKeyword());
    assertEquals("S", typeAlias.getSuperclass().getName().getName());
    assertNotNull(typeAlias.getWithClause());
    assertNull(typeAlias.getImplementsClause());
    assertNotNull(typeAlias.getSemicolon());
  }

  public void test_parseCompilationUnitMember_typedef() throws Exception {
    FunctionTypeAlias typeAlias = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "typedef F();");
    assertEquals("F", typeAlias.getName().getName());
    assertSizeOfList(0, typeAlias.getParameters().getParameters());
  }

  public void test_parseCompilationUnitMember_variable() throws Exception {
    TopLevelVariableDeclaration declaration = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "var x = 0;");
    assertNotNull(declaration.getSemicolon());
    assertNotNull(declaration.getVariables());
  }

  public void test_parseCompilationUnitMember_variableGet() throws Exception {
    TopLevelVariableDeclaration declaration = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "String get = null;");
    assertNotNull(declaration.getSemicolon());
    assertNotNull(declaration.getVariables());
  }

  public void test_parseCompilationUnitMember_variableSet() throws Exception {
    TopLevelVariableDeclaration declaration = parse(
        "parseCompilationUnitMember",
        new Object[] {emptyCommentAndMetadata()},
        "String set = null;");
    assertNotNull(declaration.getSemicolon());
    assertNotNull(declaration.getVariables());
  }

  public void test_parseConditionalExpression() throws Exception {
    ConditionalExpression expression = parse("parseConditionalExpression", "x ? y : z");
    assertNotNull(expression.getCondition());
    assertNotNull(expression.getQuestion());
    assertNotNull(expression.getThenExpression());
    assertNotNull(expression.getColon());
    assertNotNull(expression.getElseExpression());
  }

  public void test_parseConstExpression_instanceCreation() throws Exception {
    InstanceCreationExpression expression = parse("parseConstExpression", "const A()");
    assertNotNull(expression.getKeyword());
    ConstructorName name = expression.getConstructorName();
    assertNotNull(name);
    assertNotNull(name.getType());
    assertNull(name.getPeriod());
    assertNull(name.getName());
    assertNotNull(expression.getArgumentList());
  }

  public void test_parseConstExpression_listLiteral_typed() throws Exception {
    ListLiteral literal = parse("parseConstExpression", "const <A> []");
    assertNotNull(literal.getConstKeyword());
    assertNotNull(literal.getTypeArguments());
    assertNotNull(literal.getLeftBracket());
    assertSizeOfList(0, literal.getElements());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseConstExpression_listLiteral_untyped() throws Exception {
    ListLiteral literal = parse("parseConstExpression", "const []");
    assertNotNull(literal.getConstKeyword());
    assertNull(literal.getTypeArguments());
    assertNotNull(literal.getLeftBracket());
    assertSizeOfList(0, literal.getElements());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseConstExpression_mapLiteral_typed() throws Exception {
    MapLiteral literal = parse("parseConstExpression", "const <A, B> {}");
    assertNotNull(literal.getLeftBracket());
    assertSizeOfList(0, literal.getEntries());
    assertNotNull(literal.getRightBracket());
    assertNotNull(literal.getTypeArguments());
  }

  public void test_parseConstExpression_mapLiteral_untyped() throws Exception {
    MapLiteral literal = parse("parseConstExpression", "const {}");
    assertNotNull(literal.getLeftBracket());
    assertSizeOfList(0, literal.getEntries());
    assertNotNull(literal.getRightBracket());
    assertNull(literal.getTypeArguments());
  }

  public void test_parseConstructor() throws Exception {
    // TODO(brianwilkerson) Implement tests for this method.
//    parse("parseConstructor", new Class[] {Parser.CommentAndMetadata.class,
//        Token.class, Token.class, SimpleIdentifier.class, Token.class,
//        SimpleIdentifier.class, FormalParameterList.class}, new Object[] {emptyCommentAndMetadata(),
//        null, null, null, null, null, null}, "");
  }

  public void test_parseConstructor_with_pseudo_function_literal() throws Exception {
    // "(b) {}" should not be misinterpreted as a function literal even though it looks like one.
    ClassMember classMember = parse("parseClassMember", new Object[] {"C"}, "C() : a = (b) {}");
    assertInstanceOf(ConstructorDeclaration.class, classMember);
    ConstructorDeclaration constructor = (ConstructorDeclaration) classMember;
    NodeList<ConstructorInitializer> initializers = constructor.getInitializers();
    assertSizeOfList(1, initializers);
    ConstructorInitializer initializer = initializers.get(0);
    assertInstanceOf(ConstructorFieldInitializer.class, initializer);
    assertInstanceOf(
        ParenthesizedExpression.class,
        ((ConstructorFieldInitializer) initializer).getExpression());
    assertInstanceOf(BlockFunctionBody.class, constructor.getBody());
  }

  public void test_parseConstructorFieldInitializer_qualified() throws Exception {
    ConstructorFieldInitializer invocation = parse("parseConstructorFieldInitializer", "this.a = b");
    assertNotNull(invocation.getEquals());
    assertNotNull(invocation.getExpression());
    assertNotNull(invocation.getFieldName());
    assertNotNull(invocation.getKeyword());
    assertNotNull(invocation.getPeriod());
  }

  public void test_parseConstructorFieldInitializer_unqualified() throws Exception {
    ConstructorFieldInitializer invocation = parse("parseConstructorFieldInitializer", "a = b");
    assertNotNull(invocation.getEquals());
    assertNotNull(invocation.getExpression());
    assertNotNull(invocation.getFieldName());
    assertNull(invocation.getKeyword());
    assertNull(invocation.getPeriod());
  }

  public void test_parseConstructorName_named_noPrefix() throws Exception {
    ConstructorName name = parse("parseConstructorName", "A.n;");
    assertNotNull(name.getType());
    assertNull(name.getPeriod());
    assertNull(name.getName());
  }

  public void test_parseConstructorName_named_prefixed() throws Exception {
    ConstructorName name = parse("parseConstructorName", "p.A.n;");
    assertNotNull(name.getType());
    assertNotNull(name.getPeriod());
    assertNotNull(name.getName());
  }

  public void test_parseConstructorName_unnamed_noPrefix() throws Exception {
    ConstructorName name = parse("parseConstructorName", "A;");
    assertNotNull(name.getType());
    assertNull(name.getPeriod());
    assertNull(name.getName());
  }

  public void test_parseConstructorName_unnamed_prefixed() throws Exception {
    ConstructorName name = parse("parseConstructorName", "p.A;");
    assertNotNull(name.getType());
    assertNull(name.getPeriod());
    assertNull(name.getName());
  }

  public void test_parseContinueStatement_label() throws Exception {
    ContinueStatement statement = parse(
        "parseContinueStatement",
        "continue foo;",
        ParserErrorCode.CONTINUE_OUTSIDE_OF_LOOP);
    assertNotNull(statement.getKeyword());
    assertNotNull(statement.getLabel());
    assertNotNull(statement.getSemicolon());
  }

  public void test_parseContinueStatement_noLabel() throws Exception {
    ContinueStatement statement = parse(
        "parseContinueStatement",
        "continue;",
        ParserErrorCode.CONTINUE_OUTSIDE_OF_LOOP);
    assertNotNull(statement.getKeyword());
    assertNull(statement.getLabel());
    assertNotNull(statement.getSemicolon());
  }

  public void test_parseDirective_export() throws Exception {
    ExportDirective directive = parse(
        "parseDirective",
        new Object[] {emptyCommentAndMetadata()},
        "export 'lib/lib.dart';");
    assertNotNull(directive.getKeyword());
    assertNotNull(directive.getUri());
    assertSizeOfList(0, directive.getCombinators());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseDirective_import() throws Exception {
    ImportDirective directive = parse(
        "parseDirective",
        new Object[] {emptyCommentAndMetadata()},
        "import 'lib/lib.dart';");
    assertNotNull(directive.getKeyword());
    assertNotNull(directive.getUri());
    assertNull(directive.getAsToken());
    assertNull(directive.getPrefix());
    assertSizeOfList(0, directive.getCombinators());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseDirective_library() throws Exception {
    LibraryDirective directive = parse(
        "parseDirective",
        new Object[] {emptyCommentAndMetadata()},
        "library l;");
    assertNotNull(directive.getLibraryToken());
    assertNotNull(directive.getName());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseDirective_part() throws Exception {
    PartDirective directive = parse(
        "parseDirective",
        new Object[] {emptyCommentAndMetadata()},
        "part 'lib/lib.dart';");
    assertNotNull(directive.getPartToken());
    assertNotNull(directive.getUri());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseDirective_partOf() throws Exception {
    PartOfDirective directive = parse(
        "parseDirective",
        new Object[] {emptyCommentAndMetadata()},
        "part of l;");
    assertNotNull(directive.getPartToken());
    assertNotNull(directive.getOfToken());
    assertNotNull(directive.getLibraryName());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseDirectives_complete() throws Exception {
    CompilationUnit unit = parseDirectives("#! /bin/dart\nlibrary l;\nclass A {}");
    assertNotNull(unit.getScriptTag());
    assertSizeOfList(1, unit.getDirectives());
  }

  public void test_parseDirectives_empty() throws Exception {
    CompilationUnit unit = parseDirectives("");
    assertNull(unit.getScriptTag());
    assertSizeOfList(0, unit.getDirectives());
  }

  public void test_parseDirectives_mixed() throws Exception {
    CompilationUnit unit = parseDirectives("library l; class A {} part 'foo.dart';");
    assertNull(unit.getScriptTag());
    assertSizeOfList(1, unit.getDirectives());
  }

  public void test_parseDirectives_multiple() throws Exception {
    CompilationUnit unit = parseDirectives("library l;\npart 'a.dart';");
    assertNull(unit.getScriptTag());
    assertSizeOfList(2, unit.getDirectives());
  }

  public void test_parseDirectives_script() throws Exception {
    CompilationUnit unit = parseDirectives("#! /bin/dart");
    assertNotNull(unit.getScriptTag());
    assertSizeOfList(0, unit.getDirectives());
  }

  public void test_parseDirectives_single() throws Exception {
    CompilationUnit unit = parseDirectives("library l;");
    assertNull(unit.getScriptTag());
    assertSizeOfList(1, unit.getDirectives());
  }

  public void test_parseDirectives_topLevelDeclaration() throws Exception {
    CompilationUnit unit = parseDirectives("class A {}");
    assertNull(unit.getScriptTag());
    assertSizeOfList(0, unit.getDirectives());
  }

  public void test_parseDocumentationComment_block() throws Exception {
    Comment comment = parse("parseDocumentationComment", "/** */ class");
    assertFalse(comment.isBlock());
    assertTrue(comment.isDocumentation());
    assertFalse(comment.isEndOfLine());
  }

  public void test_parseDocumentationComment_block_withReference() throws Exception {
    Comment comment = parse("parseDocumentationComment", "/** [a] */ class");
    assertFalse(comment.isBlock());
    assertTrue(comment.isDocumentation());
    assertFalse(comment.isEndOfLine());
    NodeList<CommentReference> references = comment.getReferences();
    assertSizeOfList(1, references);
    CommentReference reference = references.get(0);
    assertNotNull(reference);
    assertEquals(5, reference.getOffset());
  }

  public void test_parseDocumentationComment_endOfLine() throws Exception {
    Comment comment = parse("parseDocumentationComment", "/// \n/// \n class");
    assertFalse(comment.isBlock());
    assertTrue(comment.isDocumentation());
    assertFalse(comment.isEndOfLine());
  }

  public void test_parseDoStatement() throws Exception {
    DoStatement statement = parse("parseDoStatement", "do {} while (x);");
    assertNotNull(statement.getDoKeyword());
    assertNotNull(statement.getBody());
    assertNotNull(statement.getWhileKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNotNull(statement.getCondition());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getSemicolon());
  }

  public void test_parseEmptyStatement() throws Exception {
    EmptyStatement statement = parse("parseEmptyStatement", ";");
    assertNotNull(statement.getSemicolon());
  }

  public void test_parseEnumDeclaration_one() throws Exception {
    EnumDeclaration declaration = parse(
        "parseEnumDeclaration",
        new Object[] {emptyCommentAndMetadata()},
        "enum E {ONE}");
    assertNull(declaration.getDocumentationComment());
    assertNotNull(declaration.getKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    assertSizeOfList(1, declaration.getConstants());
    assertNotNull(declaration.getRightBracket());
  }

  public void test_parseEnumDeclaration_trailingComma() throws Exception {
    EnumDeclaration declaration = parse(
        "parseEnumDeclaration",
        new Object[] {emptyCommentAndMetadata()},
        "enum E {ONE,}");
    assertNull(declaration.getDocumentationComment());
    assertNotNull(declaration.getKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    assertSizeOfList(1, declaration.getConstants());
    assertNotNull(declaration.getRightBracket());
  }

  public void test_parseEnumDeclaration_two() throws Exception {
    EnumDeclaration declaration = parse(
        "parseEnumDeclaration",
        new Object[] {emptyCommentAndMetadata()},
        "enum E {ONE, TWO}");
    assertNull(declaration.getDocumentationComment());
    assertNotNull(declaration.getKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    assertSizeOfList(2, declaration.getConstants());
    assertNotNull(declaration.getRightBracket());
  }

  public void test_parseEqualityExpression_normal() throws Exception {
    BinaryExpression expression = parse("parseEqualityExpression", "x == y");
    assertNotNull(expression.getLeftOperand());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.EQ_EQ, expression.getOperator().getType());
    assertNotNull(expression.getRightOperand());
  }

  public void test_parseEqualityExpression_super() throws Exception {
    BinaryExpression expression = parse("parseEqualityExpression", "super == y");
    assertInstanceOf(SuperExpression.class, expression.getLeftOperand());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.EQ_EQ, expression.getOperator().getType());
    assertNotNull(expression.getRightOperand());
  }

  public void test_parseExportDirective_hide() throws Exception {
    ExportDirective directive = parse(
        "parseExportDirective",
        new Object[] {emptyCommentAndMetadata()},
        "export 'lib/lib.dart' hide A, B;");
    assertNotNull(directive.getKeyword());
    assertNotNull(directive.getUri());
    assertSizeOfList(1, directive.getCombinators());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseExportDirective_hide_show() throws Exception {
    ExportDirective directive = parse(
        "parseExportDirective",
        new Object[] {emptyCommentAndMetadata()},
        "export 'lib/lib.dart' hide A show B;");
    assertNotNull(directive.getKeyword());
    assertNotNull(directive.getUri());
    assertSizeOfList(2, directive.getCombinators());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseExportDirective_noCombinator() throws Exception {
    ExportDirective directive = parse(
        "parseExportDirective",
        new Object[] {emptyCommentAndMetadata()},
        "export 'lib/lib.dart';");
    assertNotNull(directive.getKeyword());
    assertNotNull(directive.getUri());
    assertSizeOfList(0, directive.getCombinators());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseExportDirective_show() throws Exception {
    ExportDirective directive = parse(
        "parseExportDirective",
        new Object[] {emptyCommentAndMetadata()},
        "export 'lib/lib.dart' show A, B;");
    assertNotNull(directive.getKeyword());
    assertNotNull(directive.getUri());
    assertSizeOfList(1, directive.getCombinators());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseExportDirective_show_hide() throws Exception {
    ExportDirective directive = parse(
        "parseExportDirective",
        new Object[] {emptyCommentAndMetadata()},
        "export 'lib/lib.dart' show B hide A;");
    assertNotNull(directive.getKeyword());
    assertNotNull(directive.getUri());
    assertSizeOfList(2, directive.getCombinators());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseExpression_assign() throws Exception {
    // TODO(brianwilkerson) Implement more tests for this method.
    AssignmentExpression expression = parse("parseExpression", "x = y");
    assertNotNull(expression.getLeftHandSide());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.EQ, expression.getOperator().getType());
    assertNotNull(expression.getRightHandSide());
  }

  public void test_parseExpression_comparison() throws Exception {
    BinaryExpression expression = parse("parseExpression", "--a.b == c");
    assertNotNull(expression.getLeftOperand());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.EQ_EQ, expression.getOperator().getType());
    assertNotNull(expression.getRightOperand());
  }

  public void test_parseExpression_invokeFunctionExpression() throws Exception {
    FunctionExpressionInvocation invocation = parse("parseExpression", "(a) {return a + a;} (3)");
    assertInstanceOf(FunctionExpression.class, invocation.getFunction());
    FunctionExpression expression = (FunctionExpression) invocation.getFunction();
    assertNotNull(expression.getParameters());
    assertNotNull(expression.getBody());

    ArgumentList list = invocation.getArgumentList();
    assertNotNull(list);
    assertSizeOfList(1, list.getArguments());
  }

  public void test_parseExpression_superMethodInvocation() throws Exception {
    MethodInvocation invocation = parse("parseExpression", "super.m()");
    assertNotNull(invocation.getTarget());
    assertNotNull(invocation.getMethodName());
    assertNotNull(invocation.getArgumentList());
  }

  public void test_parseExpressionList_multiple() throws Exception {
    List<Expression> result = parse("parseExpressionList", "1, 2, 3");
    assertSizeOfList(3, result);
  }

  public void test_parseExpressionList_single() throws Exception {
    List<Expression> result = parse("parseExpressionList", "1");
    assertSizeOfList(1, result);
  }

  public void test_parseExpressionWithoutCascade_assign() throws Exception {
    // TODO(brianwilkerson) Implement more tests for this method.
    AssignmentExpression expression = parse("parseExpressionWithoutCascade", "x = y");
    assertNotNull(expression.getLeftHandSide());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.EQ, expression.getOperator().getType());
    assertNotNull(expression.getRightHandSide());
  }

  public void test_parseExpressionWithoutCascade_comparison() throws Exception {
    BinaryExpression expression = parse("parseExpressionWithoutCascade", "--a.b == c");
    assertNotNull(expression.getLeftOperand());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.EQ_EQ, expression.getOperator().getType());
    assertNotNull(expression.getRightOperand());
  }

  public void test_parseExpressionWithoutCascade_superMethodInvocation() throws Exception {
    MethodInvocation invocation = parse("parseExpressionWithoutCascade", "super.m()");
    assertNotNull(invocation.getTarget());
    assertNotNull(invocation.getMethodName());
    assertNotNull(invocation.getArgumentList());
  }

  public void test_parseExtendsClause() throws Exception {
    ExtendsClause clause = parse("parseExtendsClause", "extends B");
    assertNotNull(clause.getKeyword());
    assertNotNull(clause.getSuperclass());
    assertInstanceOf(TypeName.class, clause.getSuperclass());
  }

  public void test_parseFinalConstVarOrType_const_noType() throws Exception {
    FinalConstVarOrType result = parse("parseFinalConstVarOrType", new Object[] {false}, "const");
    Token keyword = result.getKeyword();
    assertNotNull(keyword);
    assertEquals(TokenType.KEYWORD, keyword.getType());
    assertEquals(Keyword.CONST, ((KeywordToken) keyword).getKeyword());
    assertNull(result.getType());
  }

  public void test_parseFinalConstVarOrType_const_type() throws Exception {
    FinalConstVarOrType result = parse(
        "parseFinalConstVarOrType",
        new Object[] {false},
        "const A a");
    Token keyword = result.getKeyword();
    assertNotNull(keyword);
    assertEquals(TokenType.KEYWORD, keyword.getType());
    assertEquals(Keyword.CONST, ((KeywordToken) keyword).getKeyword());
    assertNotNull(result.getType());
  }

  public void test_parseFinalConstVarOrType_final_noType() throws Exception {
    FinalConstVarOrType result = parse("parseFinalConstVarOrType", new Object[] {false}, "final");
    Token keyword = result.getKeyword();
    assertNotNull(keyword);
    assertEquals(TokenType.KEYWORD, keyword.getType());
    assertEquals(Keyword.FINAL, ((KeywordToken) keyword).getKeyword());
    assertNull(result.getType());
  }

  public void test_parseFinalConstVarOrType_final_prefixedType() throws Exception {
    FinalConstVarOrType result = parse(
        "parseFinalConstVarOrType",
        new Object[] {false},
        "final p.A a");
    Token keyword = result.getKeyword();
    assertNotNull(keyword);
    assertEquals(TokenType.KEYWORD, keyword.getType());
    assertEquals(Keyword.FINAL, ((KeywordToken) keyword).getKeyword());
    assertNotNull(result.getType());
  }

  public void test_parseFinalConstVarOrType_final_type() throws Exception {
    FinalConstVarOrType result = parse(
        "parseFinalConstVarOrType",
        new Object[] {false},
        "final A a");
    Token keyword = result.getKeyword();
    assertNotNull(keyword);
    assertEquals(TokenType.KEYWORD, keyword.getType());
    assertEquals(Keyword.FINAL, ((KeywordToken) keyword).getKeyword());
    assertNotNull(result.getType());
  }

  public void test_parseFinalConstVarOrType_type_parameterized() throws Exception {
    FinalConstVarOrType result = parse("parseFinalConstVarOrType", new Object[] {false}, "A<B> a");
    assertNull(result.getKeyword());
    assertNotNull(result.getType());
  }

  public void test_parseFinalConstVarOrType_type_prefixed() throws Exception {
    FinalConstVarOrType result = parse("parseFinalConstVarOrType", new Object[] {false}, "p.A a");
    assertNull(result.getKeyword());
    assertNotNull(result.getType());
  }

  public void test_parseFinalConstVarOrType_type_prefixedAndParameterized() throws Exception {
    FinalConstVarOrType result = parse("parseFinalConstVarOrType", new Object[] {false}, "p.A<B> a");
    assertNull(result.getKeyword());
    assertNotNull(result.getType());
  }

  public void test_parseFinalConstVarOrType_type_simple() throws Exception {
    FinalConstVarOrType result = parse("parseFinalConstVarOrType", new Object[] {false}, "A a");
    assertNull(result.getKeyword());
    assertNotNull(result.getType());
  }

  public void test_parseFinalConstVarOrType_var() throws Exception {
    FinalConstVarOrType result = parse("parseFinalConstVarOrType", new Object[] {false}, "var");
    Token keyword = result.getKeyword();
    assertNotNull(keyword);
    assertEquals(TokenType.KEYWORD, keyword.getType());
    assertEquals(Keyword.VAR, ((KeywordToken) keyword).getKeyword());
    assertNull(result.getType());
  }

  public void test_parseFormalParameter_final_withType_named() throws Exception {
    ParameterKind kind = ParameterKind.NAMED;
    DefaultFormalParameter parameter = parse(
        "parseFormalParameter",
        new Object[] {kind},
        "final A a : null");
    SimpleFormalParameter simpleParameter = (SimpleFormalParameter) parameter.getParameter();
    assertNotNull(simpleParameter.getIdentifier());
    assertNotNull(simpleParameter.getKeyword());
    assertNotNull(simpleParameter.getType());
    assertEquals(kind, simpleParameter.getKind());
    assertNotNull(parameter.getSeparator());
    assertNotNull(parameter.getDefaultValue());
    assertEquals(kind, parameter.getKind());
  }

  public void test_parseFormalParameter_final_withType_normal() throws Exception {
    ParameterKind kind = ParameterKind.REQUIRED;
    SimpleFormalParameter parameter = parse(
        "parseFormalParameter",
        new Object[] {kind},
        "final A a");
    assertNotNull(parameter.getIdentifier());
    assertNotNull(parameter.getKeyword());
    assertNotNull(parameter.getType());
    assertEquals(kind, parameter.getKind());
  }

  public void test_parseFormalParameter_final_withType_positional() throws Exception {
    ParameterKind kind = ParameterKind.POSITIONAL;
    DefaultFormalParameter parameter = parse(
        "parseFormalParameter",
        new Object[] {kind},
        "final A a = null");
    SimpleFormalParameter simpleParameter = (SimpleFormalParameter) parameter.getParameter();
    assertNotNull(simpleParameter.getIdentifier());
    assertNotNull(simpleParameter.getKeyword());
    assertNotNull(simpleParameter.getType());
    assertEquals(kind, simpleParameter.getKind());
    assertNotNull(parameter.getSeparator());
    assertNotNull(parameter.getDefaultValue());
    assertEquals(kind, parameter.getKind());
  }

  public void test_parseFormalParameter_nonFinal_withType_named() throws Exception {
    ParameterKind kind = ParameterKind.NAMED;
    DefaultFormalParameter parameter = parse(
        "parseFormalParameter",
        new Object[] {kind},
        "A a : null");
    SimpleFormalParameter simpleParameter = (SimpleFormalParameter) parameter.getParameter();
    assertNotNull(simpleParameter.getIdentifier());
    assertNull(simpleParameter.getKeyword());
    assertNotNull(simpleParameter.getType());
    assertEquals(kind, simpleParameter.getKind());
    assertNotNull(parameter.getSeparator());
    assertNotNull(parameter.getDefaultValue());
    assertEquals(kind, parameter.getKind());
  }

  public void test_parseFormalParameter_nonFinal_withType_normal() throws Exception {
    ParameterKind kind = ParameterKind.REQUIRED;
    SimpleFormalParameter parameter = parse("parseFormalParameter", new Object[] {kind}, "A a");
    assertNotNull(parameter.getIdentifier());
    assertNull(parameter.getKeyword());
    assertNotNull(parameter.getType());
    assertEquals(kind, parameter.getKind());
  }

  public void test_parseFormalParameter_nonFinal_withType_positional() throws Exception {
    ParameterKind kind = ParameterKind.POSITIONAL;
    DefaultFormalParameter parameter = parse(
        "parseFormalParameter",
        new Object[] {kind},
        "A a = null");
    SimpleFormalParameter simpleParameter = (SimpleFormalParameter) parameter.getParameter();
    assertNotNull(simpleParameter.getIdentifier());
    assertNull(simpleParameter.getKeyword());
    assertNotNull(simpleParameter.getType());
    assertEquals(kind, simpleParameter.getKind());
    assertNotNull(parameter.getSeparator());
    assertNotNull(parameter.getDefaultValue());
    assertEquals(kind, parameter.getKind());
  }

  public void test_parseFormalParameter_var() throws Exception {
    ParameterKind kind = ParameterKind.REQUIRED;
    SimpleFormalParameter parameter = parse("parseFormalParameter", new Object[] {kind}, "var a");
    assertNotNull(parameter.getIdentifier());
    assertNotNull(parameter.getKeyword());
    assertNull(parameter.getType());
    assertEquals(kind, parameter.getKind());
  }

  public void test_parseFormalParameter_var_named() throws Exception {
    ParameterKind kind = ParameterKind.NAMED;
    DefaultFormalParameter parameter = parse(
        "parseFormalParameter",
        new Object[] {kind},
        "var a : null");
    SimpleFormalParameter simpleParameter = (SimpleFormalParameter) parameter.getParameter();
    assertNotNull(simpleParameter.getIdentifier());
    assertNotNull(simpleParameter.getKeyword());
    assertNull(simpleParameter.getType());
    assertEquals(kind, simpleParameter.getKind());
    assertNotNull(parameter.getSeparator());
    assertNotNull(parameter.getDefaultValue());
    assertEquals(kind, parameter.getKind());
  }

  public void test_parseFormalParameter_var_positional() throws Exception {
    ParameterKind kind = ParameterKind.POSITIONAL;
    DefaultFormalParameter parameter = parse(
        "parseFormalParameter",
        new Object[] {kind},
        "var a = null");
    SimpleFormalParameter simpleParameter = (SimpleFormalParameter) parameter.getParameter();
    assertNotNull(simpleParameter.getIdentifier());
    assertNotNull(simpleParameter.getKeyword());
    assertNull(simpleParameter.getType());
    assertEquals(kind, simpleParameter.getKind());
    assertNotNull(parameter.getSeparator());
    assertNotNull(parameter.getDefaultValue());
    assertEquals(kind, parameter.getKind());
  }

  public void test_parseFormalParameterList_empty() throws Exception {
    FormalParameterList parameterList = parse("parseFormalParameterList", "()");
    assertNotNull(parameterList.getLeftParenthesis());
    assertNull(parameterList.getLeftDelimiter());
    assertSizeOfList(0, parameterList.getParameters());
    assertNull(parameterList.getRightDelimiter());
    assertNotNull(parameterList.getRightParenthesis());
  }

  public void test_parseFormalParameterList_named_multiple() throws Exception {
    FormalParameterList parameterList = parse(
        "parseFormalParameterList",
        "({A a : 1, B b, C c : 3})");
    assertNotNull(parameterList.getLeftParenthesis());
    assertNotNull(parameterList.getLeftDelimiter());
    assertSizeOfList(3, parameterList.getParameters());
    assertNotNull(parameterList.getRightDelimiter());
    assertNotNull(parameterList.getRightParenthesis());
  }

  public void test_parseFormalParameterList_named_single() throws Exception {
    FormalParameterList parameterList = parse("parseFormalParameterList", "({A a})");
    assertNotNull(parameterList.getLeftParenthesis());
    assertNotNull(parameterList.getLeftDelimiter());
    assertSizeOfList(1, parameterList.getParameters());
    assertNotNull(parameterList.getRightDelimiter());
    assertNotNull(parameterList.getRightParenthesis());
  }

  public void test_parseFormalParameterList_normal_multiple() throws Exception {
    FormalParameterList parameterList = parse("parseFormalParameterList", "(A a, B b, C c)");
    assertNotNull(parameterList.getLeftParenthesis());
    assertNull(parameterList.getLeftDelimiter());
    assertSizeOfList(3, parameterList.getParameters());
    assertNull(parameterList.getRightDelimiter());
    assertNotNull(parameterList.getRightParenthesis());
  }

  public void test_parseFormalParameterList_normal_named() throws Exception {
    FormalParameterList parameterList = parse("parseFormalParameterList", "(A a, {B b})");
    assertNotNull(parameterList.getLeftParenthesis());
    assertNotNull(parameterList.getLeftDelimiter());
    assertSizeOfList(2, parameterList.getParameters());
    assertNotNull(parameterList.getRightDelimiter());
    assertNotNull(parameterList.getRightParenthesis());
  }

  public void test_parseFormalParameterList_normal_positional() throws Exception {
    FormalParameterList parameterList = parse("parseFormalParameterList", "(A a, [B b])");
    assertNotNull(parameterList.getLeftParenthesis());
    assertNotNull(parameterList.getLeftDelimiter());
    assertSizeOfList(2, parameterList.getParameters());
    assertNotNull(parameterList.getRightDelimiter());
    assertNotNull(parameterList.getRightParenthesis());
  }

  public void test_parseFormalParameterList_normal_single() throws Exception {
    FormalParameterList parameterList = parse("parseFormalParameterList", "(A a)");
    assertNotNull(parameterList.getLeftParenthesis());
    assertNull(parameterList.getLeftDelimiter());
    assertSizeOfList(1, parameterList.getParameters());
    assertNull(parameterList.getRightDelimiter());
    assertNotNull(parameterList.getRightParenthesis());
  }

  public void test_parseFormalParameterList_positional_multiple() throws Exception {
    FormalParameterList parameterList = parse(
        "parseFormalParameterList",
        "([A a = null, B b, C c = null])");
    assertNotNull(parameterList.getLeftParenthesis());
    assertNotNull(parameterList.getLeftDelimiter());
    assertSizeOfList(3, parameterList.getParameters());
    assertNotNull(parameterList.getRightDelimiter());
    assertNotNull(parameterList.getRightParenthesis());
  }

  public void test_parseFormalParameterList_positional_single() throws Exception {
    FormalParameterList parameterList = parse("parseFormalParameterList", "([A a = null])");
    assertNotNull(parameterList.getLeftParenthesis());
    assertNotNull(parameterList.getLeftDelimiter());
    assertSizeOfList(1, parameterList.getParameters());
    assertNotNull(parameterList.getRightDelimiter());
    assertNotNull(parameterList.getRightParenthesis());
  }

  public void test_parseForStatement_each_await() throws Exception {
    ForEachStatement statement = parse("parseForStatement", "await for (element in list) {}");
    assertNotNull(statement.getAwaitKeyword());
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNull(statement.getLoopVariable());
    assertNotNull(statement.getIdentifier());
    assertNotNull(statement.getInKeyword());
    assertNotNull(statement.getIterator());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseForStatement_each_identifier() throws Exception {
    ForEachStatement statement = parse("parseForStatement", "for (element in list) {}");
    assertNull(statement.getAwaitKeyword());
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNull(statement.getLoopVariable());
    assertNotNull(statement.getIdentifier());
    assertNotNull(statement.getInKeyword());
    assertNotNull(statement.getIterator());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseForStatement_each_noType_metadata() throws Exception {
    ForEachStatement statement = parse("parseForStatement", "for (@A var element in list) {}");
    assertNull(statement.getAwaitKeyword());
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNotNull(statement.getLoopVariable());
    assertSizeOfList(1, statement.getLoopVariable().getMetadata());
    assertNull(statement.getIdentifier());
    assertNotNull(statement.getInKeyword());
    assertNotNull(statement.getIterator());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseForStatement_each_type() throws Exception {
    ForEachStatement statement = parse("parseForStatement", "for (A element in list) {}");
    assertNull(statement.getAwaitKeyword());
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNotNull(statement.getLoopVariable());
    assertNull(statement.getIdentifier());
    assertNotNull(statement.getInKeyword());
    assertNotNull(statement.getIterator());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseForStatement_each_var() throws Exception {
    ForEachStatement statement = parse("parseForStatement", "for (var element in list) {}");
    assertNull(statement.getAwaitKeyword());
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNotNull(statement.getLoopVariable());
    assertNull(statement.getIdentifier());
    assertNotNull(statement.getInKeyword());
    assertNotNull(statement.getIterator());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseForStatement_loop_c() throws Exception {
    ForStatement statement = parse("parseForStatement", "for (; i < count;) {}");
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNull(statement.getVariables());
    assertNull(statement.getInitialization());
    assertNotNull(statement.getLeftSeparator());
    assertNotNull(statement.getCondition());
    assertNotNull(statement.getRightSeparator());
    assertSizeOfList(0, statement.getUpdaters());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseForStatement_loop_cu() throws Exception {
    ForStatement statement = parse("parseForStatement", "for (; i < count; i++) {}");
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNull(statement.getVariables());
    assertNull(statement.getInitialization());
    assertNotNull(statement.getLeftSeparator());
    assertNotNull(statement.getCondition());
    assertNotNull(statement.getRightSeparator());
    assertSizeOfList(1, statement.getUpdaters());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseForStatement_loop_ecu() throws Exception {
    ForStatement statement = parse("parseForStatement", "for (i--; i < count; i++) {}");
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNull(statement.getVariables());
    assertNotNull(statement.getInitialization());
    assertNotNull(statement.getLeftSeparator());
    assertNotNull(statement.getCondition());
    assertNotNull(statement.getRightSeparator());
    assertSizeOfList(1, statement.getUpdaters());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseForStatement_loop_i() throws Exception {
    ForStatement statement = parse("parseForStatement", "for (var i = 0;;) {}");
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    VariableDeclarationList variables = statement.getVariables();
    assertNotNull(variables);
    assertSizeOfList(0, variables.getMetadata());
    assertSizeOfList(1, variables.getVariables());
    assertNull(statement.getInitialization());
    assertNotNull(statement.getLeftSeparator());
    assertNull(statement.getCondition());
    assertNotNull(statement.getRightSeparator());
    assertSizeOfList(0, statement.getUpdaters());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseForStatement_loop_i_withMetadata() throws Exception {
    ForStatement statement = parse("parseForStatement", "for (@A var i = 0;;) {}");
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    VariableDeclarationList variables = statement.getVariables();
    assertNotNull(variables);
    assertSizeOfList(1, variables.getMetadata());
    assertSizeOfList(1, variables.getVariables());
    assertNull(statement.getInitialization());
    assertNotNull(statement.getLeftSeparator());
    assertNull(statement.getCondition());
    assertNotNull(statement.getRightSeparator());
    assertSizeOfList(0, statement.getUpdaters());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseForStatement_loop_ic() throws Exception {
    ForStatement statement = parse("parseForStatement", "for (var i = 0; i < count;) {}");
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    VariableDeclarationList variables = statement.getVariables();
    assertNotNull(variables);
    assertSizeOfList(1, variables.getVariables());
    assertNull(statement.getInitialization());
    assertNotNull(statement.getLeftSeparator());
    assertNotNull(statement.getCondition());
    assertNotNull(statement.getRightSeparator());
    assertSizeOfList(0, statement.getUpdaters());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseForStatement_loop_icu() throws Exception {
    ForStatement statement = parse("parseForStatement", "for (var i = 0; i < count; i++) {}");
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    VariableDeclarationList variables = statement.getVariables();
    assertNotNull(variables);
    assertSizeOfList(1, variables.getVariables());
    assertNull(statement.getInitialization());
    assertNotNull(statement.getLeftSeparator());
    assertNotNull(statement.getCondition());
    assertNotNull(statement.getRightSeparator());
    assertSizeOfList(1, statement.getUpdaters());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseForStatement_loop_iicuu() throws Exception {
    ForStatement statement = parse(
        "parseForStatement",
        "for (int i = 0, j = count; i < j; i++, j--) {}");
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    VariableDeclarationList variables = statement.getVariables();
    assertNotNull(variables);
    assertSizeOfList(2, variables.getVariables());
    assertNull(statement.getInitialization());
    assertNotNull(statement.getLeftSeparator());
    assertNotNull(statement.getCondition());
    assertNotNull(statement.getRightSeparator());
    assertSizeOfList(2, statement.getUpdaters());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseForStatement_loop_iu() throws Exception {
    ForStatement statement = parse("parseForStatement", "for (var i = 0;; i++) {}");
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    VariableDeclarationList variables = statement.getVariables();
    assertNotNull(variables);
    assertSizeOfList(1, variables.getVariables());
    assertNull(statement.getInitialization());
    assertNotNull(statement.getLeftSeparator());
    assertNull(statement.getCondition());
    assertNotNull(statement.getRightSeparator());
    assertSizeOfList(1, statement.getUpdaters());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseForStatement_loop_u() throws Exception {
    ForStatement statement = parse("parseForStatement", "for (;; i++) {}");
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNull(statement.getVariables());
    assertNull(statement.getInitialization());
    assertNotNull(statement.getLeftSeparator());
    assertNull(statement.getCondition());
    assertNotNull(statement.getRightSeparator());
    assertSizeOfList(1, statement.getUpdaters());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseFunctionBody_block() throws Exception {
    BlockFunctionBody functionBody = parse(
        "parseFunctionBody",
        new Object[] {false, null, false},
        "{}");
    assertNull(functionBody.getKeyword());
    assertNull(functionBody.getStar());
    assertNotNull(functionBody.getBlock());
    assertFalse(functionBody.isAsynchronous());
    assertFalse(functionBody.isGenerator());
    assertTrue(functionBody.isSynchronous());
  }

  public void test_parseFunctionBody_block_async() throws Exception {
    BlockFunctionBody functionBody = parse(
        "parseFunctionBody",
        new Object[] {false, null, false},
        "async {}");
    assertNotNull(functionBody.getKeyword());
    assertEquals(Parser.ASYNC, functionBody.getKeyword().getLexeme());
    assertNull(functionBody.getStar());
    assertNotNull(functionBody.getBlock());
    assertTrue(functionBody.isAsynchronous());
    assertFalse(functionBody.isGenerator());
    assertFalse(functionBody.isSynchronous());
  }

  public void test_parseFunctionBody_block_asyncGenerator() throws Exception {
    BlockFunctionBody functionBody = parse(
        "parseFunctionBody",
        new Object[] {false, null, false},
        "async* {}");
    assertNotNull(functionBody.getKeyword());
    assertEquals(Parser.ASYNC, functionBody.getKeyword().getLexeme());
    assertNotNull(functionBody.getStar());
    assertNotNull(functionBody.getBlock());
    assertTrue(functionBody.isAsynchronous());
    assertTrue(functionBody.isGenerator());
    assertFalse(functionBody.isSynchronous());
  }

  public void test_parseFunctionBody_block_syncGenerator() throws Exception {
    BlockFunctionBody functionBody = parse(
        "parseFunctionBody",
        new Object[] {false, null, false},
        "sync* {}");
    assertNotNull(functionBody.getKeyword());
    assertEquals(Parser.SYNC, functionBody.getKeyword().getLexeme());
    assertNotNull(functionBody.getStar());
    assertNotNull(functionBody.getBlock());
    assertFalse(functionBody.isAsynchronous());
    assertTrue(functionBody.isGenerator());
    assertTrue(functionBody.isSynchronous());
  }

  public void test_parseFunctionBody_empty() throws Exception {
    EmptyFunctionBody functionBody = parse(
        "parseFunctionBody",
        new Object[] {true, null, false},
        ";");
    assertNotNull(functionBody.getSemicolon());
  }

  public void test_parseFunctionBody_expression() throws Exception {
    ExpressionFunctionBody functionBody = parse("parseFunctionBody", new Object[] {
        false, null, false}, "=> y;");
    assertNull(functionBody.getKeyword());
    assertNotNull(functionBody.getFunctionDefinition());
    assertNotNull(functionBody.getExpression());
    assertNotNull(functionBody.getSemicolon());
    assertFalse(functionBody.isAsynchronous());
    assertFalse(functionBody.isGenerator());
    assertTrue(functionBody.isSynchronous());
  }

  public void test_parseFunctionBody_expression_async() throws Exception {
    ExpressionFunctionBody functionBody = parse("parseFunctionBody", new Object[] {
        false, null, false}, "async => y;");
    assertNotNull(functionBody.getKeyword());
    assertEquals(Parser.ASYNC, functionBody.getKeyword().getLexeme());
    assertNotNull(functionBody.getFunctionDefinition());
    assertNotNull(functionBody.getExpression());
    assertNotNull(functionBody.getSemicolon());
    assertTrue(functionBody.isAsynchronous());
    assertFalse(functionBody.isGenerator());
    assertFalse(functionBody.isSynchronous());
  }

  public void test_parseFunctionBody_nativeFunctionBody() throws Exception {
    NativeFunctionBody functionBody = parse(
        "parseFunctionBody",
        new Object[] {false, null, false},
        "native 'str';");
    assertNotNull(functionBody.getNativeToken());
    assertNotNull(functionBody.getStringLiteral());
    assertNotNull(functionBody.getSemicolon());
  }

  public void test_parseFunctionBody_skip_block() throws Exception {
    ParserTestCase.parseFunctionBodies = false;
    FunctionBody functionBody = parse("parseFunctionBody", new Object[] {false, null, false}, "{}");
    assertInstanceOf(EmptyFunctionBody.class, functionBody);
  }

  public void test_parseFunctionBody_skip_block_invalid() throws Exception {
    ParserTestCase.parseFunctionBodies = false;
    FunctionBody functionBody = parse(
        "parseFunctionBody",
        new Object[] {false, null, false},
        "{",
        ParserErrorCode.EXPECTED_TOKEN);
    assertInstanceOf(EmptyFunctionBody.class, functionBody);
  }

  public void test_parseFunctionBody_skip_blocks() throws Exception {
    ParserTestCase.parseFunctionBodies = false;
    FunctionBody functionBody = parse(
        "parseFunctionBody",
        new Object[] {false, null, false},
        "{ {} }");
    assertInstanceOf(EmptyFunctionBody.class, functionBody);
  }

  public void test_parseFunctionBody_skip_expression() throws Exception {
    ParserTestCase.parseFunctionBodies = false;
    FunctionBody functionBody = parse(
        "parseFunctionBody",
        new Object[] {false, null, false},
        "=> y;");
    assertInstanceOf(EmptyFunctionBody.class, functionBody);
  }

  public void test_parseFunctionDeclaration_function() throws Exception {
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    TypeName returnType = new TypeName(new SimpleIdentifier(null), null);
    FunctionDeclaration declaration = parse("parseFunctionDeclaration", new Object[] {
        commentAndMetadata(comment), null, returnType}, "f() {}");
    assertEquals(comment, declaration.getDocumentationComment());
    assertEquals(returnType, declaration.getReturnType());
    assertNotNull(declaration.getName());
    FunctionExpression expression = declaration.getFunctionExpression();
    assertNotNull(expression);
    assertNotNull(expression.getBody());
    assertNotNull(expression.getParameters());
    assertNull(declaration.getPropertyKeyword());
  }

  public void test_parseFunctionDeclaration_getter() throws Exception {
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    TypeName returnType = new TypeName(new SimpleIdentifier(null), null);
    FunctionDeclaration declaration = parse("parseFunctionDeclaration", new Object[] {
        commentAndMetadata(comment), null, returnType}, "get p => 0;");
    assertEquals(comment, declaration.getDocumentationComment());
    assertEquals(returnType, declaration.getReturnType());
    assertNotNull(declaration.getName());
    FunctionExpression expression = declaration.getFunctionExpression();
    assertNotNull(expression);
    assertNotNull(expression.getBody());
    assertNull(expression.getParameters());
    assertNotNull(declaration.getPropertyKeyword());
  }

  public void test_parseFunctionDeclaration_setter() throws Exception {
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    TypeName returnType = new TypeName(new SimpleIdentifier(null), null);
    FunctionDeclaration declaration = parse("parseFunctionDeclaration", new Object[] {
        commentAndMetadata(comment), null, returnType}, "set p(v) {}");
    assertEquals(comment, declaration.getDocumentationComment());
    assertEquals(returnType, declaration.getReturnType());
    assertNotNull(declaration.getName());
    FunctionExpression expression = declaration.getFunctionExpression();
    assertNotNull(expression);
    assertNotNull(expression.getBody());
    assertNotNull(expression.getParameters());
    assertNotNull(declaration.getPropertyKeyword());
  }

  public void test_parseFunctionDeclarationStatement() throws Exception {
    FunctionDeclarationStatement statement = parse(
        "parseFunctionDeclarationStatement",
        "void f(int p) => p * 2;");
    assertNotNull(statement.getFunctionDeclaration());
  }

  public void test_parseFunctionExpression_body_inExpression() throws Exception {
    FunctionExpression expression = parse("parseFunctionExpression", "(int i) => i++");
    assertNotNull(expression.getBody());
    assertNotNull(expression.getParameters());
    assertNull(((ExpressionFunctionBody) expression.getBody()).getSemicolon());
  }

  public void test_parseFunctionExpression_minimal() throws Exception {
    FunctionExpression expression = parse("parseFunctionExpression", "() {}");
    assertNotNull(expression.getBody());
    assertNotNull(expression.getParameters());
  }

  public void test_parseGetter_nonStatic() throws Exception {
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    TypeName returnType = new TypeName(new SimpleIdentifier(null), null);
    MethodDeclaration method = parse("parseGetter", new Object[] {
        commentAndMetadata(comment), null, null, returnType}, "get a;");
    assertNotNull(method.getBody());
    assertEquals(comment, method.getDocumentationComment());
    assertNull(method.getExternalKeyword());
    assertNull(method.getModifierKeyword());
    assertNotNull(method.getName());
    assertNull(method.getOperatorKeyword());
    assertNull(method.getParameters());
    assertNotNull(method.getPropertyKeyword());
    assertEquals(returnType, method.getReturnType());
  }

  public void test_parseGetter_static() throws Exception {
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    Token staticKeyword = tokenFromKeyword(Keyword.STATIC);
    TypeName returnType = new TypeName(new SimpleIdentifier(null), null);
    MethodDeclaration method = parse("parseGetter", new Object[] {
        commentAndMetadata(comment), null, staticKeyword, returnType}, "get a => 42;");
    assertNotNull(method.getBody());
    assertEquals(comment, method.getDocumentationComment());
    assertNull(method.getExternalKeyword());
    assertEquals(staticKeyword, method.getModifierKeyword());
    assertNotNull(method.getName());
    assertNull(method.getOperatorKeyword());
    assertNull(method.getParameters());
    assertNotNull(method.getPropertyKeyword());
    assertEquals(returnType, method.getReturnType());
  }

  public void test_parseIdentifierList_multiple() throws Exception {
    List<SimpleIdentifier> list = parse("parseIdentifierList", "a, b, c");
    assertSizeOfList(3, list);
  }

  public void test_parseIdentifierList_single() throws Exception {
    List<SimpleIdentifier> list = parse("parseIdentifierList", "a");
    assertSizeOfList(1, list);
  }

  public void test_parseIfStatement_else_block() throws Exception {
    IfStatement statement = parse("parseIfStatement", "if (x) {} else {}");
    assertNotNull(statement.getIfKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNotNull(statement.getCondition());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getThenStatement());
    assertNotNull(statement.getElseKeyword());
    assertNotNull(statement.getElseStatement());
  }

  public void test_parseIfStatement_else_statement() throws Exception {
    IfStatement statement = parse("parseIfStatement", "if (x) f(x); else f(y);");
    assertNotNull(statement.getIfKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNotNull(statement.getCondition());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getThenStatement());
    assertNotNull(statement.getElseKeyword());
    assertNotNull(statement.getElseStatement());
  }

  public void test_parseIfStatement_noElse_block() throws Exception {
    IfStatement statement = parse("parseIfStatement", "if (x) {}");
    assertNotNull(statement.getIfKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNotNull(statement.getCondition());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getThenStatement());
    assertNull(statement.getElseKeyword());
    assertNull(statement.getElseStatement());
  }

  public void test_parseIfStatement_noElse_statement() throws Exception {
    IfStatement statement = parse("parseIfStatement", "if (x) f(x);");
    assertNotNull(statement.getIfKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNotNull(statement.getCondition());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getThenStatement());
    assertNull(statement.getElseKeyword());
    assertNull(statement.getElseStatement());
  }

  public void test_parseImplementsClause_multiple() throws Exception {
    ImplementsClause clause = parse("parseImplementsClause", "implements A, B, C");
    assertSizeOfList(3, clause.getInterfaces());
    assertNotNull(clause.getKeyword());
  }

  public void test_parseImplementsClause_single() throws Exception {
    ImplementsClause clause = parse("parseImplementsClause", "implements A");
    assertSizeOfList(1, clause.getInterfaces());
    assertNotNull(clause.getKeyword());
  }

  public void test_parseImportDirective_deferred() throws Exception {
    ImportDirective directive = parse(
        "parseImportDirective",
        new Object[] {emptyCommentAndMetadata()},
        "import 'lib/lib.dart' deferred as a;");
    assertNotNull(directive.getKeyword());
    assertNotNull(directive.getUri());
    assertNotNull(directive.getDeferredToken());
    assertNotNull(directive.getAsToken());
    assertNotNull(directive.getPrefix());
    assertSizeOfList(0, directive.getCombinators());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseImportDirective_hide() throws Exception {
    ImportDirective directive = parse(
        "parseImportDirective",
        new Object[] {emptyCommentAndMetadata()},
        "import 'lib/lib.dart' hide A, B;");
    assertNotNull(directive.getKeyword());
    assertNotNull(directive.getUri());
    assertNull(directive.getDeferredToken());
    assertNull(directive.getAsToken());
    assertNull(directive.getPrefix());
    assertSizeOfList(1, directive.getCombinators());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseImportDirective_noCombinator() throws Exception {
    ImportDirective directive = parse(
        "parseImportDirective",
        new Object[] {emptyCommentAndMetadata()},
        "import 'lib/lib.dart';");
    assertNotNull(directive.getKeyword());
    assertNotNull(directive.getUri());
    assertNull(directive.getDeferredToken());
    assertNull(directive.getAsToken());
    assertNull(directive.getPrefix());
    assertSizeOfList(0, directive.getCombinators());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseImportDirective_prefix() throws Exception {
    ImportDirective directive = parse(
        "parseImportDirective",
        new Object[] {emptyCommentAndMetadata()},
        "import 'lib/lib.dart' as a;");
    assertNotNull(directive.getKeyword());
    assertNotNull(directive.getUri());
    assertNull(directive.getDeferredToken());
    assertNotNull(directive.getAsToken());
    assertNotNull(directive.getPrefix());
    assertSizeOfList(0, directive.getCombinators());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseImportDirective_prefix_hide_show() throws Exception {
    ImportDirective directive = parse(
        "parseImportDirective",
        new Object[] {emptyCommentAndMetadata()},
        "import 'lib/lib.dart' as a hide A show B;");
    assertNotNull(directive.getKeyword());
    assertNotNull(directive.getUri());
    assertNull(directive.getDeferredToken());
    assertNotNull(directive.getAsToken());
    assertNotNull(directive.getPrefix());
    assertSizeOfList(2, directive.getCombinators());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseImportDirective_prefix_show_hide() throws Exception {
    ImportDirective directive = parse(
        "parseImportDirective",
        new Object[] {emptyCommentAndMetadata()},
        "import 'lib/lib.dart' as a show B hide A;");
    assertNotNull(directive.getKeyword());
    assertNotNull(directive.getUri());
    assertNull(directive.getDeferredToken());
    assertNotNull(directive.getAsToken());
    assertNotNull(directive.getPrefix());
    assertSizeOfList(2, directive.getCombinators());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseImportDirective_show() throws Exception {
    ImportDirective directive = parse(
        "parseImportDirective",
        new Object[] {emptyCommentAndMetadata()},
        "import 'lib/lib.dart' show A, B;");
    assertNotNull(directive.getKeyword());
    assertNotNull(directive.getUri());
    assertNull(directive.getDeferredToken());
    assertNull(directive.getAsToken());
    assertNull(directive.getPrefix());
    assertSizeOfList(1, directive.getCombinators());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseInitializedIdentifierList_type() throws Exception {
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    Token staticKeyword = tokenFromKeyword(Keyword.STATIC);
    TypeName type = new TypeName(new SimpleIdentifier(null), null);
    FieldDeclaration declaration = parse("parseInitializedIdentifierList", new Object[] {
        commentAndMetadata(comment), staticKeyword, null, type}, "a = 1, b, c = 3;");
    assertEquals(comment, declaration.getDocumentationComment());
    VariableDeclarationList fields = declaration.getFields();
    assertNotNull(fields);
    assertNull(fields.getKeyword());
    assertEquals(type, fields.getType());
    assertSizeOfList(3, fields.getVariables());
    assertEquals(staticKeyword, declaration.getStaticKeyword());
    assertNotNull(declaration.getSemicolon());
  }

  public void test_parseInitializedIdentifierList_var() throws Exception {
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    Token staticKeyword = tokenFromKeyword(Keyword.STATIC);
    Token varKeyword = tokenFromKeyword(Keyword.VAR);
    FieldDeclaration declaration = parse("parseInitializedIdentifierList", new Object[] {
        commentAndMetadata(comment), staticKeyword, varKeyword, null}, "a = 1, b, c = 3;");
    assertEquals(comment, declaration.getDocumentationComment());
    VariableDeclarationList fields = declaration.getFields();
    assertNotNull(fields);
    assertEquals(varKeyword, fields.getKeyword());
    assertNull(fields.getType());
    assertSizeOfList(3, fields.getVariables());
    assertEquals(staticKeyword, declaration.getStaticKeyword());
    assertNotNull(declaration.getSemicolon());
  }

  public void test_parseInstanceCreationExpression_qualifiedType() throws Exception {
    Token token = tokenFromKeyword(Keyword.NEW);
    InstanceCreationExpression expression = parse(
        "parseInstanceCreationExpression",
        new Object[] {token},
        "A.B()");
    assertEquals(token, expression.getKeyword());
    ConstructorName name = expression.getConstructorName();
    assertNotNull(name);
    assertNotNull(name.getType());
    assertNull(name.getPeriod());
    assertNull(name.getName());
    assertNotNull(expression.getArgumentList());
  }

  public void test_parseInstanceCreationExpression_qualifiedType_named() throws Exception {
    Token token = tokenFromKeyword(Keyword.NEW);
    InstanceCreationExpression expression = parse(
        "parseInstanceCreationExpression",
        new Object[] {token},
        "A.B.c()");
    assertEquals(token, expression.getKeyword());
    ConstructorName name = expression.getConstructorName();
    assertNotNull(name);
    assertNotNull(name.getType());
    assertNotNull(name.getPeriod());
    assertNotNull(name.getName());
    assertNotNull(expression.getArgumentList());
  }

  public void test_parseInstanceCreationExpression_type() throws Exception {
    Token token = tokenFromKeyword(Keyword.NEW);
    InstanceCreationExpression expression = parse(
        "parseInstanceCreationExpression",
        new Object[] {token},
        "A()");
    assertEquals(token, expression.getKeyword());
    ConstructorName name = expression.getConstructorName();
    assertNotNull(name);
    assertNotNull(name.getType());
    assertNull(name.getPeriod());
    assertNull(name.getName());
    assertNotNull(expression.getArgumentList());
  }

  public void test_parseInstanceCreationExpression_type_named() throws Exception {
    Token token = tokenFromKeyword(Keyword.NEW);
    InstanceCreationExpression expression = parse(
        "parseInstanceCreationExpression",
        new Object[] {token},
        "A<B>.c()");
    assertEquals(token, expression.getKeyword());
    ConstructorName name = expression.getConstructorName();
    assertNotNull(name);
    assertNotNull(name.getType());
    assertNotNull(name.getPeriod());
    assertNotNull(name.getName());
    assertNotNull(expression.getArgumentList());
  }

  public void test_parseLibraryDirective() throws Exception {
    LibraryDirective directive = parse(
        "parseLibraryDirective",
        new Object[] {emptyCommentAndMetadata()},
        "library l;");
    assertNotNull(directive.getLibraryToken());
    assertNotNull(directive.getName());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseLibraryIdentifier_multiple() throws Exception {
    String name = "a.b.c";
    LibraryIdentifier identifier = parse("parseLibraryIdentifier", name);
    assertEquals(name, identifier.getName());
  }

  public void test_parseLibraryIdentifier_single() throws Exception {
    String name = "a";
    LibraryIdentifier identifier = parse("parseLibraryIdentifier", name);
    assertEquals(name, identifier.getName());
  }

  public void test_parseListLiteral_empty_oneToken() throws Exception {
    Token token = tokenFromKeyword(Keyword.CONST);
    TypeArgumentList typeArguments = null;
    ListLiteral literal = parse("parseListLiteral", new Object[] {token, typeArguments}, "[]");
    assertEquals(token, literal.getConstKeyword());
    assertEquals(typeArguments, literal.getTypeArguments());
    assertNotNull(literal.getLeftBracket());
    assertSizeOfList(0, literal.getElements());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseListLiteral_empty_twoTokens() throws Exception {
    Token token = tokenFromKeyword(Keyword.CONST);
    TypeArgumentList typeArguments = null;
    ListLiteral literal = parse("parseListLiteral", new Object[] {token, typeArguments}, "[ ]");
    assertEquals(token, literal.getConstKeyword());
    assertEquals(typeArguments, literal.getTypeArguments());
    assertNotNull(literal.getLeftBracket());
    assertSizeOfList(0, literal.getElements());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseListLiteral_multiple() throws Exception {
    ListLiteral literal = parse("parseListLiteral", new Object[] {null, null}, "[1, 2, 3]");
    assertNull(literal.getConstKeyword());
    assertNull(literal.getTypeArguments());
    assertNotNull(literal.getLeftBracket());
    assertSizeOfList(3, literal.getElements());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseListLiteral_single() throws Exception {
    ListLiteral literal = parse("parseListLiteral", new Object[] {null, null}, "[1]");
    assertNull(literal.getConstKeyword());
    assertNull(literal.getTypeArguments());
    assertNotNull(literal.getLeftBracket());
    assertSizeOfList(1, literal.getElements());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseListOrMapLiteral_list_noType() throws Exception {
    ListLiteral literal = parse("parseListOrMapLiteral", new Object[] {null}, "[1]");
    assertNull(literal.getConstKeyword());
    assertNull(literal.getTypeArguments());
    assertNotNull(literal.getLeftBracket());
    assertSizeOfList(1, literal.getElements());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseListOrMapLiteral_list_type() throws Exception {
    ListLiteral literal = parse("parseListOrMapLiteral", new Object[] {null}, "<int> [1]");
    assertNull(literal.getConstKeyword());
    assertNotNull(literal.getTypeArguments());
    assertNotNull(literal.getLeftBracket());
    assertSizeOfList(1, literal.getElements());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseListOrMapLiteral_map_noType() throws Exception {
    MapLiteral literal = parse("parseListOrMapLiteral", new Object[] {null}, "{'1' : 1}");
    assertNull(literal.getConstKeyword());
    assertNull(literal.getTypeArguments());
    assertNotNull(literal.getLeftBracket());
    assertSizeOfList(1, literal.getEntries());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseListOrMapLiteral_map_type() throws Exception {
    MapLiteral literal = parse(
        "parseListOrMapLiteral",
        new Object[] {null},
        "<String, int> {'1' : 1}");
    assertNull(literal.getConstKeyword());
    assertNotNull(literal.getTypeArguments());
    assertNotNull(literal.getLeftBracket());
    assertSizeOfList(1, literal.getEntries());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseLogicalAndExpression() throws Exception {
    BinaryExpression expression = parse("parseLogicalAndExpression", "x && y");
    assertNotNull(expression.getLeftOperand());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.AMPERSAND_AMPERSAND, expression.getOperator().getType());
    assertNotNull(expression.getRightOperand());
  }

  public void test_parseLogicalOrExpression() throws Exception {
    BinaryExpression expression = parse("parseLogicalOrExpression", "x || y");
    assertNotNull(expression.getLeftOperand());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.BAR_BAR, expression.getOperator().getType());
    assertNotNull(expression.getRightOperand());
  }

  public void test_parseMapLiteral_empty() throws Exception {
    Token token = tokenFromKeyword(Keyword.CONST);
    TypeArgumentList typeArguments = typeArgumentList(typeName("String"), typeName("int"));
    MapLiteral literal = parse("parseMapLiteral", new Object[] {token, typeArguments}, "{}");
    assertEquals(token, literal.getConstKeyword());
    assertEquals(typeArguments, literal.getTypeArguments());
    assertNotNull(literal.getLeftBracket());
    assertSizeOfList(0, literal.getEntries());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseMapLiteral_multiple() throws Exception {
    MapLiteral literal = parse("parseMapLiteral", new Object[] {null, null}, "{'a' : b, 'x' : y}");
    assertNotNull(literal.getLeftBracket());
    assertSizeOfList(2, literal.getEntries());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseMapLiteral_single() throws Exception {
    MapLiteral literal = parse("parseMapLiteral", new Object[] {null, null}, "{'x' : y}");
    assertNotNull(literal.getLeftBracket());
    assertSizeOfList(1, literal.getEntries());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseMapLiteralEntry_complex() throws Exception {
    MapLiteralEntry entry = parse("parseMapLiteralEntry", "2 + 2 : y");
    assertNotNull(entry.getKey());
    assertNotNull(entry.getSeparator());
    assertNotNull(entry.getValue());
  }

  public void test_parseMapLiteralEntry_int() throws Exception {
    MapLiteralEntry entry = parse("parseMapLiteralEntry", "0 : y");
    assertNotNull(entry.getKey());
    assertNotNull(entry.getSeparator());
    assertNotNull(entry.getValue());
  }

  public void test_parseMapLiteralEntry_string() throws Exception {
    MapLiteralEntry entry = parse("parseMapLiteralEntry", "'x' : y");
    assertNotNull(entry.getKey());
    assertNotNull(entry.getSeparator());
    assertNotNull(entry.getValue());
  }

  public void test_parseModifiers_abstract() throws Exception {
    Modifiers modifiers = parse("parseModifiers", "abstract A");
    assertNotNull(modifiers.getAbstractKeyword());
  }

  public void test_parseModifiers_const() throws Exception {
    Modifiers modifiers = parse("parseModifiers", "const A");
    assertNotNull(modifiers.getConstKeyword());
  }

  public void test_parseModifiers_external() throws Exception {
    Modifiers modifiers = parse("parseModifiers", "external A");
    assertNotNull(modifiers.getExternalKeyword());
  }

  public void test_parseModifiers_factory() throws Exception {
    Modifiers modifiers = parse("parseModifiers", "factory A");
    assertNotNull(modifiers.getFactoryKeyword());
  }

  public void test_parseModifiers_final() throws Exception {
    Modifiers modifiers = parse("parseModifiers", "final A");
    assertNotNull(modifiers.getFinalKeyword());
  }

  public void test_parseModifiers_static() throws Exception {
    Modifiers modifiers = parse("parseModifiers", "static A");
    assertNotNull(modifiers.getStaticKeyword());
  }

  public void test_parseModifiers_var() throws Exception {
    Modifiers modifiers = parse("parseModifiers", "var A");
    assertNotNull(modifiers.getVarKeyword());
  }

  public void test_parseMultiplicativeExpression_normal() throws Exception {
    BinaryExpression expression = parse("parseMultiplicativeExpression", "x * y");
    assertNotNull(expression.getLeftOperand());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.STAR, expression.getOperator().getType());
    assertNotNull(expression.getRightOperand());
  }

  public void test_parseMultiplicativeExpression_super() throws Exception {
    BinaryExpression expression = parse("parseMultiplicativeExpression", "super * y");
    assertInstanceOf(SuperExpression.class, expression.getLeftOperand());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.STAR, expression.getOperator().getType());
    assertNotNull(expression.getRightOperand());
  }

  public void test_parseNewExpression() throws Exception {
    InstanceCreationExpression expression = parse("parseNewExpression", "new A()");
    assertNotNull(expression.getKeyword());
    ConstructorName name = expression.getConstructorName();
    assertNotNull(name);
    assertNotNull(name.getType());
    assertNull(name.getPeriod());
    assertNull(name.getName());
    assertNotNull(expression.getArgumentList());
  }

  public void test_parseNonLabeledStatement_const_list_empty() throws Exception {
    ExpressionStatement statement = parse("parseNonLabeledStatement", "const [];");
    assertNotNull(statement.getExpression());
  }

  public void test_parseNonLabeledStatement_const_list_nonEmpty() throws Exception {
    ExpressionStatement statement = parse("parseNonLabeledStatement", "const [1, 2];");
    assertNotNull(statement.getExpression());
  }

  public void test_parseNonLabeledStatement_const_map_empty() throws Exception {
    ExpressionStatement statement = parse("parseNonLabeledStatement", "const {};");
    assertNotNull(statement.getExpression());
  }

  public void test_parseNonLabeledStatement_const_map_nonEmpty() throws Exception {
    // TODO(brianwilkerson) Implement more tests for this method.
    ExpressionStatement statement = parse("parseNonLabeledStatement", "const {'a' : 1};");
    assertNotNull(statement.getExpression());
  }

  public void test_parseNonLabeledStatement_const_object() throws Exception {
    ExpressionStatement statement = parse("parseNonLabeledStatement", "const A();");
    assertNotNull(statement.getExpression());
  }

  public void test_parseNonLabeledStatement_const_object_named_typeParameters() throws Exception {
    ExpressionStatement statement = parse("parseNonLabeledStatement", "const A<B>.c();");
    assertNotNull(statement.getExpression());
  }

  public void test_parseNonLabeledStatement_constructorInvocation() throws Exception {
    ExpressionStatement statement = parse("parseNonLabeledStatement", "new C().m();");
    assertNotNull(statement.getExpression());
  }

  public void test_parseNonLabeledStatement_false() throws Exception {
    ExpressionStatement statement = parse("parseNonLabeledStatement", "false;");
    assertNotNull(statement.getExpression());
  }

  public void test_parseNonLabeledStatement_functionDeclaration() throws Exception {
    parse("parseNonLabeledStatement", "f() {};");
  }

  public void test_parseNonLabeledStatement_functionDeclaration_arguments() throws Exception {
    parse("parseNonLabeledStatement", "f(void g()) {};");
  }

  public void test_parseNonLabeledStatement_functionExpressionIndex() throws Exception {
    parse("parseNonLabeledStatement", "() {}[0] = null;");
  }

  public void test_parseNonLabeledStatement_functionInvocation() throws Exception {
    ExpressionStatement statement = parse("parseNonLabeledStatement", "f();");
    assertNotNull(statement.getExpression());
  }

  public void test_parseNonLabeledStatement_invokeFunctionExpression() throws Exception {
    ExpressionStatement statement = parse("parseNonLabeledStatement", "(a) {return a + a;} (3);");
    assertInstanceOf(FunctionExpressionInvocation.class, statement.getExpression());
    FunctionExpressionInvocation invocation = (FunctionExpressionInvocation) statement.getExpression();
    assertInstanceOf(FunctionExpression.class, invocation.getFunction());
    FunctionExpression expression = (FunctionExpression) invocation.getFunction();
    assertNotNull(expression.getParameters());
    assertNotNull(expression.getBody());

    ArgumentList list = invocation.getArgumentList();
    assertNotNull(list);
    assertSizeOfList(1, list.getArguments());
  }

  public void test_parseNonLabeledStatement_null() throws Exception {
    ExpressionStatement statement = parse("parseNonLabeledStatement", "null;");
    assertNotNull(statement.getExpression());
  }

  public void test_parseNonLabeledStatement_startingWithBuiltInIdentifier() throws Exception {
    ExpressionStatement statement = parse("parseNonLabeledStatement", "library.getName();");
    assertNotNull(statement.getExpression());
  }

  public void test_parseNonLabeledStatement_true() throws Exception {
    ExpressionStatement statement = parse("parseNonLabeledStatement", "true;");
    assertNotNull(statement.getExpression());
  }

  public void test_parseNonLabeledStatement_typeCast() throws Exception {
    ExpressionStatement statement = parse("parseNonLabeledStatement", "double.NAN as num;");
    assertNotNull(statement.getExpression());
  }

  public void test_parseNormalFormalParameter_field_const_noType() throws Exception {
    FieldFormalParameter parameter = parse("parseNormalFormalParameter", "const this.a)");
    assertNotNull(parameter.getKeyword());
    assertNull(parameter.getType());
    assertNotNull(parameter.getIdentifier());
    assertNull(parameter.getParameters());
  }

  public void test_parseNormalFormalParameter_field_const_type() throws Exception {
    FieldFormalParameter parameter = parse("parseNormalFormalParameter", "const A this.a)");
    assertNotNull(parameter.getKeyword());
    assertNotNull(parameter.getType());
    assertNotNull(parameter.getIdentifier());
    assertNull(parameter.getParameters());
  }

  public void test_parseNormalFormalParameter_field_final_noType() throws Exception {
    FieldFormalParameter parameter = parse("parseNormalFormalParameter", "final this.a)");
    assertNotNull(parameter.getKeyword());
    assertNull(parameter.getType());
    assertNotNull(parameter.getIdentifier());
    assertNull(parameter.getParameters());
  }

  public void test_parseNormalFormalParameter_field_final_type() throws Exception {
    FieldFormalParameter parameter = parse("parseNormalFormalParameter", "final A this.a)");
    assertNotNull(parameter.getKeyword());
    assertNotNull(parameter.getType());
    assertNotNull(parameter.getIdentifier());
    assertNull(parameter.getParameters());
  }

  public void test_parseNormalFormalParameter_field_function_nested() throws Exception {
    FieldFormalParameter parameter = parse("parseNormalFormalParameter", "this.a(B b))");
    assertNull(parameter.getKeyword());
    assertNull(parameter.getType());
    assertNotNull(parameter.getIdentifier());
    FormalParameterList parameterList = parameter.getParameters();
    assertNotNull(parameterList);
    assertSizeOfList(1, parameterList.getParameters());
  }

  public void test_parseNormalFormalParameter_field_function_noNested() throws Exception {
    FieldFormalParameter parameter = parse("parseNormalFormalParameter", "this.a())");
    assertNull(parameter.getKeyword());
    assertNull(parameter.getType());
    assertNotNull(parameter.getIdentifier());
    FormalParameterList parameterList = parameter.getParameters();
    assertNotNull(parameterList);
    assertSizeOfList(0, parameterList.getParameters());
  }

  public void test_parseNormalFormalParameter_field_noType() throws Exception {
    FieldFormalParameter parameter = parse("parseNormalFormalParameter", "this.a)");
    assertNull(parameter.getKeyword());
    assertNull(parameter.getType());
    assertNotNull(parameter.getIdentifier());
    assertNull(parameter.getParameters());
  }

  public void test_parseNormalFormalParameter_field_type() throws Exception {
    FieldFormalParameter parameter = parse("parseNormalFormalParameter", "A this.a)");
    assertNull(parameter.getKeyword());
    assertNotNull(parameter.getType());
    assertNotNull(parameter.getIdentifier());
    assertNull(parameter.getParameters());
  }

  public void test_parseNormalFormalParameter_field_var() throws Exception {
    FieldFormalParameter parameter = parse("parseNormalFormalParameter", "var this.a)");
    assertNotNull(parameter.getKeyword());
    assertNull(parameter.getType());
    assertNotNull(parameter.getIdentifier());
    assertNull(parameter.getParameters());
  }

  public void test_parseNormalFormalParameter_function_noType() throws Exception {
    FunctionTypedFormalParameter parameter = parse("parseNormalFormalParameter", "a())");
    assertNull(parameter.getReturnType());
    assertNotNull(parameter.getIdentifier());
    assertNotNull(parameter.getParameters());
  }

  public void test_parseNormalFormalParameter_function_type() throws Exception {
    FunctionTypedFormalParameter parameter = parse("parseNormalFormalParameter", "A a())");
    assertNotNull(parameter.getReturnType());
    assertNotNull(parameter.getIdentifier());
    assertNotNull(parameter.getParameters());
  }

  public void test_parseNormalFormalParameter_function_void() throws Exception {
    FunctionTypedFormalParameter parameter = parse("parseNormalFormalParameter", "void a())");
    assertNotNull(parameter.getReturnType());
    assertNotNull(parameter.getIdentifier());
    assertNotNull(parameter.getParameters());
  }

  public void test_parseNormalFormalParameter_simple_const_noType() throws Exception {
    SimpleFormalParameter parameter = parse("parseNormalFormalParameter", "const a)");
    assertNotNull(parameter.getKeyword());
    assertNull(parameter.getType());
    assertNotNull(parameter.getIdentifier());
  }

  public void test_parseNormalFormalParameter_simple_const_type() throws Exception {
    SimpleFormalParameter parameter = parse("parseNormalFormalParameter", "const A a)");
    assertNotNull(parameter.getKeyword());
    assertNotNull(parameter.getType());
    assertNotNull(parameter.getIdentifier());
  }

  public void test_parseNormalFormalParameter_simple_final_noType() throws Exception {
    SimpleFormalParameter parameter = parse("parseNormalFormalParameter", "final a)");
    assertNotNull(parameter.getKeyword());
    assertNull(parameter.getType());
    assertNotNull(parameter.getIdentifier());
  }

  public void test_parseNormalFormalParameter_simple_final_type() throws Exception {
    SimpleFormalParameter parameter = parse("parseNormalFormalParameter", "final A a)");
    assertNotNull(parameter.getKeyword());
    assertNotNull(parameter.getType());
    assertNotNull(parameter.getIdentifier());
  }

  public void test_parseNormalFormalParameter_simple_noType() throws Exception {
    SimpleFormalParameter parameter = parse("parseNormalFormalParameter", "a)");
    assertNull(parameter.getKeyword());
    assertNull(parameter.getType());
    assertNotNull(parameter.getIdentifier());
  }

  public void test_parseNormalFormalParameter_simple_type() throws Exception {
    SimpleFormalParameter parameter = parse("parseNormalFormalParameter", "A a)");
    assertNull(parameter.getKeyword());
    assertNotNull(parameter.getType());
    assertNotNull(parameter.getIdentifier());
  }

  public void test_parseOperator() throws Exception {
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    TypeName returnType = new TypeName(new SimpleIdentifier(null), null);
    MethodDeclaration method = parse("parseOperator", new Object[] {
        commentAndMetadata(comment), null, returnType}, "operator +(A a);");
    assertNotNull(method.getBody());
    assertEquals(comment, method.getDocumentationComment());
    assertNull(method.getExternalKeyword());
    assertNull(method.getModifierKeyword());
    assertNotNull(method.getName());
    assertNotNull(method.getOperatorKeyword());
    assertNotNull(method.getParameters());
    assertNull(method.getPropertyKeyword());
    assertEquals(returnType, method.getReturnType());
  }

  public void test_parseOptionalReturnType() throws Exception {
    // TODO(brianwilkerson) Implement tests for this method.
  }

  public void test_parsePartDirective_part() throws Exception {
    PartDirective directive = parse(
        "parsePartDirective",
        new Object[] {emptyCommentAndMetadata()},
        "part 'lib/lib.dart';");
    assertNotNull(directive.getPartToken());
    assertNotNull(directive.getUri());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parsePartDirective_partOf() throws Exception {
    PartOfDirective directive = parse(
        "parsePartDirective",
        new Object[] {emptyCommentAndMetadata()},
        "part of l;");
    assertNotNull(directive.getPartToken());
    assertNotNull(directive.getOfToken());
    assertNotNull(directive.getLibraryName());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parsePostfixExpression_decrement() throws Exception {
    PostfixExpression expression = parse("parsePostfixExpression", "i--");
    assertNotNull(expression.getOperand());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.MINUS_MINUS, expression.getOperator().getType());
  }

  public void test_parsePostfixExpression_increment() throws Exception {
    PostfixExpression expression = parse("parsePostfixExpression", "i++");
    assertNotNull(expression.getOperand());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.PLUS_PLUS, expression.getOperator().getType());
  }

  public void test_parsePostfixExpression_none_indexExpression() throws Exception {
    IndexExpression expression = parse("parsePostfixExpression", "a[0]");
    assertNotNull(expression.getTarget());
    assertNotNull(expression.getIndex());
  }

  public void test_parsePostfixExpression_none_methodInvocation() throws Exception {
    MethodInvocation expression = parse("parsePostfixExpression", "a.m()");
    assertNotNull(expression.getTarget());
    assertNotNull(expression.getMethodName());
    assertNotNull(expression.getArgumentList());
  }

  public void test_parsePostfixExpression_none_propertyAccess() throws Exception {
    PrefixedIdentifier expression = parse("parsePostfixExpression", "a.b");
    assertNotNull(expression.getPrefix());
    assertNotNull(expression.getIdentifier());
  }

  public void test_parsePrefixedIdentifier_noPrefix() throws Exception {
    String lexeme = "bar";
    SimpleIdentifier identifier = parse("parsePrefixedIdentifier", lexeme);
    assertNotNull(identifier.getToken());
    assertEquals(lexeme, identifier.getName());
  }

  public void test_parsePrefixedIdentifier_prefix() throws Exception {
    String lexeme = "foo.bar";
    PrefixedIdentifier identifier = parse("parsePrefixedIdentifier", lexeme);
    assertEquals("foo", identifier.getPrefix().getName());
    assertNotNull(identifier.getPeriod());
    assertEquals("bar", identifier.getIdentifier().getName());
  }

  public void test_parsePrimaryExpression_const() throws Exception {
    InstanceCreationExpression expression = parse("parsePrimaryExpression", "const A()");
    assertNotNull(expression);
  }

  public void test_parsePrimaryExpression_double() throws Exception {
    String doubleLiteral = "3.2e4";
    DoubleLiteral literal = parse("parsePrimaryExpression", doubleLiteral);
    assertNotNull(literal.getLiteral());
    assertEquals(Double.parseDouble(doubleLiteral), literal.getValue());
  }

  public void test_parsePrimaryExpression_false() throws Exception {
    BooleanLiteral literal = parse("parsePrimaryExpression", "false");
    assertNotNull(literal.getLiteral());
    assertFalse(literal.getValue());
  }

  public void test_parsePrimaryExpression_function_arguments() throws Exception {
    FunctionExpression expression = parse("parsePrimaryExpression", "(int i) => i + 1");
    assertNotNull(expression.getParameters());
    assertNotNull(expression.getBody());
  }

  public void test_parsePrimaryExpression_function_noArguments() throws Exception {
    FunctionExpression expression = parse("parsePrimaryExpression", "() => 42");
    assertNotNull(expression.getParameters());
    assertNotNull(expression.getBody());
  }

  public void test_parsePrimaryExpression_hex() throws Exception {
    String hexLiteral = "3F";
    IntegerLiteral literal = parse("parsePrimaryExpression", "0x" + hexLiteral);
    assertNotNull(literal.getLiteral());
    assertEquals(BigInteger.valueOf(Integer.parseInt(hexLiteral, 16)), literal.getValue());
  }

  public void test_parsePrimaryExpression_identifier() throws Exception {
    SimpleIdentifier identifier = parse("parsePrimaryExpression", "a");
    assertNotNull(identifier);
  }

  public void test_parsePrimaryExpression_int() throws Exception {
    String intLiteral = "472";
    IntegerLiteral literal = parse("parsePrimaryExpression", intLiteral);
    assertNotNull(literal.getLiteral());
    assertEquals(BigInteger.valueOf(Integer.parseInt(intLiteral)), literal.getValue());
  }

  public void test_parsePrimaryExpression_listLiteral() throws Exception {
    ListLiteral literal = parse("parsePrimaryExpression", "[ ]");
    assertNotNull(literal);
  }

  public void test_parsePrimaryExpression_listLiteral_index() throws Exception {
    ListLiteral literal = parse("parsePrimaryExpression", "[]");
    assertNotNull(literal);
  }

  public void test_parsePrimaryExpression_listLiteral_typed() throws Exception {
    ListLiteral literal = parse("parsePrimaryExpression", "<A>[ ]");
    assertNotNull(literal.getTypeArguments());
    assertSizeOfList(1, literal.getTypeArguments().getArguments());
  }

  public void test_parsePrimaryExpression_mapLiteral() throws Exception {
    MapLiteral literal = parse("parsePrimaryExpression", "{}");
    assertNotNull(literal);
  }

  public void test_parsePrimaryExpression_mapLiteral_typed() throws Exception {
    MapLiteral literal = parse("parsePrimaryExpression", "<A, B>{}");
    assertNotNull(literal.getTypeArguments());
    assertSizeOfList(2, literal.getTypeArguments().getArguments());
  }

  public void test_parsePrimaryExpression_new() throws Exception {
    InstanceCreationExpression expression = parse("parsePrimaryExpression", "new A()");
    assertNotNull(expression);
  }

  public void test_parsePrimaryExpression_null() throws Exception {
    NullLiteral literal = parse("parsePrimaryExpression", "null");
    assertNotNull(literal.getLiteral());
  }

  public void test_parsePrimaryExpression_parenthesized() throws Exception {
    ParenthesizedExpression expression = parse("parsePrimaryExpression", "(x)");
    assertNotNull(expression);
  }

  public void test_parsePrimaryExpression_string() throws Exception {
    SimpleStringLiteral literal = parse("parsePrimaryExpression", "\"string\"");
    assertFalse(literal.isMultiline());
    assertFalse(literal.isRaw());
    assertEquals("string", literal.getValue());
  }

  public void test_parsePrimaryExpression_string_multiline() throws Exception {
    SimpleStringLiteral literal = parse("parsePrimaryExpression", "'''string'''");
    assertTrue(literal.isMultiline());
    assertFalse(literal.isRaw());
    assertEquals("string", literal.getValue());
  }

  public void test_parsePrimaryExpression_string_raw() throws Exception {
    SimpleStringLiteral literal = parse("parsePrimaryExpression", "r'string'");
    assertFalse(literal.isMultiline());
    assertTrue(literal.isRaw());
    assertEquals("string", literal.getValue());
  }

  public void test_parsePrimaryExpression_super() throws Exception {
    PropertyAccess propertyAccess = parse("parsePrimaryExpression", "super.x");
    assertTrue(propertyAccess.getTarget() instanceof SuperExpression);
    assertNotNull(propertyAccess.getOperator());
    assertEquals(TokenType.PERIOD, propertyAccess.getOperator().getType());
    assertNotNull(propertyAccess.getPropertyName());
  }

  public void test_parsePrimaryExpression_this() throws Exception {
    ThisExpression expression = parse("parsePrimaryExpression", "this");
    assertNotNull(expression.getKeyword());
  }

  public void test_parsePrimaryExpression_true() throws Exception {
    BooleanLiteral literal = parse("parsePrimaryExpression", "true");
    assertNotNull(literal.getLiteral());
    assertTrue(literal.getValue());
  }

  public void test_Parser() {
    assertNotNull(new Parser(null, null));
  }

  public void test_parseRedirectingConstructorInvocation_named() throws Exception {
    RedirectingConstructorInvocation invocation = parse(
        "parseRedirectingConstructorInvocation",
        "this.a()");
    assertNotNull(invocation.getArgumentList());
    assertNotNull(invocation.getConstructorName());
    assertNotNull(invocation.getKeyword());
    assertNotNull(invocation.getPeriod());
  }

  public void test_parseRedirectingConstructorInvocation_unnamed() throws Exception {
    RedirectingConstructorInvocation invocation = parse(
        "parseRedirectingConstructorInvocation",
        "this()");
    assertNotNull(invocation.getArgumentList());
    assertNull(invocation.getConstructorName());
    assertNotNull(invocation.getKeyword());
    assertNull(invocation.getPeriod());
  }

  public void test_parseRelationalExpression_as() throws Exception {
    AsExpression expression = parse("parseRelationalExpression", "x as Y");
    assertNotNull(expression.getExpression());
    assertNotNull(expression.getAsOperator());
    assertNotNull(expression.getType());
  }

  public void test_parseRelationalExpression_is() throws Exception {
    IsExpression expression = parse("parseRelationalExpression", "x is y");
    assertNotNull(expression.getExpression());
    assertNotNull(expression.getIsOperator());
    assertNull(expression.getNotOperator());
    assertNotNull(expression.getType());
  }

  public void test_parseRelationalExpression_isNot() throws Exception {
    IsExpression expression = parse("parseRelationalExpression", "x is! y");
    assertNotNull(expression.getExpression());
    assertNotNull(expression.getIsOperator());
    assertNotNull(expression.getNotOperator());
    assertNotNull(expression.getType());
  }

  public void test_parseRelationalExpression_normal() throws Exception {
    BinaryExpression expression = parse("parseRelationalExpression", "x < y");
    assertNotNull(expression.getLeftOperand());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.LT, expression.getOperator().getType());
    assertNotNull(expression.getRightOperand());
  }

  public void test_parseRelationalExpression_super() throws Exception {
    BinaryExpression expression = parse("parseRelationalExpression", "super < y");
    assertNotNull(expression.getLeftOperand());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.LT, expression.getOperator().getType());
    assertNotNull(expression.getRightOperand());
  }

  public void test_parseRethrowExpression() throws Exception {
    RethrowExpression expression = parse("parseRethrowExpression", "rethrow;");
    assertNotNull(expression.getKeyword());
  }

  public void test_parseReturnStatement_noValue() throws Exception {
    ReturnStatement statement = parse("parseReturnStatement", "return;");
    assertNotNull(statement.getKeyword());
    assertNull(statement.getExpression());
    assertNotNull(statement.getSemicolon());
  }

  public void test_parseReturnStatement_value() throws Exception {
    ReturnStatement statement = parse("parseReturnStatement", "return x;");
    assertNotNull(statement.getKeyword());
    assertNotNull(statement.getExpression());
    assertNotNull(statement.getSemicolon());
  }

  public void test_parseReturnType_nonVoid() throws Exception {
    TypeName typeName = parse("parseReturnType", "A<B>");
    assertNotNull(typeName.getName());
    assertNotNull(typeName.getTypeArguments());
  }

  public void test_parseReturnType_void() throws Exception {
    TypeName typeName = parse("parseReturnType", "void");
    assertNotNull(typeName.getName());
    assertNull(typeName.getTypeArguments());
  }

  public void test_parseSetter_nonStatic() throws Exception {
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    TypeName returnType = new TypeName(new SimpleIdentifier(null), null);
    MethodDeclaration method = parse("parseSetter", new Object[] {
        commentAndMetadata(comment), null, null, returnType}, "set a(var x);");
    assertNotNull(method.getBody());
    assertEquals(comment, method.getDocumentationComment());
    assertNull(method.getExternalKeyword());
    assertNull(method.getModifierKeyword());
    assertNotNull(method.getName());
    assertNull(method.getOperatorKeyword());
    assertNotNull(method.getParameters());
    assertNotNull(method.getPropertyKeyword());
    assertEquals(returnType, method.getReturnType());
  }

  public void test_parseSetter_static() throws Exception {
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    Token staticKeyword = tokenFromKeyword(Keyword.STATIC);
    TypeName returnType = new TypeName(new SimpleIdentifier(null), null);
    MethodDeclaration method = parse("parseSetter", new Object[] {
        commentAndMetadata(comment), null, staticKeyword, returnType}, "set a(var x) {}");
    assertNotNull(method.getBody());
    assertEquals(comment, method.getDocumentationComment());
    assertNull(method.getExternalKeyword());
    assertEquals(staticKeyword, method.getModifierKeyword());
    assertNotNull(method.getName());
    assertNull(method.getOperatorKeyword());
    assertNotNull(method.getParameters());
    assertNotNull(method.getPropertyKeyword());
    assertEquals(returnType, method.getReturnType());
  }

  public void test_parseShiftExpression_normal() throws Exception {
    BinaryExpression expression = parse("parseShiftExpression", "x << y");
    assertNotNull(expression.getLeftOperand());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.LT_LT, expression.getOperator().getType());
    assertNotNull(expression.getRightOperand());
  }

  public void test_parseShiftExpression_super() throws Exception {
    BinaryExpression expression = parse("parseShiftExpression", "super << y");
    assertNotNull(expression.getLeftOperand());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.LT_LT, expression.getOperator().getType());
    assertNotNull(expression.getRightOperand());
  }

  public void test_parseSimpleIdentifier_builtInIdentifier() throws Exception {
    String lexeme = "as";
    SimpleIdentifier identifier = parse("parseSimpleIdentifier", lexeme);
    assertNotNull(identifier.getToken());
    assertEquals(lexeme, identifier.getName());
  }

  public void test_parseSimpleIdentifier_normalIdentifier() throws Exception {
    String lexeme = "foo";
    SimpleIdentifier identifier = parse("parseSimpleIdentifier", lexeme);
    assertNotNull(identifier.getToken());
    assertEquals(lexeme, identifier.getName());
  }

  public void test_parseSimpleIdentifier1_normalIdentifier() throws Exception {
    // TODO(brianwilkerson) Implement tests for this method.
  }

  public void test_parseStatement_functionDeclaration() throws Exception {
    // TODO(brianwilkerson) Implement more tests for this method.
    FunctionDeclarationStatement statement = parse("parseStatement", "int f(a, b) {};");
    assertNotNull(statement.getFunctionDeclaration());
  }

  public void test_parseStatement_mulipleLabels() throws Exception {
    LabeledStatement statement = parse("parseStatement", "l: m: return x;");
    assertSizeOfList(2, statement.getLabels());
    assertNotNull(statement.getStatement());
  }

  public void test_parseStatement_noLabels() throws Exception {
    parse("parseStatement", "return x;");
  }

  public void test_parseStatement_singleLabel() throws Exception {
    LabeledStatement statement = parse("parseStatement", "l: return x;");
    assertSizeOfList(1, statement.getLabels());
    assertNotNull(statement.getStatement());
  }

  public void test_parseStatements_multiple() throws Exception {
    List<Statement> statements = parseStatements("return; return;", 2);
    assertSizeOfList(2, statements);
  }

  public void test_parseStatements_single() throws Exception {
    List<Statement> statements = parseStatements("return;", 1);
    assertSizeOfList(1, statements);
  }

  public void test_parseStringLiteral_adjacent() throws Exception {
    AdjacentStrings literal = parse("parseStringLiteral", "'a' 'b'");
    NodeList<StringLiteral> strings = literal.getStrings();
    assertSizeOfList(2, strings);
    StringLiteral firstString = strings.get(0);
    StringLiteral secondString = strings.get(1);
    assertEquals("a", ((SimpleStringLiteral) firstString).getValue());
    assertEquals("b", ((SimpleStringLiteral) secondString).getValue());
  }

  public void test_parseStringLiteral_interpolated() throws Exception {
    StringInterpolation literal = parse("parseStringLiteral", "'a ${b} c $this d'");
    NodeList<InterpolationElement> elements = literal.getElements();
    assertSizeOfList(5, elements);
    assertTrue(elements.get(0) instanceof InterpolationString);
    assertTrue(elements.get(1) instanceof InterpolationExpression);
    assertTrue(elements.get(2) instanceof InterpolationString);
    assertTrue(elements.get(3) instanceof InterpolationExpression);
    assertTrue(elements.get(4) instanceof InterpolationString);
  }

  public void test_parseStringLiteral_single() throws Exception {
    SimpleStringLiteral literal = parse("parseStringLiteral", "'a'");
    assertNotNull(literal.getLiteral());
    assertEquals("a", literal.getValue());
  }

  public void test_parseSuperConstructorInvocation_named() throws Exception {
    SuperConstructorInvocation invocation = parse("parseSuperConstructorInvocation", "super.a()");
    assertNotNull(invocation.getArgumentList());
    assertNotNull(invocation.getConstructorName());
    assertNotNull(invocation.getKeyword());
    assertNotNull(invocation.getPeriod());
  }

  public void test_parseSuperConstructorInvocation_unnamed() throws Exception {
    SuperConstructorInvocation invocation = parse("parseSuperConstructorInvocation", "super()");
    assertNotNull(invocation.getArgumentList());
    assertNull(invocation.getConstructorName());
    assertNotNull(invocation.getKeyword());
    assertNull(invocation.getPeriod());
  }

  public void test_parseSwitchStatement_case() throws Exception {
    SwitchStatement statement = parse("parseSwitchStatement", "switch (a) {case 1: return 'I';}");
    assertNotNull(statement.getKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNotNull(statement.getExpression());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getLeftBracket());
    assertSizeOfList(1, statement.getMembers());
    assertNotNull(statement.getRightBracket());
  }

  public void test_parseSwitchStatement_empty() throws Exception {
    SwitchStatement statement = parse("parseSwitchStatement", "switch (a) {}");
    assertNotNull(statement.getKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNotNull(statement.getExpression());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getLeftBracket());
    assertSizeOfList(0, statement.getMembers());
    assertNotNull(statement.getRightBracket());
  }

  public void test_parseSwitchStatement_labeledCase() throws Exception {
    SwitchStatement statement = parse("parseSwitchStatement", "switch (a) {l1: l2: l3: case(1):}");
    assertNotNull(statement.getKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNotNull(statement.getExpression());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getLeftBracket());
    assertSizeOfList(1, statement.getMembers());
    assertSizeOfList(3, statement.getMembers().get(0).getLabels());
    assertNotNull(statement.getRightBracket());
  }

  public void test_parseSwitchStatement_labeledStatementInCase() throws Exception {
    SwitchStatement statement = parse(
        "parseSwitchStatement",
        "switch (a) {case 0: f(); l1: g(); break;}");
    assertNotNull(statement.getKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNotNull(statement.getExpression());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getLeftBracket());
    assertSizeOfList(1, statement.getMembers());
    assertSizeOfList(3, statement.getMembers().get(0).getStatements());
    assertNotNull(statement.getRightBracket());
  }

  public void test_parseSymbolLiteral_builtInIdentifier() throws Exception {
    SymbolLiteral literal = parse("parseSymbolLiteral", "#dynamic.static.abstract");
    assertNotNull(literal.getPoundSign());
    Token[] components = literal.getComponents();
    assertLength(3, components);
    assertEquals("dynamic", components[0].getLexeme());
    assertEquals("static", components[1].getLexeme());
    assertEquals("abstract", components[2].getLexeme());
  }

  public void test_parseSymbolLiteral_multiple() throws Exception {
    SymbolLiteral literal = parse("parseSymbolLiteral", "#a.b.c");
    assertNotNull(literal.getPoundSign());
    Token[] components = literal.getComponents();
    assertLength(3, components);
    assertEquals("a", components[0].getLexeme());
    assertEquals("b", components[1].getLexeme());
    assertEquals("c", components[2].getLexeme());
  }

  public void test_parseSymbolLiteral_operator() throws Exception {
    SymbolLiteral literal = parse("parseSymbolLiteral", "#==");
    assertNotNull(literal.getPoundSign());
    Token[] components = literal.getComponents();
    assertLength(1, components);
    assertEquals("==", components[0].getLexeme());
  }

  public void test_parseSymbolLiteral_single() throws Exception {
    SymbolLiteral literal = parse("parseSymbolLiteral", "#a");
    assertNotNull(literal.getPoundSign());
    Token[] components = literal.getComponents();
    assertLength(1, components);
    assertEquals("a", components[0].getLexeme());
  }

  public void test_parseThrowExpression() throws Exception {
    ThrowExpression expression = parse("parseThrowExpression", "throw x;");
    assertNotNull(expression.getKeyword());
    assertNotNull(expression.getExpression());
  }

  public void test_parseThrowExpressionWithoutCascade() throws Exception {
    ThrowExpression expression = parse("parseThrowExpressionWithoutCascade", "throw x;");
    assertNotNull(expression.getKeyword());
    assertNotNull(expression.getExpression());
  }

  public void test_parseTryStatement_catch() throws Exception {
    TryStatement statement = parse("parseTryStatement", "try {} catch (e) {}");
    assertNotNull(statement.getTryKeyword());
    assertNotNull(statement.getBody());
    NodeList<CatchClause> catchClauses = statement.getCatchClauses();
    assertSizeOfList(1, catchClauses);
    CatchClause clause = catchClauses.get(0);
    assertNull(clause.getOnKeyword());
    assertNull(clause.getExceptionType());
    assertNotNull(clause.getCatchKeyword());
    assertNotNull(clause.getExceptionParameter());
    assertNull(clause.getComma());
    assertNull(clause.getStackTraceParameter());
    assertNotNull(clause.getBody());
    assertNull(statement.getFinallyKeyword());
    assertNull(statement.getFinallyBlock());
  }

  public void test_parseTryStatement_catch_finally() throws Exception {
    TryStatement statement = parse("parseTryStatement", "try {} catch (e, s) {} finally {}");
    assertNotNull(statement.getTryKeyword());
    assertNotNull(statement.getBody());
    NodeList<CatchClause> catchClauses = statement.getCatchClauses();
    assertSizeOfList(1, catchClauses);
    CatchClause clause = catchClauses.get(0);
    assertNull(clause.getOnKeyword());
    assertNull(clause.getExceptionType());
    assertNotNull(clause.getCatchKeyword());
    assertNotNull(clause.getExceptionParameter());
    assertNotNull(clause.getComma());
    assertNotNull(clause.getStackTraceParameter());
    assertNotNull(clause.getBody());
    assertNotNull(statement.getFinallyKeyword());
    assertNotNull(statement.getFinallyBlock());
  }

  public void test_parseTryStatement_finally() throws Exception {
    TryStatement statement = parse("parseTryStatement", "try {} finally {}");
    assertNotNull(statement.getTryKeyword());
    assertNotNull(statement.getBody());
    assertSizeOfList(0, statement.getCatchClauses());
    assertNotNull(statement.getFinallyKeyword());
    assertNotNull(statement.getFinallyBlock());
  }

  public void test_parseTryStatement_multiple() throws Exception {
    TryStatement statement = parse(
        "parseTryStatement",
        "try {} on NPE catch (e) {} on Error {} catch (e) {}");
    assertNotNull(statement.getTryKeyword());
    assertNotNull(statement.getBody());
    assertSizeOfList(3, statement.getCatchClauses());
    assertNull(statement.getFinallyKeyword());
    assertNull(statement.getFinallyBlock());
  }

  public void test_parseTryStatement_on() throws Exception {
    TryStatement statement = parse("parseTryStatement", "try {} on Error {}");
    assertNotNull(statement.getTryKeyword());
    assertNotNull(statement.getBody());
    NodeList<CatchClause> catchClauses = statement.getCatchClauses();
    assertSizeOfList(1, catchClauses);
    CatchClause clause = catchClauses.get(0);
    assertNotNull(clause.getOnKeyword());
    assertNotNull(clause.getExceptionType());
    assertNull(clause.getCatchKeyword());
    assertNull(clause.getExceptionParameter());
    assertNull(clause.getComma());
    assertNull(clause.getStackTraceParameter());
    assertNotNull(clause.getBody());
    assertNull(statement.getFinallyKeyword());
    assertNull(statement.getFinallyBlock());
  }

  public void test_parseTryStatement_on_catch() throws Exception {
    TryStatement statement = parse("parseTryStatement", "try {} on Error catch (e, s) {}");
    assertNotNull(statement.getTryKeyword());
    assertNotNull(statement.getBody());
    NodeList<CatchClause> catchClauses = statement.getCatchClauses();
    assertSizeOfList(1, catchClauses);
    CatchClause clause = catchClauses.get(0);
    assertNotNull(clause.getOnKeyword());
    assertNotNull(clause.getExceptionType());
    assertNotNull(clause.getCatchKeyword());
    assertNotNull(clause.getExceptionParameter());
    assertNotNull(clause.getComma());
    assertNotNull(clause.getStackTraceParameter());
    assertNotNull(clause.getBody());
    assertNull(statement.getFinallyKeyword());
    assertNull(statement.getFinallyBlock());
  }

  public void test_parseTryStatement_on_catch_finally() throws Exception {
    TryStatement statement = parse(
        "parseTryStatement",
        "try {} on Error catch (e, s) {} finally {}");
    assertNotNull(statement.getTryKeyword());
    assertNotNull(statement.getBody());
    NodeList<CatchClause> catchClauses = statement.getCatchClauses();
    assertSizeOfList(1, catchClauses);
    CatchClause clause = catchClauses.get(0);
    assertNotNull(clause.getOnKeyword());
    assertNotNull(clause.getExceptionType());
    assertNotNull(clause.getCatchKeyword());
    assertNotNull(clause.getExceptionParameter());
    assertNotNull(clause.getComma());
    assertNotNull(clause.getStackTraceParameter());
    assertNotNull(clause.getBody());
    assertNotNull(statement.getFinallyKeyword());
    assertNotNull(statement.getFinallyBlock());
  }

  public void test_parseTypeAlias_function_noParameters() throws Exception {
    FunctionTypeAlias typeAlias = parse(
        "parseTypeAlias",
        new Object[] {emptyCommentAndMetadata()},
        "typedef bool F();");
    assertNotNull(typeAlias.getKeyword());
    assertNotNull(typeAlias.getName());
    assertNotNull(typeAlias.getParameters());
    assertNotNull(typeAlias.getReturnType());
    assertNotNull(typeAlias.getSemicolon());
    assertNull(typeAlias.getTypeParameters());
  }

  public void test_parseTypeAlias_function_noReturnType() throws Exception {
    FunctionTypeAlias typeAlias = parse(
        "parseTypeAlias",
        new Object[] {emptyCommentAndMetadata()},
        "typedef F();");
    assertNotNull(typeAlias.getKeyword());
    assertNotNull(typeAlias.getName());
    assertNotNull(typeAlias.getParameters());
    assertNull(typeAlias.getReturnType());
    assertNotNull(typeAlias.getSemicolon());
    assertNull(typeAlias.getTypeParameters());
  }

  public void test_parseTypeAlias_function_parameterizedReturnType() throws Exception {
    FunctionTypeAlias typeAlias = parse(
        "parseTypeAlias",
        new Object[] {emptyCommentAndMetadata()},
        "typedef A<B> F();");
    assertNotNull(typeAlias.getKeyword());
    assertNotNull(typeAlias.getName());
    assertNotNull(typeAlias.getParameters());
    assertNotNull(typeAlias.getReturnType());
    assertNotNull(typeAlias.getSemicolon());
    assertNull(typeAlias.getTypeParameters());
  }

  public void test_parseTypeAlias_function_parameters() throws Exception {
    FunctionTypeAlias typeAlias = parse(
        "parseTypeAlias",
        new Object[] {emptyCommentAndMetadata()},
        "typedef bool F(Object value);");
    assertNotNull(typeAlias.getKeyword());
    assertNotNull(typeAlias.getName());
    assertNotNull(typeAlias.getParameters());
    assertNotNull(typeAlias.getReturnType());
    assertNotNull(typeAlias.getSemicolon());
    assertNull(typeAlias.getTypeParameters());
  }

  public void test_parseTypeAlias_function_typeParameters() throws Exception {
    FunctionTypeAlias typeAlias = parse(
        "parseTypeAlias",
        new Object[] {emptyCommentAndMetadata()},
        "typedef bool F<E>();");
    assertNotNull(typeAlias.getKeyword());
    assertNotNull(typeAlias.getName());
    assertNotNull(typeAlias.getParameters());
    assertNotNull(typeAlias.getReturnType());
    assertNotNull(typeAlias.getSemicolon());
    assertNotNull(typeAlias.getTypeParameters());
  }

  public void test_parseTypeAlias_function_voidReturnType() throws Exception {
    FunctionTypeAlias typeAlias = parse(
        "parseTypeAlias",
        new Object[] {emptyCommentAndMetadata()},
        "typedef void F();");
    assertNotNull(typeAlias.getKeyword());
    assertNotNull(typeAlias.getName());
    assertNotNull(typeAlias.getParameters());
    assertNotNull(typeAlias.getReturnType());
    assertNotNull(typeAlias.getSemicolon());
    assertNull(typeAlias.getTypeParameters());
  }

  public void test_parseTypeArgumentList_multiple() throws Exception {
    TypeArgumentList argumentList = parse("parseTypeArgumentList", "<int, int, int>");
    assertNotNull(argumentList.getLeftBracket());
    assertSizeOfList(3, argumentList.getArguments());
    assertNotNull(argumentList.getRightBracket());
  }

  public void test_parseTypeArgumentList_nested() throws Exception {
    TypeArgumentList argumentList = parse("parseTypeArgumentList", "<A<B>>");
    assertNotNull(argumentList.getLeftBracket());
    assertSizeOfList(1, argumentList.getArguments());
    TypeName argument = argumentList.getArguments().get(0);
    assertNotNull(argument);
    TypeArgumentList innerList = argument.getTypeArguments();
    assertNotNull(innerList);
    assertSizeOfList(1, innerList.getArguments());
    assertNotNull(argumentList.getRightBracket());
  }

  public void test_parseTypeArgumentList_single() throws Exception {
    TypeArgumentList argumentList = parse("parseTypeArgumentList", "<int>");
    assertNotNull(argumentList.getLeftBracket());
    assertSizeOfList(1, argumentList.getArguments());
    assertNotNull(argumentList.getRightBracket());
  }

  public void test_parseTypeName_parameterized() throws Exception {
    TypeName typeName = parse("parseTypeName", "List<int>");
    assertNotNull(typeName.getName());
    assertNotNull(typeName.getTypeArguments());
  }

  public void test_parseTypeName_simple() throws Exception {
    TypeName typeName = parse("parseTypeName", "int");
    assertNotNull(typeName.getName());
    assertNull(typeName.getTypeArguments());
  }

  public void test_parseTypeParameter_bounded() throws Exception {
    TypeParameter parameter = parse("parseTypeParameter", "A extends B");
    assertNotNull(parameter.getBound());
    assertNotNull(parameter.getKeyword());
    assertNotNull(parameter.getName());
  }

  public void test_parseTypeParameter_simple() throws Exception {
    TypeParameter parameter = parse("parseTypeParameter", "A");
    assertNull(parameter.getBound());
    assertNull(parameter.getKeyword());
    assertNotNull(parameter.getName());
  }

  public void test_parseTypeParameterList_multiple() throws Exception {
    TypeParameterList parameterList = parse("parseTypeParameterList", "<A, B extends C, D>");
    assertNotNull(parameterList.getLeftBracket());
    assertNotNull(parameterList.getRightBracket());
    assertSizeOfList(3, parameterList.getTypeParameters());
  }

  public void test_parseTypeParameterList_parameterizedWithTrailingEquals() throws Exception {
    TypeParameterList parameterList = parse("parseTypeParameterList", "<A extends B<E>>=");
    assertNotNull(parameterList.getLeftBracket());
    assertNotNull(parameterList.getRightBracket());
    assertSizeOfList(1, parameterList.getTypeParameters());
  }

  public void test_parseTypeParameterList_single() throws Exception {
    TypeParameterList parameterList = parse("parseTypeParameterList", "<A>");
    assertNotNull(parameterList.getLeftBracket());
    assertNotNull(parameterList.getRightBracket());
    assertSizeOfList(1, parameterList.getTypeParameters());
  }

  public void test_parseTypeParameterList_withTrailingEquals() throws Exception {
    TypeParameterList parameterList = parse("parseTypeParameterList", "<A>=");
    assertNotNull(parameterList.getLeftBracket());
    assertNotNull(parameterList.getRightBracket());
    assertSizeOfList(1, parameterList.getTypeParameters());
  }

  public void test_parseUnaryExpression_decrement_normal() throws Exception {
    PrefixExpression expression = parse("parseUnaryExpression", "--x");
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.MINUS_MINUS, expression.getOperator().getType());
    assertNotNull(expression.getOperand());
  }

  public void test_parseUnaryExpression_decrement_super() throws Exception {
    PrefixExpression expression = parse("parseUnaryExpression", "--super");
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.MINUS, expression.getOperator().getType());
    Expression innerExpression = expression.getOperand();
    assertNotNull(innerExpression);
    assertTrue(innerExpression instanceof PrefixExpression);
    PrefixExpression operand = (PrefixExpression) innerExpression;
    assertNotNull(operand.getOperator());
    assertEquals(TokenType.MINUS, operand.getOperator().getType());
    assertNotNull(operand.getOperand());
  }

  public void test_parseUnaryExpression_decrement_super_propertyAccess() throws Exception {
    PrefixExpression expression = parse("parseUnaryExpression", "--super.x");
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.MINUS_MINUS, expression.getOperator().getType());
    assertNotNull(expression.getOperand());
    PropertyAccess operand = (PropertyAccess) expression.getOperand();
    assertTrue(operand.getTarget() instanceof SuperExpression);
    assertEquals("x", operand.getPropertyName().getName());
  }

  public void test_parseUnaryExpression_increment_normal() throws Exception {
    PrefixExpression expression = parse("parseUnaryExpression", "++x");
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.PLUS_PLUS, expression.getOperator().getType());
    assertNotNull(expression.getOperand());
  }

  public void test_parseUnaryExpression_increment_super_index() throws Exception {
    PrefixExpression expression = parse("parseUnaryExpression", "++super[0]");
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.PLUS_PLUS, expression.getOperator().getType());
    assertNotNull(expression.getOperand());
    IndexExpression operand = (IndexExpression) expression.getOperand();
    assertTrue(operand.getRealTarget() instanceof SuperExpression);
    assertTrue(operand.getIndex() instanceof IntegerLiteral);
  }

  public void test_parseUnaryExpression_increment_super_propertyAccess() throws Exception {
    PrefixExpression expression = parse("parseUnaryExpression", "++super.x");
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.PLUS_PLUS, expression.getOperator().getType());
    assertNotNull(expression.getOperand());
    PropertyAccess operand = (PropertyAccess) expression.getOperand();
    assertTrue(operand.getTarget() instanceof SuperExpression);
    assertEquals("x", operand.getPropertyName().getName());
  }

  public void test_parseUnaryExpression_minus_normal() throws Exception {
    PrefixExpression expression = parse("parseUnaryExpression", "-x");
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.MINUS, expression.getOperator().getType());
    assertNotNull(expression.getOperand());
  }

  public void test_parseUnaryExpression_minus_super() throws Exception {
    PrefixExpression expression = parse("parseUnaryExpression", "-super");
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.MINUS, expression.getOperator().getType());
    assertNotNull(expression.getOperand());
  }

  public void test_parseUnaryExpression_not_normal() throws Exception {
    PrefixExpression expression = parse("parseUnaryExpression", "!x");
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.BANG, expression.getOperator().getType());
    assertNotNull(expression.getOperand());
  }

  public void test_parseUnaryExpression_not_super() throws Exception {
    PrefixExpression expression = parse("parseUnaryExpression", "!super");
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.BANG, expression.getOperator().getType());
    assertNotNull(expression.getOperand());
  }

  public void test_parseUnaryExpression_tilda_normal() throws Exception {
    PrefixExpression expression = parse("parseUnaryExpression", "~x");
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.TILDE, expression.getOperator().getType());
    assertNotNull(expression.getOperand());
  }

  public void test_parseUnaryExpression_tilda_super() throws Exception {
    PrefixExpression expression = parse("parseUnaryExpression", "~super");
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.TILDE, expression.getOperator().getType());
    assertNotNull(expression.getOperand());
  }

  public void test_parseVariableDeclaration_equals() throws Exception {
    VariableDeclaration declaration = parse("parseVariableDeclaration", "a = b");
    assertNotNull(declaration.getName());
    assertNotNull(declaration.getEquals());
    assertNotNull(declaration.getInitializer());
  }

  public void test_parseVariableDeclaration_noEquals() throws Exception {
    VariableDeclaration declaration = parse("parseVariableDeclaration", "a");
    assertNotNull(declaration.getName());
    assertNull(declaration.getEquals());
    assertNull(declaration.getInitializer());
  }

  public void test_parseVariableDeclarationListAfterMetadata_const_noType() throws Exception {
    VariableDeclarationList declarationList = parse(
        "parseVariableDeclarationListAfterMetadata",
        new Object[] {emptyCommentAndMetadata()},
        "const a");
    assertNotNull(declarationList.getKeyword());
    assertNull(declarationList.getType());
    assertSizeOfList(1, declarationList.getVariables());
  }

  public void test_parseVariableDeclarationListAfterMetadata_const_type() throws Exception {
    VariableDeclarationList declarationList = parse(
        "parseVariableDeclarationListAfterMetadata",
        new Object[] {emptyCommentAndMetadata()},
        "const A a");
    assertNotNull(declarationList.getKeyword());
    assertNotNull(declarationList.getType());
    assertSizeOfList(1, declarationList.getVariables());
  }

  public void test_parseVariableDeclarationListAfterMetadata_final_noType() throws Exception {
    VariableDeclarationList declarationList = parse(
        "parseVariableDeclarationListAfterMetadata",
        new Object[] {emptyCommentAndMetadata()},
        "final a");
    assertNotNull(declarationList.getKeyword());
    assertNull(declarationList.getType());
    assertSizeOfList(1, declarationList.getVariables());
  }

  public void test_parseVariableDeclarationListAfterMetadata_final_type() throws Exception {
    VariableDeclarationList declarationList = parse(
        "parseVariableDeclarationListAfterMetadata",
        new Object[] {emptyCommentAndMetadata()},
        "final A a");
    assertNotNull(declarationList.getKeyword());
    assertNotNull(declarationList.getType());
    assertSizeOfList(1, declarationList.getVariables());
  }

  public void test_parseVariableDeclarationListAfterMetadata_type_multiple() throws Exception {
    VariableDeclarationList declarationList = parse(
        "parseVariableDeclarationListAfterMetadata",
        new Object[] {emptyCommentAndMetadata()},
        "A a, b, c");
    assertNull(declarationList.getKeyword());
    assertNotNull(declarationList.getType());
    assertSizeOfList(3, declarationList.getVariables());
  }

  public void test_parseVariableDeclarationListAfterMetadata_type_single() throws Exception {
    VariableDeclarationList declarationList = parse(
        "parseVariableDeclarationListAfterMetadata",
        new Object[] {emptyCommentAndMetadata()},
        "A a");
    assertNull(declarationList.getKeyword());
    assertNotNull(declarationList.getType());
    assertSizeOfList(1, declarationList.getVariables());
  }

  public void test_parseVariableDeclarationListAfterMetadata_var_multiple() throws Exception {
    VariableDeclarationList declarationList = parse(
        "parseVariableDeclarationListAfterMetadata",
        new Object[] {emptyCommentAndMetadata()},
        "var a, b, c");
    assertNotNull(declarationList.getKeyword());
    assertNull(declarationList.getType());
    assertSizeOfList(3, declarationList.getVariables());
  }

  public void test_parseVariableDeclarationListAfterMetadata_var_single() throws Exception {
    VariableDeclarationList declarationList = parse(
        "parseVariableDeclarationListAfterMetadata",
        new Object[] {emptyCommentAndMetadata()},
        "var a");
    assertNotNull(declarationList.getKeyword());
    assertNull(declarationList.getType());
    assertSizeOfList(1, declarationList.getVariables());
  }

  public void test_parseVariableDeclarationListAfterType_type() throws Exception {
    TypeName type = new TypeName(new SimpleIdentifier(null), null);
    VariableDeclarationList declarationList = parse(
        "parseVariableDeclarationListAfterType",
        new Object[] {emptyCommentAndMetadata(), null, type},
        "a");
    assertNull(declarationList.getKeyword());
    assertEquals(type, declarationList.getType());
    assertSizeOfList(1, declarationList.getVariables());
  }

  public void test_parseVariableDeclarationListAfterType_var() throws Exception {
    Token keyword = tokenFromKeyword(Keyword.VAR);
    VariableDeclarationList declarationList = parse(
        "parseVariableDeclarationListAfterType",
        new Object[] {emptyCommentAndMetadata(), keyword, null},
        "a, b, c");
    assertEquals(keyword, declarationList.getKeyword());
    assertNull(declarationList.getType());
    assertSizeOfList(3, declarationList.getVariables());
  }

  public void test_parseVariableDeclarationStatementAfterMetadata_multiple() throws Exception {
    VariableDeclarationStatement statement = parse(
        "parseVariableDeclarationStatementAfterMetadata",
        new Object[] {emptyCommentAndMetadata()},
        "var x, y, z;");
    assertNotNull(statement.getSemicolon());
    VariableDeclarationList variableList = statement.getVariables();
    assertNotNull(variableList);
    assertSizeOfList(3, variableList.getVariables());
  }

  public void test_parseVariableDeclarationStatementAfterMetadata_single() throws Exception {
    VariableDeclarationStatement statement = parse(
        "parseVariableDeclarationStatementAfterMetadata",
        new Object[] {emptyCommentAndMetadata()},
        "var x;");
    assertNotNull(statement.getSemicolon());
    VariableDeclarationList variableList = statement.getVariables();
    assertNotNull(variableList);
    assertSizeOfList(1, variableList.getVariables());
  }

  public void test_parseWhileStatement() throws Exception {
    WhileStatement statement = parse("parseWhileStatement", "while (x) {}");
    assertNotNull(statement.getKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNotNull(statement.getCondition());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseWithClause_multiple() throws Exception {
    WithClause clause = parse("parseWithClause", "with A, B, C");
    assertNotNull(clause.getWithKeyword());
    assertSizeOfList(3, clause.getMixinTypes());
  }

  public void test_parseWithClause_single() throws Exception {
    WithClause clause = parse("parseWithClause", "with M");
    assertNotNull(clause.getWithKeyword());
    assertSizeOfList(1, clause.getMixinTypes());
  }

  public void test_parseYieldStatement_each() throws Exception {
    YieldStatement statement = parse("parseYieldStatement", "yield* x;");
    assertNotNull(statement.getYieldKeyword());
    assertNotNull(statement.getStar());
    assertNotNull(statement.getExpression());
    assertNotNull(statement.getSemicolon());
  }

  public void test_parseYieldStatement_normal() throws Exception {
    YieldStatement statement = parse("parseYieldStatement", "yield x;");
    assertNotNull(statement.getYieldKeyword());
    assertNull(statement.getStar());
    assertNotNull(statement.getExpression());
    assertNotNull(statement.getSemicolon());
  }

  public void test_skipPrefixedIdentifier_invalid() throws Exception {
    Token following = skip("skipPrefixedIdentifier", "+");
    assertNull(following);
  }

  public void test_skipPrefixedIdentifier_notPrefixed() throws Exception {
    Token following = skip("skipPrefixedIdentifier", "a +");
    assertNotNull(following);
    assertEquals(TokenType.PLUS, following.getType());
  }

  public void test_skipPrefixedIdentifier_prefixed() throws Exception {
    Token following = skip("skipPrefixedIdentifier", "a.b +");
    assertNotNull(following);
    assertEquals(TokenType.PLUS, following.getType());
  }

  public void test_skipReturnType_invalid() throws Exception {
    Token following = skip("skipReturnType", "+");
    assertNull(following);
  }

  public void test_skipReturnType_type() throws Exception {
    Token following = skip("skipReturnType", "C +");
    assertNotNull(following);
    assertEquals(TokenType.PLUS, following.getType());
  }

  public void test_skipReturnType_void() throws Exception {
    Token following = skip("skipReturnType", "void +");
    assertNotNull(following);
    assertEquals(TokenType.PLUS, following.getType());
  }

  public void test_skipSimpleIdentifier_identifier() throws Exception {
    Token following = skip("skipSimpleIdentifier", "i +");
    assertNotNull(following);
    assertEquals(TokenType.PLUS, following.getType());
  }

  public void test_skipSimpleIdentifier_invalid() throws Exception {
    Token following = skip("skipSimpleIdentifier", "9 +");
    assertNull(following);
  }

  public void test_skipSimpleIdentifier_pseudoKeyword() throws Exception {
    Token following = skip("skipSimpleIdentifier", "as +");
    assertNotNull(following);
    assertEquals(TokenType.PLUS, following.getType());
  }

  public void test_skipStringLiteral_adjacent() throws Exception {
    Token following = skip("skipStringLiteral", "'a' 'b' +");
    assertNotNull(following);
    assertEquals(TokenType.PLUS, following.getType());
  }

  public void test_skipStringLiteral_interpolated() throws Exception {
    Token following = skip("skipStringLiteral", "'a${b}c' +");
    assertNotNull(following);
    assertEquals(TokenType.PLUS, following.getType());
  }

  public void test_skipStringLiteral_invalid() throws Exception {
    Token following = skip("skipStringLiteral", "a");
    assertNull(following);
  }

  public void test_skipStringLiteral_single() throws Exception {
    Token following = skip("skipStringLiteral", "'a' +");
    assertNotNull(following);
    assertEquals(TokenType.PLUS, following.getType());
  }

  public void test_skipTypeArgumentList_invalid() throws Exception {
    Token following = skip("skipTypeArgumentList", "+");
    assertNull(following);
  }

  public void test_skipTypeArgumentList_multiple() throws Exception {
    Token following = skip("skipTypeArgumentList", "<E, F, G> +");
    assertNotNull(following);
    assertEquals(TokenType.PLUS, following.getType());
  }

  public void test_skipTypeArgumentList_single() throws Exception {
    Token following = skip("skipTypeArgumentList", "<E> +");
    assertNotNull(following);
    assertEquals(TokenType.PLUS, following.getType());
  }

  public void test_skipTypeName_invalid() throws Exception {
    Token following = skip("skipTypeName", "+");
    assertNull(following);
  }

  public void test_skipTypeName_parameterized() throws Exception {
    Token following = skip("skipTypeName", "C<E<F<G>>> +");
    assertNotNull(following);
    assertEquals(TokenType.PLUS, following.getType());
  }

  public void test_skipTypeName_simple() throws Exception {
    Token following = skip("skipTypeName", "C +");
    assertNotNull(following);
    assertEquals(TokenType.PLUS, following.getType());
  }

  /**
   * Invoke the method {@link Parser#computeStringValue(String)} with the given argument.
   * 
   * @param lexeme the argument to the method
   * @param first {@code true} if this is the first token in a string literal
   * @param last {@code true} if this is the last token in a string literal
   * @return the result of invoking the method
   * @throws Exception if the method could not be invoked or throws an exception
   */
  private String computeStringValue(String lexeme, boolean first, boolean last) throws Exception {
    AnalysisErrorListener listener = new AnalysisErrorListener() {
      @Override
      public void onError(AnalysisError event) {
        fail("Unexpected compilation error: " + event.getMessage() + " (" + event.getOffset()
            + ", " + event.getLength() + ")");
      }
    };
    Parser parser = new Parser(null, listener);
    return (String) invokeParserMethodImpl(parser, "computeStringValue", new Object[] {
        lexeme, first, last}, null);
  }

  /**
   * Invoke the method {@link Parser#createSyntheticIdentifier()} with the parser set to the token
   * stream produced by scanning the given source.
   * 
   * @param source the source to be scanned to produce the token stream being tested
   * @return the result of invoking the method
   * @throws Exception if the method could not be invoked or throws an exception
   */
  private SimpleIdentifier createSyntheticIdentifier() throws Exception {
    GatheringErrorListener listener = new GatheringErrorListener();
    return invokeParserMethod("createSyntheticIdentifier", "", listener);
  }

  /**
   * Invoke the method {@link Parser#createSyntheticIdentifier()} with the parser set to the token
   * stream produced by scanning the given source.
   * 
   * @param source the source to be scanned to produce the token stream being tested
   * @return the result of invoking the method
   * @throws Exception if the method could not be invoked or throws an exception
   */
  private SimpleStringLiteral createSyntheticStringLiteral() throws Exception {
    GatheringErrorListener listener = new GatheringErrorListener();
    return invokeParserMethod("createSyntheticStringLiteral", "", listener);
  }

  /**
   * Invoke the method {@link Parser#isFunctionDeclaration()} with the parser set to the token
   * stream produced by scanning the given source.
   * 
   * @param source the source to be scanned to produce the token stream being tested
   * @return the result of invoking the method
   * @throws Exception if the method could not be invoked or throws an exception
   */
  private boolean isFunctionDeclaration(String source) throws Exception {
    GatheringErrorListener listener = new GatheringErrorListener();
    return (Boolean) invokeParserMethod("isFunctionDeclaration", source, listener);
  }

  /**
   * Invoke the method {@link Parser#isFunctionExpression()} with the parser set to the token stream
   * produced by scanning the given source.
   * 
   * @param source the source to be scanned to produce the token stream being tested
   * @return the result of invoking the method
   * @throws Exception if the method could not be invoked or throws an exception
   */
  private boolean isFunctionExpression(String source) throws Exception {
    GatheringErrorListener listener = new GatheringErrorListener();
    //
    // Scan the source.
    //
    Scanner scanner = new Scanner(null, new CharSequenceReader(source), listener);
    Token tokenStream = scanner.tokenize();
    //
    // Parse the source.
    //
    Parser parser = new Parser(null, listener);
    return (Boolean) invokeParserMethodImpl(
        parser,
        "isFunctionExpression",
        new Object[] {tokenStream},
        tokenStream);
  }

  /**
   * Invoke the method {@link Parser#isInitializedVariableDeclaration()} with the parser set to the
   * token stream produced by scanning the given source.
   * 
   * @param source the source to be scanned to produce the token stream being tested
   * @return the result of invoking the method
   * @throws Exception if the method could not be invoked or throws an exception
   */
  private boolean isInitializedVariableDeclaration(String source) throws Exception {
    GatheringErrorListener listener = new GatheringErrorListener();
    return (Boolean) invokeParserMethod("isInitializedVariableDeclaration", source, listener);
  }

  /**
   * Invoke the method {@link Parser#isSwitchMember()} with the parser set to the token stream
   * produced by scanning the given source.
   * 
   * @param source the source to be scanned to produce the token stream being tested
   * @return the result of invoking the method
   * @throws Exception if the method could not be invoked or throws an exception
   */
  private boolean isSwitchMember(String source) throws Exception {
    GatheringErrorListener listener = new GatheringErrorListener();
    return (Boolean) invokeParserMethod("isSwitchMember", source, listener);
  }

  /**
   * Parse the given source as a compilation unit.
   * 
   * @param source the source to be parsed
   * @param errorCodes the error codes of the errors that are expected to be found
   * @return the compilation unit that was parsed
   * @throws Exception if the source could not be parsed, if the compilation errors in the source do
   *           not match those that are expected, or if the result would have been {@code null}
   */
  private CompilationUnit parseDirectives(String source, ErrorCode... errorCodes) throws Exception {
    GatheringErrorListener listener = new GatheringErrorListener();
    Scanner scanner = new Scanner(null, new CharSequenceReader(source), listener);
    listener.setLineInfo(new TestSource(), scanner.getLineStarts());
    Token token = scanner.tokenize();
    Parser parser = new Parser(null, listener);
    CompilationUnit unit = parser.parseDirectives(token);
    assertNotNull(unit);
    assertSizeOfList(0, unit.getDeclarations());
    listener.assertErrorsWithCodes(errorCodes);
    return unit;
  }

  /**
   * Invoke a "skip" method in {@link Parser}. The method is assumed to take a token as it's
   * parameter and is given the first token in the scanned source.
   * 
   * @param methodName the name of the method that should be invoked
   * @param source the source to be processed by the method
   * @return the result of invoking the method
   * @throws Exception if the method could not be invoked or throws an exception
   * @throws AssertionFailedError if the result is {@code null}
   */
  private Token skip(String methodName, String source) throws Exception {
    GatheringErrorListener listener = new GatheringErrorListener();
    //
    // Scan the source.
    //
    Scanner scanner = new Scanner(null, new CharSequenceReader(source), listener);
    Token tokenStream = scanner.tokenize();
    //
    // Parse the source.
    //
    Parser parser = new Parser(null, listener);
    return (Token) invokeParserMethodImpl(
        parser,
        methodName,
        new Object[] {tokenStream},
        tokenStream);
  }
}
