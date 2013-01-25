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
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.NormalFormalParameter;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.ParameterElementImpl;
import com.google.dart.engine.internal.element.VariableElementImpl;
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
import static com.google.dart.engine.ast.ASTFactory.conditionalExpression;
import static com.google.dart.engine.ast.ASTFactory.doubleLiteral;
import static com.google.dart.engine.ast.ASTFactory.expressionFunctionBody;
import static com.google.dart.engine.ast.ASTFactory.fieldFormalParameter;
import static com.google.dart.engine.ast.ASTFactory.formalParameterList;
import static com.google.dart.engine.ast.ASTFactory.functionExpression;
import static com.google.dart.engine.ast.ASTFactory.identifier;
import static com.google.dart.engine.ast.ASTFactory.indexExpression;
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
import static com.google.dart.engine.ast.ASTFactory.prefixExpression;
import static com.google.dart.engine.ast.ASTFactory.propertyAccess;
import static com.google.dart.engine.ast.ASTFactory.simpleFormalParameter;
import static com.google.dart.engine.ast.ASTFactory.string;
import static com.google.dart.engine.ast.ASTFactory.superExpression;
import static com.google.dart.engine.ast.ASTFactory.thisExpression;
import static com.google.dart.engine.ast.ASTFactory.throwExpression;
import static com.google.dart.engine.ast.ASTFactory.typeName;
import static com.google.dart.engine.element.ElementFactory.classElement;

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

  public void fail_visitAsExpression() throws Exception {
    // This fails because we can't compute the type of 'A'.
    InterfaceType thisType = classElement("B", classElement("A").getType()).getType();
    Expression node = asExpression(thisExpression(), typeName("A"));
    assertEquals(((ClassElementImpl) thisType.getElement()).getSupertype(), analyze(node, thisType));
    listener.assertNoErrors();
  }

  public void fail_visitAssignmentExpression_compound() throws Exception {
    // Fails because we can't compute the type of 'i'. The test needs to resolve 'i' to a
    // VarableElement whose type is 'int'.
    Type intType = typeProvider.getIntType();
    SimpleIdentifier identifier = identifier("i");
    VariableElementImpl element = new VariableElementImpl(identifier);
    element.setType(intType);
    identifier.setElement(element);
    Expression node = assignmentExpression(identifier, TokenType.PLUS_EQ, integer(1));
    assertEquals(intType, analyze(node));
    listener.assertNoErrors();
  }

  public void fail_visitBinaryExpression_plus() throws Exception {
    // Fails because the type analyzer doesn't implement method look-up.
    // TODO(brianwilkerson) Test other operators when this test starts passing.
    Type numType = typeProvider.getNumType();
    Expression node = binaryExpression(integer(2), TokenType.PLUS, integer(2));
    assertEquals(numType, analyze(node));
    listener.assertNoErrors();
  }

  public void fail_visitDefaultFormalParameter_simple_type() throws Exception {
    // This fails because we can't compute the type of 'int'.
    Type intType = typeProvider.getIntType();
    FormalParameter node = namedFormalParameter(
        simpleFormalParameter(typeName("int"), "p"),
        integer(0));
    assertEquals(intType, analyze(node));
    listener.assertNoErrors();
  }

  public void fail_visitFieldFormalParameter_noType() throws Exception {
    // This fails because this visit method is not yet implemented.
    Type dynamicType = typeProvider.getDynamicType();
    FormalParameter node = fieldFormalParameter(Keyword.VAR, null, "p");
    assertEquals(dynamicType, analyze(node));
    listener.assertNoErrors();
  }

  public void fail_visitFieldFormalParameter_type() throws Exception {
    // This fails because this visit method is not yet implemented.
    Type intType = typeProvider.getIntType();
    FormalParameter node = fieldFormalParameter(null, typeName("int"), "p");
    assertEquals(intType, analyze(node));
    listener.assertNoErrors();
  }

  public void fail_visitIndexExpression() throws Exception {
    // Fails because the identifier is not resolved and hence it's type cannot be discovered.
    Type intType = typeProvider.getIntType();
    Expression node = indexExpression(identifier("a"), integer(2));
    assertEquals(intType, analyze(node));
    listener.assertNoErrors();
  }

  public void fail_visitSimpleFormalParameter_type() throws Exception {
    // This fails because we can't compute the type of 'int'.
    Type intType = typeProvider.getIntType();
    FormalParameter node = simpleFormalParameter(typeName("int"), "p");
    assertEquals(intType, analyze(node));
    listener.assertNoErrors();
  }

  @Override
  public void setUp() {
    listener = new GatheringErrorListener();
    typeProvider = new TestTypeProvider();
    analyzer = createAnalyzer();
  }

  public void test_visitAdjacentStrings() throws Exception {
    Type stringType = typeProvider.getStringType();
    Expression node = adjacentStrings(string("a"), string("b"));
    assertEquals(stringType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitArgumentDefinitionTest() throws Exception {
    Type boolType = typeProvider.getBoolType();
    Expression node = argumentDefinitionTest("p");
    assertEquals(boolType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitAssignmentExpression_simple() throws Exception {
    Type intType = typeProvider.getIntType();
    Expression node = assignmentExpression(identifier("i"), TokenType.EQ, integer(0));
    assertEquals(intType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitBinaryExpression_equals() throws Exception {
    Type boolType = typeProvider.getBoolType();
    Expression node = binaryExpression(integer(2), TokenType.EQ_EQ, integer(3));
    assertEquals(boolType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitBinaryExpression_logicalAnd() throws Exception {
    Type boolType = typeProvider.getBoolType();
    Expression node = binaryExpression(
        booleanLiteral(false),
        TokenType.AMPERSAND_AMPERSAND,
        booleanLiteral(true));
    assertEquals(boolType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitBinaryExpression_logicalOr() throws Exception {
    Type boolType = typeProvider.getBoolType();
    Expression node = binaryExpression(
        booleanLiteral(false),
        TokenType.BAR_BAR,
        booleanLiteral(true));
    assertEquals(boolType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitBinaryExpression_notEquals() throws Exception {
    Type boolType = typeProvider.getBoolType();
    Expression node = binaryExpression(integer(2), TokenType.BANG_EQ, integer(3));
    assertEquals(boolType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitBooleanLiteral_false() throws Exception {
    Type boolType = typeProvider.getBoolType();
    Expression node = booleanLiteral(false);
    assertEquals(boolType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitBooleanLiteral_true() throws Exception {
    Type boolType = typeProvider.getBoolType();
    Expression node = booleanLiteral(true);
    assertEquals(boolType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitCascadeExpression() throws Exception {
    Type stringType = typeProvider.getStringType();
    Expression node = cascadeExpression(string("a"), propertyAccess(null, "length"));
    assertEquals(stringType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitConditionalExpression_invalid() throws Exception {
    Type intType = typeProvider.getIntType();
    Expression node = conditionalExpression(string(""), integer(1), integer(0));
    assertEquals(intType, analyze(node));
    listener.assertErrors(ResolverErrorCode.NON_BOOLEAN_CONDITION);
  }

  public void test_visitConditionalExpression_valid() throws Exception {
    Type intType = typeProvider.getIntType();
    Expression node = conditionalExpression(booleanLiteral(true), integer(1), integer(0));
    assertEquals(intType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitDefaultFormalParameter_simple_noType() throws Exception {
    Type dynamicType = typeProvider.getDynamicType();
    Type intType = typeProvider.getIntType();
    NormalFormalParameter parameter = simpleFormalParameter("p");
    setType(parameter, intType);
    FormalParameter node = namedFormalParameter(parameter, integer(0));
    assertEquals(dynamicType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitDoubleLiteral() throws Exception {
    Type doubleType = typeProvider.getDoubleType();
    Expression node = doubleLiteral(4.33);
    assertEquals(doubleType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitFunctionExpression_named_block() throws Exception {
    Type dynamicType = typeProvider.getDynamicType();
    FormalParameter p1 = namedFormalParameter(simpleFormalParameter("p1"), integer(0));
    setType(p1, dynamicType);
    FormalParameter p2 = namedFormalParameter(simpleFormalParameter("p2"), integer(0));
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
    Type dynamicType = typeProvider.getDynamicType();
    Type intType = typeProvider.getIntType();
    FormalParameter p = namedFormalParameter(simpleFormalParameter("p"), integer(0));
    setType(p, dynamicType);
    Expression node = functionExpression(formalParameterList(p), expressionFunctionBody(integer(0)));
    analyze(p);
    Type resultType = analyze(node);
    Map<String, Type> expectedNamedTypes = new HashMap<String, Type>();
    expectedNamedTypes.put("p", dynamicType);
    assertFunctionType(intType, null, null, expectedNamedTypes, resultType);
    listener.assertNoErrors();
  }

  public void test_visitFunctionExpression_normal_block() throws Exception {
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
    // TODO(brianwilkerson) Also test _normalAndPositional_block,
    // _normalAndPositional_expression, _normalAndNamed_block, _normalAndNamed_expression.
    Type dynamicType = typeProvider.getDynamicType();
    Type intType = typeProvider.getIntType();
    FormalParameter p = simpleFormalParameter("p");
    setType(p, dynamicType);
    Expression node = functionExpression(formalParameterList(p), expressionFunctionBody(integer(0)));
    analyze(p);
    Type resultType = analyze(node);
    assertFunctionType(intType, new Type[] {dynamicType}, null, null, resultType);
    listener.assertNoErrors();
  }

  public void test_visitFunctionExpression_positional_block() throws Exception {
    Type dynamicType = typeProvider.getDynamicType();
    FormalParameter p1 = positionalFormalParameter(simpleFormalParameter("p1"), integer(0));
    setType(p1, dynamicType);
    FormalParameter p2 = positionalFormalParameter(simpleFormalParameter("p2"), integer(0));
    setType(p2, dynamicType);
    Expression node = functionExpression(formalParameterList(p1, p2), blockFunctionBody());
    analyze(p1);
    analyze(p2);
    Type resultType = analyze(node);
    assertFunctionType(dynamicType, null, new Type[] {dynamicType, dynamicType}, null, resultType);
    listener.assertNoErrors();
  }

  public void test_visitFunctionExpression_positional_expression() throws Exception {
    Type dynamicType = typeProvider.getDynamicType();
    Type intType = typeProvider.getIntType();
    FormalParameter p = positionalFormalParameter(simpleFormalParameter("p"), integer(0));
    setType(p, dynamicType);
    Expression node = functionExpression(formalParameterList(p), expressionFunctionBody(integer(0)));
    analyze(p);
    Type resultType = analyze(node);
    assertFunctionType(intType, null, new Type[] {dynamicType}, null, resultType);
    listener.assertNoErrors();
  }

  public void test_visitFunctionExpressionInvocation() throws Exception {
    // TODO(brianwilkerson) Test this
  }

  public void test_visitFunctionTypedFormalParameter() throws Exception {
    // TODO(brianwilkerson) Test this
  }

  public void test_visitInstanceCreationExpression() throws Exception {
    // TODO(brianwilkerson) Test this
  }

  public void test_visitIntegerLiteral() throws Exception {
    Type intType = typeProvider.getIntType();
    Expression node = integer(42);
    assertEquals(intType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitIsExpression_negated() throws Exception {
    Type boolType = typeProvider.getBoolType();
    Expression node = isExpression(string("a"), true, typeName("String"));
    assertEquals(boolType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitIsExpression_notNegated() throws Exception {
    Type boolType = typeProvider.getBoolType();
    Expression node = isExpression(string("a"), false, typeName("String"));
    assertEquals(boolType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitListLiteral_empty() throws Exception {
    Type dynamicType = typeProvider.getDynamicType();
    Type listType = typeProvider.getListType();
    Expression node = listLiteral();
    Type resultType = analyze(node);
    assertType(((InterfaceTypeImpl) listType).substitute(new Type[] {dynamicType}), resultType);
    listener.assertNoErrors();
  }

  public void test_visitListLiteral_nonEmpty() throws Exception {
    Type dynamicType = typeProvider.getDynamicType();
    Type listType = typeProvider.getListType();
    Expression node = listLiteral(nullLiteral());
    Type resultType = analyze(node);
    assertType(((InterfaceTypeImpl) listType).substitute(new Type[] {dynamicType}), resultType);
    listener.assertNoErrors();
  }

  public void test_visitMapLiteral_empty() throws Exception {
    Type dynamicType = typeProvider.getDynamicType();
    Type mapType = typeProvider.getMapType();
    Type stringType = typeProvider.getStringType();
    Expression node = mapLiteral();
    Type resultType = analyze(node);
    assertType(
        ((InterfaceTypeImpl) mapType).substitute(new Type[] {stringType, dynamicType}),
        resultType);
    listener.assertNoErrors();
  }

  public void test_visitMapLiteral_nonEmpty() throws Exception {
    Type dynamicType = typeProvider.getDynamicType();
    Type mapType = typeProvider.getMapType();
    Type stringType = typeProvider.getStringType();
    Expression node = mapLiteral(mapLiteralEntry("k", nullLiteral()));
    Type resultType = analyze(node);
    assertType(
        ((InterfaceTypeImpl) mapType).substitute(new Type[] {stringType, dynamicType}),
        resultType);
    listener.assertNoErrors();
  }

  public void test_visitMethodInvocation() throws Exception {
    // TODO(brianwilkerson) Test this
  }

  public void test_visitNamedExpression() throws Exception {
    Type stringType = typeProvider.getStringType();
    Expression node = namedExpression("n", string("a"));
    assertEquals(stringType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitNullLiteral() throws Exception {
    Type bottomType = typeProvider.getBottomType();
    Expression node = nullLiteral();
    assertEquals(bottomType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitParenthesizedExpression() throws Exception {
    Type bottomType = typeProvider.getBottomType();
    Expression node = parenthesizedExpression(nullLiteral());
    assertEquals(bottomType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitPostfixExpression() throws Exception {
    // TODO(brianwilkerson) Test multiple operators
  }

  public void test_visitPrefixedIdentifier() throws Exception {
    // TODO(brianwilkerson) Test this
  }

  public void test_visitPrefixExpression() throws Exception {
    // TODO(brianwilkerson) Test multiple operators
  }

  public void test_visitPrefixExpression_not() throws Exception {
    Type boolType = typeProvider.getBoolType();
    Expression node = prefixExpression(TokenType.BANG, booleanLiteral(true));
    assertEquals(boolType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitPropertyAccess() throws Exception {
    // TODO(brianwilkerson) Test this
  }

  public void test_visitSimpleFormalParameter_noType() throws Exception {
    Type dynamicType = typeProvider.getDynamicType();
    FormalParameter node = simpleFormalParameter("p");
    node.getIdentifier().setElement(new ParameterElementImpl(identifier("p")));
    assertEquals(dynamicType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitSimpleIdentifier() throws Exception {
    // TODO(brianwilkerson) Test this
  }

  public void test_visitSimpleStringLiteral() throws Exception {
    Type stringType = typeProvider.getStringType();
    Expression node = string("a");
    assertEquals(stringType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitStringInterpolation() throws Exception {
    Type stringType = typeProvider.getStringType();
    Expression node = string(
        interpolationString("a", "a"),
        interpolationExpression(string("b")),
        interpolationString("c", "c"));
    assertEquals(stringType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitSuperExpression() throws Exception {
    InterfaceType thisType = classElement("B", classElement("A").getType()).getType();
    Expression node = superExpression();
    assertEquals(((ClassElementImpl) thisType.getElement()).getSupertype(), analyze(node, thisType));
    listener.assertNoErrors();
  }

  public void test_visitThisExpression() throws Exception {
    InterfaceType thisType = classElement("B", classElement("A").getType()).getType();
    Expression node = thisExpression();
    assertEquals(thisType, analyze(node, thisType));
    listener.assertNoErrors();
  }

  public void test_visitThrowExpression_withoutValue() throws Exception {
    Type bottomType = typeProvider.getBottomType();
    Expression node = throwExpression();
    assertEquals(bottomType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitThrowExpression_withValue() throws Exception {
    Type bottomType = typeProvider.getBottomType();
    Expression node = throwExpression(nullLiteral());
    assertEquals(bottomType, analyze(node));
    listener.assertNoErrors();
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
    node.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      public Void visitNode(ASTNode node) {
        node.visitChildren(this);
        node.accept(analyzer);
        return null;
      }
    });
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
    node.accept(new GeneralizingASTVisitor<Void>() {
      @Override
      public Void visitNode(ASTNode node) {
        node.visitChildren(this);
        node.accept(analyzer);
        return null;
      }
    });
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
        assertEquals(expectedNormalTypes[i], normalTypes[i]);
      }
    }

    Type[] optionalTypes = functionType.getOptionalParameterTypes();
    if (expectedOptionalTypes == null) {
      assertLength(0, optionalTypes);
    } else {
      int expectedCount = expectedOptionalTypes.length;
      assertLength(expectedCount, optionalTypes);
      for (int i = 0; i < expectedCount; i++) {
        assertEquals(expectedOptionalTypes[i], optionalTypes[i]);
      }
    }

    Map<String, Type> namedTypes = functionType.getNamedParameterTypes();
    if (expectedNamedTypes == null) {
      assertSize(0, namedTypes);
    } else {
      assertSize(expectedNamedTypes.size(), namedTypes);
      for (Map.Entry<String, Type> entry : expectedNamedTypes.entrySet()) {
        assertEquals(entry.getValue(), namedTypes.get(entry.getKey()));
      }
    }

    assertEquals(expectedReturnType, functionType.getReturnType());
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
    // TODO(brianwilkerson) Compare other kinds of types
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
