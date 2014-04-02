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
package com.google.dart.engine.internal.scope;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.ClassTypeAlias;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionTypeAlias;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.context.AnalysisContextFactory;
import com.google.dart.engine.context.AnalysisException;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ConstructorElement;
import com.google.dart.engine.element.FunctionElement;
import com.google.dart.engine.element.FunctionTypeAliasElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.internal.element.ClassElementImpl;
import com.google.dart.engine.internal.element.CompilationUnitElementImpl;
import com.google.dart.engine.internal.element.FunctionTypeAliasElementImpl;
import com.google.dart.engine.internal.element.LibraryElementImpl;

import static com.google.dart.engine.ast.AstFactory.classDeclaration;
import static com.google.dart.engine.ast.AstFactory.classTypeAlias;
import static com.google.dart.engine.ast.AstFactory.compilationUnit;
import static com.google.dart.engine.ast.AstFactory.constructorDeclaration;
import static com.google.dart.engine.ast.AstFactory.formalParameterList;
import static com.google.dart.engine.ast.AstFactory.functionDeclaration;
import static com.google.dart.engine.ast.AstFactory.functionExpression;
import static com.google.dart.engine.ast.AstFactory.identifier;
import static com.google.dart.engine.ast.AstFactory.methodDeclaration;
import static com.google.dart.engine.ast.AstFactory.typeAlias;
import static com.google.dart.engine.ast.AstFactory.typeName;
import static com.google.dart.engine.ast.AstFactory.typeParameterList;
import static com.google.dart.engine.element.ElementFactory.classElement;
import static com.google.dart.engine.element.ElementFactory.constructorElement;
import static com.google.dart.engine.element.ElementFactory.functionElement;
import static com.google.dart.engine.element.ElementFactory.library;
import static com.google.dart.engine.element.ElementFactory.methodElement;

public class ScopeBuilderTest extends EngineTestCase {
  public void test_scopeFor_ClassDeclaration() throws AnalysisException {
    GatheringErrorListener listener = new GatheringErrorListener();
    Scope scope = ScopeBuilder.scopeFor(createResolvedClassDeclaration(), listener);
    assertInstanceOf(LibraryScope.class, scope);
  }

  public void test_scopeFor_ClassTypeAlias() throws AnalysisException {
    GatheringErrorListener listener = new GatheringErrorListener();
    Scope scope = ScopeBuilder.scopeFor(createResolvedClassTypeAlias(), listener);
    assertInstanceOf(LibraryScope.class, scope);
  }

  public void test_scopeFor_CompilationUnit() throws AnalysisException {
    GatheringErrorListener listener = new GatheringErrorListener();
    Scope scope = ScopeBuilder.scopeFor(createResolvedCompilationUnit(), listener);
    assertInstanceOf(LibraryScope.class, scope);
  }

  public void test_scopeFor_ConstructorDeclaration() throws AnalysisException {
    GatheringErrorListener listener = new GatheringErrorListener();
    Scope scope = ScopeBuilder.scopeFor(createResolvedConstructorDeclaration(), listener);
    assertInstanceOf(ClassScope.class, scope);
  }

  public void test_scopeFor_ConstructorDeclaration_parameters() throws AnalysisException {
    GatheringErrorListener listener = new GatheringErrorListener();
    Scope scope = ScopeBuilder.scopeFor(
        createResolvedConstructorDeclaration().getParameters(),
        listener);
    assertInstanceOf(FunctionScope.class, scope);
  }

  public void test_scopeFor_FunctionDeclaration() throws AnalysisException {
    GatheringErrorListener listener = new GatheringErrorListener();
    Scope scope = ScopeBuilder.scopeFor(createResolvedFunctionDeclaration(), listener);
    assertInstanceOf(LibraryScope.class, scope);
  }

  public void test_scopeFor_FunctionDeclaration_parameters() throws AnalysisException {
    GatheringErrorListener listener = new GatheringErrorListener();
    Scope scope = ScopeBuilder.scopeFor(
        createResolvedFunctionDeclaration().getFunctionExpression().getParameters(),
        listener);
    assertInstanceOf(FunctionScope.class, scope);
  }

  public void test_scopeFor_FunctionTypeAlias() throws AnalysisException {
    GatheringErrorListener listener = new GatheringErrorListener();
    Scope scope = ScopeBuilder.scopeFor(createResolvedFunctionTypeAlias(), listener);
    assertInstanceOf(LibraryScope.class, scope);
  }

  public void test_scopeFor_FunctionTypeAlias_parameters() throws AnalysisException {
    GatheringErrorListener listener = new GatheringErrorListener();
    Scope scope = ScopeBuilder.scopeFor(createResolvedFunctionTypeAlias().getParameters(), listener);
    assertInstanceOf(FunctionTypeScope.class, scope);
  }

  public void test_scopeFor_MethodDeclaration() throws AnalysisException {
    GatheringErrorListener listener = new GatheringErrorListener();
    Scope scope = ScopeBuilder.scopeFor(createResolvedMethodDeclaration(), listener);
    assertInstanceOf(ClassScope.class, scope);
  }

  public void test_scopeFor_MethodDeclaration_body() throws AnalysisException {
    GatheringErrorListener listener = new GatheringErrorListener();
    Scope scope = ScopeBuilder.scopeFor(createResolvedMethodDeclaration().getBody(), listener);
    assertInstanceOf(FunctionScope.class, scope);
  }

  public void test_scopeFor_notInCompilationUnit() {
    GatheringErrorListener listener = new GatheringErrorListener();
    try {
      ScopeBuilder.scopeFor(identifier("x"), listener);
      fail("Expected AnalysisException");
    } catch (AnalysisException exception) {
      // Expected
    }
  }

  public void test_scopeFor_null() {
    GatheringErrorListener listener = new GatheringErrorListener();
    try {
      ScopeBuilder.scopeFor(null, listener);
      fail("Expected AnalysisException");
    } catch (AnalysisException exception) {
      // Expected
    }
  }

  public void test_scopeFor_unresolved() {
    GatheringErrorListener listener = new GatheringErrorListener();
    try {
      ScopeBuilder.scopeFor(compilationUnit(), listener);
      fail("Expected AnalysisException");
    } catch (AnalysisException exception) {
      // Expected
    }
  }

  private ClassDeclaration createResolvedClassDeclaration() {
    CompilationUnit unit = createResolvedCompilationUnit();
    String className = "C";
    ClassDeclaration classNode = classDeclaration(
        null,
        className,
        typeParameterList(),
        null,
        null,
        null);
    unit.getDeclarations().add(classNode);
    ClassElement classElement = classElement(className);
    classNode.getName().setStaticElement(classElement);
    ((CompilationUnitElementImpl) unit.getElement()).setTypes(new ClassElement[] {classElement});
    return classNode;
  }

  private ClassTypeAlias createResolvedClassTypeAlias() {
    CompilationUnit unit = createResolvedCompilationUnit();
    String className = "C";
    ClassTypeAlias classNode = classTypeAlias(
        className,
        typeParameterList(),
        null,
        null,
        null,
        null);
    unit.getDeclarations().add(classNode);
    ClassElement classElement = classElement(className);
    classNode.getName().setStaticElement(classElement);
    ((CompilationUnitElementImpl) unit.getElement()).setTypes(new ClassElement[] {classElement});
    return classNode;
  }

  private CompilationUnit createResolvedCompilationUnit() {
    CompilationUnit unit = compilationUnit();
    LibraryElementImpl library = library(AnalysisContextFactory.contextWithCore(), "lib");
    unit.setElement(library.getDefiningCompilationUnit());
    return unit;
  }

  private ConstructorDeclaration createResolvedConstructorDeclaration() {
    ClassDeclaration classNode = createResolvedClassDeclaration();
    String constructorName = "f";
    ConstructorDeclaration constructorNode = constructorDeclaration(
        identifier(constructorName),
        null,
        formalParameterList(),
        null);
    classNode.getMembers().add(constructorNode);
    ConstructorElement constructorElement = constructorElement(classNode.getElement(), null);
    constructorNode.setElement(constructorElement);
    ((ClassElementImpl) classNode.getElement()).setConstructors(new ConstructorElement[] {constructorElement});
    return constructorNode;
  }

  private FunctionDeclaration createResolvedFunctionDeclaration() {
    CompilationUnit unit = createResolvedCompilationUnit();
    String functionName = "f";
    FunctionDeclaration functionNode = functionDeclaration(
        null,
        null,
        functionName,
        functionExpression());
    unit.getDeclarations().add(functionNode);
    FunctionElement functionElement = functionElement(functionName);
    functionNode.getName().setStaticElement(functionElement);
    ((CompilationUnitElementImpl) unit.getElement()).setFunctions(new FunctionElement[] {functionElement});
    return functionNode;
  }

  private FunctionTypeAlias createResolvedFunctionTypeAlias() {
    CompilationUnit unit = createResolvedCompilationUnit();
    FunctionTypeAlias aliasNode = typeAlias(
        typeName("A"),
        "F",
        typeParameterList(),
        formalParameterList());
    unit.getDeclarations().add(aliasNode);
    SimpleIdentifier aliasName = aliasNode.getName();
    FunctionTypeAliasElement aliasElement = new FunctionTypeAliasElementImpl(aliasName);
    aliasName.setStaticElement(aliasElement);
    ((CompilationUnitElementImpl) unit.getElement()).setTypeAliases(new FunctionTypeAliasElement[] {aliasElement});
    return aliasNode;
  }

  private MethodDeclaration createResolvedMethodDeclaration() {
    ClassDeclaration classNode = createResolvedClassDeclaration();
    String methodName = "f";
    MethodDeclaration methodNode = methodDeclaration(
        null,
        null,
        null,
        null,
        identifier(methodName),
        formalParameterList());
    classNode.getMembers().add(methodNode);
    MethodElement methodElement = methodElement(methodName, null);
    methodNode.getName().setStaticElement(methodElement);
    ((ClassElementImpl) classNode.getElement()).setMethods(new MethodElement[] {methodElement});
    return methodNode;
  }
}
