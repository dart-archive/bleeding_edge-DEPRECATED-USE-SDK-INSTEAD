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

import com.google.dart.engine.ast.Annotation;
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.BlockFunctionBody;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassMember;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.EmptyStatement;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.IfStatement;
import com.google.dart.engine.ast.IsExpression;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.TokenType;

import java.util.List;

/**
 * The class {@code RecoveryParserTest} defines parser tests that test the parsing of invalid code
 * sequences to ensure that the correct recovery steps are taken in the parser.
 */
public class RecoveryParserTest extends ParserTestCase {
  public void fail_incomplete_returnType() throws Exception {
    parseCompilationUnit(createSource(
        "Map<Symbol, convertStringToSymbolMap(Map<String, dynamic> map) {",
        "  if (map == null) return null;",
        "  Map<Symbol, dynamic> result = new Map<Symbol, dynamic>();",
        "  map.forEach((name, value) {",
        "    result[new Symbol(name)] = value;",
        "  });",
        "  return result;",
        "}"));
  }

  public void test_additiveExpression_missing_LHS() throws Exception {
    BinaryExpression expression = parseExpression("+ y", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
  }

  public void test_additiveExpression_missing_LHS_RHS() throws Exception {
    BinaryExpression expression = parseExpression(
        "+",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_additiveExpression_missing_RHS() throws Exception {
    BinaryExpression expression = parseExpression("x +", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_additiveExpression_missing_RHS_super() throws Exception {
    BinaryExpression expression = parseExpression("super +", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_additiveExpression_precedence_multiplicative_left() throws Exception {
    BinaryExpression expression = parseExpression(
        "* +",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_additiveExpression_precedence_multiplicative_right() throws Exception {
    BinaryExpression expression = parseExpression(
        "+ *",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(BinaryExpression.class, expression.getRightOperand());
  }

  public void test_additiveExpression_super() throws Exception {
    BinaryExpression expression = parseExpression(
        "super + +",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_assignmentExpression_missing_compound1() throws Exception {
    AssignmentExpression expression = parseExpression("= y = 0", ParserErrorCode.MISSING_IDENTIFIER);
    Expression syntheticExpression = expression.getLeftHandSide();
    assertInstanceOf(SimpleIdentifier.class, syntheticExpression);
    assertTrue(syntheticExpression.isSynthetic());
  }

  public void test_assignmentExpression_missing_compound2() throws Exception {
    AssignmentExpression expression = parseExpression("x = = 0", ParserErrorCode.MISSING_IDENTIFIER);
    Expression syntheticExpression = ((AssignmentExpression) expression.getRightHandSide()).getLeftHandSide();
    assertInstanceOf(SimpleIdentifier.class, syntheticExpression);
    assertTrue(syntheticExpression.isSynthetic());
  }

  public void test_assignmentExpression_missing_compound3() throws Exception {
    AssignmentExpression expression = parseExpression("x = y =", ParserErrorCode.MISSING_IDENTIFIER);
    Expression syntheticExpression = ((AssignmentExpression) expression.getRightHandSide()).getRightHandSide();
    assertInstanceOf(SimpleIdentifier.class, syntheticExpression);
    assertTrue(syntheticExpression.isSynthetic());
  }

  public void test_assignmentExpression_missing_LHS() throws Exception {
    AssignmentExpression expression = parseExpression("= 0", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftHandSide());
    assertTrue(expression.getLeftHandSide().isSynthetic());
  }

  public void test_assignmentExpression_missing_RHS() throws Exception {
    AssignmentExpression expression = parseExpression("x =", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftHandSide());
    assertTrue(expression.getRightHandSide().isSynthetic());
  }

  public void test_bitwiseAndExpression_missing_LHS() throws Exception {
    BinaryExpression expression = parseExpression("& y", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
  }

  public void test_bitwiseAndExpression_missing_LHS_RHS() throws Exception {
    BinaryExpression expression = parseExpression(
        "&",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_bitwiseAndExpression_missing_RHS() throws Exception {
    BinaryExpression expression = parseExpression("x &", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_bitwiseAndExpression_missing_RHS_super() throws Exception {
    BinaryExpression expression = parseExpression("super &", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_bitwiseAndExpression_precedence_equality_left() throws Exception {
    BinaryExpression expression = parseExpression(
        "== &&",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_bitwiseAndExpression_precedence_equality_right() throws Exception {
    BinaryExpression expression = parseExpression(
        "&& ==",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(BinaryExpression.class, expression.getRightOperand());
  }

  public void test_bitwiseAndExpression_super() throws Exception {
    BinaryExpression expression = parseExpression(
        "super &  &",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_bitwiseOrExpression_missing_LHS() throws Exception {
    BinaryExpression expression = parseExpression("| y", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
  }

  public void test_bitwiseOrExpression_missing_LHS_RHS() throws Exception {
    BinaryExpression expression = parseExpression(
        "|",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_bitwiseOrExpression_missing_RHS() throws Exception {
    BinaryExpression expression = parseExpression("x |", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_bitwiseOrExpression_missing_RHS_super() throws Exception {
    BinaryExpression expression = parseExpression("super |", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_bitwiseOrExpression_precedence_xor_left() throws Exception {
    BinaryExpression expression = parseExpression(
        "^ |",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_bitwiseOrExpression_precedence_xor_right() throws Exception {
    BinaryExpression expression = parseExpression(
        "| ^",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(BinaryExpression.class, expression.getRightOperand());
  }

  public void test_bitwiseOrExpression_super() throws Exception {
    BinaryExpression expression = parseExpression(
        "super |  |",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_bitwiseXorExpression_missing_LHS() throws Exception {
    BinaryExpression expression = parseExpression("^ y", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
  }

  public void test_bitwiseXorExpression_missing_LHS_RHS() throws Exception {
    BinaryExpression expression = parseExpression(
        "^",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_bitwiseXorExpression_missing_RHS() throws Exception {
    BinaryExpression expression = parseExpression("x ^", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_bitwiseXorExpression_missing_RHS_super() throws Exception {
    BinaryExpression expression = parseExpression("super ^", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_bitwiseXorExpression_precedence_and_left() throws Exception {
    BinaryExpression expression = parseExpression(
        "& ^",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_bitwiseXorExpression_precedence_and_right() throws Exception {
    BinaryExpression expression = parseExpression(
        "^ &",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(BinaryExpression.class, expression.getRightOperand());
  }

  public void test_bitwiseXorExpression_super() throws Exception {
    BinaryExpression expression = parseExpression(
        "super ^  ^",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_classTypeAlias_withBody() throws Exception {
    parseCompilationUnit(createSource(//
        "class A {}",
        "class B = Object with A {}"), ParserErrorCode.EXPECTED_TOKEN);
  }

  public void test_conditionalExpression_missingElse() throws Exception {
    ConditionalExpression expression = parse(
        "parseConditionalExpression",
        "x ? y :",
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getElseExpression());
    assertTrue(expression.getElseExpression().isSynthetic());
  }

  public void test_conditionalExpression_missingThen() throws Exception {
    ConditionalExpression expression = parse(
        "parseConditionalExpression",
        "x ? : z",
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getThenExpression());
    assertTrue(expression.getThenExpression().isSynthetic());
  }

  public void test_equalityExpression_missing_LHS() throws Exception {
    BinaryExpression expression = parseExpression("== y", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
  }

  public void test_equalityExpression_missing_LHS_RHS() throws Exception {
    BinaryExpression expression = parseExpression(
        "==",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_equalityExpression_missing_RHS() throws Exception {
    BinaryExpression expression = parseExpression("x ==", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_equalityExpression_missing_RHS_super() throws Exception {
    BinaryExpression expression = parseExpression("super ==", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_equalityExpression_precedence_relational_left() throws Exception {
    BinaryExpression expression = parseExpression(
        "is ==",
        ParserErrorCode.EXPECTED_TYPE_NAME,
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(IsExpression.class, expression.getLeftOperand());
  }

  public void test_equalityExpression_precedence_relational_right() throws Exception {
    BinaryExpression expression = parseExpression(
        "== is",
        ParserErrorCode.EXPECTED_TYPE_NAME,
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(IsExpression.class, expression.getRightOperand());
  }

  public void test_equalityExpression_super() throws Exception {
    BinaryExpression expression = parseExpression(
        "super ==  ==",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.EQUALITY_CANNOT_BE_EQUALITY_OPERAND);
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_expressionList_multiple_end() throws Exception {
    List<Expression> result = parse(
        "parseExpressionList",
        ", 2, 3, 4",
        ParserErrorCode.MISSING_IDENTIFIER);
    assertSizeOfList(4, result);
    Expression syntheticExpression = result.get(0);
    assertInstanceOf(SimpleIdentifier.class, syntheticExpression);
    assertTrue(syntheticExpression.isSynthetic());
  }

  public void test_expressionList_multiple_middle() throws Exception {
    List<Expression> result = parse(
        "parseExpressionList",
        "1, 2, , 4",
        ParserErrorCode.MISSING_IDENTIFIER);
    assertSizeOfList(4, result);
    Expression syntheticExpression = result.get(2);
    assertInstanceOf(SimpleIdentifier.class, syntheticExpression);
    assertTrue(syntheticExpression.isSynthetic());
  }

  public void test_expressionList_multiple_start() throws Exception {
    List<Expression> result = parse(
        "parseExpressionList",
        "1, 2, 3,",
        ParserErrorCode.MISSING_IDENTIFIER);
    assertSizeOfList(4, result);
    Expression syntheticExpression = result.get(3);
    assertInstanceOf(SimpleIdentifier.class, syntheticExpression);
    assertTrue(syntheticExpression.isSynthetic());
  }

  public void test_functionExpression_in_ConstructorFieldInitializer() throws Exception {
    CompilationUnit unit = parseCompilationUnit(
        "class A { A() : a = (){}; var v; }",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.UNEXPECTED_TOKEN);
    // Make sure we recovered and parsed "var v" correctly
    ClassDeclaration declaration = (ClassDeclaration) unit.getDeclarations().get(0);
    NodeList<ClassMember> members = declaration.getMembers();
    ClassMember fieldDecl = members.get(1);
    assertInstanceOf(FieldDeclaration.class, fieldDecl);
    NodeList<VariableDeclaration> vars = ((FieldDeclaration) fieldDecl).getFields().getVariables();
    assertSizeOfList(1, vars);
    assertEquals("v", vars.get(0).getName().getName());
  }

  public void test_incomplete_topLevelVariable() throws Exception {
    CompilationUnit unit = parseCompilationUnit("String", ParserErrorCode.EXPECTED_EXECUTABLE);
    NodeList<CompilationUnitMember> declarations = unit.getDeclarations();
    assertSizeOfList(1, declarations);
    CompilationUnitMember member = declarations.get(0);
    assertInstanceOf(TopLevelVariableDeclaration.class, member);
    NodeList<VariableDeclaration> variables = ((TopLevelVariableDeclaration) member).getVariables().getVariables();
    assertSizeOfList(1, variables);
    SimpleIdentifier name = variables.get(0).getName();
    assertTrue(name.isSynthetic());
  }

  public void test_incomplete_topLevelVariable_const() throws Exception {
    CompilationUnit unit = parseCompilationUnit(
        "const ",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.EXPECTED_TOKEN);
    NodeList<CompilationUnitMember> declarations = unit.getDeclarations();
    assertSizeOfList(1, declarations);
    CompilationUnitMember member = declarations.get(0);
    assertInstanceOf(TopLevelVariableDeclaration.class, member);
    NodeList<VariableDeclaration> variables = ((TopLevelVariableDeclaration) member).getVariables().getVariables();
    assertSizeOfList(1, variables);
    SimpleIdentifier name = variables.get(0).getName();
    assertTrue(name.isSynthetic());
  }

  public void test_incomplete_topLevelVariable_final() throws Exception {
    CompilationUnit unit = parseCompilationUnit(
        "final ",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.EXPECTED_TOKEN);
    NodeList<CompilationUnitMember> declarations = unit.getDeclarations();
    assertSizeOfList(1, declarations);
    CompilationUnitMember member = declarations.get(0);
    assertInstanceOf(TopLevelVariableDeclaration.class, member);
    NodeList<VariableDeclaration> variables = ((TopLevelVariableDeclaration) member).getVariables().getVariables();
    assertSizeOfList(1, variables);
    SimpleIdentifier name = variables.get(0).getName();
    assertTrue(name.isSynthetic());
  }

  public void test_incomplete_topLevelVariable_var() throws Exception {
    CompilationUnit unit = parseCompilationUnit(
        "var ",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.EXPECTED_TOKEN);
    NodeList<CompilationUnitMember> declarations = unit.getDeclarations();
    assertSizeOfList(1, declarations);
    CompilationUnitMember member = declarations.get(0);
    assertInstanceOf(TopLevelVariableDeclaration.class, member);
    NodeList<VariableDeclaration> variables = ((TopLevelVariableDeclaration) member).getVariables().getVariables();
    assertSizeOfList(1, variables);
    SimpleIdentifier name = variables.get(0).getName();
    assertTrue(name.isSynthetic());
  }

  public void test_incompleteField_const() throws Exception {
    CompilationUnit unit = parseCompilationUnit(
        createSource("class C {", "  const", "}"),
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.EXPECTED_TOKEN);
    NodeList<CompilationUnitMember> declarations = unit.getDeclarations();
    assertSizeOfList(1, declarations);
    CompilationUnitMember unitMember = declarations.get(0);
    assertInstanceOf(ClassDeclaration.class, unitMember);
    NodeList<ClassMember> members = ((ClassDeclaration) unitMember).getMembers();
    assertSizeOfList(1, members);
    ClassMember classMember = members.get(0);
    assertInstanceOf(FieldDeclaration.class, classMember);
    VariableDeclarationList fieldList = ((FieldDeclaration) classMember).getFields();
    assertEquals(Keyword.CONST, ((KeywordToken) fieldList.getKeyword()).getKeyword());
    NodeList<VariableDeclaration> fields = fieldList.getVariables();
    assertSizeOfList(1, fields);
    VariableDeclaration field = fields.get(0);
    assertTrue(field.getName().isSynthetic());
  }

  public void test_incompleteField_final() throws Exception {
    CompilationUnit unit = parseCompilationUnit(
        createSource("class C {", "  final", "}"),
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.EXPECTED_TOKEN);
    NodeList<CompilationUnitMember> declarations = unit.getDeclarations();
    assertSizeOfList(1, declarations);
    CompilationUnitMember unitMember = declarations.get(0);
    assertInstanceOf(ClassDeclaration.class, unitMember);
    NodeList<ClassMember> members = ((ClassDeclaration) unitMember).getMembers();
    assertSizeOfList(1, members);
    ClassMember classMember = members.get(0);
    assertInstanceOf(FieldDeclaration.class, classMember);
    VariableDeclarationList fieldList = ((FieldDeclaration) classMember).getFields();
    assertEquals(Keyword.FINAL, ((KeywordToken) fieldList.getKeyword()).getKeyword());
    NodeList<VariableDeclaration> fields = fieldList.getVariables();
    assertSizeOfList(1, fields);
    VariableDeclaration field = fields.get(0);
    assertTrue(field.getName().isSynthetic());
  }

  public void test_incompleteField_var() throws Exception {
    CompilationUnit unit = parseCompilationUnit(
        createSource("class C {", "  var", "}"),
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.EXPECTED_TOKEN);
    NodeList<CompilationUnitMember> declarations = unit.getDeclarations();
    assertSizeOfList(1, declarations);
    CompilationUnitMember unitMember = declarations.get(0);
    assertInstanceOf(ClassDeclaration.class, unitMember);
    NodeList<ClassMember> members = ((ClassDeclaration) unitMember).getMembers();
    assertSizeOfList(1, members);
    ClassMember classMember = members.get(0);
    assertInstanceOf(FieldDeclaration.class, classMember);
    VariableDeclarationList fieldList = ((FieldDeclaration) classMember).getFields();
    assertEquals(Keyword.VAR, ((KeywordToken) fieldList.getKeyword()).getKeyword());
    NodeList<VariableDeclaration> fields = fieldList.getVariables();
    assertSizeOfList(1, fields);
    VariableDeclaration field = fields.get(0);
    assertTrue(field.getName().isSynthetic());
  }

  public void test_isExpression_noType() throws Exception {
    CompilationUnit unit = parseCompilationUnit(
        "class Bar<T extends Foo> {m(x){if (x is ) return;if (x is !)}}",
        ParserErrorCode.EXPECTED_TYPE_NAME,
        ParserErrorCode.EXPECTED_TYPE_NAME,
        ParserErrorCode.MISSING_STATEMENT);
    ClassDeclaration declaration = (ClassDeclaration) unit.getDeclarations().get(0);
    MethodDeclaration method = (MethodDeclaration) declaration.getMembers().get(0);
    BlockFunctionBody body = (BlockFunctionBody) method.getBody();
    IfStatement ifStatement = (IfStatement) body.getBlock().getStatements().get(1);
    IsExpression expression = (IsExpression) ifStatement.getCondition();
    assertNotNull(expression.getExpression());
    assertNotNull(expression.getIsOperator());
    assertNotNull(expression.getNotOperator());
    TypeName type = expression.getType();
    assertNotNull(type);
    assertTrue(type.getName().isSynthetic());
    assertInstanceOf(EmptyStatement.class, ifStatement.getThenStatement());
  }

  public void test_logicalAndExpression_missing_LHS() throws Exception {
    BinaryExpression expression = parseExpression("&& y", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
  }

  public void test_logicalAndExpression_missing_LHS_RHS() throws Exception {
    BinaryExpression expression = parseExpression(
        "&&",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_logicalAndExpression_missing_RHS() throws Exception {
    BinaryExpression expression = parseExpression("x &&", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_logicalAndExpression_precedence_bitwiseOr_left() throws Exception {
    BinaryExpression expression = parseExpression(
        "| &&",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_logicalAndExpression_precedence_bitwiseOr_right() throws Exception {
    BinaryExpression expression = parseExpression(
        "&& |",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(BinaryExpression.class, expression.getRightOperand());
  }

  public void test_logicalOrExpression_missing_LHS() throws Exception {
    BinaryExpression expression = parseExpression("|| y", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
  }

  public void test_logicalOrExpression_missing_LHS_RHS() throws Exception {
    BinaryExpression expression = parseExpression(
        "||",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_logicalOrExpression_missing_RHS() throws Exception {
    BinaryExpression expression = parseExpression("x ||", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_logicalOrExpression_precedence_logicalAnd_left() throws Exception {
    BinaryExpression expression = parseExpression(
        "&& ||",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_logicalOrExpression_precedence_logicalAnd_right() throws Exception {
    BinaryExpression expression = parseExpression(
        "|| &&",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(BinaryExpression.class, expression.getRightOperand());
  }

  public void test_missingGet() throws Exception {
    CompilationUnit unit = parseCompilationUnit(createSource(//
        "class C {",
        "  int length {}",
        "  void foo() {}",
        "}"), ParserErrorCode.MISSING_GET);
    assertNotNull(unit);
    ClassDeclaration classDeclaration = (ClassDeclaration) unit.getDeclarations().get(0);
    NodeList<ClassMember> members = classDeclaration.getMembers();
    assertSizeOfList(2, members);
    assertInstanceOf(MethodDeclaration.class, members.get(0));
    ClassMember member = members.get(1);
    assertInstanceOf(MethodDeclaration.class, member);
    assertEquals("foo", ((MethodDeclaration) member).getName().getName());
  }

  public void test_missingIdentifier_afterAnnotation() throws Exception {
    MethodDeclaration method = parse(
        "parseClassMember",
        new Object[] {"C"},
        "@override }",
        ParserErrorCode.EXPECTED_CLASS_MEMBER);
    assertNull(method.getDocumentationComment());
    NodeList<Annotation> metadata = method.getMetadata();
    assertSizeOfList(1, metadata);
    assertEquals("override", metadata.get(0).getName().getName());
  }

  public void test_multiplicativeExpression_missing_LHS() throws Exception {
    BinaryExpression expression = parseExpression("* y", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
  }

  public void test_multiplicativeExpression_missing_LHS_RHS() throws Exception {
    BinaryExpression expression = parseExpression(
        "*",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_multiplicativeExpression_missing_RHS() throws Exception {
    BinaryExpression expression = parseExpression("x *", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_multiplicativeExpression_missing_RHS_super() throws Exception {
    BinaryExpression expression = parseExpression("super *", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_multiplicativeExpression_precedence_unary_left() throws Exception {
    BinaryExpression expression = parseExpression("-x *", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(PrefixExpression.class, expression.getLeftOperand());
  }

  public void test_multiplicativeExpression_precedence_unary_right() throws Exception {
    BinaryExpression expression = parseExpression("* -y", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(PrefixExpression.class, expression.getRightOperand());
  }

  public void test_multiplicativeExpression_super() throws Exception {
    BinaryExpression expression = parseExpression(
        "super ==  ==",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.EQUALITY_CANNOT_BE_EQUALITY_OPERAND);
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_prefixExpression_missing_operand_minus() throws Exception {
    PrefixExpression expression = parseExpression("-", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getOperand());
    assertTrue(expression.getOperand().isSynthetic());
    assertEquals(TokenType.MINUS, expression.getOperator().getType());
  }

  public void test_primaryExpression_argumentDefinitionTest() throws Exception {
    Expression expression = parse("parsePrimaryExpression", "?a", ParserErrorCode.UNEXPECTED_TOKEN);
    assertInstanceOf(SimpleIdentifier.class, expression);
  }

  public void test_relationalExpression_missing_LHS() throws Exception {
    IsExpression expression = parseExpression("is y", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getExpression());
    assertTrue(expression.getExpression().isSynthetic());
  }

  public void test_relationalExpression_missing_LHS_RHS() throws Exception {
    IsExpression expression = parseExpression(
        "is",
        ParserErrorCode.EXPECTED_TYPE_NAME,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getExpression());
    assertTrue(expression.getExpression().isSynthetic());
    assertInstanceOf(TypeName.class, expression.getType());
    assertTrue(expression.getType().isSynthetic());
  }

  public void test_relationalExpression_missing_RHS() throws Exception {
    IsExpression expression = parseExpression("x is", ParserErrorCode.EXPECTED_TYPE_NAME);
    assertInstanceOf(TypeName.class, expression.getType());
    assertTrue(expression.getType().isSynthetic());
  }

  public void test_relationalExpression_precedence_shift_right() throws Exception {
    IsExpression expression = parseExpression(
        "<< is",
        ParserErrorCode.EXPECTED_TYPE_NAME,
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(BinaryExpression.class, expression.getExpression());
  }

  public void test_shiftExpression_missing_LHS() throws Exception {
    BinaryExpression expression = parseExpression("<< y", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
  }

  public void test_shiftExpression_missing_LHS_RHS() throws Exception {
    BinaryExpression expression = parseExpression(
        "<<",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getLeftOperand());
    assertTrue(expression.getLeftOperand().isSynthetic());
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_shiftExpression_missing_RHS() throws Exception {
    BinaryExpression expression = parseExpression("x <<", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_shiftExpression_missing_RHS_super() throws Exception {
    BinaryExpression expression = parseExpression("super <<", ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(SimpleIdentifier.class, expression.getRightOperand());
    assertTrue(expression.getRightOperand().isSynthetic());
  }

  public void test_shiftExpression_precedence_unary_left() throws Exception {
    BinaryExpression expression = parseExpression(
        "+ <<",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_shiftExpression_precedence_unary_right() throws Exception {
    BinaryExpression expression = parseExpression(
        "<< +",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(BinaryExpression.class, expression.getRightOperand());
  }

  public void test_shiftExpression_super() throws Exception {
    BinaryExpression expression = parseExpression(
        "super << <<",
        ParserErrorCode.MISSING_IDENTIFIER,
        ParserErrorCode.MISSING_IDENTIFIER);
    assertInstanceOf(BinaryExpression.class, expression.getLeftOperand());
  }

  public void test_typedef_eof() throws Exception {
    CompilationUnit unit = parseCompilationUnit(
        "typedef n",
        ParserErrorCode.EXPECTED_TOKEN,
        ParserErrorCode.MISSING_TYPEDEF_PARAMETERS);
    NodeList<CompilationUnitMember> declarations = unit.getDeclarations();
    assertSizeOfList(1, declarations);
    CompilationUnitMember member = declarations.get(0);
    assertInstanceOf(FunctionTypeAlias.class, member);
  }
}
