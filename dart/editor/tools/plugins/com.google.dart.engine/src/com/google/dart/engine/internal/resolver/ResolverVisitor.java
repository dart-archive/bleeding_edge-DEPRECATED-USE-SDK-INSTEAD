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
import com.google.dart.engine.ast.ArgumentList;
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.Label;
import com.google.dart.engine.ast.LibraryIdentifier;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.PrefixedIdentifier;
import com.google.dart.engine.ast.PropertyAccess;
import com.google.dart.engine.ast.RedirectingConstructorInvocation;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.source.Source;

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
      super.visitFunctionExpression(node);
    } finally {
      enclosingFunction = outerFunction;
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
    SimpleIdentifier prefix = node.getPrefix();
    if (prefix != null) {
      prefix.accept(this);
    }
    // TODO(brianwilkerson) Re-write the AST structure if the prefix did not resolve to a PrefixElement.
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
    Expression target = node.getTarget();
    if (target != null) {
      target.accept(this);
    }
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
    ArgumentList argumentList = node.getArgumentList();
    if (argumentList != null) {
      argumentList.accept(this);
    }
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
    ArgumentList argumentList = node.getArgumentList();
    if (argumentList != null) {
      argumentList.accept(this);
    }
    node.accept(elementResolver);
    node.accept(typeAnalyzer);
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
}
