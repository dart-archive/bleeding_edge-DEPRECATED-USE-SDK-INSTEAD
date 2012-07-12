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

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.ast.AdjacentStrings;
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
import com.google.dart.engine.ast.ConstructorInitializer;
import com.google.dart.engine.ast.ContinueStatement;
import com.google.dart.engine.ast.DoStatement;
import com.google.dart.engine.ast.DoubleLiteral;
import com.google.dart.engine.ast.EmptyFunctionBody;
import com.google.dart.engine.ast.EmptyStatement;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.ExpressionFunctionBody;
import com.google.dart.engine.ast.ExtendsClause;
import com.google.dart.engine.ast.ForEachStatement;
import com.google.dart.engine.ast.ForStatement;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionExpressionInvocation;
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
import com.google.dart.engine.ast.NamedExpression;
import com.google.dart.engine.ast.NamedFormalParameter;
import com.google.dart.engine.ast.NodeList;
import com.google.dart.engine.ast.NullLiteral;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.ResourceDirective;
import com.google.dart.engine.ast.ReturnStatement;
import com.google.dart.engine.ast.SimpleFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.SourceDirective;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.StringInterpolation;
import com.google.dart.engine.ast.StringLiteral;
import com.google.dart.engine.ast.SuperExpression;
import com.google.dart.engine.ast.SwitchMember;
import com.google.dart.engine.ast.SwitchStatement;
import com.google.dart.engine.ast.ThisExpression;
import com.google.dart.engine.ast.ThrowStatement;
import com.google.dart.engine.ast.TryStatement;
import com.google.dart.engine.ast.TypeAlias;
import com.google.dart.engine.ast.TypeArgumentList;
import com.google.dart.engine.ast.TypeMember;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.TypeParameter;
import com.google.dart.engine.ast.TypeParameterList;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.ast.VariableDeclarationStatement;
import com.google.dart.engine.ast.WhileStatement;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.KeywordToken;
import com.google.dart.engine.scanner.StringScanner;
import com.google.dart.engine.scanner.StringToken;
import com.google.dart.engine.scanner.Token;
import com.google.dart.engine.scanner.TokenType;

import java.lang.reflect.Field;
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
public class SimpleParserTest extends EngineTestCase {
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

  public void test_parseAdditiveExpression_normal() throws Exception {
    BinaryExpression expression = parse("parseAdditiveExpression", "x + y");
    assertNotNull(expression.getLeftOperand());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.PLUS, expression.getOperator().getType());
    assertNotNull(expression.getRightOperand());
  }

  public void test_parseAdditiveExpression_super() throws Exception {
    BinaryExpression expression = parse("parseAdditiveExpression", "super + y");
    assertNotNull(expression.getLeftOperand());
    assertInstanceOf(SuperExpression.class, expression.getLeftOperand());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.PLUS, expression.getOperator().getType());
    assertNotNull(expression.getRightOperand());
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
    assertEquals(lexeme, identifier.getIdentifier());
  }

  public void test_parseArgumentList_empty() throws Exception {
    ArgumentList argumentList = parse("parseArgumentList", "()");
    NodeList<Expression> arguments = argumentList.getArguments();
    assertNotNull(arguments);
    assertEquals(0, arguments.size());
  }

  public void test_parseArgumentList_mixed() throws Exception {
    ArgumentList argumentList = parse("parseArgumentList", "(w, x, y: y, z: z)");
    NodeList<Expression> arguments = argumentList.getArguments();
    assertNotNull(arguments);
    assertEquals(4, arguments.size());
  }

  public void test_parseArgumentList_noNamed() throws Exception {
    ArgumentList argumentList = parse("parseArgumentList", "(x, y, z)");
    NodeList<Expression> arguments = argumentList.getArguments();
    assertNotNull(arguments);
    assertEquals(3, arguments.size());
  }

  public void test_parseArgumentList_onlyNamed() throws Exception {
    ArgumentList argumentList = parse("parseArgumentList", "(x: x, y: y)");
    NodeList<Expression> arguments = argumentList.getArguments();
    assertNotNull(arguments);
    assertEquals(2, arguments.size());
    assertTrue(arguments.get(0) instanceof NamedExpression);
    assertTrue(arguments.get(1) instanceof NamedExpression);
  }

  public void test_parseAssignableExpression_dot_normal() throws Exception {
    PropertyAccess propertyAccess = parse("parseAssignableExpression", "(x).y");
    assertNotNull(propertyAccess.getTarget());
    assertNotNull(propertyAccess.getOperator());
    assertEquals(TokenType.PERIOD, propertyAccess.getOperator().getType());
    assertNotNull(propertyAccess.getPropertyName());
  }

  public void test_parseAssignableExpression_dot_super() throws Exception {
    PropertyAccess propertyAccess = parse("parseAssignableExpression", "super.y");
    assertNotNull(propertyAccess.getTarget());
    assertInstanceOf(SuperExpression.class, propertyAccess.getTarget());
    assertNotNull(propertyAccess.getOperator());
    assertEquals(TokenType.PERIOD, propertyAccess.getOperator().getType());
    assertNotNull(propertyAccess.getPropertyName());
  }

  public void test_parseAssignableExpression_index_normal() throws Exception {
    ArrayAccess arrayAccess = parse("parseAssignableExpression", "x[y]");
    assertNotNull(arrayAccess.getArray());
    assertNotNull(arrayAccess.getLeftBracket());
    assertNotNull(arrayAccess.getIndex());
    assertNotNull(arrayAccess.getRightBracket());
  }

  public void test_parseAssignableExpression_index_super() throws Exception {
    ArrayAccess expression = parse("parseAssignableExpression", "super[y]");
    assertNotNull(expression.getArray());
    assertNotNull(expression.getLeftBracket());
    assertNotNull(expression.getIndex());
    assertNotNull(expression.getRightBracket());
  }

  public void test_parseAssignableExpression_invoke() throws Exception {
    FunctionExpressionInvocation invocation = parse("parseAssignableExpression", "x(y)");
    assertNotNull(invocation.getFunction());
    ArgumentList argumentList = invocation.getArgumentList();
    assertNotNull(argumentList);
    assertEquals(1, argumentList.getArguments().size());
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
    assertNotNull(expression.getLeftOperand());
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
    assertNotNull(expression.getLeftOperand());
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
    assertNotNull(expression.getLeftOperand());
    assertInstanceOf(SuperExpression.class, expression.getLeftOperand());
    assertNotNull(expression.getOperator());
    assertEquals(TokenType.CARET, expression.getOperator().getType());
    assertNotNull(expression.getRightOperand());
  }

  public void test_parseBlock_empty() throws Exception {
    Block block = parse("parseBlock", "{}");
    assertNotNull(block.getLeftBracket());
    NodeList<Statement> statements = block.getStatements();
    assertNotNull(statements);
    assertEquals(0, statements.size());
    assertNotNull(block.getRightBracket());
  }

  public void test_parseBlock_nonEmpty() throws Exception {
    Block block = parse("parseBlock", "{;}");
    assertNotNull(block.getLeftBracket());
    NodeList<Statement> statements = block.getStatements();
    assertNotNull(statements);
    assertEquals(1, statements.size());
    assertNotNull(block.getRightBracket());
  }

  public void test_parseBooleanLiteral_false() throws Exception {
    BooleanLiteral literal = parse("parseBooleanLiteral", "false");
    assertNotNull(literal.getLiteral());
    assertEquals(false, literal.getValue());
  }

  public void test_parseBooleanLiteral_true() throws Exception {
    BooleanLiteral literal = parse("parseBooleanLiteral", "true");
    assertNotNull(literal.getLiteral());
    assertEquals(true, literal.getValue());
  }

  public void test_parseBreakStatement_label() throws Exception {
    BreakStatement statement = parse("parseBreakStatement", "break foo;");
    assertNotNull(statement.getKeyword());
    assertNotNull(statement.getLabel());
    assertNotNull(statement.getSemicolon());
  }

  public void test_parseBreakStatement_noLabel() throws Exception {
    BreakStatement statement = parse("parseBreakStatement", "break;");
    assertNotNull(statement.getKeyword());
    assertNull(statement.getLabel());
    assertNotNull(statement.getSemicolon());
  }

  // TODO add methods to test cascades: test_parseCascaseSection_*

  public void test_parseClassDeclaration_abstract() throws Exception {
    ClassDeclaration declaration = parse("parseClassDeclaration", "abstract class A {}");
    assertNull(declaration.getDocumentationComment());
    assertNotNull(declaration.getAbstractKeyword());
    assertNull(declaration.getExtendsClause());
    assertNull(declaration.getImplementsClause());
    assertNotNull(declaration.getKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    NodeList<TypeMember> members = declaration.getMembers();
    assertNotNull(members);
    assertEquals(0, members.size());
    assertNotNull(declaration.getRightBracket());
    assertNull(declaration.getTypeParameters());
  }

  public void test_parseClassDeclaration_empty() throws Exception {
    ClassDeclaration declaration = parse("parseClassDeclaration", "class A {}");
    assertNull(declaration.getDocumentationComment());
    assertNull(declaration.getAbstractKeyword());
    assertNull(declaration.getExtendsClause());
    assertNull(declaration.getImplementsClause());
    assertNotNull(declaration.getKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    NodeList<TypeMember> members = declaration.getMembers();
    assertNotNull(members);
    assertEquals(0, members.size());
    assertNotNull(declaration.getRightBracket());
    assertNull(declaration.getTypeParameters());
  }

  public void test_parseClassDeclaration_emptyWithComment() throws Exception {
    ClassDeclaration declaration = parse("parseClassDeclaration", "/** comment */\nclass A {}");
    assertNotNull(declaration.getDocumentationComment());
    assertNull(declaration.getAbstractKeyword());
    assertNull(declaration.getExtendsClause());
    assertNull(declaration.getImplementsClause());
    assertNotNull(declaration.getKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    NodeList<TypeMember> members = declaration.getMembers();
    assertNotNull(members);
    assertEquals(0, members.size());
    assertNotNull(declaration.getRightBracket());
    assertNull(declaration.getTypeParameters());
  }

  public void test_parseClassDeclaration_extends() throws Exception {
    ClassDeclaration declaration = parse("parseClassDeclaration", "class A extends B {}");
    assertNull(declaration.getDocumentationComment());
    assertNull(declaration.getAbstractKeyword());
    assertNotNull(declaration.getExtendsClause());
    assertNull(declaration.getImplementsClause());
    assertNotNull(declaration.getKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    NodeList<TypeMember> members = declaration.getMembers();
    assertNotNull(members);
    assertEquals(0, members.size());
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
    assertNotNull(declaration.getKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    NodeList<TypeMember> members = declaration.getMembers();
    assertNotNull(members);
    assertEquals(0, members.size());
    assertNotNull(declaration.getRightBracket());
    assertNull(declaration.getTypeParameters());
  }

  public void test_parseClassDeclaration_implements() throws Exception {
    ClassDeclaration declaration = parse("parseClassDeclaration", "class A implements C {}");
    assertNull(declaration.getDocumentationComment());
    assertNull(declaration.getAbstractKeyword());
    assertNull(declaration.getExtendsClause());
    assertNotNull(declaration.getImplementsClause());
    assertNotNull(declaration.getKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    NodeList<TypeMember> members = declaration.getMembers();
    assertNotNull(members);
    assertEquals(0, members.size());
    assertNotNull(declaration.getRightBracket());
    assertNull(declaration.getTypeParameters());
  }

  public void test_parseClassDeclaration_nonEmptyMembers() throws Exception {
    ClassDeclaration declaration = parse("parseClassDeclaration", "class A {;}");
    assertNull(declaration.getDocumentationComment());
    assertNull(declaration.getAbstractKeyword());
    assertNull(declaration.getExtendsClause());
    assertNull(declaration.getImplementsClause());
    assertNotNull(declaration.getKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    NodeList<TypeMember> members = declaration.getMembers();
    assertNotNull(members);
    assertEquals(1, members.size());
    assertNotNull(declaration.getRightBracket());
    assertNull(declaration.getTypeParameters());
  }

  public void test_parseClassDeclaration_typeParameters() throws Exception {
    ClassDeclaration declaration = parse("parseClassDeclaration", "class A<B> {}");
    assertNull(declaration.getDocumentationComment());
    assertNull(declaration.getAbstractKeyword());
    assertNull(declaration.getExtendsClause());
    assertNull(declaration.getImplementsClause());
    assertNotNull(declaration.getKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    NodeList<TypeMember> members = declaration.getMembers();
    assertNotNull(members);
    assertEquals(0, members.size());
    assertNotNull(declaration.getRightBracket());
    assertNotNull(declaration.getTypeParameters());
    assertEquals(1, declaration.getTypeParameters().getTypeParameters().size());
  }

  public void test_parseCommentReference_prefixed() throws Exception {
    CommentReference reference = parse("parseCommentReference", new Class[] {
        String.class, int.class}, new Object[] {"a.b", 7}, "");
    PrefixedIdentifier prefixedIdentifier = assertInstanceOf(
        PrefixedIdentifier.class,
        reference.getIdentifier());
    SimpleIdentifier prefix = prefixedIdentifier.getPrefix();
    assertNotNull(prefix.getToken());
    assertEquals("a", prefix.getIdentifier());
    assertEquals(7, prefix.getOffset());
    assertNotNull(prefixedIdentifier.getPeriod());
    SimpleIdentifier identifier = prefixedIdentifier.getIdentifier();
    assertNotNull(identifier.getToken());
    assertEquals("b", identifier.getIdentifier());
    assertEquals(9, identifier.getOffset());
  }

  public void test_parseCommentReference_simple() throws Exception {
    CommentReference reference = parse("parseCommentReference", new Class[] {
        String.class, int.class}, new Object[] {"a", 5}, "");
    SimpleIdentifier identifier = assertInstanceOf(
        SimpleIdentifier.class,
        reference.getIdentifier());
    assertNotNull(identifier.getToken());
    assertEquals("a", identifier.getIdentifier());
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
    assertEquals(2, references.size());
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
    assertEquals(3, references.size());
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
    CompilationUnit unit = parse(
        "parseCompilationUnit",
        "#library('LibraryName');\n#source('A.dart');");
    assertNull(unit.getScriptTag());
    assertEquals(2, unit.getDirectives().size());
    assertEmpty("declaration", unit.getDeclarations());
  }

  public void test_parseCompilationUnit_directives_single() throws Exception {
    CompilationUnit unit = parse("parseCompilationUnit", "#library('LibraryName');");
    assertNull(unit.getScriptTag());
    assertEquals(1, unit.getDirectives().size());
    assertEmpty("declaration", unit.getDeclarations());
  }

  public void test_parseCompilationUnit_empty() throws Exception {
    CompilationUnit unit = parse("parseCompilationUnit", "");
    assertNull(unit.getScriptTag());
    assertEmpty("directive", unit.getDirectives());
    assertEmpty("declaration", unit.getDeclarations());
  }

  public void test_parseCompilationUnit_script() throws Exception {
    CompilationUnit unit = parse("parseCompilationUnit", "#! /bin/dart");
    assertNotNull(unit.getScriptTag());
    assertEmpty("directive", unit.getDirectives());
    assertEmpty("declaration", unit.getDeclarations());
  }

  public void test_parseCompilationUnit_topLevelDeclaration() throws Exception {
    CompilationUnit unit = parse("parseCompilationUnit", "class A {}");
    assertNull(unit.getScriptTag());
    assertEmpty("directive", unit.getDirectives());
    assertEquals(1, unit.getDeclarations().size());
  }

  public void test_parseCompilationUnitMember_class() throws Exception {
    ClassDeclaration declaration = parse("parseCompilationUnitMember", "class A {}");
    assertEquals("A", declaration.getName().getIdentifier());
    assertEmpty("class member", declaration.getMembers());
  }

  public void test_parseCompilationUnitMember_typedef() throws Exception {
    TypeAlias typeAlias = parse("parseCompilationUnitMember", "typedef F();");
    assertEquals("F", typeAlias.getName().getIdentifier());
    assertEmpty("parameter", typeAlias.getParameters().getParameters());
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
    NodeList<Expression> elements = literal.getElements();
    assertNotNull(elements);
    assertEquals(0, elements.size());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseConstExpression_listLiteral_untyped() throws Exception {
    ListLiteral literal = parse("parseConstExpression", "const []");
    assertNotNull(literal.getModifier());
    assertNull(literal.getTypeArguments());
    assertNotNull(literal.getLeftBracket());
    NodeList<Expression> elements = literal.getElements();
    assertNotNull(elements);
    assertEquals(0, elements.size());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseConstExpression_mapLiteral_typed() throws Exception {
    MapLiteral literal = parse("parseConstExpression", "const <A> {}");
    assertNotNull(literal.getLeftBracket());
    NodeList<MapLiteralEntry> entries = literal.getEntries();
    assertNotNull(entries);
    assertEquals(0, entries.size());
    assertNotNull(literal.getRightBracket());
    assertNotNull(literal.getTypeArguments());
  }

  public void test_parseConstExpression_mapLiteral_untyped() throws Exception {
    MapLiteral literal = parse("parseConstExpression", "const {}");
    assertNotNull(literal.getLeftBracket());
    NodeList<MapLiteralEntry> entries = literal.getEntries();
    assertNotNull(entries);
    assertEquals(0, entries.size());
    assertNotNull(literal.getRightBracket());
    assertNull(literal.getTypeArguments());
  }

  public void test_parseContinueStatement_label() throws Exception {
    ContinueStatement statement = parse("parseContinueStatement", "continue foo;");
    assertNotNull(statement.getKeyword());
    assertNotNull(statement.getLabel());
    assertNotNull(statement.getSemicolon());
  }

  public void test_parseContinueStatement_noLabel() throws Exception {
    ContinueStatement statement = parse("parseContinueStatement", "continue;");
    assertNotNull(statement.getKeyword());
    assertNull(statement.getLabel());
    assertNotNull(statement.getSemicolon());
  }

  // TODO add parseDirective() tests

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
    assertNotNull(expression.getLeftOperand());
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

  public void test_parseExpressionList_multiple() throws Exception {
    List<Expression> result = parse("parseExpressionList", "1, 2, 3");
    assertEquals(3, result.size());
  }

  public void test_parseExpressionList_single() throws Exception {
    List<Expression> result = parse("parseExpressionList", "1");
    assertEquals(1, result.size());
  }

  // TODO add tests for parseExpressionStatementOrDeclaration()

  // TODO add tests for parseExpressionWithoutCascade()

  public void test_parseExtendsClause() throws Exception {
    ExtendsClause clause = parse("parseExtendsClause", "extends B");
    assertNotNull(clause.getKeyword());
    assertNotNull(clause.getSuperclass());
    assertInstanceOf(TypeName.class, clause.getSuperclass());
  }

  public void test_parseFactoryConstructor_nameAndQualifier() throws Exception {
    ConstructorDeclaration constructor = parse("parseFactoryConstructor", "factory A.B.c()");
    assertNull(constructor.getColon());
    assertNull(constructor.getDocumentationComment());
    NodeList<ConstructorInitializer> initializers = constructor.getInitializers();
    assertNotNull(initializers);
    assertEquals(0, initializers.size());
    assertNotNull(constructor.getKeyword());
    assertNotNull(constructor.getName());
    assertNotNull(constructor.getParameters());
    assertNotNull(constructor.getPeriod());
    assertNotNull(constructor.getReturnType());
  }

  public void test_parseFactoryConstructor_nameAndQualifier_withComment() throws Exception {
    ConstructorDeclaration constructor = parse(
        "parseFactoryConstructor",
        "/** comment */\nfactory A.B.c()");
    assertNull(constructor.getColon());
    assertNotNull(constructor.getDocumentationComment());
    NodeList<ConstructorInitializer> initializers = constructor.getInitializers();
    assertNotNull(initializers);
    assertEquals(0, initializers.size());
    assertNotNull(constructor.getKeyword());
    assertNotNull(constructor.getName());
    assertNotNull(constructor.getParameters());
    assertNotNull(constructor.getPeriod());
    assertNotNull(constructor.getReturnType());
  }

  public void test_parseFactoryConstructor_noName_noQualifier() throws Exception {
    ConstructorDeclaration constructor = parse("parseFactoryConstructor", "factory A()");
    assertNull(constructor.getColon());
    assertNull(constructor.getDocumentationComment());
    NodeList<ConstructorInitializer> initializers = constructor.getInitializers();
    assertNotNull(initializers);
    assertEquals(0, initializers.size());
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
    NodeList<FormalParameter> parameters = parameterList.getParameters();
    assertNotNull(parameters);
    assertEquals(0, parameters.size());
    assertNull(parameterList.getRightBracket());
    assertNotNull(parameterList.getRightParenthesis());
  }

  public void test_parseFormalParameterList_mixed() throws Exception {
    FormalParameterList parameterList = parse("parseFormalParameterList", "(A a, [B b])");
    assertNotNull(parameterList.getLeftParenthesis());
    assertNotNull(parameterList.getLeftBracket());
    NodeList<FormalParameter> parameters = parameterList.getParameters();
    assertNotNull(parameters);
    assertEquals(2, parameters.size());
    assertNotNull(parameterList.getRightBracket());
    assertNotNull(parameterList.getRightParenthesis());
  }

  public void test_parseFormalParameterList_normal_multiple() throws Exception {
    FormalParameterList parameterList = parse("parseFormalParameterList", "(A a, B b, C c)");
    assertNotNull(parameterList.getLeftParenthesis());
    assertNull(parameterList.getLeftBracket());
    NodeList<FormalParameter> parameters = parameterList.getParameters();
    assertNotNull(parameters);
    assertEquals(3, parameters.size());
    assertNull(parameterList.getRightBracket());
    assertNotNull(parameterList.getRightParenthesis());
  }

  public void test_parseFormalParameterList_normal_single() throws Exception {
    FormalParameterList parameterList = parse("parseFormalParameterList", "(A a)");
    assertNotNull(parameterList.getLeftParenthesis());
    assertNull(parameterList.getLeftBracket());
    NodeList<FormalParameter> parameters = parameterList.getParameters();
    assertNotNull(parameters);
    assertEquals(1, parameters.size());
    assertNull(parameterList.getRightBracket());
    assertNotNull(parameterList.getRightParenthesis());
  }

  public void test_parseFormalParameterList_optional_multiple() throws Exception {
    FormalParameterList parameterList = parse(
        "parseFormalParameterList",
        "([A a = null, B b, C c = null])");
    assertNotNull(parameterList.getLeftParenthesis());
    assertNotNull(parameterList.getLeftBracket());
    NodeList<FormalParameter> parameters = parameterList.getParameters();
    assertNotNull(parameters);
    assertEquals(3, parameters.size());
    assertNotNull(parameterList.getRightBracket());
    assertNotNull(parameterList.getRightParenthesis());
  }

  public void test_parseFormalParameterList_optional_single() throws Exception {
    FormalParameterList parameterList = parse("parseFormalParameterList", "([A a = null])");
    assertNotNull(parameterList.getLeftParenthesis());
    assertNotNull(parameterList.getLeftBracket());
    NodeList<FormalParameter> parameters = parameterList.getParameters();
    assertNotNull(parameters);
    assertEquals(1, parameters.size());
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
    NodeList<Expression> updaters = statement.getUpdaters();
    assertNotNull(updaters);
    assertEquals(0, updaters.size());
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
    NodeList<Expression> updaters = statement.getUpdaters();
    assertNotNull(updaters);
    assertEquals(1, updaters.size());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseForStatement_loop_i() throws Exception {
    ForStatement statement = parse("parseForStatement", "for (var i = 0;;) {}");
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    VariableDeclarationList variables = statement.getVariables();
    assertNotNull(variables);
    assertEquals(1, variables.getVariables().size());
    assertNull(statement.getInitialization());
    assertNotNull(statement.getLeftSeparator());
    assertNull(statement.getCondition());
    assertNotNull(statement.getRightSeparator());
    NodeList<Expression> updaters = statement.getUpdaters();
    assertNotNull(updaters);
    assertEquals(0, updaters.size());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseForStatement_loop_ic() throws Exception {
    ForStatement statement = parse("parseForStatement", "for (var i = 0; i < count;) {}");
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    VariableDeclarationList variables = statement.getVariables();
    assertNotNull(variables);
    assertEquals(1, variables.getVariables().size());
    assertNull(statement.getInitialization());
    assertNotNull(statement.getLeftSeparator());
    assertNotNull(statement.getCondition());
    assertNotNull(statement.getRightSeparator());
    NodeList<Expression> updaters = statement.getUpdaters();
    assertNotNull(updaters);
    assertEquals(0, updaters.size());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseForStatement_loop_icu() throws Exception {
    ForStatement statement = parse("parseForStatement", "for (var i = 0; i < count; i++) {}");
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    VariableDeclarationList variables = statement.getVariables();
    assertNotNull(variables);
    assertEquals(1, variables.getVariables().size());
    assertNull(statement.getInitialization());
    assertNotNull(statement.getLeftSeparator());
    assertNotNull(statement.getCondition());
    assertNotNull(statement.getRightSeparator());
    NodeList<Expression> updaters = statement.getUpdaters();
    assertNotNull(updaters);
    assertEquals(1, updaters.size());
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
    assertEquals(2, variables.getVariables().size());
    assertNull(statement.getInitialization());
    assertNotNull(statement.getLeftSeparator());
    assertNotNull(statement.getCondition());
    assertNotNull(statement.getRightSeparator());
    NodeList<Expression> updaters = statement.getUpdaters();
    assertNotNull(updaters);
    assertEquals(2, updaters.size());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void test_parseForStatement_loop_iu() throws Exception {
    ForStatement statement = parse("parseForStatement", "for (var i = 0;; i++) {}");
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    VariableDeclarationList variables = statement.getVariables();
    assertNotNull(variables);
    assertEquals(1, variables.getVariables().size());
    assertNull(statement.getInitialization());
    assertNotNull(statement.getLeftSeparator());
    assertNull(statement.getCondition());
    assertNotNull(statement.getRightSeparator());
    NodeList<Expression> updaters = statement.getUpdaters();
    assertNotNull(updaters);
    assertEquals(1, updaters.size());
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
    NodeList<Expression> updaters = statement.getUpdaters();
    assertNotNull(updaters);
    assertEquals(1, updaters.size());
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
    assertEquals(3, clause.getInterfaces().size());
    assertNotNull(clause.getKeyword());
  }

  public void test_parseImplementsClause_single() throws Exception {
    ImplementsClause clause = parse("parseImplementsClause", "implements A");
    assertEquals(1, clause.getInterfaces().size());
    assertNotNull(clause.getKeyword());
  }

  public void test_parseImportDirective_export() throws Exception {
    Token hash = new Token(TokenType.HASH, 0);
    ImportDirective directive = parse(
        "parseImportDirective",
        new Class[] {Token.class},
        new Object[] {hash},
        "import('lib/lib.dart', export: true);");
    assertNotNull(directive.getHash());
    assertNotNull(directive.getKeyword());
    assertNotNull(directive.getLeftParenthesis());
    assertNotNull(directive.getLibraryUri());
    assertEquals(1, directive.getCombinators().size());
    assertNotNull(directive.getRightParenthesis());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseImportDirective_full() throws Exception {
    Token hash = new Token(TokenType.HASH, 0);
    ImportDirective directive = parse(
        "parseImportDirective",
        new Class[] {Token.class},
        new Object[] {hash},
        "import('lib/lib.dart', export: true, hide: ['A'], show: ['B'], prefix: 'a');");
    assertNotNull(directive.getHash());
    assertNotNull(directive.getKeyword());
    assertNotNull(directive.getLeftParenthesis());
    assertNotNull(directive.getLibraryUri());
    assertEquals(4, directive.getCombinators().size());
    assertNotNull(directive.getRightParenthesis());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseImportDirective_hide() throws Exception {
    Token hash = new Token(TokenType.HASH, 0);
    ImportDirective directive = parse(
        "parseImportDirective",
        new Class[] {Token.class},
        new Object[] {hash},
        "import('lib/lib.dart', hide: ['A']);");
    assertNotNull(directive.getHash());
    assertNotNull(directive.getKeyword());
    assertNotNull(directive.getLeftParenthesis());
    assertNotNull(directive.getLibraryUri());
    assertEquals(1, directive.getCombinators().size());
    assertNotNull(directive.getRightParenthesis());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseImportDirective_noCombinator() throws Exception {
    Token hash = new Token(TokenType.HASH, 0);
    ImportDirective directive = parse(
        "parseImportDirective",
        new Class[] {Token.class},
        new Object[] {hash},
        "import('lib/lib.dart');");
    assertNotNull(directive.getHash());
    assertNotNull(directive.getKeyword());
    assertNotNull(directive.getLeftParenthesis());
    assertNotNull(directive.getLibraryUri());
    assertEquals(0, directive.getCombinators().size());
    assertNotNull(directive.getRightParenthesis());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseImportDirective_prefix() throws Exception {
    Token hash = new Token(TokenType.HASH, 0);
    ImportDirective directive = parse(
        "parseImportDirective",
        new Class[] {Token.class},
        new Object[] {hash},
        "import('lib/lib.dart', prefix: 'a');");
    assertNotNull(directive.getHash());
    assertNotNull(directive.getKeyword());
    assertNotNull(directive.getLeftParenthesis());
    assertNotNull(directive.getLibraryUri());
    assertEquals(1, directive.getCombinators().size());
    assertNotNull(directive.getRightParenthesis());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseImportDirective_show() throws Exception {
    Token hash = new Token(TokenType.HASH, 0);
    ImportDirective directive = parse(
        "parseImportDirective",
        new Class[] {Token.class},
        new Object[] {hash},
        "import('lib/lib.dart', show: ['A']);");
    assertNotNull(directive.getHash());
    assertNotNull(directive.getKeyword());
    assertNotNull(directive.getLeftParenthesis());
    assertNotNull(directive.getLibraryUri());
    assertEquals(1, directive.getCombinators().size());
    assertNotNull(directive.getRightParenthesis());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseInstanceCreationExpression() throws Exception {
    Token token = new KeywordToken(Keyword.NEW, 0);
    InstanceCreationExpression expression = parse(
        "parseInstanceCreationExpression",
        new Class[] {Token.class},
        new Object[] {token},
        "A()");
    assertNotNull(expression.getArgumentList());
    assertNull(expression.getIdentifier());
    assertEquals(token, expression.getKeyword());
    assertNotNull(expression.getType());
  }

  public void test_parseLibraryDirective() throws Exception {
    Token hash = new Token(TokenType.HASH, 0);
    LibraryDirective directive = parse(
        "parseLibraryDirective",
        new Class[] {Token.class},
        new Object[] {hash},
        "library('lib');");
    assertNotNull(directive.getHash());
    assertNotNull(directive.getKeyword());
    assertNotNull(directive.getLeftParenthesis());
    assertNotNull(directive.getName());
    assertNotNull(directive.getRightParenthesis());
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
    NodeList<Expression> elements = literal.getElements();
    assertNotNull(elements);
    assertEquals(0, elements.size());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseListLiteral_multiple() throws Exception {
    ListLiteral literal = parse("parseListLiteral", new Class[] {
        Token.class, TypeArgumentList.class}, new Object[] {null, null}, "[1, 2, 3]");
    assertNull(literal.getModifier());
    assertNull(literal.getTypeArguments());
    assertNotNull(literal.getLeftBracket());
    NodeList<Expression> elements = literal.getElements();
    assertNotNull(elements);
    assertEquals(3, elements.size());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseListLiteral_single() throws Exception {
    ListLiteral literal = parse("parseListLiteral", new Class[] {
        Token.class, TypeArgumentList.class}, new Object[] {null, null}, "[1]");
    assertNull(literal.getModifier());
    assertNull(literal.getTypeArguments());
    assertNotNull(literal.getLeftBracket());
    NodeList<Expression> elements = literal.getElements();
    assertNotNull(elements);
    assertEquals(1, elements.size());
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
    NodeList<MapLiteralEntry> entries = literal.getEntries();
    assertNotNull(entries);
    assertEquals(0, entries.size());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseMapLiteral_multiple() throws Exception {
    MapLiteral literal = parse(
        "parseMapLiteral",
        new Class[] {Token.class, TypeArgumentList.class},
        new Object[] {null, null},
        "{'a' : b, 'x' : y}");
    assertNotNull(literal.getLeftBracket());
    NodeList<MapLiteralEntry> entries = literal.getEntries();
    assertNotNull(entries);
    assertEquals(2, entries.size());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseMapLiteral_single() throws Exception {
    MapLiteral literal = parse(
        "parseMapLiteral",
        new Class[] {Token.class, TypeArgumentList.class},
        new Object[] {null, null},
        "{'x' : y}");
    assertNotNull(literal.getLeftBracket());
    NodeList<MapLiteralEntry> entries = literal.getEntries();
    assertNotNull(entries);
    assertEquals(1, entries.size());
    assertNotNull(literal.getRightBracket());
  }

  public void test_parseMapLiteralEntry() throws Exception {
    MapLiteralEntry entry = parse("parseMapLiteralEntry", "'x' : y");
    assertNotNull(entry.getKey());
    assertNotNull(entry.getSeparator());
    assertNotNull(entry.getValue());
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
    assertNotNull(expression.getLeftOperand());
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
    assertEquals(lexeme, identifier.getIdentifier());
  }

  public void test_parsePrefixedIdentifier_prefix() throws Exception {
    String lexeme = "foo.bar";
    PrefixedIdentifier identifier = parse("parsePrefixedIdentifier", lexeme);
    assertEquals("foo", identifier.getPrefix().getIdentifier());
    assertNotNull(identifier.getPeriod());
    assertEquals("bar", identifier.getIdentifier().getIdentifier());
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
  }

  public void test_parsePrimaryExpression_hex() throws Exception {
    String hexLiteral = "3F";
    IntegerLiteral literal = parse("parsePrimaryExpression", "0x" + hexLiteral);
    assertNotNull(literal.getLiteral());
    assertEquals(BigInteger.valueOf(Integer.parseInt(hexLiteral, 16)), literal.getValue());
  }

  public void test_parsePrimaryExpression_int() throws Exception {
    String intLiteral = "472";
    IntegerLiteral literal = parse("parsePrimaryExpression", intLiteral);
    assertNotNull(literal.getLiteral());
    assertEquals(BigInteger.valueOf(Integer.parseInt(intLiteral)), literal.getValue());
  }

  public void test_parsePrimaryExpression_null() throws Exception {
    NullLiteral literal = parse("parsePrimaryExpression", "null");
    assertNotNull(literal.getLiteral());
  }

  public void test_parsePrimaryExpression_super() throws Exception {
    SuperExpression expression = parse("parsePrimaryExpression", "super");
    assertNotNull(expression.getKeyword());
  }

  public void test_parsePrimaryExpression_this() throws Exception {
    ThisExpression expression = parse("parsePrimaryExpression", "this");
    assertNotNull(expression.getKeyword());
  }

  public void test_parsePrimaryExpression_true() throws Exception {
    BooleanLiteral literal = parse("parsePrimaryExpression", "true");
    assertNotNull(literal.getLiteral());
  }

  public void test_Parser() {
    assertNotNull(new Parser(null, null));
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

  public void test_parseResourceDirective() throws Exception {
    Token hash = new Token(TokenType.HASH, 0);
    ResourceDirective directive = parse(
        "parseResourceDirective",
        new Class[] {Token.class},
        new Object[] {hash},
        "resource('lib/lib.dart');");
    assertNotNull(directive.getHash());
    assertNotNull(directive.getKeyword());
    assertNotNull(directive.getLeftParenthesis());
    assertNotNull(directive.getResourceUri());
    assertNotNull(directive.getRightParenthesis());
    assertNotNull(directive.getSemicolon());
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

  public void test_parseSimpleFormalParameter_final() throws Exception {
    SimpleFormalParameter parameter = parse("parseSimpleFormalParameter", "final A a");
    assertNotNull(parameter.getIdentifier());
    assertNotNull(parameter.getKeyword());
    assertNotNull(parameter.getType());
  }

  public void test_parseSimpleFormalParameter_type() throws Exception {
    SimpleFormalParameter parameter = parse("parseSimpleFormalParameter", "A a");
    assertNotNull(parameter.getIdentifier());
    assertNull(parameter.getKeyword());
    assertNotNull(parameter.getType());
  }

  public void test_parseSimpleFormalParameter_var() throws Exception {
    SimpleFormalParameter parameter = parse("parseSimpleFormalParameter", "var a");
    assertNotNull(parameter.getIdentifier());
    assertNotNull(parameter.getKeyword());
    assertNull(parameter.getType());
  }

  public void test_parseSimpleIdentifier() throws Exception {
    String lexeme = "foo";
    SimpleIdentifier identifier = parse("parseSimpleIdentifier", lexeme);
    assertNotNull(identifier.getToken());
    assertEquals(lexeme, identifier.getIdentifier());
  }

  public void test_parseSourceDirective() throws Exception {
    Token hash = new Token(TokenType.HASH, 0);
    SourceDirective directive = parse(
        "parseSourceDirective",
        new Class[] {Token.class},
        new Object[] {hash},
        "source('lib/lib.dart');");
    assertNotNull(directive.getHash());
    assertNotNull(directive.getKeyword());
    assertNotNull(directive.getLeftParenthesis());
    assertNotNull(directive.getSourceUri());
    assertNotNull(directive.getRightParenthesis());
    assertNotNull(directive.getSemicolon());
  }

  public void test_parseStatement_singleLabel() throws Exception {
    LabeledStatement statement = parse("parseStatement", "l: return x;");
    assertEquals(1, statement.getLabels().size());
    assertNotNull(statement.getStatement());
  }

  public void test_parseStringLiteral_adjacent() throws Exception {
    AdjacentStrings literal = parse("parseStringLiteral", "'a' 'b'");
    NodeList<StringLiteral> strings = literal.getStrings();
    assertNotNull(strings);
    assertEquals(2, strings.size());
    StringLiteral firstString = strings.get(0);
    StringLiteral secondString = strings.get(1);
    assertEquals("a", ((SimpleStringLiteral) firstString).getValue());
    assertEquals("b", ((SimpleStringLiteral) secondString).getValue());
  }

  public void test_parseStringLiteral_interpolated() throws Exception {
    StringInterpolation literal = parse("parseStringLiteral", "'a${b}c'");
    NodeList<InterpolationElement> elements = literal.getElements();
    assertNotNull(elements);
    assertEquals(3, elements.size());
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

  public void test_parseSwitchStatement_case() throws Exception {
    SwitchStatement statement = parse("parseSwitchStatement", "switch (a) {case 1: return '1';}");
    assertNotNull(statement.getExpression());
    assertNotNull(statement.getKeyword());
    assertNotNull(statement.getLeftBracket());
    assertNotNull(statement.getLeftParenthesis());
    NodeList<SwitchMember> members = statement.getMembers();
    assertNotNull(members);
    assertEquals(1, members.size());
    assertNotNull(statement.getRightBracket());
    assertNotNull(statement.getRightBracket());
  }

  public void test_parseSwitchStatement_empty() throws Exception {
    SwitchStatement statement = parse("parseSwitchStatement", "switch (a) {}");
    assertNotNull(statement.getExpression());
    assertNotNull(statement.getKeyword());
    assertNotNull(statement.getLeftBracket());
    assertNotNull(statement.getLeftParenthesis());
    NodeList<SwitchMember> members = statement.getMembers();
    assertNotNull(members);
    assertEquals(0, members.size());
    assertNotNull(statement.getRightBracket());
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
    assertNotNull(catchClauses);
    assertEquals(1, catchClauses.size());
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
    assertNotNull(catchClauses);
    assertEquals(1, catchClauses.size());
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
    NodeList<CatchClause> catchClauses = statement.getCatchClauses();
    assertNotNull(catchClauses);
    assertEquals(0, catchClauses.size());
    assertNotNull(statement.getFinallyKeyword());
    assertNotNull(statement.getFinallyClause());
  }

  public void test_parseTryStatement_multiple() throws Exception {
    TryStatement statement = parse(
        "parseTryStatement",
        "try {} on NPE catch (e) {} on Error {} catch (e) {}");
    assertNotNull(statement.getTryKeyword());
    assertNotNull(statement.getBody());
    NodeList<CatchClause> catchClauses = statement.getCatchClauses();
    assertNotNull(catchClauses);
    assertEquals(3, catchClauses.size());
    assertNull(statement.getFinallyKeyword());
    assertNull(statement.getFinallyClause());
  }

  public void test_parseTryStatement_on() throws Exception {
    TryStatement statement = parse("parseTryStatement", "try {} on Error {}");
    assertNotNull(statement.getTryKeyword());
    assertNotNull(statement.getBody());
    NodeList<CatchClause> catchClauses = statement.getCatchClauses();
    assertNotNull(catchClauses);
    assertEquals(1, catchClauses.size());
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
    assertNotNull(catchClauses);
    assertEquals(1, catchClauses.size());
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
    assertNotNull(catchClauses);
    assertEquals(1, catchClauses.size());
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

  public void test_parseTypeArgumentList_multiple() throws Exception {
    TypeArgumentList argumentList = parse("parseTypeArgumentList", "<int, int, int>");
    assertNotNull(argumentList.getLeftBracket());
    NodeList<TypeName> arguments = argumentList.getArguments();
    assertNotNull(arguments);
    assertEquals(3, arguments.size());
    assertNotNull(argumentList.getRightBracket());
  }

  public void test_parseTypeArgumentList_single() throws Exception {
    TypeArgumentList argumentList = parse("parseTypeArgumentList", "<int>");
    assertNotNull(argumentList.getLeftBracket());
    NodeList<TypeName> arguments = argumentList.getArguments();
    assertNotNull(arguments);
    assertEquals(1, arguments.size());
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
    NodeList<TypeParameter> parameters = parameterList.getTypeParameters();
    assertNotNull(parameters);
    assertEquals(3, parameters.size());
  }

  public void test_parseTypeParameterList_single() throws Exception {
    TypeParameterList parameterList = parse("parseTypeParameterList", "<A>");
    assertNotNull(parameterList.getLeftBracket());
    assertNotNull(parameterList.getRightBracket());
    NodeList<TypeParameter> parameters = parameterList.getTypeParameters();
    assertNotNull(parameters);
    assertEquals(1, parameters.size());
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
    VariableDeclaration declaration = parse(
        "parseVariableDeclaration",
        new Class[] {Comment.class},
        new Object[] {null},
        "a = b");
    assertNotNull(declaration.getName());
    assertNotNull(declaration.getEquals());
    assertNotNull(declaration.getInitializer());
  }

  public void test_parseVariableDeclaration_noEquals() throws Exception {
    VariableDeclaration declaration = parse(
        "parseVariableDeclaration",
        new Class[] {Comment.class},
        new Object[] {null},
        "a");
    assertNotNull(declaration.getName());
    assertNull(declaration.getEquals());
    assertNull(declaration.getInitializer());
  }

  public void test_parseVariableDeclarationList_constNoType() throws Exception {
    VariableDeclarationList declarationList = parse("parseVariableDeclarationList", "const a");
    assertNotNull(declarationList.getKeyword());
    assertNull(declarationList.getType());
    NodeList<VariableDeclaration> variables = declarationList.getVariables();
    assertNotNull(variables);
    assertEquals(1, variables.size());
  }

  public void test_parseVariableDeclarationList_constType() throws Exception {
    VariableDeclarationList declarationList = parse("parseVariableDeclarationList", "const A a");
    assertNotNull(declarationList.getKeyword());
    assertNotNull(declarationList.getType());
    NodeList<VariableDeclaration> variables = declarationList.getVariables();
    assertNotNull(variables);
    assertEquals(1, variables.size());
  }

  public void test_parseVariableDeclarationList_finalNoType() throws Exception {
    VariableDeclarationList declarationList = parse("parseVariableDeclarationList", "final a");
    assertNotNull(declarationList.getKeyword());
    assertNull(declarationList.getType());
    NodeList<VariableDeclaration> variables = declarationList.getVariables();
    assertNotNull(variables);
    assertEquals(1, variables.size());
  }

  public void test_parseVariableDeclarationList_finalType() throws Exception {
    VariableDeclarationList declarationList = parse("parseVariableDeclarationList", "final A a");
    assertNotNull(declarationList.getKeyword());
    assertNotNull(declarationList.getType());
    NodeList<VariableDeclaration> variables = declarationList.getVariables();
    assertNotNull(variables);
    assertEquals(1, variables.size());
  }

  public void test_parseVariableDeclarationList_multiple() throws Exception {
    VariableDeclarationList declarationList = parse("parseVariableDeclarationList", "var a, b, c");
    assertNotNull(declarationList.getKeyword());
    assertNull(declarationList.getType());
    NodeList<VariableDeclaration> variables = declarationList.getVariables();
    assertNotNull(variables);
    assertEquals(3, variables.size());
  }

  public void test_parseVariableDeclarationList_type() throws Exception {
    VariableDeclarationList declarationList = parse("parseVariableDeclarationList", "A a");
    assertNull(declarationList.getKeyword());
    assertNotNull(declarationList.getType());
    NodeList<VariableDeclaration> variables = declarationList.getVariables();
    assertNotNull(variables);
    assertEquals(1, variables.size());
  }

  public void test_parseVariableDeclarationList_var() throws Exception {
    VariableDeclarationList declarationList = parse("parseVariableDeclarationList", "var a");
    assertNotNull(declarationList.getKeyword());
    assertNull(declarationList.getType());
    NodeList<VariableDeclaration> variables = declarationList.getVariables();
    assertNotNull(variables);
    assertEquals(1, variables.size());
  }

  public void test_parseVariableDeclarationStatement_multiple() throws Exception {
    VariableDeclarationStatement statement = parse(
        "parseVariableDeclarationStatement",
        "var x, y, z;");
    assertNotNull(statement.getSemicolon());
    VariableDeclarationList variableList = statement.getVariables();
    assertNotNull(variableList);
    assertEquals(3, variableList.getVariables().size());
  }

  public void test_parseVariableDeclarationStatement_single() throws Exception {
    VariableDeclarationStatement statement = parse("parseVariableDeclarationStatement", "var x;");
    assertNotNull(statement.getSemicolon());
    VariableDeclarationList variableList = statement.getVariables();
    assertNotNull(variableList);
    assertEquals(1, variableList.getVariables().size());
  }

  public void test_parseWhileStatement() throws Exception {
    WhileStatement statement = parse("parseWhileStatement", "while (x) {}");
    assertNotNull(statement.getKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNotNull(statement.getCondition());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

  public void xtest_parseClassDeclaration_nonEmpty() throws Exception {
    ClassDeclaration declaration = parse("parseClassDeclaration", "class A {var x;}");
    assertNull(declaration.getExtendsClause());
    assertNull(declaration.getImplementsClause());
    assertNotNull(declaration.getKeyword());
    assertNotNull(declaration.getLeftBracket());
    assertNotNull(declaration.getName());
    NodeList<TypeMember> members = declaration.getMembers();
    assertNotNull(members);
    assertEquals(1, members.size());
    assertNotNull(declaration.getRightBracket());
    assertNull(declaration.getTypeParameters());
  }

//  public void xtest_parseCompilationUnitMember() throws Exception {
//    CompilationUnitMember declaration = parse("parseCompilationUnitMember", "");
//  }

  public void xtest_parseCompilationUnit_nonEmpty() throws Exception {
    CompilationUnit unit = parse("parseCompilationUnit", "class Foo {}");
    assertEquals(1, unit.getDeclarations().size());
    assertEquals(0, unit.getDirectives().size());
    assertNull(unit.getScriptTag());
  }

  public void xtest_parseCompilationUnitMember_function() throws Exception {
    FunctionDeclaration declaration = parse("parseCompilationUnitMember", "f() {}");
    assertNotNull(declaration.getFunctionExpression());
  }

  public void xtest_parseCompilationUnitMember_variable() throws Exception {
    VariableDeclaration declaration = parse("parseCompilationUnitMember", "var x;");
    assertNull(declaration.getInitializer());
    assertNotNull(declaration.getName());
  }

//  public void xtest_parseExpressionWithoutCascade() throws Exception {
//    ASTNode expression = parse("parseExpressionWithoutCascade", "");
//  }

  public void xtest_parseForStatement_loop_ecu() throws Exception {
    ForStatement statement = parse("parseForStatement", "for (i--; i < count; i++) {}");
    assertNotNull(statement.getForKeyword());
    assertNotNull(statement.getLeftParenthesis());
    assertNull(statement.getVariables());
    assertNotNull(statement.getInitialization());
    assertNotNull(statement.getLeftSeparator());
    assertNotNull(statement.getCondition());
    assertNotNull(statement.getRightSeparator());
    NodeList<Expression> updaters = statement.getUpdaters();
    assertNotNull(updaters);
    assertEquals(1, updaters.size());
    assertNotNull(statement.getRightParenthesis());
    assertNotNull(statement.getBody());
  }

//  public void xtest_parseFunctionSignature() throws Exception {
//    ASTNode result = parse("parseFunctionSignature", "");
//  }

//  public void xtest_parseInterfaceDeclaration_nonEmpty() throws Exception {
//    InterfaceDeclaration result = parse("parseInterfaceDeclaration", "interface A {var x;}");
//  }

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
   * Invoke a parse method in {@link Parser}. The method is assumed to have the given number and
   * type of parameters and will be invoked with the given arguments.
   * <p>
   * The given source is scanned and the parser is initialized to start with the first token in the
   * source before the parse method is invoked.
   * 
   * @param methodName the name of the parse method that should be invoked to parse the source
   * @param classes the types of the arguments to the method
   * @param objects the values of the arguments to the method
   * @param source the source to be parsed by the parse method
   * @return the result of invoking the method
   * @throws Exception if the method could not be invoked or throws an exception
   */
  @SuppressWarnings("unchecked")
  private <E> E parse(String methodName, Class<?>[] classes, Object[] objects, String source)
      throws Exception {
    if (classes.length != objects.length) {
      fail("Invalid test: number of parameters specified (" + classes.length
          + ") does not match number of arguments provided (" + objects.length + ")");
    }
    AnalysisErrorListener listener = new AnalysisErrorListener() {
      @Override
      public void onError(AnalysisError event) {
        fail("Unexpected compilation error: " + event.getMessage() + " (" + event.getOffset()
            + ", " + event.getLength() + ")");
      }
    };
    StringScanner scanner = new StringScanner(null, source, listener);
    Token tokenStream = scanner.tokenize();
    Parser parser = new Parser(null, listener);
    Field currentTokenField = Parser.class.getDeclaredField("currentToken");
    currentTokenField.setAccessible(true);
    currentTokenField.set(parser, tokenStream);
    Method parseMethod = Parser.class.getDeclaredMethod(methodName, classes);
    parseMethod.setAccessible(true);
    Object result = parseMethod.invoke(parser, objects);
    assertNotNull(result);
    return (E) result;
  }

  /**
   * Invoke a parse method in {@link Parser}. The method is assumed to have no arguments.
   * <p>
   * The given source is scanned and the parser is initialized to start with the first token in the
   * source before the parse method is invoked.
   * 
   * @param methodName the name of the parse method that should be invoked to parse the source
   * @param source the source to be parsed by the parse method
   * @return the result of invoking the method
   * @throws Exception if the method could not be invoked or throws an exception
   */
  @SuppressWarnings("unchecked")
  private <E> E parse(String methodName, String source) throws Exception {
    AnalysisErrorListener listener = new AnalysisErrorListener() {
      @Override
      public void onError(AnalysisError event) {
        fail("Unexpected compilation error: " + event.getMessage() + " (" + event.getOffset()
            + ", " + event.getLength() + ")");
      }
    };
    StringScanner scanner = new StringScanner(null, source, listener);
    Token tokenStream = scanner.tokenize();
    Parser parser = new Parser(null, listener);
    Field currentTokenField = Parser.class.getDeclaredField("currentToken");
    currentTokenField.setAccessible(true);
    currentTokenField.set(parser, tokenStream);
    Method parseMethod = Parser.class.getDeclaredMethod(methodName);
    parseMethod.setAccessible(true);
    Object result = parseMethod.invoke(parser);
    assertNotNull(result);
    return (E) result;
  }
}
