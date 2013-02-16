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
import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.BreakStatement;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.ContinueStatement;
import com.google.dart.engine.ast.ExportDirective;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.ImportDirective;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ImportElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.element.PropertyAccessorElement;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.LabelElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.VariableElementImpl;
import com.google.dart.engine.internal.scope.ClassScope;
import com.google.dart.engine.internal.scope.EnclosedScope;
import com.google.dart.engine.internal.scope.LabelScope;
import com.google.dart.engine.internal.scope.Scope;
import com.google.dart.engine.scanner.Keyword;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.type.InterfaceType;

import static com.google.dart.engine.ast.ASTFactory.assignmentExpression;
import static com.google.dart.engine.ast.ASTFactory.binaryExpression;
import static com.google.dart.engine.ast.ASTFactory.breakStatement;
import static com.google.dart.engine.ast.ASTFactory.constructorName;
import static com.google.dart.engine.ast.ASTFactory.continueStatement;
import static com.google.dart.engine.ast.ASTFactory.exportDirective;
import static com.google.dart.engine.ast.ASTFactory.hideCombinator;
import static com.google.dart.engine.ast.ASTFactory.identifier;
import static com.google.dart.engine.ast.ASTFactory.importDirective;
import static com.google.dart.engine.ast.ASTFactory.indexExpression;
import static com.google.dart.engine.ast.ASTFactory.instanceCreationExpression;
import static com.google.dart.engine.ast.ASTFactory.integer;
import static com.google.dart.engine.ast.ASTFactory.methodInvocation;
import static com.google.dart.engine.ast.ASTFactory.postfixExpression;
import static com.google.dart.engine.ast.ASTFactory.prefixExpression;
import static com.google.dart.engine.ast.ASTFactory.propertyAccess;
import static com.google.dart.engine.ast.ASTFactory.showCombinator;
import static com.google.dart.engine.ast.ASTFactory.typeName;
import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.constructorElement;
import static com.google.dart.engine.element.ElementFactory.exportFor;
import static com.google.dart.engine.element.ElementFactory.getterElement;
import static com.google.dart.engine.element.ElementFactory.importFor;
import static com.google.dart.engine.element.ElementFactory.library;
import static com.google.dart.engine.element.ElementFactory.localVariableElement;
import static com.google.dart.engine.element.ElementFactory.methodElement;
import static com.google.dart.engine.element.ElementFactory.prefix;

import java.lang.reflect.Field;

public class ElementResolverTest extends EngineTestCase {
  /**
   * The error listener to which errors will be reported.
   */
  private GatheringErrorListener listener;

  /**
   * The type provider used to access the types.
   */
  private TestTypeProvider typeProvider;

  /**
   * The library containing the code being resolved.
   */
  private LibraryElementImpl definingLibrary;

  /**
   * The resolver visitor that maintains the state for the resolver.
   */
  private ResolverVisitor visitor;

  /**
   * The resolver being used to resolve the test cases.
   */
  private ElementResolver resolver;

  public void fail_visitExportDirective_combinators() {
    fail("Not yet tested");
    // Need to set up the exported library so that the identifier can be resolved
    ExportDirective directive = exportDirective(null, hideCombinator("A"));
    resolveNode(directive);
    listener.assertNoErrors();
  }

  public void fail_visitFunctionExpressionInvocation() {
    fail("Not yet tested");
    listener.assertNoErrors();
  }

  public void fail_visitImportDirective_combinators_noPrefix() {
    fail("Not yet tested");
    // Need to set up the imported library so that the identifier can be resolved
    ImportDirective directive = importDirective(null, null, showCombinator("A"));
    resolveNode(directive);
    listener.assertNoErrors();
  }

  public void fail_visitImportDirective_combinators_prefix() {
    fail("Not yet tested");
    // Need to set up the imported library so that the identifiers can be resolved
    String prefixName = "p";
    definingLibrary.setImports(new ImportElement[] {importFor(null, prefix(prefixName))});
    ImportDirective directive = importDirective(
        null,
        prefixName,
        showCombinator("A"),
        hideCombinator("B"));
    resolveNode(directive);
    listener.assertNoErrors();
  }

  public void fail_visitRedirectingConstructorInvocation() throws Exception {
    fail("Not yet tested");
    listener.assertNoErrors();
  }

  public void fail_visitSuperConstructorInvocation() throws Exception {
    fail("Not yet tested");
    listener.assertNoErrors();
  }

  @Override
  public void setUp() {
    listener = new GatheringErrorListener();
    typeProvider = new TestTypeProvider();
    resolver = createResolver();
  }

  public void test_visitAssignmentExpression_compound() throws Exception {
    InterfaceType intType = typeProvider.getIntType();
    SimpleIdentifier leftHandSide = identifier("a");
    leftHandSide.setStaticType(intType);
    AssignmentExpression assignment = assignmentExpression(
        leftHandSide,
        TokenType.PLUS_EQ,
        integer(1L));
    resolveNode(assignment);
    assertSame(getMethod(typeProvider.getNumType(), "+"), assignment.getElement());
    listener.assertNoErrors();
  }

  public void test_visitAssignmentExpression_simple() throws Exception {
    AssignmentExpression expression = assignmentExpression(
        identifier("x"),
        TokenType.EQ,
        integer(0));
    resolveNode(expression);
    assertNull(expression.getElement());
    listener.assertNoErrors();
  }

  public void test_visitBinaryExpression() throws Exception { // _found and _notFound?
    InterfaceType numType = typeProvider.getNumType();
    SimpleIdentifier left = identifier("i");
    left.setStaticType(numType);
    BinaryExpression expression = binaryExpression(left, TokenType.PLUS, identifier("j"));
    resolveNode(expression);
    assertEquals(getMethod(numType, "+"), expression.getElement());
    listener.assertNoErrors();
  }

  public void test_visitBreakStatement_withLabel() throws Exception {
    String label = "loop";
    LabelElementImpl labelElement = new LabelElementImpl(identifier(label), false, false);
    BreakStatement statement = breakStatement(label);
    assertSame(labelElement, resolve(statement, labelElement));
    listener.assertNoErrors();
  }

  public void test_visitBreakStatement_withoutLabel() throws Exception {
    BreakStatement statement = breakStatement();
    resolveStatement(statement, null);
    listener.assertNoErrors();
  }

  public void test_visitConstructorName_named() {
    ClassElementImpl classA = classElement("A");
    String constructorName = "a";
    ConstructorElement constructor = constructorElement(constructorName);
    classA.setConstructors(new ConstructorElement[] {constructor});
    ConstructorName name = constructorName(typeName(classA), constructorName);
    resolveNode(name);
    assertSame(constructor, name.getElement());
    listener.assertNoErrors();
  }

  public void test_visitConstructorName_unnamed() {
    ClassElementImpl classA = classElement("A");
    String constructorName = null;
    ConstructorElement constructor = constructorElement(constructorName);
    classA.setConstructors(new ConstructorElement[] {constructor});
    ConstructorName name = constructorName(typeName(classA), constructorName);
    resolveNode(name);
    assertSame(constructor, name.getElement());
    listener.assertNoErrors();
  }

  public void test_visitContinueStatement_withLabel() throws Exception {
    String label = "loop";
    LabelElementImpl labelElement = new LabelElementImpl(identifier(label), false, false);
    ContinueStatement statement = continueStatement(label);
    assertSame(labelElement, resolve(statement, labelElement));
    listener.assertNoErrors();
  }

  public void test_visitContinueStatement_withoutLabel() throws Exception {
    ContinueStatement statement = continueStatement();
    resolveStatement(statement, null);
    listener.assertNoErrors();
  }

  public void test_visitExportDirective_noCombinators() {
    ExportDirective directive = exportDirective(null);
    directive.setElement(exportFor(library(definingLibrary.getContext(), "lib")));
    resolveNode(directive);
    listener.assertNoErrors();
  }

  public void test_visitImportDirective_noCombinators_noPrefix() {
    ImportDirective directive = importDirective(null, null);
    directive.setElement(importFor(library(definingLibrary.getContext(), "lib"), null));
    resolveNode(directive);
    listener.assertNoErrors();
  }

  public void test_visitImportDirective_noCombinators_prefix() {
    String prefixName = "p";
    ImportElement importElement = importFor(
        library(definingLibrary.getContext(), "lib"),
        prefix(prefixName));
    definingLibrary.setImports(new ImportElement[] {importElement});
    ImportDirective directive = importDirective(null, prefixName);
    directive.setElement(importElement);
    resolveNode(directive);
    listener.assertNoErrors();
  }

  public void test_visitIndexExpression_get() throws Exception {
    ClassElementImpl classA = classElement("A");
    InterfaceType intType = typeProvider.getIntType();
    MethodElement getter = methodElement("[]", intType, intType);
    classA.setMethods(new MethodElement[] {getter});
    SimpleIdentifier array = identifier("a");
    array.setStaticType(classA.getType());
    IndexExpression expression = indexExpression(array, identifier("i"));
    assertSame(getter, resolve(expression));
    listener.assertNoErrors();
  }

  public void test_visitIndexExpression_set() throws Exception {
    ClassElementImpl classA = classElement("A");
    InterfaceType intType = typeProvider.getIntType();
    MethodElement setter = methodElement("[]=", intType, intType);
    classA.setMethods(new MethodElement[] {setter});
    SimpleIdentifier array = identifier("a");
    array.setStaticType(classA.getType());
    IndexExpression expression = indexExpression(array, identifier("i"));
    assignmentExpression(expression, TokenType.EQ, integer(0L));
    assertSame(setter, resolve(expression));
    listener.assertNoErrors();
  }

  public void test_visitInstanceCreationExpression_named() {
    ClassElementImpl classA = classElement("A");
    String constructorName = "a";
    ConstructorElement constructor = constructorElement(constructorName);
    classA.setConstructors(new ConstructorElement[] {constructor});
    ConstructorName name = constructorName(typeName(classA), constructorName);
    name.setElement(constructor);
    InstanceCreationExpression creation = instanceCreationExpression(Keyword.NEW, name);
    resolveNode(creation);
    assertSame(constructor, creation.getElement());
    listener.assertNoErrors();
  }

  public void test_visitInstanceCreationExpression_unnamed() {
    ClassElementImpl classA = classElement("A");
    String constructorName = null;
    ConstructorElement constructor = constructorElement(constructorName);
    classA.setConstructors(new ConstructorElement[] {constructor});
    ConstructorName name = constructorName(typeName(classA), constructorName);
    name.setElement(constructor);
    InstanceCreationExpression creation = instanceCreationExpression(Keyword.NEW, name);
    resolveNode(creation);
    assertSame(constructor, creation.getElement());
    listener.assertNoErrors();
  }

  public void test_visitMethodInvocation() throws Exception {
    InterfaceType numType = typeProvider.getNumType();
    SimpleIdentifier left = identifier("i");
    left.setStaticType(numType);
    String methodName = "abs";
    MethodInvocation invocation = methodInvocation(left, methodName);
    resolveNode(invocation);
    assertSame(getMethod(numType, methodName), invocation.getMethodName().getElement());
    listener.assertNoErrors();
  }

  public void test_visitPostfixExpression() throws Exception {
    InterfaceType numType = typeProvider.getNumType();
    SimpleIdentifier operand = identifier("i");
    operand.setStaticType(numType);
    PostfixExpression expression = postfixExpression(operand, TokenType.PLUS_PLUS);
    resolveNode(expression);
    assertEquals(getMethod(numType, "+"), expression.getElement());
    listener.assertNoErrors();
  }

  public void test_visitPrefixedIdentifier() throws Exception {
    ClassElementImpl classA = classElement("A");
    String getterName = "b";
    PropertyAccessorElement getter = getterElement(getterName, false, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {getter});
    SimpleIdentifier target = identifier("a");
    VariableElementImpl variable = localVariableElement(target);
    variable.setType(classA.getType());
    target.setElement(variable);
    target.setStaticType(classA.getType());
    PrefixedIdentifier identifier = identifier(target, identifier(getterName));
    resolveNode(identifier);
    assertSame(getter, identifier.getElement());
    assertSame(getter, identifier.getIdentifier().getElement());
    listener.assertNoErrors();
  }

  public void test_visitPrefixExpression() throws Exception {
    InterfaceType numType = typeProvider.getNumType();
    SimpleIdentifier operand = identifier("i");
    operand.setStaticType(numType);
    PrefixExpression expression = prefixExpression(TokenType.PLUS_PLUS, operand);
    resolveNode(expression);
    assertEquals(getMethod(numType, "+"), expression.getElement());
    listener.assertNoErrors();
  }

  public void test_visitPropertyAccess() throws Exception {
    ClassElementImpl classA = classElement("A");
    String getterName = "b";
    PropertyAccessorElement getter = getterElement(getterName, false, typeProvider.getIntType());
    classA.setAccessors(new PropertyAccessorElement[] {getter});
    SimpleIdentifier target = identifier("a");
    target.setStaticType(classA.getType());
    PropertyAccess access = propertyAccess(target, getterName);
    resolveNode(access);
    assertSame(getter, access.getPropertyName().getElement());
    listener.assertNoErrors();
  }

  public void test_visitSimpleIdentifier_inClassScope() throws Exception {
    InterfaceType doubleType = typeProvider.getDoubleType();
    String fieldName = "NAN";
    SimpleIdentifier node = identifier(fieldName);
    resolveInClass(node, doubleType.getElement());
    assertEquals(getGetter(doubleType, fieldName), node.getElement());
    listener.assertNoErrors();
  }

  public void test_visitSimpleIdentifier_inLexicalScope() throws Exception {
    SimpleIdentifier node = identifier("i");
    VariableElementImpl element = localVariableElement(node);
    assertSame(element, resolve(node, element));
    listener.assertNoErrors();
  }

  /**
   * Create the resolver used by the tests.
   * 
   * @return the resolver that was created
   */
  private ElementResolver createResolver() {
    AnalysisContextImpl context = new AnalysisContextImpl();
    context.setSourceFactory(new SourceFactory(new DartUriResolver(DartSdk.getDefaultSdk())));
    CompilationUnitElementImpl definingCompilationUnit = new CompilationUnitElementImpl("test.dart");
    definingLibrary = library(context, "test");
    definingLibrary.setDefiningCompilationUnit(definingCompilationUnit);
    Library library = new Library(context, listener, null);
    library.setLibraryElement(definingLibrary);
    visitor = new ResolverVisitor(library, null, typeProvider);
    try {
      Field resolverField = visitor.getClass().getDeclaredField("elementResolver");
      resolverField.setAccessible(true);
      return (ElementResolver) resolverField.get(visitor);
    } catch (Exception exception) {
      throw new IllegalArgumentException("Could not create resolver", exception);
    }
  }

  /**
   * Return the element associated with the label of the given statement after the resolver has
   * resolved the statement.
   * 
   * @param statement the statement to be resolved
   * @param labelElement the label element to be defined in the statement's label scope
   * @return the element to which the statement's label was resolved
   */
  private Element resolve(BreakStatement statement, LabelElementImpl labelElement) {
    resolveStatement(statement, labelElement);
    return statement.getLabel().getElement();
  }

  /**
   * Return the element associated with the label of the given statement after the resolver has
   * resolved the statement.
   * 
   * @param statement the statement to be resolved
   * @param labelElement the label element to be defined in the statement's label scope
   * @return the element to which the statement's label was resolved
   */
  private Element resolve(ContinueStatement statement, LabelElementImpl labelElement) {
    resolveStatement(statement, labelElement);
    return statement.getLabel().getElement();
  }

  /**
   * Return the element associated with the given identifier after the resolver has resolved the
   * identifier.
   * 
   * @param node the expression to be resolved
   * @param definedElements the elements that are to be defined in the scope in which the element is
   *          being resolved
   * @return the element to which the expression was resolved
   */
  private Element resolve(Identifier node, Element... definedElements) {
    resolveNode(node, definedElements);
    return node.getElement();
  }

  /**
   * Return the element associated with the given expression after the resolver has resolved the
   * expression.
   * 
   * @param node the expression to be resolved
   * @param definedElements the elements that are to be defined in the scope in which the element is
   *          being resolved
   * @return the element to which the expression was resolved
   */
  private Element resolve(IndexExpression node, Element... definedElements) {
    resolveNode(node, definedElements);
    return node.getElement();
  }

  /**
   * Return the element associated with the given identifier after the resolver has resolved the
   * identifier.
   * 
   * @param node the expression to be resolved
   * @param enclosingClass the element representing the class enclosing the identifier
   * @return the element to which the expression was resolved
   */
  private void resolveInClass(ASTNode node, ClassElement enclosingClass) {
    try {
      Field enclosingClassField = visitor.getClass().getDeclaredField("enclosingClass");
      enclosingClassField.setAccessible(true);

      Field scopeField = visitor.getClass().getSuperclass().getDeclaredField("nameScope");
      scopeField.setAccessible(true);
      Scope outerScope = (Scope) scopeField.get(visitor);

      try {
        enclosingClassField.set(visitor, enclosingClass);

        EnclosedScope innerScope = new ClassScope(outerScope, enclosingClass);
        scopeField.set(visitor, innerScope);

        node.accept(resolver);
      } finally {
        enclosingClassField.set(visitor, null);

        scopeField.set(visitor, outerScope);
      }
    } catch (Exception exception) {
      throw new IllegalArgumentException("Could not resolve node", exception);
    }
  }

  /**
   * Return the element associated with the given identifier after the resolver has resolved the
   * identifier.
   * 
   * @param node the expression to be resolved
   * @param definedElements the elements that are to be defined in the scope in which the element is
   *          being resolved
   * @return the element to which the expression was resolved
   */
  private void resolveNode(ASTNode node, Element... definedElements) {
    try {
      Field scopeField = visitor.getClass().getSuperclass().getDeclaredField("nameScope");
      scopeField.setAccessible(true);
      Scope outerScope = (Scope) scopeField.get(visitor);
      try {
        EnclosedScope innerScope = new EnclosedScope(outerScope);
        for (Element element : definedElements) {
          innerScope.define(element);
        }
        scopeField.set(visitor, innerScope);
        node.accept(resolver);
      } finally {
        scopeField.set(visitor, outerScope);
      }
    } catch (Exception exception) {
      throw new IllegalArgumentException("Could not resolve node", exception);
    }
  }

  /**
   * Return the element associated with the label of the given statement after the resolver has
   * resolved the statement.
   * 
   * @param statement the statement to be resolved
   * @param labelElement the label element to be defined in the statement's label scope
   * @return the element to which the statement's label was resolved
   */
  private void resolveStatement(Statement statement, LabelElementImpl labelElement) {
    try {
      Field scopeField = visitor.getClass().getSuperclass().getDeclaredField("labelScope");
      scopeField.setAccessible(true);
      LabelScope outerScope = (LabelScope) scopeField.get(visitor);
      try {
        LabelScope innerScope;
        if (labelElement == null) {
          innerScope = new LabelScope(outerScope, false, false);
        } else {
          innerScope = new LabelScope(outerScope, labelElement.getName(), labelElement);
        }
        scopeField.set(visitor, innerScope);
        statement.accept(resolver);
      } finally {
        scopeField.set(visitor, outerScope);
      }
    } catch (Exception exception) {
      throw new IllegalArgumentException("Could not resolve node", exception);
    }
  }
}
