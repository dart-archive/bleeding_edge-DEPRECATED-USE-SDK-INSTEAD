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
import com.google.dart.engine.ast.ClassDeclaration;
import com.google.dart.engine.ast.FunctionDeclaration;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.MethodDeclaration;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.TypeName;
import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.ExecutableElement;
import com.google.dart.engine.source.Source;

/**
 * Instances of the class {@code ResolverVisitor} are used to resolve the nodes within a single
 * compilation unit.
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
   * The type element representing the type most recently being visited.
   */
  private ClassElement enclosingType = null;

  /**
   * The executable element representing the method or function most recently being visited.
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
    ClassElement outerType = enclosingType;
    try {
      enclosingType = node.getElement();
      typeAnalyzer.setThisType(enclosingType == null ? null : enclosingType.getType());
      super.visitClassDeclaration(node);
    } finally {
      typeAnalyzer.setThisType(outerType == null ? null : outerType.getType());
      enclosingType = outerType;
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
  public Void visitTypeName(TypeName node) {
    //
    // We don't visit type names or their children because they have already been resolved.
    //
    return null;
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
