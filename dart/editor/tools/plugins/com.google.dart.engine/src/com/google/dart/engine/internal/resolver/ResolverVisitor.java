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

import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.BreakStatement;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.Comment;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.CompilationUnitMember;
import com.google.dart.engine.ast.ConditionalExpression;
import com.google.dart.engine.ast.ConstructorFieldInitializer;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.ContinueStatement;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.FieldDeclaration;
import com.google.dart.engine.ast.ForStatement;
import com.google.dart.engine.ast.FunctionBody;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.IfStatement;
import com.google.dart.engine.ast.Label;
import com.google.dart.engine.ast.LibraryIdentifier;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.MethodInvocation;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.RedirectingConstructorInvocation;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.Statement;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.SwitchCase;
import com.google.dart.engine.ast.SwitchDefault;
import com.google.dart.engine.ast.TopLevelVariableDeclaration;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.ast.VariableDeclaration;
import com.google.dart.engine.ast.VariableDeclarationList;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.error.AnalysisErrorListener;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.type.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Instances of the class {@code ResolverVisitor} are used to resolve the nodes within a single
 * compilation unit.
 * 
 * @coverage dart.engine.resolver
 */
public class ResolverVisitor extends ScopedVisitor {
  /**
   * The object used to resolve the element associated with the current node.
   */
  private ElementResolver elementResolver;

  /**
   * The object used to compute the type associated with the current node.
   */
  private StaticTypeAnalyzer typeAnalyzer;

  /**
   * The class element representing the class containing the current node, or {@code null} if the
   * current node is not contained in a class.
   */
  private ClassElement enclosingClass = null;

  /**
   * The element representing the function containing the current node, or {@code null} if the
   * current node is not contained in a function.
   */
  private ExecutableElement enclosingFunction = null;

  /**
   * The object keeping track of which elements have had their types overridden.
   */
  private TypeOverrideManager overrideManager = new TypeOverrideManager();

  /**
   * Initialize a newly created visitor to resolve the nodes in a compilation unit.
   * 
   * @param library the library containing the compilation unit being resolved
   * @param source the source representing the compilation unit being visited
   * @param typeProvider the object used to access the types from the core library
   */
  public ResolverVisitor(Library library, Source source, TypeProvider typeProvider) {
    super(library, source, typeProvider);
    this.elementResolver = new ElementResolver(this);
    this.typeAnalyzer = new StaticTypeAnalyzer(this);
  }

  /**
   * Initialize a newly created visitor to resolve the nodes in a compilation unit.
   * 
   * @param definingLibrary the element for the library containing the compilation unit being
   *          visited
   * @param source the source representing the compilation unit being visited
   * @param typeProvider the object used to access the types from the core library
   * @param errorListener the error listener that will be informed of any errors that are found
   *          during resolution
   */
  public ResolverVisitor(LibraryElement definingLibrary, Source source, TypeProvider typeProvider,
      AnalysisErrorListener errorListener) {
    super(definingLibrary, source, typeProvider, errorListener);
    this.elementResolver = new ElementResolver(this);
    this.typeAnalyzer = new StaticTypeAnalyzer(this);
  }

  /**
   * Return the object keeping track of which elements have had their types overridden.
   * 
   * @return the object keeping track of which elements have had their types overridden
   */
  public TypeOverrideManager getOverrideManager() {
    return overrideManager;
  }

  @Override
  public Void visitBreakStatement(BreakStatement node) {
    //
    // We do not visit the label because it needs to be visited in the context of the statement.
    //
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
    return null;
  }

  @Override
  public Void visitClassDeclaration(ClassDeclaration node) {
    ClassElement outerType = enclosingClass;
    try {
      enclosingClass = node.getElement();
      typeAnalyzer.setThisType(enclosingClass == null ? null : enclosingClass.getType());
      super.visitClassDeclaration(node);
    } finally {
      typeAnalyzer.setThisType(outerType == null ? null : outerType.getType());
      enclosingClass = outerType;
    }
    return null;
  }

  @Override
  public Void visitComment(Comment node) {
    // TODO(jwren) Implement resolution of comments.
    //
    // We do not visit the comments as part of the ResolverVisitor as it requires a special scope.
    //
    return null;
  }

  @Override
  public Void visitCompilationUnit(CompilationUnit node) {
    try {
      overrideManager.enterScope();
      for (Directive directive : node.getDirectives()) {
        directive.accept(this);
      }
      ArrayList<CompilationUnitMember> classes = new ArrayList<CompilationUnitMember>();
      for (CompilationUnitMember declaration : node.getDeclarations()) {
        if (declaration instanceof ClassDeclaration) {
          classes.add(declaration);
        } else {
          declaration.accept(this);
        }
      }
      for (CompilationUnitMember declaration : classes) {
        declaration.accept(this);
      }
    } finally {
      overrideManager.exitScope();
    }
    return null;
  }

  @Override
  public Void visitConditionalExpression(ConditionalExpression node) {
//    BooleanConditionState conditionState;
//    try {
//      overrideManager.enterCondition();
    node.getCondition().accept(this);
//    } finally {
//      conditionState = overrideManager.exitCondition();
//    }
    try {
      overrideManager.enterScope();
//      conditionState.applyTrueState(conditionState);
      node.getThenExpression().accept(this);
    } finally {
      overrideManager.exitScope();
    }
    try {
      overrideManager.enterScope();
//      conditionState.applyFalseState(conditionState);
      node.getElseExpression().accept(this);
    } finally {
      overrideManager.exitScope();
    }
    return null;
  }

  @Override
  public Void visitConstructorFieldInitializer(ConstructorFieldInitializer node) {
    //
    // We visit the expression, but do not visit the field name because it needs to be visited in
    // the context of the constructor field initializer node.
    //
    safelyVisit(node.getExpression());
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
    return null;
  }

  @Override
  public Void visitConstructorName(ConstructorName node) {
    //
    // We do not visit either the type name, because it won't be visited anyway, or the name,
    // because it needs to be visited in the context of the constructor name.
    //
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
    return null;
  }

  @Override
  public Void visitContinueStatement(ContinueStatement node) {
    //
    // We do not visit the label because it needs to be visited in the context of the statement.
    //
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
    return null;
  }

  @Override
  public Void visitFieldDeclaration(FieldDeclaration node) {
    try {
      overrideManager.enterScope();
      super.visitFieldDeclaration(node);
    } finally {
      HashMap<Element, Type> overrides = captureOverrides(node.getFields());
      overrideManager.exitScope();
      for (Map.Entry<Element, Type> entry : overrides.entrySet()) {
        overrideManager.setType(entry.getKey(), entry.getValue());
      }
    }
    return null;
  }

  @Override
  public Void visitForStatement(ForStatement node) {
    try {
      overrideManager.enterScope();
      super.visitForStatement(node);
    } finally {
      overrideManager.exitScope();
    }
    return null;
  }

  @Override
  public Void visitFunctionBody(FunctionBody node) {
    try {
      overrideManager.enterScope();
      super.visitFunctionBody(node);
    } finally {
      overrideManager.exitScope();
    }
    return null;
  }

  @Override
  public Void visitFunctionDeclaration(FunctionDeclaration node) {
    ExecutableElement outerFunction = enclosingFunction;
    try {
      SimpleIdentifier functionName = node.getName();
      enclosingFunction = (ExecutableElement) functionName.getElement();
      super.visitFunctionDeclaration(node);
    } finally {
      enclosingFunction = outerFunction;
    }
    return null;
  }

  @Override
  public Void visitFunctionExpression(FunctionExpression node) {
    ExecutableElement outerFunction = enclosingFunction;
    try {
      enclosingFunction = node.getElement();
      overrideManager.enterScope();
      super.visitFunctionExpression(node);
    } finally {
      overrideManager.exitScope();
      enclosingFunction = outerFunction;
    }
    return null;
  }

  @Override
  public Void visitIfStatement(IfStatement node) {
//  BooleanConditionState conditionState;
//  try {
//    overrideManager.enterCondition();
    node.getCondition().accept(this);
//  } finally {
//    conditionState = overrideManager.exitCondition();
//  }
    Statement thenStatement = node.getThenStatement();
    if (thenStatement != null) {
      try {
        overrideManager.enterScope();
//      conditionState.applyTrueState(conditionState);
        thenStatement.accept(this);
      } finally {
        overrideManager.exitScope();
      }
    }
    Statement elseStatement = node.getElseStatement();
    if (elseStatement != null) {
      try {
        overrideManager.enterScope();
//      conditionState.applyFalseState(conditionState);
        elseStatement.accept(this);
      } finally {
        overrideManager.exitScope();
      }
    }
    return null;
  }

  @Override
  public Void visitLabel(Label node) {
    //
    // We don't visit labels or their children because they don't have a type. Instead, the element
    // resolver is responsible for resolving the labels in the context of their parent (either a
    // BreakStatement, ContinueStatement, or NamedExpression).
    //
    return null;
  }

  @Override
  public Void visitLibraryIdentifier(LibraryIdentifier node) {
    //
    // We don't visit library identifiers or their children because they have already been resolved.
    //
    return null;
  }

  @Override
  public Void visitMethodDeclaration(MethodDeclaration node) {
    ExecutableElement outerFunction = enclosingFunction;
    try {
      enclosingFunction = node.getElement();
      super.visitMethodDeclaration(node);
    } finally {
      enclosingFunction = outerFunction;
    }
    return null;
  }

  @Override
  public Void visitMethodInvocation(MethodInvocation node) {
    //
    // We visit the target and argument list, but do not visit the method name because it needs to
    // be visited in the context of the invocation.
    //
    safelyVisit(node.getTarget());
    safelyVisit(node.getArgumentList());
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
    return null;
  }

  @Override
  public Void visitNode(ASTNode node) {
    node.visitChildren(this);
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
    return null;
  }

  @Override
  public Void visitPrefixedIdentifier(PrefixedIdentifier node) {
    //
    // We visit the prefix, but do not visit the identifier because it needs to be visited in the
    // context of the prefix.
    //
    safelyVisit(node.getPrefix());
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
    return null;
  }

  @Override
  public Void visitPropertyAccess(PropertyAccess node) {
    //
    // We visit the target, but do not visit the property name because it needs to be visited in the
    // context of the property access node.
    //
    safelyVisit(node.getTarget());
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
    return null;
  }

  @Override
  public Void visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) {
    //
    // We visit the argument list, but do not visit the optional identifier because it needs to be
    // visited in the context of the constructor invocation.
    //
    safelyVisit(node.getArgumentList());
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
    return null;
  }

  @Override
  public Void visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    //
    // We visit the argument list, but do not visit the optional identifier because it needs to be
    // visited in the context of the constructor invocation.
    //
    safelyVisit(node.getArgumentList());
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
    return null;
  }

  @Override
  public Void visitSwitchCase(SwitchCase node) {
    try {
      overrideManager.enterScope();
      super.visitSwitchCase(node);
    } finally {
      overrideManager.exitScope();
    }
    return null;
  }

  @Override
  public Void visitSwitchDefault(SwitchDefault node) {
    try {
      overrideManager.enterScope();
      super.visitSwitchDefault(node);
    } finally {
      overrideManager.exitScope();
    }
    return null;
  }

  @Override
  public Void visitTopLevelVariableDeclaration(TopLevelVariableDeclaration node) {
    try {
      overrideManager.enterScope();
      super.visitTopLevelVariableDeclaration(node);
    } finally {
      HashMap<Element, Type> overrides = captureOverrides(node.getVariables());
      overrideManager.exitScope();
      for (Map.Entry<Element, Type> entry : overrides.entrySet()) {
        overrideManager.setType(entry.getKey(), entry.getValue());
      }
    }
    return null;
  }

  @Override
  public Void visitTypeName(TypeName node) {
    //
    // We don't visit type names or their children because they have already been resolved.
    //
    return null;
  }

  /**
   * Return the class element representing the class containing the current node, or {@code null} if
   * the current node is not contained in a class.
   * 
   * @return the class element representing the class containing the current node
   */
  protected ClassElement getEnclosingClass() {
    return enclosingClass;
  }

  /**
   * Return the element representing the function containing the current node, or {@code null} if
   * the current node is not contained in a function.
   * 
   * @return the element representing the function containing the current node
   */
  protected ExecutableElement getEnclosingFunction() {
    return enclosingFunction;
  }

  /**
   * Return a map from the elements for the variables in the given list that have their types
   * overridden to the overriding type.
   * 
   * @param variableList the list of variables whose overriding types are to be captured
   * @return a table mapping elements to their overriding types
   */
  private HashMap<Element, Type> captureOverrides(VariableDeclarationList variableList) {
    HashMap<Element, Type> overrides = new HashMap<Element, Type>();
    if (variableList.isConst() || variableList.isFinal()) {
      for (VariableDeclaration variable : variableList.getVariables()) {
        Element element = variable.getElement();
        if (element != null) {
          Type type = overrideManager.getType(element);
          if (type != null) {
            overrides.put(element, type);
          }
        }
      }
    }
    return overrides;
  }

  /**
   * Visit the given AST node if it is not null.
   * 
   * @param node the node to be visited
   */
  private void safelyVisit(ASTNode node) {
    if (node != null) {
      node.accept(this);
    }
  }
}
