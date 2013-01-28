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
import com.google.dart.engine.ast.ContinueStatement;
import com.google.dart.engine.ast.Identifier;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.context.AnalysisContextImpl;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.LabelElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;
import com.google.dart.engine.internal.element.VariableElementImpl;
import com.google.dart.engine.resolver.scope.EnclosedScope;
import com.google.dart.engine.resolver.scope.LabelScope;
import com.google.dart.engine.resolver.scope.Scope;
import com.google.dart.engine.scanner.TokenType;
import com.google.dart.engine.sdk.DartSdk;
import com.google.dart.engine.source.DartUriResolver;
import com.google.dart.engine.source.SourceFactory;
import com.google.dart.engine.type.InterfaceType;

import static com.google.dart.engine.ast.ASTFactory.assignmentExpression;
import static com.google.dart.engine.ast.ASTFactory.binaryExpression;
import static com.google.dart.engine.ast.ASTFactory.breakStatement;
import static com.google.dart.engine.ast.ASTFactory.continueStatement;
import static com.google.dart.engine.ast.ASTFactory.identifier;
import static com.google.dart.engine.ast.ASTFactory.indexExpression;
import static com.google.dart.engine.ast.ASTFactory.integer;
import static com.google.dart.engine.ast.ASTFactory.methodInvocation;
import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.methodElement;

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
   * The resolver visitor that maintains the state for the resolver.
   */
  private ResolverVisitor visitor;

  /**
   * The resolver being used to resolve the test cases.
   */
  private ElementResolver resolver;

  public void fail_visitAssignmentExpression() throws Exception {
    fail("Not yet tested");
    listener.assertNoErrors();
  }

  public void fail_visitFunctionExpressionInvocation() {
    fail("Not yet tested");
    listener.assertNoErrors();
  }

  public void fail_visitImportDirective() {
    fail("Not yet tested");
    listener.assertNoErrors();
  }

  public void fail_visitMethodInvocation() throws Exception {
    InterfaceType numType = typeProvider.getNumType();
    SimpleIdentifier left = identifier("i");
    left.setStaticType(numType);
    MethodInvocation invocation = methodInvocation(left, "abs");
    resolveNode(invocation);
    // TODO(brianwilkerson) Implement a more reliable mechanism for getting the expected element
    assertSame(numType.getElement().getMethods()[9], invocation.getMethodName().getElement());
    listener.assertNoErrors();
  }

  public void fail_visitPostfixExpression() throws Exception {
    fail("Not yet tested");
    listener.assertNoErrors();
  }

  public void fail_visitPrefixedIdentifier() throws Exception {
    fail("Not yet tested");
    listener.assertNoErrors();
  }

  public void fail_visitPrefixExpression() throws Exception {
    fail("Not yet tested");
    listener.assertNoErrors();
  }

  public void fail_visitPropertyAccess() throws Exception {
    fail("Not yet tested");
    listener.assertNoErrors();
  }

  @Override
  public void setUp() {
    listener = new GatheringErrorListener();
    typeProvider = new TestTypeProvider();
    resolver = createResolver();
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
    // TODO(brianwilkerson) Implement a more reliable mechanism for getting the expected element
    assertEquals(numType.getElement().getMethods()[0], expression.getElement());
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

  public void test_visitIndexExpression_get() throws Exception {
    ClassElementImpl classA = (ClassElementImpl) classElement("A");
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
    ClassElementImpl classA = (ClassElementImpl) classElement("A");
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

  public void test_visitSimpleIdentifier() throws Exception {
    VariableElementImpl element = new VariableElementImpl(identifier("i"));
    SimpleIdentifier node = identifier("i");
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
    CompilationUnitElementImpl definingCompilationUnit = new CompilationUnitElementImpl("lib.dart");
    LibraryElementImpl definingLibrary = new LibraryElementImpl(context, null);
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
   * @param definedElements the elements that are to be defined in the scope in which the element is
   *          being resolved
   * @return the element to which the expression was resolved
   */
  private void resolveNode(ASTNode node, Element... definedElements) {
    try {
      Field scopeField = visitor.getClass().getDeclaredField("nameScope");
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
      Field scopeField = visitor.getClass().getDeclaredField("labelScope");
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
