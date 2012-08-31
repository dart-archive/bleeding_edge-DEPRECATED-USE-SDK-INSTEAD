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

import com.google.dart.engine.ast.AdjacentStrings;
import com.google.dart.engine.ast.Annotation;
import com.google.dart.engine.ast.ArgumentDefinitionTest;
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.ArrayAccess;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.Block;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.BooleanLiteral;
import com.google.dart.engine.ast.BreakStatement;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.Comment;
import com.google.dart.engine.ast.CommentReference;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorFieldInitializer;
import com.google.dart.engine.ast.ConstructorInitializer;
import com.google.dart.engine.ast.ContinueStatement;
import com.google.dart.engine.ast.DoStatement;
import com.google.dart.engine.ast.DoubleLiteral;
import com.google.dart.engine.ast.EmptyFunctionBody;
import com.google.dart.engine.ast.EmptyStatement;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionFunctionBody;
import com.google.dart.engine.ast.ExpressionStatement;
import com.google.dart.engine.ast.ExtendsClause;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.ForEachStatement;
import com.google.dart.engine.ast.ForStatement;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionDeclarationStatement;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.IfStatement;
import com.google.dart.engine.ast.ImplementsClause;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.IntegerLiteral;
import com.google.dart.engine.ast.InterpolationElement;
import com.google.dart.engine.ast.InterpolationExpression;
import com.google.dart.engine.ast.InterpolationString;
import com.google.dart.engine.ast.IsExpression;
import com.google.dart.engine.ast.Label;
import com.google.dart.engine.ast.LabeledStatement;
import com.google.dart.engine.ast.LibraryDirective;
import com.google.dart.engine.ast.ListLiteral;
import com.google.dart.engine.ast.MapLiteral;
import com.google.dart.engine.ast.MapLiteralEntry;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.NamedExpression;
import com.google.dart.engine.ast.NamedFormalParameter;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.NullLiteral;
import com.google.dart.engine.ast.ParenthesizedExpression;
import com.google.dart.engine.ast.PartDirective;
import com.google.dart.engine.ast.PartOfDirective;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.RedirectingConstructorInvocation;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.StringInterpolation;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.SuperExpression;
import com.google.dart.engine.ast.SwitchStatement;
import com.google.dart.engine.ast.ThisExpression;
import com.google.dart.engine.ast.ThrowStatement;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.TryStatement;
import com.google.dart.engine.ast.TypeAlias;
import com.google.dart.engine.ast.TypeArgumentList;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.TypeParameterList;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.VariableDeclarationStatement;
import com.google.dart.engine.ast.WhileStatement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.StringScanner;
import com.google.dart.engine.scanner.StringToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;

import junit.framework.AssertionFailedError;

import java.lang.reflect.Method;
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
  // TODO Add tests for the following methods:
  // parseCascadeSection(Expression)
  // parseDirective()
  // parseExpressionWithoutCascade()
  // parseNormalFormalParameter()?

//  public void test_parseExpressionWithoutCascade() throws Exception {
//    ASTNode expression = parse("parseExpressionWithoutCascade", "");
//  }

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
  private static Token skip(String methodName, String source) throws Exception {
    GatheringErrorListener listener = new GatheringErrorListener();
    //
    // Scan the source.
    //
    StringScanner scanner = new StringScanner(null, source, listener);
    Token tokenStream = scanner.tokenize();
    //
    // Parse the source.
    //
    Parser parser = new Parser(null, listener);
    Method skipMethod = Parser.class.getDeclaredMethod(methodName, new Class[] {Token.class});
    skipMethod.setAccessible(true);
    return (Token) skipMethod.invoke(parser, new Object[] {tokenStream});
  }

  public void fail_parseExpression_superMethodInvocation() throws Exception {
    MethodInvocation invocation = parse("parseExpression", "super.m()");
    assertNotNull(invocation.getTarget());
    assertNotNull(invocation.getMethodName());
    assertNotNull(invocation.getArgumentList());
  }

  public void test_computeStringValue_emptyInterpolationPrefix() throws Exception {
    assertEquals("", computeStringValue("'''"));
  }

  public void test_computeStringValue_escape_b() throws Exception {
    assertEquals("\b", computeStringValue("'\\b'"));
  }

  public void test_computeStringValue_escape_f() throws Exception {
    assertEquals("\f", computeStringValue("'\\f'"));
  }

  public void test_computeStringValue_escape_n() throws Exception {
    assertEquals("\n", computeStringValue("'\\n'"));
  }

  public void test_computeStringValue_escape_notSpecial() throws Exception {
    assertEquals(":", computeStringValue("'\\:'"));
  }

  public void test_computeStringValue_escape_r() throws Exception {
    assertEquals("\r", computeStringValue("'\\r'"));
  }

  public void test_computeStringValue_escape_t() throws Exception {
    assertEquals("\t", computeStringValue("'\\t'"));
  }

  public void test_computeStringValue_escape_u_fixed() throws Exception {
    assertEquals("\u1234", computeStringValue("'\\u1234'"));
  }

  public void test_computeStringValue_escape_u_variable() throws Exception {
    assertEquals("\u0123", computeStringValue("'\\u{123}'"));
  }

  public void test_computeStringValue_escape_v() throws Exception {
    assertEquals("\u000B", computeStringValue("'\\v'"));
  }

  public void test_computeStringValue_escape_x() throws Exception {
    assertEquals("\u00FF", computeStringValue("'\\xFF'"));
  }

  public void test_computeStringValue_noEscape_single() throws Exception {
    assertEquals("text", computeStringValue("'text'"));
  }

  public void test_computeStringValue_noEscape_triple() throws Exception {
    assertEquals("text", computeStringValue("'''text'''"));
  }

  public void test_computeStringValue_raw_single() throws Exception {
    assertEquals("text", computeStringValue("@'text'"));
  }

  public void test_computeStringValue_raw_triple() throws Exception {
    assertEquals("text", computeStringValue("@'''text'''"));
  }

  public void test_computeStringValue_raw_withEscape() throws Exception {
    assertEquals("two\\nlines", computeStringValue("@'two\\nlines'"));
  }

  public void test_isFunctionExpression_false() throws Exception {
    assertFalse(isFunctionExpression("f();"));
  }

  public void test_isFunctionExpression_nameButNoReturn_block() throws Exception {
    assertTrue(isFunctionExpression("f() {}"));
  }

  public void test_isFunctionExpression_nameButNoReturn_expression() throws Exception {
    assertTrue(isFunctionExpression("f() => e"));
  }

  public void test_isFunctionExpression_noName_block() throws Exception {
    assertTrue(isFunctionExpression("() {}"));
  }

  public void test_isFunctionExpression_noName_expression() throws Exception {
    assertTrue(isFunctionExpression("() => e"));
  }

  public void test_isFunctionExpression_normalReturn_block() throws Exception {
    assertTrue(isFunctionExpression("C f() {}"));
  }

  public void test_isFunctionExpression_normalReturn_expression() throws Exception {
    assertTrue(isFunctionExpression("C f() => e"));
  }

  public void test_isFunctionExpression_voidReturn_block() throws Exception {
    assertTrue(isFunctionExpression("void f() {}"));
  }

  public void test_isFunctionExpression_voidReturn_expression() throws Exception {
    assertTrue(isFunctionExpression("void f() => e"));
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

  public void test_isInitializedVariableDeclaration_final_noType_initialized() throws Exception {
    assertTrue(isInitializedVariableDeclaration("final a = 0;"));
  }

  public void test_isInitializedVariableDeclaration_final_noType_uninitialized() throws Exception {
    assertTrue(isInitializedVariableDeclaration("final a;"));
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
    assertTrue(isInitializedVariableDeclaration("int a = 0;"));
  }

  public void test_isInitializedVariableDeclaration_simpleType_uninitialized() throws Exception {
    assertTrue(isInitializedVariableDeclaration("int a;"));
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

  public void test_parseAnnotation_n() throws Exception {
    Annotation annotation = parse("parseAnnotation", "@A");
    assertNotNull(annotation.getAtSign());
    assertNotNull(annotation.getName());
    assertNull(annotation.getPeriod());
    assertNull(annotation.getConstructorName());
    assertNull(annotation.getArguments());
  }

  public void test_parseAnnotation_n_a() throws Exception {
    Annotation annotation = parse("parseAnnotation", "@A(x,y)");
    assertNotNull(annotation.getAtSign());
    assertNotNull(annotation.getName());
    assertNull(annotation.getPeriod());
    assertNull(annotation.getConstructorName());
    assertNotNull(annotation.getArguments());
  }

  public void test_parseAnnotation_n_c() throws Exception {
    Annotation annotation = parse("parseAnnotation", "@A.B.C");
    assertNotNull(annotation.getAtSign());
    assertNotNull(annotation.getName());
    assertNotNull(annotation.getPeriod());
    assertNotNull(annotation.getConstructorName());
    assertNull(annotation.getArguments());
  }

  public void test_parseAnnotation_n_c_a() throws Exception {
    Annotation annotation = parse("parseAnnotation", "@A.B.C(x,y)");
    assertNotNull(annotation.getAtSign());
    assertNotNull(annotation.getName());
    assertNotNull(annotation.getPeriod());
    assertNotNull(annotation.getConstructorName());
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

  public void test_parseArgument_named() throws Exception {
    NamedExpression expression = parse("parseArgument", "named: x");
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
    assertSize(0, arguments);
  }

  public void test_parseArgumentList_mixed() throws Exception {
    ArgumentList argumentList = parse("parseArgumentList", "(w, x, y: y, z: z)");
    NodeList<Expression> arguments = argumentList.getArguments();
    assertSize(4, arguments);
  }

  public void test_parseArgumentList_noNamed() throws Exception {
    ArgumentList argumentList = parse("parseArgumentList", "(x, y, z)");
    NodeList<Expression> arguments = argumentList.getArguments();
    assertSize(3, arguments);
  }

  public void test_parseArgumentList_onlyNamed() throws Exception {
    ArgumentList argumentList = parse("parseArgumentList", "(x: x, y: y)");
    NodeList<Expression> arguments = argumentList.getArguments();
    assertSize(2, arguments);
    assertTrue(arguments.get(0) instanceof NamedExpression);
    assertTrue(arguments.get(1) instanceof NamedExpression);
  }

  public void test_parseAssignableExpression_dot_normal() throws Exception {
    PropertyAccess propertyAccess = parse(
        "parseAssignableExpression",
        new Class[] {boolean.class},
        new Object[] {false},
        "(x).y");
    assertNotNull(propertyAccess.getTarget());
    assertNotNull(propertyAccess.getOperator());
    assertEquals(TokenType.PERIOD, propertyAccess.getOperator().getType());
    assertNotNull(propertyAccess.getPropertyName());
  }

  public void test_parseAssignableExpression_dot_super() throws Exception {
    PropertyAccess propertyAccess = parse(
        "parseAssignableExpression",
        new Class[] {boolean.class},
        new Object[] {false},
        "super.y");
    assertInstanceOf(SuperExpression.class, propertyAccess.getTarget());
    assertNotNull(propertyAccess.getOperator());
    assertEquals(TokenType.PERIOD, propertyAccess.getOperator().getType());
    assertNotNull(propertyAccess.getPropertyName());
  }

  public void test_parseAssignableExpression_index_normal() throws Exception {
    ArrayAccess arrayAccess = parse(
        "parseAssignableExpression",
        new Class[] {boolean.class},
        new Object[] {false},
        "x[y]");
    assertNotNull(arrayAccess.getArray());
    assertNotNull(arrayAccess.getLeftBracket());
    assertNotNull(arrayAccess.getIndex());
    assertNotNull(arrayAccess.getRightBracket());
  }

  public void test_parseAssignableExpression_index_super() throws Exception {
    ArrayAccess arrayAccess = parse(
        "parseAssignableExpression",
        new Class[] {boolean.class},
        new Object[] {false},
        "super[y]");
    assertNotNull(arrayAccess.getArray());
    assertNotNull(arrayAccess.getLeftBracket());
    assertNotNull(arrayAccess.getIndex());
    assertNotNull(arrayAccess.getRightBracket());
  }

  public void test_parseAssignableExpression_invoke() throws Exception {
    PropertyAccess propertyAccess = parse(
        "parseAssignableExpression",
        new Class[] {boolean.class},
        new Object[] {false},
        "x(y).z");
    MethodInvocation invocation = (MethodInvocation) propertyAccess.getTarget();
    assertEquals("x", invocation.getMethodName().getName());
    ArgumentList argumentList = invocation.getArgumentList();
    assertNotNull(argumentList);
    assertSize(1, argumentList.getArguments());
    assertNotNull(propertyAccess.getOperator());
    assertNotNull(propertyAccess.getPropertyName());
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
    assertSize(0, block.getStatements());
    assertNotNull(block.getRightBracket());
  }

  public void test_parseBlock_nonEmpty() throws Exception {
    Block block = parse("parseBlock", "{;}");
    assertNotNull(block.getLeftBracket());
    assertSize(1, block.getStatements());
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

  public void test_parseClassDeclaration_abstract() throws Exception {
    ClassDeclaration declaration = parse("parseClassDeclaration", "abstract class A {}");
    assertNull(declaration.getDocumentationComment());
    assertNotNull(declaration.getAbstractKeyword());
    assertNull(declaration.getExtendsClause());
    assertNull(declaration.getImplementsClause());
    assertNotNull(declaration.getClassKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    assertSize(0, declaration.getMembers());
    assertNotNull(declaration.getRightBracket());
    assertNull(declaration.getTypeParameters());
  }

  public void test_parseClassDeclaration_empty() throws Exception {
    ClassDeclaration declaration = parse("parseClassDeclaration", "class A {}");
    assertNull(declaration.getDocumentationComment());
    assertNull(declaration.getAbstractKeyword());
    assertNull(declaration.getExtendsClause());
    assertNull(declaration.getImplementsClause());
    assertNotNull(declaration.getClassKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    assertSize(0, declaration.getMembers());
    assertNotNull(declaration.getRightBracket());
    assertNull(declaration.getTypeParameters());
  }

  public void test_parseClassDeclaration_emptyWithComment() throws Exception {
    ClassDeclaration declaration = parse("parseClassDeclaration", "/** comment */\nclass A {}");
    assertNotNull(declaration.getDocumentationComment());
    assertNull(declaration.getAbstractKeyword());
    assertNull(declaration.getExtendsClause());
    assertNull(declaration.getImplementsClause());
    assertNotNull(declaration.getClassKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    assertSize(0, declaration.getMembers());
    assertNotNull(declaration.getRightBracket());
    assertNull(declaration.getTypeParameters());
  }

  public void test_parseClassDeclaration_extends() throws Exception {
    ClassDeclaration declaration = parse("parseClassDeclaration", "class A extends B {}");
    assertNull(declaration.getDocumentationComment());
    assertNull(declaration.getAbstractKeyword());
    assertNotNull(declaration.getExtendsClause());
    assertNull(declaration.getImplementsClause());
    assertNotNull(declaration.getClassKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    assertSize(0, declaration.getMembers());
    assertNotNull(declaration.getRightBracket());
    assertNull(declaration.getTypeParameters());
  }

  public void test_parseClassDeclaration_extendsAndImplements() throws Exception {
    ClassDeclaration declaration = parse(
        "parseClassDeclaration",
        "class A extends B implements C {}");
    assertNull(declaration.getDocumentationComment());
    assertNull(declaration.getAbstractKeyword());
    assertNotNull(declaration.getExtendsClause());
    assertNotNull(declaration.getImplementsClause());
    assertNotNull(declaration.getClassKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    assertSize(0, declaration.getMembers());
    assertNotNull(declaration.getRightBracket());
    assertNull(declaration.getTypeParameters());
  }

  public void test_parseClassDeclaration_implements() throws Exception {
    ClassDeclaration declaration = parse("parseClassDeclaration", "class A implements C {}");
    assertNull(declaration.getDocumentationComment());
    assertNull(declaration.getAbstractKeyword());
    assertNull(declaration.getExtendsClause());
    assertNotNull(declaration.getImplementsClause());
    assertNotNull(declaration.getClassKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    assertSize(0, declaration.getMembers());
    assertNotNull(declaration.getRightBracket());
    assertNull(declaration.getTypeParameters());
  }

  public void test_parseClassDeclaration_nonEmpty() throws Exception {
    ClassDeclaration declaration = parse("parseClassDeclaration", "class A {var f;}");
    assertNull(declaration.getDocumentationComment());
    assertNull(declaration.getAbstractKeyword());
    assertNull(declaration.getExtendsClause());
    assertNull(declaration.getImplementsClause());
    assertNotNull(declaration.getClassKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    assertSize(1, declaration.getMembers());
    assertNotNull(declaration.getRightBracket());
    assertNull(declaration.getTypeParameters());
  }

  public void test_parseClassDeclaration_typeParameters() throws Exception {
    ClassDeclaration declaration = parse("parseClassDeclaration", "class A<B> {}");
    assertNull(declaration.getDocumentationComment());
    assertNull(declaration.getAbstractKeyword());
    assertNull(declaration.getExtendsClause());
    assertNull(declaration.getImplementsClause());
    assertNotNull(declaration.getClassKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    assertSize(0, declaration.getMembers());
    assertNotNull(declaration.getRightBracket());
    assertNotNull(declaration.getTypeParameters());
    assertSize(1, declaration.getTypeParameters().getTypeParameters());
  }

  public void test_parseClassMember_constructor_withInitializers() throws Exception {
    ConstructorDeclaration constructor = parse(
        "parseClassMember",
        "A(_, _$, this.__) : _a = _ + _$ {}");
    assertNotNull(constructor.getBody());
    assertNotNull(constructor.getColon());
    assertNull(constructor.getExternalKeyword());
    assertNull(constructor.getKeyword());
    assertNull(constructor.getName());
    assertNotNull(constructor.getParameters());
    assertNull(constructor.getPeriod());
    assertNotNull(constructor.getReturnType());
    NodeList<ConstructorInitializer> initializers = constructor.getInitializers();
    assertNotNull(initializers);
    assertEquals(1, initializers.size());
  }

  public void test_parseClassMember_method_external() throws Exception {
    MethodDeclaration method = parse("parseClassMember", "external m();");
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
    MethodDeclaration method = parse("parseClassMember", "external int m(int a);");
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

  public void test_parseCommentReference_prefixed() throws Exception {
    CommentReference reference = parse("parseCommentReference", new Class[] {
        String.class, int.class}, new Object[] {"a.b", 7}, "");
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
    CommentReference reference = parse("parseCommentReference", new Class[] {
        String.class, int.class}, new Object[] {"a", 5}, "");
    SimpleIdentifier identifier = assertInstanceOf(
        SimpleIdentifier.class,
        reference.getIdentifier());
    assertNotNull(identifier.getToken());
    assertEquals("a", identifier.getName());
    assertEquals(5, identifier.getOffset());
  }

  public void test_parseCommentReferences_multiLine() throws Exception {
    Token[] tokens = new Token[] {new StringToken(
        TokenType.MULTI_LINE_COMMENT,
        "/** xxx [a] yyy [b] zzz */",
        3),};
    List<CommentReference> references = parse(
        "parseCommentReferences",
        new Class[] {Token[].class},
        new Object[] {tokens},
        "");
    assertSize(2, references);
    CommentReference reference = references.get(0);
    assertNotNull(reference);
    assertNotNull(reference.getIdentifier());
    assertEquals(12, reference.getOffset());

    reference = references.get(1);
    assertNotNull(reference);
    assertNotNull(reference.getIdentifier());
    assertEquals(20, reference.getOffset());
  }

  public void test_parseCommentReferences_singleLine() throws Exception {
    Token[] tokens = new Token[] {
        new StringToken(TokenType.SINGLE_LINE_COMMENT, "/// xxx [a] yyy [b] zzz", 3),
        new StringToken(TokenType.SINGLE_LINE_COMMENT, "/// x [c]", 28),};
    List<CommentReference> references = parse(
        "parseCommentReferences",
        new Class[] {Token[].class},
        new Object[] {tokens},
        "");
    assertSize(3, references);
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

  public void test_parseCompilationUnit_directives_multiple() throws Exception {
    CompilationUnit unit = parse("parseCompilationUnit", "library l;\npart 'a.dart';");
    assertNull(unit.getScriptTag());
    assertSize(2, unit.getDirectives());
    assertSize(0, unit.getDeclarations());
  }

  public void test_parseCompilationUnit_directives_single() throws Exception {
    CompilationUnit unit = parse("parseCompilationUnit", "library l;");
    assertNull(unit.getScriptTag());
    assertSize(1, unit.getDirectives());
    assertSize(0, unit.getDeclarations());
  }

  public void test_parseCompilationUnit_empty() throws Exception {
    CompilationUnit unit = parse("parseCompilationUnit", "");
    assertNull(unit.getScriptTag());
    assertSize(0, unit.getDirectives());
    assertSize(0, unit.getDeclarations());
  }

  public void test_parseCompilationUnit_nonEmpty() throws Exception {
    CompilationUnit unit = parse("parseCompilationUnit", "class Foo {}");
    assertSize(1, unit.getDeclarations());
    assertSize(0, unit.getDirectives());
    assertNull(unit.getScriptTag());
  }

  public void test_parseCompilationUnit_script() throws Exception {
    CompilationUnit unit = parse("parseCompilationUnit", "#! /bin/dart");
    assertNotNull(unit.getScriptTag());
    assertSize(0, unit.getDirectives());
    assertSize(0, unit.getDeclarations());
  }

  public void test_parseCompilationUnit_topLevelDeclaration() throws Exception {
    CompilationUnit unit = parse("parseCompilationUnit", "class A {}");
    assertNull(unit.getScriptTag());
    assertSize(0, unit.getDirectives());
    assertSize(1, unit.getDeclarations());
  }

  public void test_parseCompilationUnitMember_class() throws Exception {
    ClassDeclaration declaration = parse("parseCompilationUnitMember", "class A {}");
    assertEquals("A", declaration.getName().getName());
    assertSize(0, declaration.getMembers());
  }

  public void test_parseCompilationUnitMember_constVariable() throws Exception {
    TopLevelVariableDeclaration declaration = parse(
        "parseCompilationUnitMember",
        "const int x = 0;");
    assertNotNull(declaration.getSemicolon());
    assertNotNull(declaration.getVariables());
  }

  public void test_parseCompilationUnitMember_finalVariable() throws Exception {
    TopLevelVariableDeclaration declaration = parse("parseCompilationUnitMember", "final x = 0;");
    assertNotNull(declaration.getSemicolon());
    assertNotNull(declaration.getVariables());
  }

  public void test_parseCompilationUnitMember_function_noType() throws Exception {
    FunctionDeclaration declaration = parse("parseCompilationUnitMember", "f() {}");
    assertNotNull(declaration.getFunctionExpression());
    assertNull(declaration.getPropertyKeyword());
  }

  public void test_parseCompilationUnitMember_function_type() throws Exception {
    FunctionDeclaration declaration = parse("parseCompilationUnitMember", "int f() {}");
    assertNotNull(declaration.getFunctionExpression());
    assertNull(declaration.getPropertyKeyword());
  }

  public void test_parseCompilationUnitMember_getter_noType() throws Exception {
    FunctionDeclaration declaration = parse("parseCompilationUnitMember", "get p() => 0;");
    assertNotNull(declaration.getFunctionExpression());
    assertNotNull(declaration.getPropertyKeyword());
  }

  public void test_parseCompilationUnitMember_getter_type() throws Exception {
    FunctionDeclaration declaration = parse("parseCompilationUnitMember", "int get p() => 0;");
    assertNotNull(declaration.getFunctionExpression());
    assertNotNull(declaration.getPropertyKeyword());
  }

  public void test_parseCompilationUnitMember_setter_noType() throws Exception {
    FunctionDeclaration declaration = parse("parseCompilationUnitMember", "set p(v) {}");
    assertNotNull(declaration.getFunctionExpression());
    assertNotNull(declaration.getPropertyKeyword());
  }

  public void test_parseCompilationUnitMember_setter_type() throws Exception {
    FunctionDeclaration declaration = parse("parseCompilationUnitMember", "void set p(int v) {}");
    assertNotNull(declaration.getFunctionExpression());
    assertNotNull(declaration.getPropertyKeyword());
  }

  public void test_parseCompilationUnitMember_typedef() throws Exception {
    TypeAlias typeAlias = parse("parseCompilationUnitMember", "typedef F();");
    assertEquals("F", typeAlias.getName().getName());
    assertSize(0, typeAlias.getParameters().getParameters());
  }

  public void test_parseCompilationUnitMember_variable() throws Exception {
    TopLevelVariableDeclaration declaration = parse("parseCompilationUnitMember", "var x = 0;");
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

  public void test_parseConstantConstructor_initializers() throws Exception {
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    ConstructorDeclaration constructor = parse("parseConstantConstructor", new Class[] {
        Comment.class, Token.class}, new Object[] {comment, null}, "const A(var b) : this.a = b;");
    assertInstanceOf(EmptyFunctionBody.class, constructor.getBody());
    assertNotNull(constructor.getColon());
    assertEquals(comment, constructor.getDocumentationComment());
    assertNull(constructor.getExternalKeyword());
    assertSize(1, constructor.getInitializers());
    assertNotNull(constructor.getKeyword());
    assertNull(constructor.getName());
    assertNotNull(constructor.getParameters());
    assertNull(constructor.getPeriod());
    assertNotNull(constructor.getReturnType());
  }

  public void test_parseConstantConstructor_noInitializers() throws Exception {
    Comment comment = null;
    ConstructorDeclaration constructor = parse("parseConstantConstructor", new Class[] {
        Comment.class, Token.class}, new Object[] {comment, null}, "const A();");
    assertInstanceOf(EmptyFunctionBody.class, constructor.getBody());
    assertNull(constructor.getColon());
    assertEquals(comment, constructor.getDocumentationComment());
    assertNull(constructor.getExternalKeyword());
    assertSize(0, constructor.getInitializers());
    assertNotNull(constructor.getKeyword());
    assertNull(constructor.getName());
    assertNotNull(constructor.getParameters());
    assertNull(constructor.getPeriod());
    assertNotNull(constructor.getReturnType());
  }

  public void test_parseConstantConstructor_redirecting() throws Exception {
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    ConstructorDeclaration constructor = parse(
        "parseConstantConstructor",
        new Class[] {Comment.class, Token.class},
        new Object[] {comment, null},
        "const A.b() : this.a(), super();");
    assertInstanceOf(EmptyFunctionBody.class, constructor.getBody());
    assertNotNull(constructor.getColon());
    assertEquals(comment, constructor.getDocumentationComment());
    assertNull(constructor.getExternalKeyword());
    assertSize(2, constructor.getInitializers());
    assertNotNull(constructor.getKeyword());
    assertNotNull(constructor.getName());
    assertNotNull(constructor.getParameters());
    assertNotNull(constructor.getPeriod());
    assertNotNull(constructor.getReturnType());
  }

  public void test_parseConstExpression_instanceCreation() throws Exception {
    InstanceCreationExpression expression = parse("parseConstExpression", "const A()");
    assertNotNull(expression.getArgumentList());
    assertNull(expression.getIdentifier());
    assertNotNull(expression.getKeyword());
    assertNotNull(expression.getType());
  }

  public void test_parseConstExpression_listLiteral_typed() throws Exception {
    ListLiteral literal = parse("parseConstExpression", "const <A> []");
    assertNotNull(literal.getModifier());
    assertNotNull(literal.getTypeArguments());
    assertNotNull(literal.getLeftBracket());
    assertSize(0, literal.getElements());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseConstExpression_listLiteral_untyped() throws Exception {
    ListLiteral literal = parse("parseConstExpression", "const []");
    assertNotNull(literal.getModifier());
    assertNull(literal.getTypeArguments());
    assertNotNull(literal.getLeftBracket());
    assertSize(0, literal.getElements());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseConstExpression_mapLiteral_typed() throws Exception {
    MapLiteral literal = parse("parseConstExpression", "const <A> {}");
    assertNotNull(literal.getLeftBracket());
    assertSize(0, literal.getEntries());
    assertNotNull(literal.getRightBracket());
    assertNotNull(literal.getTypeArguments());
  }

  public void test_parseConstExpression_mapLiteral_untyped() throws Exception {
    MapLiteral literal = parse("parseConstExpression", "const {}");
    assertNotNull(literal.getLeftBracket());
    assertSize(0, literal.getEntries());
    assertNotNull(literal.getRightBracket());
    assertNull(literal.getTypeArguments());
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

  public void test_parseDocumentationComment_block() throws Exception {
    Comment comment = parse("parseDocumentationComment", "/** */ class");
    assertFalse(comment.isBlock());
    assertTrue(comment.isDocumentation());
    assertFalse(comment.isEndOfLine());
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

  public void test_parseExpression_assign() throws Exception {
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

  public void test_parseExpressionList_multiple() throws Exception {
    List<Expression> result = parse("parseExpressionList", "1, 2, 3");
    assertSize(3, result);
  }

  public void test_parseExpressionList_single() throws Exception {
    List<Expression> result = parse("parseExpressionList", "1");
    assertSize(1, result);
  }

  public void test_parseExtendsClause() throws Exception {
    ExtendsClause clause = parse("parseExtendsClause", "extends B");
    assertNotNull(clause.getKeyword());
    assertNotNull(clause.getSuperclass());
    assertInstanceOf(TypeName.class, clause.getSuperclass());
  }

  public void test_parseFactoryConstructor_nameAndQualifier() throws Exception {
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    ConstructorDeclaration constructor = parse("parseFactoryConstructor", new Class[] {
        Comment.class, Token.class}, new Object[] {comment, null}, "factory A.B.c()");
    assertNull(constructor.getColon());
    assertEquals(comment, constructor.getDocumentationComment());
    assertNull(constructor.getExternalKeyword());
    assertSize(0, constructor.getInitializers());
    assertNotNull(constructor.getKeyword());
    assertNotNull(constructor.getName());
    assertNotNull(constructor.getParameters());
    assertNotNull(constructor.getPeriod());
    assertNotNull(constructor.getReturnType());
  }

  public void test_parseFactoryConstructor_noName_noQualifier() throws Exception {
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    ConstructorDeclaration constructor = parse("parseFactoryConstructor", new Class[] {
        Comment.class, Token.class}, new Object[] {comment, null}, "factory A()");
    assertNull(constructor.getColon());
    assertEquals(comment, constructor.getDocumentationComment());
    assertNull(constructor.getExternalKeyword());
    assertSize(0, constructor.getInitializers());
    assertNotNull(constructor.getKeyword());
    assertNull(constructor.getName());
    assertNotNull(constructor.getParameters());
    assertNull(constructor.getPeriod());
    assertNotNull(constructor.getReturnType());
  }

  public void test_parseFinalConstVarOrType_const_noType() throws Exception {
    Parser.FinalConstVarOrType result = parse(
        "parseFinalConstVarOrType",
        new Class[] {boolean.class},
        new Object[] {false},
        "const");
    Token keyword = result.getKeyword();
    assertNotNull(keyword);
    assertEquals(TokenType.KEYWORD, keyword.getType());
    assertEquals(Keyword.CONST, ((KeywordToken) keyword).getKeyword());
    assertNull(result.getType());
  }

  public void test_parseFinalConstVarOrType_const_type() throws Exception {
    Parser.FinalConstVarOrType result = parse(
        "parseFinalConstVarOrType",
        new Class[] {boolean.class},
        new Object[] {false},
        "const A a");
    Token keyword = result.getKeyword();
    assertNotNull(keyword);
    assertEquals(TokenType.KEYWORD, keyword.getType());
    assertEquals(Keyword.CONST, ((KeywordToken) keyword).getKeyword());
    assertNotNull(result.getType());
  }

  public void test_parseFinalConstVarOrType_final_noType() throws Exception {
    Parser.FinalConstVarOrType result = parse(
        "parseFinalConstVarOrType",
        new Class[] {boolean.class},
        new Object[] {false},
        "final");
    Token keyword = result.getKeyword();
    assertNotNull(keyword);
    assertEquals(TokenType.KEYWORD, keyword.getType());
    assertEquals(Keyword.FINAL, ((KeywordToken) keyword).getKeyword());
    assertNull(result.getType());
  }

  public void test_parseFinalConstVarOrType_final_type() throws Exception {
    Parser.FinalConstVarOrType result = parse(
        "parseFinalConstVarOrType",
        new Class[] {boolean.class},
        new Object[] {false},
        "final A a");
    Token keyword = result.getKeyword();
    assertNotNull(keyword);
    assertEquals(TokenType.KEYWORD, keyword.getType());
    assertEquals(Keyword.FINAL, ((KeywordToken) keyword).getKeyword());
    assertNotNull(result.getType());
  }

  public void test_parseFinalConstVarOrType_type() throws Exception {
    Parser.FinalConstVarOrType result = parse(
        "parseFinalConstVarOrType",
        new Class[] {boolean.class},
        new Object[] {false},
        "A a");
    assertNull(result.getKeyword());
    assertNotNull(result.getType());
  }

  public void test_parseFinalConstVarOrType_var() throws Exception {
    Parser.FinalConstVarOrType result = parse(
        "parseFinalConstVarOrType",
        new Class[] {boolean.class},
        new Object[] {false},
        "var");
    Token keyword = result.getKeyword();
    assertNotNull(keyword);
    assertEquals(TokenType.KEYWORD, keyword.getType());
    assertEquals(Keyword.VAR, ((KeywordToken) keyword).getKeyword());
    assertNull(result.getType());
  }

  public void test_parseFormalParameter_final_withType() throws Exception {
    SimpleFormalParameter parameter = parse("parseFormalParameter", "final A a");
    assertNotNull(parameter.getIdentifier());
    assertNotNull(parameter.getKeyword());
    assertNotNull(parameter.getType());
  }

  public void test_parseFormalParameter_final_withType_optional() throws Exception {
    NamedFormalParameter namedParameter = parse("parseFormalParameter", "final A a = null");
    SimpleFormalParameter simpleParameter = (SimpleFormalParameter) namedParameter.getParameter();
    assertNotNull(simpleParameter.getIdentifier());
    assertNotNull(simpleParameter.getKeyword());
    assertNotNull(simpleParameter.getType());
    assertNotNull(namedParameter.getEquals());
    assertNotNull(namedParameter.getDefaultValue());
  }

  public void test_parseFormalParameter_nonFinal_withType() throws Exception {
    SimpleFormalParameter parameter = parse("parseFormalParameter", "A a");
    assertNotNull(parameter.getIdentifier());
    assertNull(parameter.getKeyword());
    assertNotNull(parameter.getType());
  }

  public void test_parseFormalParameter_nonFinal_withType_optional() throws Exception {
    NamedFormalParameter namedParameter = parse("parseFormalParameter", "A a = null");
    SimpleFormalParameter simpleParameter = (SimpleFormalParameter) namedParameter.getParameter();
    assertNotNull(simpleParameter.getIdentifier());
    assertNull(simpleParameter.getKeyword());
    assertNotNull(simpleParameter.getType());
    assertNotNull(namedParameter.getEquals());
    assertNotNull(namedParameter.getDefaultValue());
  }

  public void test_parseFormalParameter_var() throws Exception {
    SimpleFormalParameter parameter = parse("parseFormalParameter", "var a");
    assertNotNull(parameter.getIdentifier());
    assertNotNull(parameter.getKeyword());
    assertNull(parameter.getType());
  }

  public void test_parseFormalParameter_var_optional() throws Exception {
    NamedFormalParameter namedParameter = parse("parseFormalParameter", "var a = null");
    SimpleFormalParameter simpleParameter = (SimpleFormalParameter) namedParameter.getParameter();
    assertNotNull(simpleParameter.getIdentifier());
    assertNotNull(simpleParameter.getKeyword());
    assertNull(simpleParameter.getType());
    assertNotNull(namedParameter.getEquals());
    assertNotNull(namedParameter.getDefaultValue());
  }

  public void test_parseFormalParameterList_empty() throws Exception {
    FormalParameterList parameterList = parse("parseFormalParameterList", "()");
    assertNotNull(parameterList.getLeftParenthesis());
    assertNull(parameterList.getLeftBracket());
    assertSize(0, parameterList.getParameters());
    assertNull(parameterList.getRightBracket());
    assertNotNull(parameterList.getRightParenthesis());
  }

  public void test_parseFormalParameterList_mixed() throws Exception {
    FormalParameterList parameterList = parse("parseFormalParameterList", "(A a, [B b])");
    assertNotNull(parameterList.getLeftParenthesis());
    assertNotNull(parameterList.getLeftBracket());
    assertSize(2, parameterList.getParameters());
    assertNotNull(parameterList.getRightBracket());
    assertNotNull(parameterList.getRightParenthesis());
  }

  public void test_parseFormalParameterList_named_multiple() throws Exception {
    FormalParameterList parameterList = parse("parseFormalParameterList", "([A a, B b, C c])");
    assertNotNull(parameterList.getLeftParenthesis());
    assertNotNull(parameterList.getLeftBracket());
    assertSize(3, parameterList.getParameters());
    assertNotNull(parameterList.getRightBracket());
    assertNotNull(parameterList.getRightParenthesis());
  }

  public void test_parseFormalParameterList_named_single() throws Exception {
    FormalParameterList parameterList = parse("parseFormalParameterList", "([A a])");
    assertNotNull(parameterList.getLeftParenthesis());
    assertNotNull(parameterList.getLeftBracket());
    assertSize(1, parameterList.getParameters());
    assertNotNull(parameterList.getRightBracket());
    assertNotNull(parameterList.getRightParenthesis());
  }

  public void test_parseFormalParameterList_normal_multiple() throws Exception {
    FormalParameterList parameterList = parse("parseFormalParameterList", "(A a, B b, C c)");
    assertNotNull(parameterList.getLeftParenthesis());
    assertNull(parameterList.getLeftBracket());
    assertSize(3, parameterList.getParameters());
    assertNull(parameterList.getRightBracket());
    assertNotNull(parameterList.getRightParenthesis());
  }

  public void test_parseFormalParameterList_normal_single() throws Exception {
    FormalParameterList parameterList = parse("parseFormalParameterList", "(A a)");
    assertNotNull(parameterList.getLeftParenthesis());
    assertNull(parameterList.getLeftBracket());
    assertSize(1, parameterList.getParameters());
    assertNull(parameterList.getRightBracket());
    assertNotNull(parameterList.getRightParenthesis());
  }

  public void test_parseFormalParameterList_optional_multiple() throws Exception {
    FormalParameterList parameterList = parse(
        "parseFormalParameterList",
        "([A a = null, B b, C c = null])");
    assertNotNull(parameterList.getLeftParenthesis());
    assertNotNull(parameterList.getLeftBracket());
    assertSize(3, parameterList.getParameters());
    assertNotNull(parameterList.getRightBracket());
    assertNotNull(parameterList.getRightParenthesis());
  }

  public void test_parseFormalParameterList_optional_single() throws Exception {
    FormalParameterList parameterList = parse("parseFormalParameterList", "([A a = null])");
    assertNotNull(parameterList.getLeftParenthesis());
    assertNotNull(parameterList.getLeftBracket());
    assertSize(1, parameterList.getParameters());
    assertNotNull(parameterList.getRightBracket());
    assertNotNull(parameterList.getRightParenthesis());
  }

  public void test_parseForStatement_each_noType() throws Exception {
    ForEachStatement statement = parse("parseForStatement", "for (element in list) {}");
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNotNull(statement.getLoopParameter());
    assertNotNull(statement.getInKeyword());
    assertNotNull(statement.getIterator());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseForStatement_each_type() throws Exception {
    ForEachStatement statement = parse("parseForStatement", "for (A element in list) {}");
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNotNull(statement.getLoopParameter());
    assertNotNull(statement.getInKeyword());
    assertNotNull(statement.getIterator());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseForStatement_each_var() throws Exception {
    ForEachStatement statement = parse("parseForStatement", "for (var element in list) {}");
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNotNull(statement.getLoopParameter());
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
    assertSize(0, statement.getUpdaters());
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
    assertSize(1, statement.getUpdaters());
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
    assertSize(1, statement.getUpdaters());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseForStatement_loop_i() throws Exception {
    ForStatement statement = parse("parseForStatement", "for (var i = 0;;) {}");
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    VariableDeclarationList variables = statement.getVariables();
    assertNotNull(variables);
    assertSize(1, variables.getVariables());
    assertNull(statement.getInitialization());
    assertNotNull(statement.getLeftSeparator());
    assertNull(statement.getCondition());
    assertNotNull(statement.getRightSeparator());
    assertSize(0, statement.getUpdaters());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseForStatement_loop_ic() throws Exception {
    ForStatement statement = parse("parseForStatement", "for (var i = 0; i < count;) {}");
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    VariableDeclarationList variables = statement.getVariables();
    assertNotNull(variables);
    assertSize(1, variables.getVariables());
    assertNull(statement.getInitialization());
    assertNotNull(statement.getLeftSeparator());
    assertNotNull(statement.getCondition());
    assertNotNull(statement.getRightSeparator());
    assertSize(0, statement.getUpdaters());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseForStatement_loop_icu() throws Exception {
    ForStatement statement = parse("parseForStatement", "for (var i = 0; i < count; i++) {}");
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    VariableDeclarationList variables = statement.getVariables();
    assertNotNull(variables);
    assertSize(1, variables.getVariables());
    assertNull(statement.getInitialization());
    assertNotNull(statement.getLeftSeparator());
    assertNotNull(statement.getCondition());
    assertNotNull(statement.getRightSeparator());
    assertSize(1, statement.getUpdaters());
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
    assertSize(2, variables.getVariables());
    assertNull(statement.getInitialization());
    assertNotNull(statement.getLeftSeparator());
    assertNotNull(statement.getCondition());
    assertNotNull(statement.getRightSeparator());
    assertSize(2, statement.getUpdaters());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseForStatement_loop_iu() throws Exception {
    ForStatement statement = parse("parseForStatement", "for (var i = 0;; i++) {}");
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    VariableDeclarationList variables = statement.getVariables();
    assertNotNull(variables);
    assertSize(1, variables.getVariables());
    assertNull(statement.getInitialization());
    assertNotNull(statement.getLeftSeparator());
    assertNull(statement.getCondition());
    assertNotNull(statement.getRightSeparator());
    assertSize(1, statement.getUpdaters());
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
    assertSize(1, statement.getUpdaters());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseFunctionBody_block() throws Exception {
    BlockFunctionBody functionBody = parse("parseFunctionBody", new Class[] {
        boolean.class, boolean.class}, new Object[] {false, false}, "{}");
    assertNotNull(functionBody.getBlock());
  }

  public void test_parseFunctionBody_empty() throws Exception {
    EmptyFunctionBody functionBody = parse("parseFunctionBody", new Class[] {
        boolean.class, boolean.class}, new Object[] {true, false}, ";");
    assertNotNull(functionBody.getSemicolon());
  }

  public void test_parseFunctionBody_expression() throws Exception {
    ExpressionFunctionBody functionBody = parse("parseFunctionBody", new Class[] {
        boolean.class, boolean.class}, new Object[] {false, false}, "=> y;");
    assertNotNull(functionBody.getFunctionDefinition());
    assertNotNull(functionBody.getExpression());
    assertNotNull(functionBody.getSemicolon());
  }

  public void test_parseFunctionDeclaration_function() throws Exception {
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    TypeName returnType = new TypeName(new SimpleIdentifier(null), null);
    FunctionDeclaration declaration = parse("parseFunctionDeclaration", new Class[] {
        Comment.class, TypeName.class}, new Object[] {comment, returnType}, "f() {}");
    assertEquals(comment, declaration.getDocumentationComment());
    FunctionExpression expression = declaration.getFunctionExpression();
    assertNotNull(expression);
    assertNotNull(expression.getBody());
    assertNotNull(expression.getName());
    assertNotNull(expression.getParameters());
    assertEquals(returnType, expression.getReturnType());
    assertNull(declaration.getPropertyKeyword());
  }

  public void test_parseFunctionDeclaration_getter() throws Exception {
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    TypeName returnType = new TypeName(new SimpleIdentifier(null), null);
    FunctionDeclaration declaration = parse("parseFunctionDeclaration", new Class[] {
        Comment.class, TypeName.class}, new Object[] {comment, returnType}, "get p => 0;");
    assertEquals(comment, declaration.getDocumentationComment());
    FunctionExpression expression = declaration.getFunctionExpression();
    assertNotNull(expression);
    assertNotNull(expression.getBody());
    assertNotNull(expression.getName());
    assertNull(expression.getParameters());
    assertEquals(returnType, expression.getReturnType());
    assertNotNull(declaration.getPropertyKeyword());
  }

  public void test_parseFunctionDeclaration_setter() throws Exception {
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    TypeName returnType = new TypeName(new SimpleIdentifier(null), null);
    FunctionDeclaration declaration = parse("parseFunctionDeclaration", new Class[] {
        Comment.class, TypeName.class}, new Object[] {comment, returnType}, "set p(v) {}");
    assertEquals(comment, declaration.getDocumentationComment());
    FunctionExpression expression = declaration.getFunctionExpression();
    assertNotNull(expression);
    assertNotNull(expression.getBody());
    assertNotNull(expression.getName());
    assertNotNull(expression.getParameters());
    assertEquals(returnType, expression.getReturnType());
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
    assertNull(expression.getName());
    assertNotNull(expression.getParameters());
    assertNull(expression.getReturnType());
    assertNull(((ExpressionFunctionBody) expression.getBody()).getSemicolon());
  }

  public void test_parseFunctionExpression_minimal() throws Exception {
    FunctionExpression expression = parse("parseFunctionExpression", "() {}");
    assertNotNull(expression.getBody());
    assertNull(expression.getName());
    assertNotNull(expression.getParameters());
    assertNull(expression.getReturnType());
  }

  public void test_parseFunctionExpression_name() throws Exception {
    FunctionExpression expression = parse("parseFunctionExpression", "f() {}");
    assertNotNull(expression.getBody());
    assertNotNull(expression.getName());
    assertNotNull(expression.getParameters());
    assertNull(expression.getReturnType());
  }

  public void test_parseFunctionExpression_returnType() throws Exception {
    FunctionExpression expression = parse("parseFunctionExpression", "A<T> () {}");
    assertNotNull(expression.getBody());
    assertNull(expression.getName());
    assertNotNull(expression.getParameters());
    assertNotNull(expression.getReturnType());
  }

  public void test_parseFunctionExpression_returnType_name() throws Exception {
    FunctionExpression expression = parse("parseFunctionExpression", "A f() {}");
    assertNotNull(expression.getBody());
    assertNotNull(expression.getName());
    assertNotNull(expression.getParameters());
    assertNotNull(expression.getReturnType());
  }

  public void test_parseGetter_nonStatic() throws Exception {
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    TypeName returnType = new TypeName(new SimpleIdentifier(null), null);
    MethodDeclaration method = parse("parseGetter", new Class[] {
        Comment.class, Token.class, Token.class, TypeName.class}, new Object[] {
        comment, null, null, returnType}, "get a;");
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
    Token staticKeyword = new KeywordToken(Keyword.STATIC, 0);
    TypeName returnType = new TypeName(new SimpleIdentifier(null), null);
    MethodDeclaration method = parse("parseGetter", new Class[] {
        Comment.class, Token.class, Token.class, TypeName.class}, new Object[] {
        comment, null, staticKeyword, returnType}, "get a;");
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

  public void test_parseIfStatement_else() throws Exception {
    IfStatement statement = parse("parseIfStatement", "if (x) {} else {}");
    assertNotNull(statement.getIfKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNotNull(statement.getCondition());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getThenStatement());
    assertNotNull(statement.getElseKeyword());
    assertNotNull(statement.getElseStatement());
  }

  public void test_parseIfStatement_noElse() throws Exception {
    IfStatement statement = parse("parseIfStatement", "if (x) {}");
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
    assertSize(3, clause.getInterfaces());
    assertNotNull(clause.getKeyword());
  }

  public void test_parseImplementsClause_single() throws Exception {
    ImplementsClause clause = parse("parseImplementsClause", "implements A");
    assertSize(1, clause.getInterfaces());
    assertNotNull(clause.getKeyword());
  }

  public void test_parseImportDirective_export() throws Exception {
    ImportDirective directive = parse("parseImportDirective", "import 'lib/lib.dart' & export;");
    assertNotNull(directive.getImportToken());
    assertNotNull(directive.getLibraryUri());
    assertNull(directive.getAsToken());
    assertNull(directive.getPrefix());
    assertSize(0, directive.getCombinators());
    assertNotNull(directive.getAmpersand());
    assertNotNull(directive.getExportToken());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseImportDirective_full() throws Exception {
    ImportDirective directive = parse(
        "parseImportDirective",
        "import 'lib/lib.dart' as a hide A show B & export;");
    assertNotNull(directive.getImportToken());
    assertNotNull(directive.getLibraryUri());
    assertNotNull(directive.getAsToken());
    assertNotNull(directive.getPrefix());
    assertSize(2, directive.getCombinators());
    assertNotNull(directive.getAmpersand());
    assertNotNull(directive.getExportToken());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseImportDirective_hide() throws Exception {
    ImportDirective directive = parse("parseImportDirective", "import 'lib/lib.dart' hide A, B;");
    assertNotNull(directive.getImportToken());
    assertNotNull(directive.getLibraryUri());
    assertNull(directive.getAsToken());
    assertNull(directive.getPrefix());
    assertSize(1, directive.getCombinators());
    assertNull(directive.getAmpersand());
    assertNull(directive.getExportToken());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseImportDirective_noCombinator() throws Exception {
    ImportDirective directive = parse("parseImportDirective", "import 'lib/lib.dart';");
    assertNotNull(directive.getImportToken());
    assertNotNull(directive.getLibraryUri());
    assertNull(directive.getAsToken());
    assertNull(directive.getPrefix());
    assertSize(0, directive.getCombinators());
    assertNull(directive.getAmpersand());
    assertNull(directive.getExportToken());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseImportDirective_prefix() throws Exception {
    ImportDirective directive = parse("parseImportDirective", "import 'lib/lib.dart' as a;");
    assertNotNull(directive.getImportToken());
    assertNotNull(directive.getLibraryUri());
    assertNotNull(directive.getAsToken());
    assertNotNull(directive.getPrefix());
    assertSize(0, directive.getCombinators());
    assertNull(directive.getAmpersand());
    assertNull(directive.getExportToken());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseImportDirective_show() throws Exception {
    ImportDirective directive = parse("parseImportDirective", "import 'lib/lib.dart' show A, B;");
    assertNotNull(directive.getImportToken());
    assertNotNull(directive.getLibraryUri());
    assertNull(directive.getAsToken());
    assertNull(directive.getPrefix());
    assertSize(1, directive.getCombinators());
    assertNull(directive.getAmpersand());
    assertNull(directive.getExportToken());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseInitializedIdentifierList_type() throws Exception {
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    Token staticKeyword = new KeywordToken(Keyword.STATIC, 0);
    TypeName type = new TypeName(new SimpleIdentifier(null), null);
    FieldDeclaration declaration = parse("parseInitializedIdentifierList", new Class[] {
        Comment.class, Token.class, Token.class, TypeName.class}, new Object[] {
        comment, staticKeyword, null, type}, "a = 1, b, c = 3;");
    assertEquals(comment, declaration.getDocumentationComment());
    VariableDeclarationList fields = declaration.getFields();
    assertNotNull(fields);
    assertNull(fields.getKeyword());
    assertEquals(type, fields.getType());
    assertSize(3, fields.getVariables());
    assertEquals(staticKeyword, declaration.getKeyword());
    assertNotNull(declaration.getSemicolon());
  }

  public void test_parseInitializedIdentifierList_var() throws Exception {
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    Token staticKeyword = new KeywordToken(Keyword.STATIC, 0);
    Token varKeyword = new KeywordToken(Keyword.VAR, 0);
    FieldDeclaration declaration = parse("parseInitializedIdentifierList", new Class[] {
        Comment.class, Token.class, Token.class, TypeName.class}, new Object[] {
        comment, staticKeyword, varKeyword, null}, "a = 1, b, c = 3;");
    assertEquals(comment, declaration.getDocumentationComment());
    VariableDeclarationList fields = declaration.getFields();
    assertNotNull(fields);
    assertEquals(varKeyword, fields.getKeyword());
    assertNull(fields.getType());
    assertSize(3, fields.getVariables());
    assertEquals(staticKeyword, declaration.getKeyword());
    assertNotNull(declaration.getSemicolon());
  }

  public void test_parseInstanceCreationExpression_qualifiedType() throws Exception {
    Token token = new KeywordToken(Keyword.NEW, 0);
    InstanceCreationExpression expression = parse(
        "parseInstanceCreationExpression",
        new Class[] {Token.class},
        new Object[] {token},
        "A.B()");
    assertEquals(token, expression.getKeyword());
    assertNotNull(expression.getType());
    assertNull(expression.getPeriod());
    assertNull(expression.getIdentifier());
    assertNotNull(expression.getArgumentList());
  }

  public void test_parseInstanceCreationExpression_qualifiedTypeWithIdentifier() throws Exception {
    Token token = new KeywordToken(Keyword.NEW, 0);
    InstanceCreationExpression expression = parse(
        "parseInstanceCreationExpression",
        new Class[] {Token.class},
        new Object[] {token},
        "A.B.c()");
    assertEquals(token, expression.getKeyword());
    assertNotNull(expression.getType());
    assertNotNull(expression.getPeriod());
    assertNotNull(expression.getIdentifier());
    assertNotNull(expression.getArgumentList());
  }

  public void test_parseInstanceCreationExpression_type() throws Exception {
    Token token = new KeywordToken(Keyword.NEW, 0);
    InstanceCreationExpression expression = parse(
        "parseInstanceCreationExpression",
        new Class[] {Token.class},
        new Object[] {token},
        "A()");
    assertEquals(token, expression.getKeyword());
    assertNotNull(expression.getType());
    assertNull(expression.getPeriod());
    assertNull(expression.getIdentifier());
    assertNotNull(expression.getArgumentList());
  }

  public void test_parseInstanceCreationExpression_type_withArg() throws Exception {
    Token token = new KeywordToken(Keyword.NEW, 0);
    InstanceCreationExpression expression = parse(
        "parseInstanceCreationExpression",
        new Class[] {Token.class},
        new Object[] {token},
        "A<B>.c()");
    assertEquals(token, expression.getKeyword());
    assertNotNull(expression.getType());
    assertNotNull(expression.getPeriod());
    assertNotNull(expression.getIdentifier());
    assertNotNull(expression.getArgumentList());
  }

  public void test_parseLibraryDirective_multiple() throws Exception {
    LibraryDirective directive = parse("parseLibraryDirective", "library l.m;");
    assertNotNull(directive.getLibraryToken());
    assertNotNull(directive.getName());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseLibraryDirective_single() throws Exception {
    LibraryDirective directive = parse("parseLibraryDirective", "library l;");
    assertNotNull(directive.getLibraryToken());
    assertNotNull(directive.getName());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseListLiteral_empty() throws Exception {
    Token token = new KeywordToken(Keyword.CONST, 0);
    TypeArgumentList typeArguments = new TypeArgumentList(null, null, null);
    ListLiteral literal = parse("parseListLiteral", new Class[] {
        Token.class, TypeArgumentList.class}, new Object[] {token, typeArguments}, "[]");
    assertEquals(token, literal.getModifier());
    assertEquals(typeArguments, literal.getTypeArguments());
    assertNotNull(literal.getLeftBracket());
    assertSize(0, literal.getElements());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseListLiteral_empty_twoTokens() throws Exception {
    Token token = new KeywordToken(Keyword.CONST, 0);
    TypeArgumentList typeArguments = new TypeArgumentList(null, null, null);
    ListLiteral literal = parse("parseListLiteral", new Class[] {
        Token.class, TypeArgumentList.class}, new Object[] {token, typeArguments}, "[ ]");
    assertEquals(token, literal.getModifier());
    assertEquals(typeArguments, literal.getTypeArguments());
    assertNotNull(literal.getLeftBracket());
    assertSize(0, literal.getElements());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseListLiteral_multiple() throws Exception {
    ListLiteral literal = parse("parseListLiteral", new Class[] {
        Token.class, TypeArgumentList.class}, new Object[] {null, null}, "[1, 2, 3]");
    assertNull(literal.getModifier());
    assertNull(literal.getTypeArguments());
    assertNotNull(literal.getLeftBracket());
    assertSize(3, literal.getElements());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseListLiteral_single() throws Exception {
    ListLiteral literal = parse("parseListLiteral", new Class[] {
        Token.class, TypeArgumentList.class}, new Object[] {null, null}, "[1]");
    assertNull(literal.getModifier());
    assertNull(literal.getTypeArguments());
    assertNotNull(literal.getLeftBracket());
    assertSize(1, literal.getElements());
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
    Token token = new KeywordToken(Keyword.CONST, 0);
    TypeArgumentList typeArguments = new TypeArgumentList(null, null, null);
    MapLiteral literal = parse(
        "parseMapLiteral",
        new Class[] {Token.class, TypeArgumentList.class},
        new Object[] {token, typeArguments},
        "{}");
    assertEquals(token, literal.getModifier());
    assertEquals(typeArguments, literal.getTypeArguments());
    assertNotNull(literal.getLeftBracket());
    assertSize(0, literal.getEntries());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseMapLiteral_multiple() throws Exception {
    MapLiteral literal = parse(
        "parseMapLiteral",
        new Class[] {Token.class, TypeArgumentList.class},
        new Object[] {null, null},
        "{'a' : b, 'x' : y}");
    assertNotNull(literal.getLeftBracket());
    assertSize(2, literal.getEntries());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseMapLiteral_single() throws Exception {
    MapLiteral literal = parse(
        "parseMapLiteral",
        new Class[] {Token.class, TypeArgumentList.class},
        new Object[] {null, null},
        "{'x' : y}");
    assertNotNull(literal.getLeftBracket());
    assertSize(1, literal.getEntries());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseMapLiteralEntry() throws Exception {
    MapLiteralEntry entry = parse("parseMapLiteralEntry", "'x' : y");
    assertNotNull(entry.getKey());
    assertNotNull(entry.getSeparator());
    assertNotNull(entry.getValue());
  }

  public void test_parseMethodDeclaration_notStatic_returnType() throws Exception {
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    MethodDeclaration method = parse("parseMethodDeclaration", new Class[] {
        Comment.class, Token.class, Token.class}, new Object[] {comment, null, null}, "int m() {}");
    assertNotNull(method.getBody());
    assertEquals(comment, method.getDocumentationComment());
    assertNull(method.getExternalKeyword());
    assertNull(method.getModifierKeyword());
    assertNotNull(method.getName());
    assertNull(method.getOperatorKeyword());
    assertNotNull(method.getParameters());
    assertNull(method.getPropertyKeyword());
    assertNotNull(method.getReturnType());
  }

  public void test_parseMethodDeclaration_static_noReturnType() throws Exception {
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    Token staticKeyword = new KeywordToken(Keyword.STATIC, 0);
    MethodDeclaration method = parse(
        "parseMethodDeclaration",
        new Class[] {Comment.class, Token.class, Token.class},
        new Object[] {comment, null, staticKeyword},
        "m() {}");
    assertNotNull(method.getBody());
    assertEquals(comment, method.getDocumentationComment());
    assertNull(method.getExternalKeyword());
    assertEquals(staticKeyword, method.getModifierKeyword());
    assertNotNull(method.getName());
    assertNull(method.getOperatorKeyword());
    assertNotNull(method.getParameters());
    assertNull(method.getPropertyKeyword());
    assertNull(method.getReturnType());
  }

  public void test_parseMethodOrConstructor() throws Exception {
    // TODO(brianwilkerson) Test other variations on this method.
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    Token staticKeyword = new KeywordToken(Keyword.STATIC, 0);
    TypeName returnType = new TypeName(new SimpleIdentifier(null), null);
    MethodDeclaration method = parse("parseMethodOrConstructor", new Class[] {
        Comment.class, Token.class, Token.class, TypeName.class}, new Object[] {
        comment, null, staticKeyword, returnType}, "m() {}");
    assertNotNull(method.getBody());
    assertEquals(comment, method.getDocumentationComment());
    assertNull(method.getExternalKeyword());
    assertEquals(staticKeyword, method.getModifierKeyword());
    assertNotNull(method.getName());
    assertNull(method.getOperatorKeyword());
    assertNotNull(method.getParameters());
    assertNull(method.getPropertyKeyword());
    assertEquals(returnType, method.getReturnType());
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
    assertNotNull(expression.getArgumentList());
    assertNull(expression.getIdentifier());
    assertNotNull(expression.getKeyword());
    assertNotNull(expression.getType());
  }

  public void test_parseNonLabeledStatement_const_map() throws Exception {
    ExpressionStatement statement = parse("parseNonLabeledStatement", "const {'a' : 1};");
    assertNotNull(statement.getExpression());
  }

  public void test_parseNonLabeledStatement_const_object() throws Exception {
    ExpressionStatement statement = parse("parseNonLabeledStatement", "const A();");
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

  public void test_parseNonLabeledStatement_functionInvocation() throws Exception {
    ExpressionStatement statement = parse("parseNonLabeledStatement", "f();");
    assertNotNull(statement.getExpression());
  }

  public void test_parseNonLabeledStatement_null() throws Exception {
    ExpressionStatement statement = parse("parseNonLabeledStatement", "null;");
    assertNotNull(statement.getExpression());
  }

  public void test_parseNonLabeledStatement_true() throws Exception {
    ExpressionStatement statement = parse("parseNonLabeledStatement", "true;");
    assertNotNull(statement.getExpression());
  }

  public void test_parseOperator() throws Exception {
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    TypeName returnType = new TypeName(new SimpleIdentifier(null), null);
    MethodDeclaration method = parse(
        "parseOperator",
        new Class[] {Comment.class, Token.class, TypeName.class},
        new Object[] {comment, null, returnType},
        "operator +(A a);");
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

  public void test_parsePartDirective_part() throws Exception {
    PartDirective directive = parse("parsePartDirective", "part 'lib/lib.dart';");
    assertNotNull(directive.getPartToken());
    assertNotNull(directive.getPartUri());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parsePartDirective_partOf() throws Exception {
    PartOfDirective directive = parse("parsePartDirective", "part of l;");
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

  public void test_parsePrimaryExpression_argumentDefinitionTest() throws Exception {
    ArgumentDefinitionTest expression = parse("parseArgumentDefinitionTest", "?a");
    assertNotNull(expression.getQuestion());
    assertNotNull(expression.getIdentifier());
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
    assertNull(expression.getReturnType());
    assertNull(expression.getName());
    assertNotNull(expression.getParameters());
    assertNotNull(expression.getBody());
  }

  public void test_parsePrimaryExpression_function_noArguments() throws Exception {
    FunctionExpression expression = parse("parsePrimaryExpression", "() => 42");
    assertNull(expression.getReturnType());
    assertNull(expression.getName());
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
    assertSize(1, literal.getTypeArguments().getArguments());
  }

  public void test_parsePrimaryExpression_mapLiteral() throws Exception {
    MapLiteral literal = parse("parsePrimaryExpression", "{}");
    assertNotNull(literal);
  }

  public void test_parsePrimaryExpression_mapLiteral_typed() throws Exception {
    MapLiteral literal = parse("parsePrimaryExpression", "<A>{}");
    assertNotNull(literal.getTypeArguments());
    assertSize(1, literal.getTypeArguments().getArguments());
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
    ParenthesizedExpression expression = parse("parsePrimaryExpression", "()");
    assertNotNull(expression);
  }

  public void test_parsePrimaryExpression_string() throws Exception {
    SimpleStringLiteral literal = parse("parsePrimaryExpression", "\"string\"");
    assertFalse(literal.isMultiline());
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
    IsExpression expression = parse("parseRelationalExpression", "x as Y");
    assertNotNull(expression.getExpression());
    assertNotNull(expression.getIsOperator());
    assertNull(expression.getNotOperator());
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

  public void test_parseReturnType_void() throws Exception {
    TypeName typeName = parse("parseReturnType", "void");
    assertNotNull(typeName.getName());
    assertNull(typeName.getTypeArguments());
  }

  public void test_parseSetter_nonStatic() throws Exception {
    Comment comment = Comment.createDocumentationComment(new Token[0]);
    TypeName returnType = new TypeName(new SimpleIdentifier(null), null);
    MethodDeclaration method = parse("parseSetter", new Class[] {
        Comment.class, Token.class, Token.class, TypeName.class}, new Object[] {
        comment, null, null, returnType}, "set a(var x);");
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
    Token staticKeyword = new KeywordToken(Keyword.STATIC, 0);
    TypeName returnType = new TypeName(new SimpleIdentifier(null), null);
    MethodDeclaration method = parse("parseSetter", new Class[] {
        Comment.class, Token.class, Token.class, TypeName.class}, new Object[] {
        comment, null, staticKeyword, returnType}, "set a(var x);");
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

  public void test_parseStatement_functionDeclaration() throws Exception {
    ExpressionStatement statement = parse("parseStatement", "int f(a, b) {};");
    assertInstanceOf(FunctionExpression.class, statement.getExpression());
  }

  public void test_parseStatement_mulipleLabels() throws Exception {
    LabeledStatement statement = parse("parseStatement", "l: m: return x;");
    assertSize(2, statement.getLabels());
    assertNotNull(statement.getStatement());
  }

  public void test_parseStatement_noLabels() throws Exception {
    parse("parseStatement", "return x;");
  }

  public void test_parseStatement_singleLabel() throws Exception {
    LabeledStatement statement = parse("parseStatement", "l: return x;");
    assertSize(1, statement.getLabels());
    assertNotNull(statement.getStatement());
  }

  public void test_parseStringLiteral_adjacent() throws Exception {
    AdjacentStrings literal = parse("parseStringLiteral", "'a' 'b'");
    NodeList<StringLiteral> strings = literal.getStrings();
    assertSize(2, strings);
    StringLiteral firstString = strings.get(0);
    StringLiteral secondString = strings.get(1);
    assertEquals("a", ((SimpleStringLiteral) firstString).getValue());
    assertEquals("b", ((SimpleStringLiteral) secondString).getValue());
  }

  public void test_parseStringLiteral_interpolated() throws Exception {
    StringInterpolation literal = parse("parseStringLiteral", "'a${b}c'");
    NodeList<InterpolationElement> elements = literal.getElements();
    assertSize(3, elements);
    InterpolationElement element1 = elements.get(0);
    InterpolationElement element2 = elements.get(1);
    InterpolationElement element3 = elements.get(2);
    assertTrue(element1 instanceof InterpolationString);
    assertTrue(element2 instanceof InterpolationExpression);
    assertTrue(element3 instanceof InterpolationString);
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
    SwitchStatement statement = parse("parseSwitchStatement", "switch (a) {case 1: return '1';}");
    assertNotNull(statement.getKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNotNull(statement.getExpression());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getLeftBracket());
    assertSize(1, statement.getMembers());
    assertNotNull(statement.getRightBracket());
  }

  public void test_parseSwitchStatement_empty() throws Exception {
    SwitchStatement statement = parse("parseSwitchStatement", "switch (a) {}");
    assertNotNull(statement.getKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNotNull(statement.getExpression());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getLeftBracket());
    assertSize(0, statement.getMembers());
    assertNotNull(statement.getRightBracket());
  }

  public void test_parseSwitchStatement_labeledCase() throws Exception {
    SwitchStatement statement = parse("parseSwitchStatement", "switch (a) {l1: l2: l3: case(1):}");
    assertNotNull(statement.getKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNotNull(statement.getExpression());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getLeftBracket());
    assertSize(1, statement.getMembers());
    assertSize(3, statement.getMembers().get(0).getLabels());
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
    assertSize(1, statement.getMembers());
    assertSize(3, statement.getMembers().get(0).getStatements());
    assertNotNull(statement.getRightBracket());
  }

  public void test_parseThrowStatement_expression() throws Exception {
    ThrowStatement statement = parse("parseThrowStatement", "throw x;");
    assertNotNull(statement.getKeyword());
    assertNotNull(statement.getExpression());
    assertNotNull(statement.getSemicolon());
  }

  public void test_parseThrowStatement_noExpression() throws Exception {
    ThrowStatement statement = parse("parseThrowStatement", "throw;");
    assertNotNull(statement.getKeyword());
    assertNull(statement.getExpression());
    assertNotNull(statement.getSemicolon());
  }

  public void test_parseTryStatement_catch() throws Exception {
    TryStatement statement = parse("parseTryStatement", "try {} catch (e) {}");
    assertNotNull(statement.getTryKeyword());
    assertNotNull(statement.getBody());
    NodeList<CatchClause> catchClauses = statement.getCatchClauses();
    assertSize(1, catchClauses);
    CatchClause clause = catchClauses.get(0);
    assertNull(clause.getOnKeyword());
    assertNull(clause.getExceptionType());
    assertNotNull(clause.getCatchKeyword());
    assertNotNull(clause.getExceptionParameter());
    assertNull(clause.getComma());
    assertNull(clause.getStackTraceParameter());
    assertNotNull(clause.getBody());
    assertNull(statement.getFinallyKeyword());
    assertNull(statement.getFinallyClause());
  }

  public void test_parseTryStatement_catch_finally() throws Exception {
    TryStatement statement = parse("parseTryStatement", "try {} catch (e, s) {} finally {}");
    assertNotNull(statement.getTryKeyword());
    assertNotNull(statement.getBody());
    NodeList<CatchClause> catchClauses = statement.getCatchClauses();
    assertSize(1, catchClauses);
    CatchClause clause = catchClauses.get(0);
    assertNull(clause.getOnKeyword());
    assertNull(clause.getExceptionType());
    assertNotNull(clause.getCatchKeyword());
    assertNotNull(clause.getExceptionParameter());
    assertNotNull(clause.getComma());
    assertNotNull(clause.getStackTraceParameter());
    assertNotNull(clause.getBody());
    assertNotNull(statement.getFinallyKeyword());
    assertNotNull(statement.getFinallyClause());
  }

  public void test_parseTryStatement_finally() throws Exception {
    TryStatement statement = parse("parseTryStatement", "try {} finally {}");
    assertNotNull(statement.getTryKeyword());
    assertNotNull(statement.getBody());
    assertSize(0, statement.getCatchClauses());
    assertNotNull(statement.getFinallyKeyword());
    assertNotNull(statement.getFinallyClause());
  }

  public void test_parseTryStatement_multiple() throws Exception {
    TryStatement statement = parse(
        "parseTryStatement",
        "try {} on NPE catch (e) {} on Error {} catch (e) {}");
    assertNotNull(statement.getTryKeyword());
    assertNotNull(statement.getBody());
    assertSize(3, statement.getCatchClauses());
    assertNull(statement.getFinallyKeyword());
    assertNull(statement.getFinallyClause());
  }

  public void test_parseTryStatement_on() throws Exception {
    TryStatement statement = parse("parseTryStatement", "try {} on Error {}");
    assertNotNull(statement.getTryKeyword());
    assertNotNull(statement.getBody());
    NodeList<CatchClause> catchClauses = statement.getCatchClauses();
    assertSize(1, catchClauses);
    CatchClause clause = catchClauses.get(0);
    assertNotNull(clause.getOnKeyword());
    assertNotNull(clause.getExceptionType());
    assertNull(clause.getCatchKeyword());
    assertNull(clause.getExceptionParameter());
    assertNull(clause.getComma());
    assertNull(clause.getStackTraceParameter());
    assertNotNull(clause.getBody());
    assertNull(statement.getFinallyKeyword());
    assertNull(statement.getFinallyClause());
  }

  public void test_parseTryStatement_on_catch() throws Exception {
    TryStatement statement = parse("parseTryStatement", "try {} on Error catch (e, s) {}");
    assertNotNull(statement.getTryKeyword());
    assertNotNull(statement.getBody());
    NodeList<CatchClause> catchClauses = statement.getCatchClauses();
    assertSize(1, catchClauses);
    CatchClause clause = catchClauses.get(0);
    assertNotNull(clause.getOnKeyword());
    assertNotNull(clause.getExceptionType());
    assertNotNull(clause.getCatchKeyword());
    assertNotNull(clause.getExceptionParameter());
    assertNotNull(clause.getComma());
    assertNotNull(clause.getStackTraceParameter());
    assertNotNull(clause.getBody());
    assertNull(statement.getFinallyKeyword());
    assertNull(statement.getFinallyClause());
  }

  public void test_parseTryStatement_on_catch_finally() throws Exception {
    TryStatement statement = parse(
        "parseTryStatement",
        "try {} on Error catch (e, s) {} finally {}");
    assertNotNull(statement.getTryKeyword());
    assertNotNull(statement.getBody());
    NodeList<CatchClause> catchClauses = statement.getCatchClauses();
    assertSize(1, catchClauses);
    CatchClause clause = catchClauses.get(0);
    assertNotNull(clause.getOnKeyword());
    assertNotNull(clause.getExceptionType());
    assertNotNull(clause.getCatchKeyword());
    assertNotNull(clause.getExceptionParameter());
    assertNotNull(clause.getComma());
    assertNotNull(clause.getStackTraceParameter());
    assertNotNull(clause.getBody());
    assertNotNull(statement.getFinallyKeyword());
    assertNotNull(statement.getFinallyClause());
  }

  public void test_parseTypeAlias_noParameters() throws Exception {
    TypeAlias typeAlias = parse("parseTypeAlias", "typedef bool F();");
    assertNotNull(typeAlias.getKeyword());
    assertNotNull(typeAlias.getName());
    assertNotNull(typeAlias.getParameters());
    assertNotNull(typeAlias.getReturnType());
    assertNotNull(typeAlias.getSemicolon());
    assertNull(typeAlias.getTypeParameters());
  }

  public void test_parseTypeAlias_noReturnType() throws Exception {
    TypeAlias typeAlias = parse("parseTypeAlias", "typedef F();");
    assertNotNull(typeAlias.getKeyword());
    assertNotNull(typeAlias.getName());
    assertNotNull(typeAlias.getParameters());
    assertNull(typeAlias.getReturnType());
    assertNotNull(typeAlias.getSemicolon());
    assertNull(typeAlias.getTypeParameters());
  }

  public void test_parseTypeAlias_parameters() throws Exception {
    TypeAlias typeAlias = parse("parseTypeAlias", "typedef bool F(Object value);");
    assertNotNull(typeAlias.getKeyword());
    assertNotNull(typeAlias.getName());
    assertNotNull(typeAlias.getParameters());
    assertNotNull(typeAlias.getReturnType());
    assertNotNull(typeAlias.getSemicolon());
    assertNull(typeAlias.getTypeParameters());
  }

  public void test_parseTypeAlias_voidReturnType() throws Exception {
    TypeAlias typeAlias = parse("parseTypeAlias", "typedef void F();");
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
    assertSize(3, argumentList.getArguments());
    assertNotNull(argumentList.getRightBracket());
  }

  public void test_parseTypeArgumentList_single() throws Exception {
    TypeArgumentList argumentList = parse("parseTypeArgumentList", "<int>");
    assertNotNull(argumentList.getLeftBracket());
    assertSize(1, argumentList.getArguments());
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
    assertSize(3, parameterList.getTypeParameters());
  }

  public void test_parseTypeParameterList_single() throws Exception {
    TypeParameterList parameterList = parse("parseTypeParameterList", "<A>");
    assertNotNull(parameterList.getLeftBracket());
    assertNotNull(parameterList.getRightBracket());
    assertSize(1, parameterList.getTypeParameters());
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

  public void test_parseUnaryExpression_increment() throws Exception {
    PrefixExpression expression = parse("parseUnaryExpression", "++x");
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.PLUS_PLUS, expression.getOperator().getType());
    assertNotNull(expression.getOperand());
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

  public void test_parseVariableDeclarationList_const_noType() throws Exception {
    VariableDeclarationList declarationList = parse("parseVariableDeclarationList", "const a");
    assertNotNull(declarationList.getKeyword());
    assertNull(declarationList.getType());
    assertSize(1, declarationList.getVariables());
  }

  public void test_parseVariableDeclarationList_const_type() throws Exception {
    VariableDeclarationList declarationList = parse("parseVariableDeclarationList", "const A a");
    assertNotNull(declarationList.getKeyword());
    assertNotNull(declarationList.getType());
    assertSize(1, declarationList.getVariables());
  }

  public void test_parseVariableDeclarationList_final_noType() throws Exception {
    VariableDeclarationList declarationList = parse("parseVariableDeclarationList", "final a");
    assertNotNull(declarationList.getKeyword());
    assertNull(declarationList.getType());
    assertSize(1, declarationList.getVariables());
  }

  public void test_parseVariableDeclarationList_final_type() throws Exception {
    VariableDeclarationList declarationList = parse("parseVariableDeclarationList", "final A a");
    assertNotNull(declarationList.getKeyword());
    assertNotNull(declarationList.getType());
    assertSize(1, declarationList.getVariables());
  }

  public void test_parseVariableDeclarationList_multiple() throws Exception {
    VariableDeclarationList declarationList = parse("parseVariableDeclarationList", "var a, b, c");
    assertNotNull(declarationList.getKeyword());
    assertNull(declarationList.getType());
    assertSize(3, declarationList.getVariables());
  }

  public void test_parseVariableDeclarationList_type() throws Exception {
    VariableDeclarationList declarationList = parse("parseVariableDeclarationList", "A a");
    assertNull(declarationList.getKeyword());
    assertNotNull(declarationList.getType());
    assertSize(1, declarationList.getVariables());
  }

  public void test_parseVariableDeclarationList_var() throws Exception {
    VariableDeclarationList declarationList = parse("parseVariableDeclarationList", "var a");
    assertNotNull(declarationList.getKeyword());
    assertNull(declarationList.getType());
    assertSize(1, declarationList.getVariables());
  }

  public void test_parseVariableDeclarationList1() throws Exception {
    TypeName returnType = new TypeName(new SimpleIdentifier(null), null);
    VariableDeclarationList declarationList = parse(
        "parseVariableDeclarationList",
        new Class[] {TypeName.class},
        new Object[] {returnType},
        "a, b;");
    assertNull(declarationList.getKeyword());
    assertEquals(returnType, declarationList.getType());
    assertSize(2, declarationList.getVariables());
  }

  public void test_parseVariableDeclarationList2_type() throws Exception {
    TypeName type = new TypeName(new SimpleIdentifier(null), null);
    VariableDeclarationList declarationList = parse("parseVariableDeclarationList", new Class[] {
        Token.class, TypeName.class}, new Object[] {null, type}, "a");
    assertNull(declarationList.getKeyword());
    assertEquals(type, declarationList.getType());
    assertSize(1, declarationList.getVariables());
  }

  public void test_parseVariableDeclarationList2_var() throws Exception {
    Token keyword = new KeywordToken(Keyword.VAR, 0);
    VariableDeclarationList declarationList = parse("parseVariableDeclarationList", new Class[] {
        Token.class, TypeName.class}, new Object[] {keyword, null}, "a");
    assertEquals(keyword, declarationList.getKeyword());
    assertNull(declarationList.getType());
    assertSize(1, declarationList.getVariables());
  }

  public void test_parseVariableDeclarationStatement_multiple() throws Exception {
    VariableDeclarationStatement statement = parse(
        "parseVariableDeclarationStatement",
        "var x, y, z;");
    assertNotNull(statement.getSemicolon());
    VariableDeclarationList variableList = statement.getVariables();
    assertNotNull(variableList);
    assertSize(3, variableList.getVariables());
  }

  public void test_parseVariableDeclarationStatement_single() throws Exception {
    VariableDeclarationStatement statement = parse("parseVariableDeclarationStatement", "var x;");
    assertNotNull(statement.getSemicolon());
    VariableDeclarationList variableList = statement.getVariables();
    assertNotNull(variableList);
    assertSize(1, variableList.getVariables());
  }

  public void test_parseWhileStatement() throws Exception {
    WhileStatement statement = parse("parseWhileStatement", "while (x) {}");
    assertNotNull(statement.getKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNotNull(statement.getCondition());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
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
   * @return the result of invoking the method
   * @throws Exception if the method could not be invoked or throws an exception
   */
  private String computeStringValue(String lexeme) throws Exception {
    AnalysisErrorListener listener = new AnalysisErrorListener() {
      @Override
      public void onError(AnalysisError event) {
        fail("Unexpected compilation error: " + event.getMessage() + " (" + event.getOffset()
            + ", " + event.getLength() + ")");
      }
    };
    Parser parser = new Parser(null, listener);
    Method method = Parser.class.getDeclaredMethod("computeStringValue", String.class);
    method.setAccessible(true);
    return (String) method.invoke(parser, lexeme);
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
    return invokeParserMethod("isFunctionExpression", source, listener);
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
    return invokeParserMethod("isInitializedVariableDeclaration", source, listener);
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
    return invokeParserMethod("isSwitchMember", source, listener);
  }
}
