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
package com.google.dart.engine.internal.resolver;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.ast.CatchClause;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.IntegerLiteral;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.ConstructorElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.ParameterElementImpl;
import com.google.dart.engine.internal.element.VariableElementImpl;
import com.google.dart.engine.internal.type.FunctionTypeImpl;
import com.google.dart.engine.internal.type.InterfaceTypeImpl;
import com.google.dart.engine.resolver.ResolverErrorCode;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;

import static com.google.dart.engine.ast.ASTFactory.adjacentStrings;
import static com.google.dart.engine.ast.ASTFactory.argumentDefinitionTest;
import static com.google.dart.engine.ast.ASTFactory.asExpression;
import static com.google.dart.engine.ast.ASTFactory.assignmentExpression;
import static com.google.dart.engine.ast.ASTFactory.binaryExpression;
import static com.google.dart.engine.ast.ASTFactory.blockFunctionBody;
import static com.google.dart.engine.ast.ASTFactory.booleanLiteral;
import static com.google.dart.engine.ast.ASTFactory.cascadeExpression;
import static com.google.dart.engine.ast.ASTFactory.catchClause;
import static com.google.dart.engine.ast.ASTFactory.conditionalExpression;
import static com.google.dart.engine.ast.ASTFactory.doubleLiteral;
import static com.google.dart.engine.ast.ASTFactory.expressionFunctionBody;
import static com.google.dart.engine.ast.ASTFactory.fieldFormalParameter;
import static com.google.dart.engine.ast.ASTFactory.formalParameterList;
import static com.google.dart.engine.ast.ASTFactory.functionExpression;
import static com.google.dart.engine.ast.ASTFactory.identifier;
import static com.google.dart.engine.ast.ASTFactory.indexExpression;
import static com.google.dart.engine.ast.ASTFactory.instanceCreationExpression;
import static com.google.dart.engine.ast.ASTFactory.integer;
import static com.google.dart.engine.ast.ASTFactory.interpolationExpression;
import static com.google.dart.engine.ast.ASTFactory.interpolationString;
import static com.google.dart.engine.ast.ASTFactory.isExpression;
import static com.google.dart.engine.ast.ASTFactory.listLiteral;
import static com.google.dart.engine.ast.ASTFactory.mapLiteral;
import static com.google.dart.engine.ast.ASTFactory.mapLiteralEntry;
import static com.google.dart.engine.ast.ASTFactory.namedExpression;
import static com.google.dart.engine.ast.ASTFactory.namedFormalParameter;
import static com.google.dart.engine.ast.ASTFactory.nullLiteral;
import static com.google.dart.engine.ast.ASTFactory.parenthesizedExpression;
import static com.google.dart.engine.ast.ASTFactory.positionalFormalParameter;
import static com.google.dart.engine.ast.ASTFactory.postfixExpression;
import static com.google.dart.engine.ast.ASTFactory.prefixExpression;
import static com.google.dart.engine.ast.ASTFactory.propertyAccess;
import static com.google.dart.engine.ast.ASTFactory.simpleFormalParameter;
import static com.google.dart.engine.ast.ASTFactory.string;
import static com.google.dart.engine.ast.ASTFactory.superExpression;
import static com.google.dart.engine.ast.ASTFactory.thisExpression;
import static com.google.dart.engine.ast.ASTFactory.throwExpression;
import static com.google.dart.engine.ast.ASTFactory.typeName;
import static com.google.dart.engine.ast.ASTFactory.variableDeclaration;
import static com.google.dart.engine.ast.ASTFactory.variableDeclarationList;
import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.constructorElement;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class StaticTypeAnalyzerTest extends EngineTestCase {
  /**
   * The error listener to which errors will be reported.
   */
  private GatheringErrorListener listener;

  /**
   * The analyzer being used to analyze the test cases.
   */
  private StaticTypeAnalyzer analyzer;

  /**
   * The type provider used to access the types.
   */
  private TestTypeProvider typeProvider;

  public void fail_visitAssignmentExpression_compound() throws Exception {
    // Fails because the type analyzer doesn't implement method look-up.
    InterfaceType intType = typeProvider.getIntType();
    SimpleIdentifier identifier = resolvedVariable(intType, "i");
    Expression node = assignmentExpression(identifier, TokenType.PLUS_EQ, resolvedInteger(1));
    assertSame(intType, analyze(node));
    listener.assertNoErrors();
  }

  public void fail_visitBinaryExpression_plus() throws Exception {
    // Fails because the type analyzer doesn't implement method look-up.
    // TODO(brianwilkerson) Test other operators when this test starts passing.
    Expression node = binaryExpression(resolvedInteger(2), TokenType.PLUS, resolvedInteger(2));
    assertSame(typeProvider.getNumType(), analyze(node));
    listener.assertNoErrors();
  }

  public void fail_visitFieldFormalParameter_noType() throws Exception {
    // This fails because this visit method is not yet implemented.
    FormalParameter node = fieldFormalParameter(Keyword.VAR, null, "p");
    assertSame(typeProvider.getDynamicType(), analyze(node));
    listener.assertNoErrors();
  }

  public void fail_visitFieldFormalParameter_type() throws Exception {
    // This fails because this visit method is not yet implemented.
    FormalParameter node = fieldFormalParameter(null, typeName("int"), "p");
    assertSame(typeProvider.getIntType(), analyze(node));
    listener.assertNoErrors();
  }

  public void fail_visitFunctionExpressionInvocation() throws Exception {
    fail("Not yet tested");
    listener.assertNoErrors();
  }

  public void fail_visitFunctionTypedFormalParameter() throws Exception {
    fail("Not yet tested");
    listener.assertNoErrors();
  }

  public void fail_visitIndexExpression_getter() throws Exception {
    // Fails because the type analyzer doesn't implement method look-up.
    SimpleIdentifier identifier = resolvedVariable(typeProvider.getListType(), "a");
    Expression node = indexExpression(identifier, resolvedInteger(2));
    assertSame(typeProvider.getIntType(), analyze(node));
    listener.assertNoErrors();
  }

  public void fail_visitIndexExpression_setter() throws Exception {
    // Fails because the type analyzer doesn't implement method look-up.
    SimpleIdentifier identifier = resolvedVariable(typeProvider.getListType(), "a");
    Expression node = indexExpression(identifier, resolvedInteger(2));
    assignmentExpression(node, TokenType.EQ, resolvedInteger(0));
    assertSame(typeProvider.getIntType(), analyze(node));
    listener.assertNoErrors();
  }

  public void fail_visitMethodInvocation() throws Exception {
    fail("Not yet tested");
    listener.assertNoErrors();
  }

  public void fail_visitPrefixedIdentifier() throws Exception {
    fail("Not yet tested");
    listener.assertNoErrors();
  }

  public void fail_visitPrefixExpression_minus() throws Exception {
    PrefixExpression node = prefixExpression(TokenType.MINUS, resolvedInteger(0));
    assertSame(typeProvider.getIntType(), analyze(node));
    listener.assertNoErrors();
  }

  public void fail_visitPrefixExpression_minusMinus() throws Exception {
    PrefixExpression node = prefixExpression(TokenType.MINUS_MINUS, resolvedInteger(0));
    assertSame(typeProvider.getIntType(), analyze(node));
    listener.assertNoErrors();
  }

  public void fail_visitPrefixExpression_plusPlus() throws Exception {
    PrefixExpression node = prefixExpression(TokenType.PLUS_PLUS, resolvedInteger(0));
    assertSame(typeProvider.getIntType(), analyze(node));
    listener.assertNoErrors();
  }

  public void fail_visitPrefixExpression_tilde() throws Exception {
    PrefixExpression node = prefixExpression(TokenType.TILDE, resolvedInteger(0));
    assertSame(typeProvider.getIntType(), analyze(node));
    listener.assertNoErrors();
  }

  public void fail_visitPropertyAccess() throws Exception {
    fail("Not yet tested");
    listener.assertNoErrors();
  }

  public void fail_visitSimpleIdentifier() throws Exception {
    fail("Not yet tested");
    listener.assertNoErrors();
  }

  public void fail_visitTypeName() throws Exception {
    fail("Not yet tested"); // with and without type arguments
    listener.assertNoErrors();
  }

  public void fail_visitVariableDeclaration() throws Exception {
    fail("Not yet tested");
    ClassElement type = classElement("A");
    VariableDeclaration node = variableDeclaration("a");
    variableDeclarationList(null, typeName(type), node);
    //analyze(node);
    assertSame(type.getType(), node.getName().getStaticType());
    listener.assertNoErrors();
  }

  @Override
  public void setUp() {
    listener = new GatheringErrorListener();
    typeProvider = new TestTypeProvider();
    analyzer = createAnalyzer();
  }

  public void test_visitAdjacentStrings() throws Exception {
    // "a" "b"
    Expression node = adjacentStrings(resolvedString("a"), resolvedString("b"));
    assertSame(typeProvider.getStringType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitArgumentDefinitionTest() throws Exception {
    // ?p
    Expression node = argumentDefinitionTest("p");
    assertSame(typeProvider.getBoolType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitAsExpression() throws Exception {
    // class A { ... this as B ... }
    // class B extends A {}
    ClassElement superclass = classElement("A");
    InterfaceType superclassType = superclass.getType();
    ClassElement subclass = classElement("B", superclassType);
    Expression node = asExpression(thisExpression(), typeName(subclass));
    assertSame(subclass.getType(), analyze(node, superclassType));
    listener.assertNoErrors();
  }

  public void test_visitAssignmentExpression_simple() throws Exception {
    // i = 0
    InterfaceType intType = typeProvider.getIntType();
    Expression node = assignmentExpression(
        resolvedVariable(intType, "i"),
        TokenType.EQ,
        resolvedInteger(0));
    assertSame(intType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitBinaryExpression_equals() throws Exception {
    // 2 == 3
    Expression node = binaryExpression(resolvedInteger(2), TokenType.EQ_EQ, resolvedInteger(3));
    assertSame(typeProvider.getBoolType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitBinaryExpression_logicalAnd() throws Exception {
    // false && true
    Expression node = binaryExpression(
        booleanLiteral(false),
        TokenType.AMPERSAND_AMPERSAND,
        booleanLiteral(true));
    assertSame(typeProvider.getBoolType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitBinaryExpression_logicalOr() throws Exception {
    // false || true
    Expression node = binaryExpression(
        booleanLiteral(false),
        TokenType.BAR_BAR,
        booleanLiteral(true));
    assertSame(typeProvider.getBoolType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitBinaryExpression_notEquals() throws Exception {
    // 2 != 3
    Expression node = binaryExpression(resolvedInteger(2), TokenType.BANG_EQ, resolvedInteger(3));
    assertSame(typeProvider.getBoolType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitBooleanLiteral_false() throws Exception {
    // false
    Expression node = booleanLiteral(false);
    assertSame(typeProvider.getBoolType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitBooleanLiteral_true() throws Exception {
    // true
    Expression node = booleanLiteral(true);
    assertSame(typeProvider.getBoolType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitCascadeExpression() throws Exception {
    // a..length
    Expression node = cascadeExpression(resolvedString("a"), propertyAccess(null, "length"));
    assertSame(typeProvider.getStringType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitCatchClause_exception() throws Exception {
    // catch (e)
    CatchClause clause = catchClause("e");
    analyze(clause, typeProvider.getObjectType(), null);
    listener.assertNoErrors();
  }

  public void test_visitCatchClause_exception_stackTrace() throws Exception {
    // catch (e, s)
    CatchClause clause = catchClause("e", "s");
    analyze(clause, typeProvider.getObjectType(), typeProvider.getStackTraceType());
    listener.assertNoErrors();
  }

  public void test_visitCatchClause_on_exception() throws Exception {
    // on E catch (e)
    ClassElement exceptionElement = classElement("E");
    TypeName exceptionType = typeName(exceptionElement);
    CatchClause clause = catchClause(exceptionType, "e");
    analyze(clause, exceptionElement.getType(), null);
    listener.assertNoErrors();
  }

  public void test_visitCatchClause_on_exception_stackTrace() throws Exception {
    // on E catch (e, s)
    ClassElement exceptionElement = classElement("E");
    TypeName exceptionType = typeName(exceptionElement);
    exceptionType.getName().setElement(exceptionElement);
    CatchClause clause = catchClause(exceptionType, "e", "s");
    analyze(clause, exceptionElement.getType(), typeProvider.getStackTraceType());
    listener.assertNoErrors();
  }

  public void test_visitConditionalExpression_invalid() throws Exception {
    // "" ? 1 : 0
    Expression node = conditionalExpression(
        resolvedString(""),
        resolvedInteger(1),
        resolvedInteger(0));
    assertSame(typeProvider.getIntType(), analyze(node));
    listener.assertErrors(ResolverErrorCode.NON_BOOLEAN_CONDITION);
  }

  public void test_visitConditionalExpression_valid() throws Exception {
    // true ? 1 : 0
    Expression node = conditionalExpression(
        booleanLiteral(true),
        resolvedInteger(1),
        resolvedInteger(0));
    assertSame(typeProvider.getIntType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitDoubleLiteral() throws Exception {
    // 4.33
    Expression node = doubleLiteral(4.33);
    assertSame(typeProvider.getDoubleType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitFunctionExpression_named_block() throws Exception {
    // ({p1 : 0, p2 : 0}) {}
    Type dynamicType = typeProvider.getDynamicType();
    FormalParameter p1 = namedFormalParameter(simpleFormalParameter("p1"), resolvedInteger(0));
    setType(p1, dynamicType);
    FormalParameter p2 = namedFormalParameter(simpleFormalParameter("p2"), resolvedInteger(0));
    setType(p2, dynamicType);
    Expression node = functionExpression(formalParameterList(p1, p2), blockFunctionBody());
    analyze(p1);
    analyze(p2);
    Type resultType = analyze(node);
    Map<String, Type> expectedNamedTypes = new HashMap<String, Type>();
    expectedNamedTypes.put("p1", dynamicType);
    expectedNamedTypes.put("p2", dynamicType);
    assertFunctionType(dynamicType, null, null, expectedNamedTypes, resultType);
    listener.assertNoErrors();
  }

  public void test_visitFunctionExpression_named_expression() throws Exception {
    // ({p : 0}) -> 0;
    Type dynamicType = typeProvider.getDynamicType();
    FormalParameter p = namedFormalParameter(simpleFormalParameter("p"), resolvedInteger(0));
    setType(p, dynamicType);
    Expression node = functionExpression(
        formalParameterList(p),
        expressionFunctionBody(resolvedInteger(0)));
    analyze(p);
    Type resultType = analyze(node);
    Map<String, Type> expectedNamedTypes = new HashMap<String, Type>();
    expectedNamedTypes.put("p", dynamicType);
    assertFunctionType(typeProvider.getIntType(), null, null, expectedNamedTypes, resultType);
    listener.assertNoErrors();
  }

  public void test_visitFunctionExpression_normal_block() throws Exception {
    // (p1, p2) {}
    Type dynamicType = typeProvider.getDynamicType();
    FormalParameter p1 = simpleFormalParameter("p1");
    setType(p1, dynamicType);
    FormalParameter p2 = simpleFormalParameter("p2");
    setType(p2, dynamicType);
    Expression node = functionExpression(formalParameterList(p1, p2), blockFunctionBody());
    analyze(p1);
    analyze(p2);
    Type resultType = analyze(node);
    assertFunctionType(dynamicType, new Type[] {dynamicType, dynamicType}, null, null, resultType);
    listener.assertNoErrors();
  }

  public void test_visitFunctionExpression_normal_expression() throws Exception {
    // (p1, p2) -> 0
    Type dynamicType = typeProvider.getDynamicType();
    FormalParameter p = simpleFormalParameter("p");
    setType(p, dynamicType);
    Expression node = functionExpression(
        formalParameterList(p),
        expressionFunctionBody(resolvedInteger(0)));
    analyze(p);
    Type resultType = analyze(node);
    assertFunctionType(typeProvider.getIntType(), new Type[] {dynamicType}, null, null, resultType);
    listener.assertNoErrors();
  }

  public void test_visitFunctionExpression_normalAndNamed_block() throws Exception {
    // (p1, {p2 : 0}) {}
    Type dynamicType = typeProvider.getDynamicType();
    FormalParameter p1 = simpleFormalParameter("p1");
    setType(p1, dynamicType);
    FormalParameter p2 = namedFormalParameter(simpleFormalParameter("p2"), resolvedInteger(0));
    setType(p2, dynamicType);
    Expression node = functionExpression(formalParameterList(p1, p2), blockFunctionBody());
    analyze(p2);
    Type resultType = analyze(node);
    Map<String, Type> expectedNamedTypes = new HashMap<String, Type>();
    expectedNamedTypes.put("p2", dynamicType);
    assertFunctionType(dynamicType, new Type[] {dynamicType}, null, expectedNamedTypes, resultType);
    listener.assertNoErrors();
  }

  public void test_visitFunctionExpression_normalAndNamed_expression() throws Exception {
    // (p1, {p2 : 0}) -> 0
    Type dynamicType = typeProvider.getDynamicType();
    FormalParameter p1 = simpleFormalParameter("p1");
    setType(p1, dynamicType);
    FormalParameter p2 = namedFormalParameter(simpleFormalParameter("p2"), resolvedInteger(0));
    setType(p2, dynamicType);
    Expression node = functionExpression(
        formalParameterList(p1, p2),
        expressionFunctionBody(resolvedInteger(0)));
    analyze(p2);
    Type resultType = analyze(node);
    Map<String, Type> expectedNamedTypes = new HashMap<String, Type>();
    expectedNamedTypes.put("p2", dynamicType);
    assertFunctionType(
        typeProvider.getIntType(),
        new Type[] {dynamicType},
        null,
        expectedNamedTypes,
        resultType);
    listener.assertNoErrors();
  }

  public void test_visitFunctionExpression_normalAndPositional_block() throws Exception {
    // (p1, [p2 = 0]) {}
    Type dynamicType = typeProvider.getDynamicType();
    FormalParameter p1 = simpleFormalParameter("p1");
    setType(p1, dynamicType);
    FormalParameter p2 = positionalFormalParameter(simpleFormalParameter("p2"), resolvedInteger(0));
    setType(p2, dynamicType);
    Expression node = functionExpression(formalParameterList(p1, p2), blockFunctionBody());
    analyze(p1);
    analyze(p2);
    Type resultType = analyze(node);
    assertFunctionType(
        dynamicType,
        new Type[] {dynamicType},
        new Type[] {dynamicType},
        null,
        resultType);
    listener.assertNoErrors();
  }

  public void test_visitFunctionExpression_normalAndPositional_expression() throws Exception {
    // (p1, [p2 = 0]) -> 0
    Type dynamicType = typeProvider.getDynamicType();
    FormalParameter p1 = simpleFormalParameter("p1");
    setType(p1, dynamicType);
    FormalParameter p2 = positionalFormalParameter(simpleFormalParameter("p2"), resolvedInteger(0));
    setType(p2, dynamicType);
    Expression node = functionExpression(
        formalParameterList(p1, p2),
        expressionFunctionBody(resolvedInteger(0)));
    analyze(p1);
    analyze(p2);
    Type resultType = analyze(node);
    assertFunctionType(
        typeProvider.getIntType(),
        new Type[] {dynamicType},
        new Type[] {dynamicType},
        null,
        resultType);
    listener.assertNoErrors();
  }

  public void test_visitFunctionExpression_positional_block() throws Exception {
    // ([p1 = 0, p2 = 0]) {}
    Type dynamicType = typeProvider.getDynamicType();
    FormalParameter p1 = positionalFormalParameter(simpleFormalParameter("p1"), resolvedInteger(0));
    setType(p1, dynamicType);
    FormalParameter p2 = positionalFormalParameter(simpleFormalParameter("p2"), resolvedInteger(0));
    setType(p2, dynamicType);
    Expression node = functionExpression(formalParameterList(p1, p2), blockFunctionBody());
    analyze(p1);
    analyze(p2);
    Type resultType = analyze(node);
    assertFunctionType(dynamicType, null, new Type[] {dynamicType, dynamicType}, null, resultType);
    listener.assertNoErrors();
  }

  public void test_visitFunctionExpression_positional_expression() throws Exception {
    // ([p1 = 0, p2 = 0]) -> 0
    Type dynamicType = typeProvider.getDynamicType();
    FormalParameter p = positionalFormalParameter(simpleFormalParameter("p"), resolvedInteger(0));
    setType(p, dynamicType);
    Expression node = functionExpression(
        formalParameterList(p),
        expressionFunctionBody(resolvedInteger(0)));
    analyze(p);
    Type resultType = analyze(node);
    assertFunctionType(typeProvider.getIntType(), null, new Type[] {dynamicType}, null, resultType);
    listener.assertNoErrors();
  }

  public void test_visitInstanceCreationExpression_named() throws Exception {
    // new C.m()
    ClassElement type = classElement("C");
    String constructorName = "m";
    ConstructorElementImpl constructor = (ConstructorElementImpl) constructorElement(constructorName);
    FunctionTypeImpl constructorType = new FunctionTypeImpl(constructor);
    constructorType.setReturnType(type.getType());
    constructor.setType(constructorType);
    InstanceCreationExpression node = instanceCreationExpression(
        null,
        typeName(type),
        identifier(constructorName));
    node.setElement(constructor);
    assertSame(type.getType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitInstanceCreationExpression_unnamed() throws Exception {
    // new C()
    ClassElement type = classElement("C");
    ConstructorElementImpl constructor = (ConstructorElementImpl) constructorElement(null);
    FunctionTypeImpl constructorType = new FunctionTypeImpl(constructor);
    constructorType.setReturnType(type.getType());
    constructor.setType(constructorType);
    InstanceCreationExpression node = instanceCreationExpression(null, typeName(type));
    node.setElement(constructor);
    assertSame(type.getType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitIntegerLiteral() throws Exception {
    // 42
    Expression node = resolvedInteger(42);
    assertSame(typeProvider.getIntType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitIsExpression_negated() throws Exception {
    // a is! String
    Expression node = isExpression(resolvedString("a"), true, typeName("String"));
    assertSame(typeProvider.getBoolType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitIsExpression_notNegated() throws Exception {
    // a is String
    Expression node = isExpression(resolvedString("a"), false, typeName("String"));
    assertSame(typeProvider.getBoolType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitListLiteral_empty() throws Exception {
    // []
    Expression node = listLiteral();
    Type resultType = analyze(node);
    assertType(
        typeProvider.getListType().substitute(new Type[] {typeProvider.getDynamicType()}),
        resultType);
    listener.assertNoErrors();
  }

  public void test_visitListLiteral_nonEmpty() throws Exception {
    // [0]
    Expression node = listLiteral(resolvedInteger(0));
    Type resultType = analyze(node);
    assertType(
        typeProvider.getListType().substitute(new Type[] {typeProvider.getDynamicType()}),
        resultType);
    listener.assertNoErrors();
  }

  public void test_visitMapLiteral_empty() throws Exception {
    // {}
    Expression node = mapLiteral();
    Type resultType = analyze(node);
    assertType(
        typeProvider.getMapType().substitute(
            new Type[] {typeProvider.getStringType(), typeProvider.getDynamicType()}),
        resultType);
    listener.assertNoErrors();
  }

  public void test_visitMapLiteral_nonEmpty() throws Exception {
    // {"k" : 0}
    Expression node = mapLiteral(mapLiteralEntry("k", resolvedInteger(0)));
    Type resultType = analyze(node);
    assertType(
        typeProvider.getMapType().substitute(
            new Type[] {typeProvider.getStringType(), typeProvider.getDynamicType()}),
        resultType);
    listener.assertNoErrors();
  }

  public void test_visitNamedExpression() throws Exception {
    // n: a
    Expression node = namedExpression("n", resolvedString("a"));
    assertSame(typeProvider.getStringType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitNullLiteral() throws Exception {
    // null
    Expression node = nullLiteral();
    assertSame(typeProvider.getBottomType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitParenthesizedExpression() throws Exception {
    // (0)
    Expression node = parenthesizedExpression(resolvedInteger(0));
    assertSame(typeProvider.getIntType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitPostfixExpression_minusMinus() throws Exception {
    // 0--
    PostfixExpression node = postfixExpression(resolvedInteger(0), TokenType.MINUS_MINUS);
    assertSame(typeProvider.getIntType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitPostfixExpression_plusPlus() throws Exception {
    // 0++
    PostfixExpression node = postfixExpression(resolvedInteger(0), TokenType.PLUS_PLUS);
    assertSame(typeProvider.getIntType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitPrefixExpression_bang() throws Exception {
    // !0
    PrefixExpression node = prefixExpression(TokenType.BANG, resolvedInteger(0));
    assertSame(typeProvider.getBoolType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitPrefixExpression_not() throws Exception {
    // !true
    Expression node = prefixExpression(TokenType.BANG, booleanLiteral(true));
    assertSame(typeProvider.getBoolType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitSimpleFormalParameter_noType() throws Exception {
    // p
    FormalParameter node = simpleFormalParameter("p");
    node.getIdentifier().setElement(new ParameterElementImpl(identifier("p")));
    assertSame(typeProvider.getDynamicType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitSimpleFormalParameter_type() throws Exception {
    // int p
    InterfaceType intType = typeProvider.getIntType();
    FormalParameter node = simpleFormalParameter(typeName(intType.getElement()), "p");
    SimpleIdentifier identifier = node.getIdentifier();
    ParameterElementImpl element = new ParameterElementImpl(identifier);
    identifier.setElement(element);
    assertSame(intType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitSimpleStringLiteral() throws Exception {
    // "a"
    Expression node = resolvedString("a");
    assertSame(typeProvider.getStringType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitStringInterpolation() throws Exception {
    // "a${'b'}c"
    Expression node = string(
        interpolationString("a", "a"),
        interpolationExpression(resolvedString("b")),
        interpolationString("c", "c"));
    assertSame(typeProvider.getStringType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitSuperExpression() throws Exception {
    // super
    InterfaceType superType = classElement("A").getType();
    InterfaceType thisType = classElement("B", superType).getType();
    Expression node = superExpression();
    assertSame(superType, analyze(node, thisType));
    listener.assertNoErrors();
  }

  public void test_visitThisExpression() throws Exception {
    // this
    InterfaceType thisType = classElement("B", classElement("A").getType()).getType();
    Expression node = thisExpression();
    assertSame(thisType, analyze(node, thisType));
    listener.assertNoErrors();
  }

  public void test_visitThrowExpression_withoutValue() throws Exception {
    // throw
    Expression node = throwExpression();
    assertSame(typeProvider.getBottomType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitThrowExpression_withValue() throws Exception {
    // throw 0
    Expression node = throwExpression(resolvedInteger(0));
    assertSame(typeProvider.getBottomType(), analyze(node));
    listener.assertNoErrors();
  }

  /**
   * Analyze the given catch clause and assert that the types of the parameters have been set to the
   * given types. The types can be null if the catch clause does not have the corresponding
   * parameter.
   * 
   * @param node the catch clause to be analyzed
   * @param exceptionType the expected type of the exception parameter
   * @param stackTraceType the expected type of the stack trace parameter
   */
  private void analyze(CatchClause node, InterfaceType exceptionType, InterfaceType stackTraceType) {
    node.accept(analyzer);
    SimpleIdentifier exceptionParameter = node.getExceptionParameter();
    if (exceptionParameter != null) {
      assertType(exceptionType, exceptionParameter.getStaticType());
    }
    SimpleIdentifier stackTraceParameter = node.getStackTraceParameter();
    if (stackTraceParameter != null) {
      assertType(stackTraceType, stackTraceParameter.getStaticType());
    }
  }

  /**
   * Return the type associated with the given expression after the static type analyzer has
   * computed a type for it.
   * 
   * @param node the expression with which the type is associated
   * @return the type associated with the expression
   */
  private Type analyze(Expression node) {
    return analyze(node, null);
  }

  /**
   * Return the type associated with the given expression after the static type analyzer has
   * computed a type for it.
   * 
   * @param node the expression with which the type is associated
   * @param thisType the type of 'this'
   * @return the type associated with the expression
   */
  private Type analyze(Expression node, InterfaceType thisType) {
    Field typeField = null;
    try {
      typeField = analyzer.getClass().getDeclaredField("thisType");
      typeField.setAccessible(true);
      typeField.set(analyzer, thisType);
    } catch (Exception exception) {
      throw new IllegalArgumentException("Could not set type of 'this'", exception);
    }
    node.accept(analyzer);
    return node.getStaticType();
  }

  /**
   * Return the type associated with the given parameter after the static type analyzer has computed
   * a type for it.
   * 
   * @param node the parameter with which the type is associated
   * @return the type associated with the parameter
   */
  private Type analyze(FormalParameter node) {
    node.accept(analyzer);
    return ((ParameterElement) node.getIdentifier().getElement()).getType();
  }

  /**
   * Assert that the actual type is a function type with the expected characteristics.
   * 
   * @param expectedReturnType the expected return type of the function
   * @param expectedNormalTypes the expected types of the normal parameters
   * @param expectedOptionalTypes the expected types of the optional parameters
   * @param expectedNamedTypes the expected types of the named parameters
   * @param actualType the type being tested
   */
  private void assertFunctionType(Type expectedReturnType, Type[] expectedNormalTypes,
      Type[] expectedOptionalTypes, Map<String, Type> expectedNamedTypes, Type actualType) {
    assertInstanceOf(FunctionType.class, actualType);
    FunctionType functionType = (FunctionType) actualType;

    Type[] normalTypes = functionType.getNormalParameterTypes();
    if (expectedNormalTypes == null) {
      assertLength(0, normalTypes);
    } else {
      int expectedCount = expectedNormalTypes.length;
      assertLength(expectedCount, normalTypes);
      for (int i = 0; i < expectedCount; i++) {
        assertSame(expectedNormalTypes[i], normalTypes[i]);
      }
    }

    Type[] optionalTypes = functionType.getOptionalParameterTypes();
    if (expectedOptionalTypes == null) {
      assertLength(0, optionalTypes);
    } else {
      int expectedCount = expectedOptionalTypes.length;
      assertLength(expectedCount, optionalTypes);
      for (int i = 0; i < expectedCount; i++) {
        assertSame(expectedOptionalTypes[i], optionalTypes[i]);
      }
    }

    Map<String, Type> namedTypes = functionType.getNamedParameterTypes();
    if (expectedNamedTypes == null) {
      assertSize(0, namedTypes);
    } else {
      assertSize(expectedNamedTypes.size(), namedTypes);
      for (Map.Entry<String, Type> entry : expectedNamedTypes.entrySet()) {
        assertSame(entry.getValue(), namedTypes.get(entry.getKey()));
      }
    }

    assertSame(expectedReturnType, functionType.getReturnType());
  }

  private void assertType(InterfaceTypeImpl expectedType, InterfaceTypeImpl actualType) {
    assertEquals(expectedType.getName(), actualType.getName());
    assertEquals(expectedType.getElement(), actualType.getElement());
    Type[] expectedArguments = expectedType.getTypeArguments();
    int length = expectedArguments.length;
    Type[] actualArguments = actualType.getTypeArguments();
    assertLength(length, actualArguments);
    for (int i = 0; i < length; i++) {
      assertType(expectedArguments[i], actualArguments[i]);
    }
  }

  private void assertType(Type expectedType, Type actualType) {
    if (expectedType instanceof InterfaceTypeImpl) {
      assertInstanceOf(InterfaceTypeImpl.class, actualType);
      assertType((InterfaceTypeImpl) expectedType, (InterfaceTypeImpl) actualType);
    }
    // TODO(brianwilkerson) Compare other kinds of types then make this a shared utility method
  }

  /**
   * Create the analyzer used by the tests.
   * 
   * @return the analyzer to be used by the tests
   */
  private StaticTypeAnalyzer createAnalyzer() {
    AnalysisContextImpl context = new AnalysisContextImpl();
    context.setSourceFactory(new SourceFactory(new DartUriResolver(DartSdk.getDefaultSdk())));
    CompilationUnitElementImpl definingCompilationUnit = new CompilationUnitElementImpl("lib.dart");
    LibraryElementImpl definingLibrary = new LibraryElementImpl(context, null);
    definingLibrary.setDefiningCompilationUnit(definingCompilationUnit);
    Library library = new Library(context, listener, null);
    library.setLibraryElement(definingLibrary);
    ResolverVisitor visitor = new ResolverVisitor(library, null, typeProvider);
    try {
      Field analyzerField = visitor.getClass().getDeclaredField("typeAnalyzer");
      analyzerField.setAccessible(true);
      return (StaticTypeAnalyzer) analyzerField.get(visitor);
    } catch (Exception exception) {
      throw new IllegalArgumentException("Could not create analyzer", exception);
    }
  }

  /**
   * Return an integer literal that has been resolved to the correct type.
   * 
   * @param value the value of the literal
   * @return an integer literal that has been resolved to the correct type
   */
  private IntegerLiteral resolvedInteger(int value) {
    IntegerLiteral literal = integer(value);
    literal.setStaticType(typeProvider.getIntType());
    return literal;
  }

  /**
   * Return a string literal that has been resolved to the correct type.
   * 
   * @param value the value of the literal
   * @return a string literal that has been resolved to the correct type
   */
  private SimpleStringLiteral resolvedString(String value) {
    SimpleStringLiteral string = string(value);
    string.setStaticType(typeProvider.getStringType());
    return string;
  }

  /**
   * Return a simple identifier that has been resolved to a variable element with the given type.
   * 
   * @param type the type of the variable being represented
   * @param variableName the name of the variable
   * @return a simple identifier that has been resolved to a variable element with the given type
   */
  private SimpleIdentifier resolvedVariable(InterfaceType type, String variableName) {
    SimpleIdentifier identifier = identifier(variableName);
    VariableElementImpl element = new VariableElementImpl(identifier);
    element.setType(type);
    identifier.setElement(element);
    identifier.setStaticType(type);
    return identifier;
  }

  /**
   * Set the type of the given parameter to the given type.
   * 
   * @param parameter the parameter whose type is to be set
   * @param type the new type of the given parameter
   */
  private void setType(FormalParameter parameter, Type type) {
    SimpleIdentifier identifier = parameter.getIdentifier();
    Element element = identifier.getElement();
    if (!(element instanceof ParameterElement)) {
      element = new ParameterElementImpl(identifier);
      identifier.setElement(element);
    }
    ((ParameterElementImpl) element).setType(type);
  }
}
