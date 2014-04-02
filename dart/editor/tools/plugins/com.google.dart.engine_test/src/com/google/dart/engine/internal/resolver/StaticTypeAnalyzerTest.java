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
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.DoubleLiteral;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FormalParameter;
import com.google.dart.engine.ast.FormalParameterList;
import com.google.dart.engine.ast.FunctionBody;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.IntegerLiteral;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SimpleStringLiteral;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.ParameterElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.builder.ElementBuilder;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.ConstructorElementImpl;
import com.google.dart.engine.internal.element.FieldElementImpl;
import com.google.dart.engine.internal.element.FunctionElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.ParameterElementImpl;
import com.google.dart.engine.internal.element.PropertyAccessorElementImpl;
import com.google.dart.engine.internal.element.VariableElementImpl;
import com.google.dart.engine.internal.element.member.MethodMember;
import com.google.dart.engine.internal.type.FunctionTypeImpl;
import com.google.dart.engine.internal.type.InterfaceTypeImpl;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.sdk.DirectoryBasedDartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.FileBasedSource;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.type.FunctionType;
import com.google.dart.engine.type.InterfaceType;
import com.google.dart.engine.type.Type;
import com.google.dart.engine.utilities.io.FileUtilities2;

import static com.google.dart.engine.ast.AstFactory.adjacentStrings;
import static com.google.dart.engine.ast.AstFactory.argumentDefinitionTest;
import static com.google.dart.engine.ast.AstFactory.asExpression;
import static com.google.dart.engine.ast.AstFactory.assignmentExpression;
import static com.google.dart.engine.ast.AstFactory.binaryExpression;
import static com.google.dart.engine.ast.AstFactory.blockFunctionBody;
import static com.google.dart.engine.ast.AstFactory.booleanLiteral;
import static com.google.dart.engine.ast.AstFactory.cascadeExpression;
import static com.google.dart.engine.ast.AstFactory.conditionalExpression;
import static com.google.dart.engine.ast.AstFactory.doubleLiteral;
import static com.google.dart.engine.ast.AstFactory.expressionFunctionBody;
import static com.google.dart.engine.ast.AstFactory.formalParameterList;
import static com.google.dart.engine.ast.AstFactory.functionExpression;
import static com.google.dart.engine.ast.AstFactory.identifier;
import static com.google.dart.engine.ast.AstFactory.indexExpression;
import static com.google.dart.engine.ast.AstFactory.instanceCreationExpression;
import static com.google.dart.engine.ast.AstFactory.integer;
import static com.google.dart.engine.ast.AstFactory.interpolationExpression;
import static com.google.dart.engine.ast.AstFactory.interpolationString;
import static com.google.dart.engine.ast.AstFactory.isExpression;
import static com.google.dart.engine.ast.AstFactory.listLiteral;
import static com.google.dart.engine.ast.AstFactory.mapLiteral;
import static com.google.dart.engine.ast.AstFactory.mapLiteralEntry;
import static com.google.dart.engine.ast.AstFactory.methodInvocation;
import static com.google.dart.engine.ast.AstFactory.namedExpression;
import static com.google.dart.engine.ast.AstFactory.namedFormalParameter;
import static com.google.dart.engine.ast.AstFactory.nullLiteral;
import static com.google.dart.engine.ast.AstFactory.parenthesizedExpression;
import static com.google.dart.engine.ast.AstFactory.positionalFormalParameter;
import static com.google.dart.engine.ast.AstFactory.postfixExpression;
import static com.google.dart.engine.ast.AstFactory.prefixExpression;
import static com.google.dart.engine.ast.AstFactory.propertyAccess;
import static com.google.dart.engine.ast.AstFactory.simpleFormalParameter;
import static com.google.dart.engine.ast.AstFactory.string;
import static com.google.dart.engine.ast.AstFactory.superExpression;
import static com.google.dart.engine.ast.AstFactory.symbolLiteral;
import static com.google.dart.engine.ast.AstFactory.thisExpression;
import static com.google.dart.engine.ast.AstFactory.throwExpression;
import static com.google.dart.engine.ast.AstFactory.typeName;
import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.constructorElement;
import static com.google.dart.engine.element.ElementFactory.fieldElement;
import static com.google.dart.engine.element.ElementFactory.getterElement;
import static com.google.dart.engine.element.ElementFactory.localVariableElement;
import static com.google.dart.engine.element.ElementFactory.methodElement;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class StaticTypeAnalyzerTest extends EngineTestCase {
  /**
   * The error listener to which errors will be reported.
   */
  private GatheringErrorListener listener;

  /**
   * The resolver visitor used to create the analyzer.
   */
  private ResolverVisitor visitor;

  /**
   * The analyzer being used to analyze the test cases.
   */
  private StaticTypeAnalyzer analyzer;

  /**
   * The type provider used to access the types.
   */
  private TestTypeProvider typeProvider;

  public void fail_visitFunctionExpressionInvocation() throws Exception {
    fail("Not yet tested");
    listener.assertNoErrors();
  }

  public void fail_visitMethodInvocation() throws Exception {
    fail("Not yet tested");
    listener.assertNoErrors();
  }

  public void fail_visitSimpleIdentifier() throws Exception {
    fail("Not yet tested");
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

  public void test_visitAssignmentExpression_compound() throws Exception {
    // i += 1
    InterfaceType numType = typeProvider.getNumType();
    SimpleIdentifier identifier = resolvedVariable(typeProvider.getIntType(), "i");
    AssignmentExpression node = assignmentExpression(
        identifier,
        TokenType.PLUS_EQ,
        resolvedInteger(1));
    MethodElement plusMethod = getMethod(numType, "+");
    node.setStaticElement(plusMethod);
    assertSame(numType, analyze(node));
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

  public void test_visitBinaryExpression_plusID() throws Exception {
    // 1 + 2.0
    BinaryExpression node = binaryExpression(
        resolvedInteger(1),
        TokenType.PLUS,
        resolvedDouble(2.0));
    node.setStaticElement(getMethod(typeProvider.getNumType(), "+"));
    assertSame(typeProvider.getDoubleType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitBinaryExpression_plusII() throws Exception {
    // 1 + 2
    BinaryExpression node = binaryExpression(resolvedInteger(1), TokenType.PLUS, resolvedInteger(2));
    node.setStaticElement(getMethod(typeProvider.getNumType(), "+"));
    assertSame(typeProvider.getIntType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitBinaryExpression_slash() throws Exception {
    // 2 / 2
    BinaryExpression node = binaryExpression(
        resolvedInteger(2),
        TokenType.SLASH,
        resolvedInteger(2));
    node.setStaticElement(getMethod(typeProvider.getNumType(), "/"));
    assertSame(typeProvider.getDoubleType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitBinaryExpression_star_notSpecial() throws Exception {
    // class A {
    //   A operator *(double value);
    // }
    // (a as A) * 2.0
    ClassElementImpl classA = classElement("A");
    InterfaceType typeA = classA.getType();
    MethodElement operator = methodElement("*", typeA, typeProvider.getDoubleType());
    classA.setMethods(new MethodElement[] {operator});
    BinaryExpression node = binaryExpression(
        asExpression(identifier("a"), typeName(classA)),
        TokenType.PLUS,
        resolvedDouble(2.0));
    node.setStaticElement(operator);
    assertSame(typeA, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitBinaryExpression_starID() throws Exception {
    // 1 * 2.0
    BinaryExpression node = binaryExpression(
        resolvedInteger(1),
        TokenType.PLUS,
        resolvedDouble(2.0));
    node.setStaticElement(getMethod(typeProvider.getNumType(), "*"));
    assertSame(typeProvider.getDoubleType(), analyze(node));
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

  public void test_visitConditionalExpression_differentTypes() throws Exception {
    // true ? 1.0 : 0
    Expression node = conditionalExpression(
        booleanLiteral(true),
        resolvedDouble(1.0),
        resolvedInteger(0));
    assertSame(typeProvider.getNumType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitConditionalExpression_sameTypes() throws Exception {
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
    FunctionExpression node = resolvedFunctionExpression(
        formalParameterList(p1, p2),
        blockFunctionBody());
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
    FunctionExpression node = resolvedFunctionExpression(
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
    FunctionExpression node = resolvedFunctionExpression(
        formalParameterList(p1, p2),
        blockFunctionBody());
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
    FunctionExpression node = resolvedFunctionExpression(
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
    FunctionExpression node = resolvedFunctionExpression(
        formalParameterList(p1, p2),
        blockFunctionBody());
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
    FunctionExpression node = resolvedFunctionExpression(
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
    FunctionExpression node = resolvedFunctionExpression(
        formalParameterList(p1, p2),
        blockFunctionBody());
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
    FunctionExpression node = resolvedFunctionExpression(
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
    FunctionExpression node = resolvedFunctionExpression(
        formalParameterList(p1, p2),
        blockFunctionBody());
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
    FunctionExpression node = resolvedFunctionExpression(
        formalParameterList(p),
        expressionFunctionBody(resolvedInteger(0)));
    analyze(p);
    Type resultType = analyze(node);
    assertFunctionType(typeProvider.getIntType(), null, new Type[] {dynamicType}, null, resultType);
    listener.assertNoErrors();
  }

  public void test_visitIndexExpression_getter() throws Exception {
    // List a;
    // a[2]
    InterfaceType listType = typeProvider.getListType();
    SimpleIdentifier identifier = resolvedVariable(listType, "a");
    IndexExpression node = indexExpression(identifier, resolvedInteger(2));
    MethodElement indexMethod = listType.getElement().getMethods()[0];
    node.setStaticElement(indexMethod);
    assertSame(listType.getTypeArguments()[0], analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitIndexExpression_setter() throws Exception {
    // List a;
    // a[2] = 0
    InterfaceType listType = typeProvider.getListType();
    SimpleIdentifier identifier = resolvedVariable(listType, "a");
    IndexExpression node = indexExpression(identifier, resolvedInteger(2));
    MethodElement indexMethod = listType.getElement().getMethods()[1];
    node.setStaticElement(indexMethod);
    assignmentExpression(node, TokenType.EQ, integer(0));
    assertSame(listType.getTypeArguments()[0], analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitIndexExpression_typeParameters() throws Exception {
    // List<int> list = ...
    // list[0]
    InterfaceType intType = typeProvider.getIntType();
    InterfaceType listType = typeProvider.getListType();
    // (int) -> E
    MethodElement methodElement = getMethod(listType, "[]");
    // "list" has type List<int>
    SimpleIdentifier identifier = identifier("list");
    InterfaceType listOfIntType = listType.substitute(new Type[] {intType});
    identifier.setStaticType(listOfIntType);
    // list[0] has MethodElement element (int) -> E
    IndexExpression indexExpression = indexExpression(identifier, integer(0));
    MethodElement indexMethod = MethodMember.from(methodElement, listOfIntType);
    indexExpression.setStaticElement(indexMethod);
    // analyze and assert result of the index expression
    assertSame(intType, analyze(indexExpression));
    listener.assertNoErrors();
  }

  public void test_visitIndexExpression_typeParameters_inSetterContext() throws Exception {
    // List<int> list = ...
    // list[0] = 0;
    InterfaceType intType = typeProvider.getIntType();
    InterfaceType listType = typeProvider.getListType();
    // (int, E) -> void
    MethodElement methodElement = getMethod(listType, "[]=");
    // "list" has type List<int>
    SimpleIdentifier identifier = identifier("list");
    InterfaceType listOfIntType = listType.substitute(new Type[] {intType});
    identifier.setStaticType(listOfIntType);
    // list[0] has MethodElement element (int) -> E
    IndexExpression indexExpression = indexExpression(identifier, integer(0));
    MethodElement indexMethod = MethodMember.from(methodElement, listOfIntType);
    indexExpression.setStaticElement(indexMethod);
    // list[0] should be in a setter context
    assignmentExpression(indexExpression, TokenType.EQ, integer(0));
    // analyze and assert result of the index expression
    assertSame(intType, analyze(indexExpression));
    listener.assertNoErrors();
  }

  public void test_visitInstanceCreationExpression_named() throws Exception {
    // new C.m()
    ClassElementImpl classElement = classElement("C");
    String constructorName = "m";
    ConstructorElementImpl constructor = constructorElement(classElement, constructorName);
    constructor.setReturnType(classElement.getType());
    FunctionTypeImpl constructorType = new FunctionTypeImpl(constructor);
    constructor.setType(constructorType);
    classElement.setConstructors(new ConstructorElement[] {constructor});
    InstanceCreationExpression node = instanceCreationExpression(
        null,
        typeName(classElement),
        identifier(constructorName));
    node.setStaticElement(constructor);
    assertSame(classElement.getType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitInstanceCreationExpression_typeParameters() throws Exception {
    // new C<I>()
    ClassElementImpl elementC = classElement("C", "E");
    ClassElementImpl elementI = classElement("I");
    ConstructorElementImpl constructor = constructorElement(elementC, null);
    elementC.setConstructors(new ConstructorElement[] {constructor});
    constructor.setReturnType(elementC.getType());
    FunctionTypeImpl constructorType = new FunctionTypeImpl(constructor);
    constructor.setType(constructorType);
    TypeName typeName = typeName(elementC, typeName(elementI));
    typeName.setType(elementC.getType().substitute(new Type[] {elementI.getType()}));
    InstanceCreationExpression node = instanceCreationExpression(null, typeName);
    node.setStaticElement(constructor);
    InterfaceType interfaceType = (InterfaceType) analyze(node);
    Type[] typeArgs = interfaceType.getTypeArguments();
    assertEquals(1, typeArgs.length);
    assertEquals(elementI.getType(), typeArgs[0]);
    listener.assertNoErrors();
  }

  public void test_visitInstanceCreationExpression_unnamed() throws Exception {
    // new C()
    ClassElementImpl classElement = classElement("C");
    ConstructorElementImpl constructor = constructorElement(classElement, null);
    constructor.setReturnType(classElement.getType());
    FunctionTypeImpl constructorType = new FunctionTypeImpl(constructor);
    constructor.setType(constructorType);
    classElement.setConstructors(new ConstructorElement[] {constructor});
    InstanceCreationExpression node = instanceCreationExpression(null, typeName(classElement));
    node.setStaticElement(constructor);
    assertSame(classElement.getType(), analyze(node));
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
            new Type[] {typeProvider.getDynamicType(), typeProvider.getDynamicType()}),
        resultType);
    listener.assertNoErrors();
  }

  public void test_visitMapLiteral_nonEmpty() throws Exception {
    // {"k" : 0}
    Expression node = mapLiteral(mapLiteralEntry("k", resolvedInteger(0)));
    Type resultType = analyze(node);
    assertType(
        typeProvider.getMapType().substitute(
            new Type[] {typeProvider.getDynamicType(), typeProvider.getDynamicType()}),
        resultType);
    listener.assertNoErrors();
  }

  public void test_visitMethodInvocation_then() throws Exception {
    // then()
    Expression node = methodInvocation(null, "then");
    analyze(node);
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

  public void test_visitPrefixedIdentifier_getter() throws Exception {
    Type boolType = typeProvider.getBoolType();
    PropertyAccessorElementImpl getter = getterElement("b", false, boolType);
    PrefixedIdentifier node = identifier("a", "b");
    node.getIdentifier().setStaticElement(getter);
    assertSame(boolType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitPrefixedIdentifier_setter() throws Exception {
    Type boolType = typeProvider.getBoolType();
    FieldElementImpl field = fieldElement("b", false, false, false, boolType);
    PropertyAccessorElement setter = field.getSetter();
    PrefixedIdentifier node = identifier("a", "b");
    node.getIdentifier().setStaticElement(setter);
    assertSame(boolType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitPrefixedIdentifier_variable() throws Exception {
    VariableElementImpl variable = localVariableElement("b");
    variable.setType(typeProvider.getBoolType());
    PrefixedIdentifier node = identifier("a", "b");
    node.getIdentifier().setStaticElement(variable);
    assertSame(typeProvider.getBoolType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitPrefixExpression_bang() throws Exception {
    // !0
    PrefixExpression node = prefixExpression(TokenType.BANG, resolvedInteger(0));
    assertSame(typeProvider.getBoolType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitPrefixExpression_minus() throws Exception {
    // -0
    PrefixExpression node = prefixExpression(TokenType.MINUS, resolvedInteger(0));
    MethodElement minusMethod = getMethod(typeProvider.getNumType(), "-");
    node.setStaticElement(minusMethod);
    assertSame(typeProvider.getNumType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitPrefixExpression_minusMinus() throws Exception {
    // --0
    PrefixExpression node = prefixExpression(TokenType.MINUS_MINUS, resolvedInteger(0));
    MethodElement minusMethod = getMethod(typeProvider.getNumType(), "-");
    node.setStaticElement(minusMethod);
    assertSame(typeProvider.getIntType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitPrefixExpression_not() throws Exception {
    // !true
    Expression node = prefixExpression(TokenType.BANG, booleanLiteral(true));
    assertSame(typeProvider.getBoolType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitPrefixExpression_plusPlus() throws Exception {
    // ++0
    PrefixExpression node = prefixExpression(TokenType.PLUS_PLUS, resolvedInteger(0));
    MethodElement plusMethod = getMethod(typeProvider.getNumType(), "+");
    node.setStaticElement(plusMethod);
    assertSame(typeProvider.getIntType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitPrefixExpression_tilde() throws Exception {
    // ~0
    PrefixExpression node = prefixExpression(TokenType.TILDE, resolvedInteger(0));
    MethodElement tildeMethod = getMethod(typeProvider.getIntType(), "~");
    node.setStaticElement(tildeMethod);
    assertSame(typeProvider.getIntType(), analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitPropertyAccess_propagated_getter() throws Exception {
    Type boolType = typeProvider.getBoolType();
    PropertyAccessorElementImpl getter = getterElement("b", false, boolType);
    PropertyAccess node = propertyAccess(identifier("a"), "b");
    node.getPropertyName().setPropagatedElement(getter);
    assertSame(boolType, analyze(node, false));
    listener.assertNoErrors();
  }

  public void test_visitPropertyAccess_propagated_setter() throws Exception {
    Type boolType = typeProvider.getBoolType();
    FieldElementImpl field = fieldElement("b", false, false, false, boolType);
    PropertyAccessorElement setter = field.getSetter();
    PropertyAccess node = propertyAccess(identifier("a"), "b");
    node.getPropertyName().setPropagatedElement(setter);
    assertSame(boolType, analyze(node, false));
    listener.assertNoErrors();
  }

  public void test_visitPropertyAccess_static_getter() throws Exception {
    Type boolType = typeProvider.getBoolType();
    PropertyAccessorElementImpl getter = getterElement("b", false, boolType);
    PropertyAccess node = propertyAccess(identifier("a"), "b");
    node.getPropertyName().setStaticElement(getter);
    assertSame(boolType, analyze(node));
    listener.assertNoErrors();
  }

  public void test_visitPropertyAccess_static_setter() throws Exception {
    Type boolType = typeProvider.getBoolType();
    FieldElementImpl field = fieldElement("b", false, false, false, boolType);
    PropertyAccessorElement setter = field.getSetter();
    PropertyAccess node = propertyAccess(identifier("a"), "b");
    node.getPropertyName().setStaticElement(setter);
    assertSame(boolType, analyze(node));
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
    assertSame(thisType, analyze(node, thisType));
    listener.assertNoErrors();
  }

  public void test_visitSymbolLiteral() throws Exception {
    assertSame(typeProvider.getSymbolType(), analyze(symbolLiteral("a")));
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
   * Return the type associated with the given expression after the static type analyzer has
   * computed a type for it.
   * 
   * @param node the expression with which the type is associated
   * @return the type associated with the expression
   */
  private Type analyze(Expression node) {
    return analyze(node, null, true);
  }

  /**
   * Return the type associated with the given expression after the static or propagated type
   * analyzer has computed a type for it.
   * 
   * @param node the expression with which the type is associated
   * @param useStaticType {@code true} if the static type is being requested, and {@code false} if
   *          the propagated type is being requested
   * @return the type associated with the expression
   */
  private Type analyze(Expression node, boolean useStaticType) {
    return analyze(node, null, useStaticType);
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
    return analyze(node, thisType, true);
  }

  /**
   * Return the type associated with the given expression after the static type analyzer has
   * computed a type for it.
   * 
   * @param node the expression with which the type is associated
   * @param thisType the type of 'this'
   * @param useStaticType {@code true} if the static type is being requested, and {@code false} if
   *          the propagated type is being requested
   * @return the type associated with the expression
   */
  private Type analyze(Expression node, InterfaceType thisType, boolean useStaticType) {
    try {
      Field typeField = analyzer.getClass().getDeclaredField("thisType");
      typeField.setAccessible(true);
      typeField.set(analyzer, thisType);
    } catch (Exception exception) {
      throw new IllegalArgumentException("Could not set type of 'this'", exception);
    }
    node.accept(analyzer);
    if (useStaticType) {
      return node.getStaticType();
    } else {
      return node.getPropagatedType();
    }
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
    return ((ParameterElement) node.getIdentifier().getStaticElement()).getType();
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
      assertSizeOfMap(0, namedTypes);
    } else {
      assertSizeOfMap(expectedNamedTypes.size(), namedTypes);
      for (Map.Entry<String, Type> entry : expectedNamedTypes.entrySet()) {
        assertSame(entry.getValue(), namedTypes.get(entry.getKey()));
      }
    }

    assertSame(expectedReturnType, functionType.getReturnType());
  }

  private void assertType(InterfaceTypeImpl expectedType, InterfaceTypeImpl actualType) {
    assertEquals(expectedType.getDisplayName(), actualType.getDisplayName());
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
    SourceFactory sourceFactory = new SourceFactory(new DartUriResolver(
        DirectoryBasedDartSdk.getDefaultSdk()));
    context.setSourceFactory(sourceFactory);
    FileBasedSource source = new FileBasedSource(FileUtilities2.createFile("/lib.dart"));
    CompilationUnitElementImpl definingCompilationUnit = new CompilationUnitElementImpl("lib.dart");
    definingCompilationUnit.setSource(source);
    LibraryElementImpl definingLibrary = new LibraryElementImpl(context, null);
    definingLibrary.setDefiningCompilationUnit(definingCompilationUnit);
    Library library = new Library(context, listener, source);
    library.setLibraryElement(definingLibrary);
    visitor = new ResolverVisitor(library, source, typeProvider);
    visitor.getOverrideManager().enterScope();
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
  private DoubleLiteral resolvedDouble(double value) {
    DoubleLiteral literal = doubleLiteral(value);
    literal.setStaticType(typeProvider.getDoubleType());
    return literal;
  }

  /**
   * Create a function expression that has an element associated with it, where the element has an
   * incomplete type associated with it (just like the one
   * {@link ElementBuilder#visitFunctionExpression(FunctionExpression)} would have built if we had
   * run it).
   * 
   * @param parameters the parameters to the function
   * @param body the body of the function
   * @return a resolved function expression
   */
  private FunctionExpression resolvedFunctionExpression(FormalParameterList parameters,
      FunctionBody body) {
    ArrayList<ParameterElement> parameterElements = new ArrayList<ParameterElement>();
    for (FormalParameter parameter : parameters.getParameters()) {
      ParameterElementImpl element = new ParameterElementImpl(parameter.getIdentifier());
      element.setParameterKind(parameter.getKind());
      element.setType(typeProvider.getDynamicType());
      parameter.getIdentifier().setStaticElement(element);
      parameterElements.add(element);
    }
    FunctionExpression node = functionExpression(parameters, body);
    FunctionElementImpl element = new FunctionElementImpl(null);
    element.setParameters(parameterElements.toArray(new ParameterElement[parameterElements.size()]));
    element.setType(new FunctionTypeImpl(element));
    node.setElement(element);
    return node;
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
    VariableElementImpl element = localVariableElement(identifier);
    element.setType(type);
    identifier.setStaticElement(element);
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
    Element element = identifier.getStaticElement();
    if (!(element instanceof ParameterElement)) {
      element = new ParameterElementImpl(identifier);
      identifier.setStaticElement(element);
    }
    ((ParameterElementImpl) element).setType(type);
  }
}
