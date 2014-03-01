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
package com.google.dart.engine.internal.context;

import com.google.dart.engine.ast.AssignmentExpression;
import com.google.dart.engine.ast.BinaryExpression;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.ast.ConstructorDeclaration;
import com.google.dart.engine.ast.ConstructorName;
import com.google.dart.engine.ast.Directive;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.ast.FunctionExpression;
import com.google.dart.engine.ast.FunctionExpressionInvocation;
import com.google.dart.engine.ast.IndexExpression;
import com.google.dart.engine.ast.InstanceCreationExpression;
import com.google.dart.engine.ast.PostfixExpression;
import com.google.dart.engine.ast.PrefixExpression;
import com.google.dart.engine.ast.RedirectingConstructorInvocation;
import com.google.dart.engine.ast.SimpleIdentifier;
import com.google.dart.engine.ast.SuperConstructorInvocation;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;

/**
 * Instances of the class {@code ResolutionEraser} remove any resolution information from an AST
 * structure when used to visit that structure.
 */
public class ResolutionEraser extends GeneralizingAstVisitor<Void> {
  @Override
  public Void visitAssignmentExpression(AssignmentExpression node) {
    node.setStaticElement(null);
    node.setPropagatedElement(null);
    return super.visitAssignmentExpression(node);
  }

  @Override
  public Void visitBinaryExpression(BinaryExpression node) {
    node.setStaticElement(null);
    node.setPropagatedElement(null);
    return super.visitBinaryExpression(node);
  }

  @Override
  public Void visitCompilationUnit(CompilationUnit node) {
    node.setElement(null);
    return super.visitCompilationUnit(node);
  }

  @Override
  public Void visitConstructorDeclaration(ConstructorDeclaration node) {
    node.setElement(null);
    return super.visitConstructorDeclaration(node);
  }

  @Override
  public Void visitConstructorName(ConstructorName node) {
    node.setStaticElement(null);
    return super.visitConstructorName(node);
  }

  @Override
  public Void visitDirective(Directive node) {
    node.setElement(null);
    return super.visitDirective(node);
  }

  @Override
  public Void visitExpression(Expression node) {
    node.setStaticType(null);
    node.setPropagatedType(null);
    return super.visitExpression(node);
  }

  @Override
  public Void visitFunctionExpression(FunctionExpression node) {
    node.setElement(null);
    return super.visitFunctionExpression(node);
  }

  @Override
  public Void visitFunctionExpressionInvocation(FunctionExpressionInvocation node) {
    node.setStaticElement(null);
    node.setPropagatedElement(null);
    return super.visitFunctionExpressionInvocation(node);
  }

  @Override
  public Void visitIndexExpression(IndexExpression node) {
    node.setStaticElement(null);
    node.setPropagatedElement(null);
    return super.visitIndexExpression(node);
  }

  @Override
  public Void visitInstanceCreationExpression(InstanceCreationExpression node) {
    node.setStaticElement(null);
    return super.visitInstanceCreationExpression(node);
  }

  @Override
  public Void visitPostfixExpression(PostfixExpression node) {
    node.setStaticElement(null);
    node.setPropagatedElement(null);
    return super.visitPostfixExpression(node);
  }

  @Override
  public Void visitPrefixExpression(PrefixExpression node) {
    node.setStaticElement(null);
    node.setPropagatedElement(null);
    return super.visitPrefixExpression(node);
  }

  @Override
  public Void visitRedirectingConstructorInvocation(RedirectingConstructorInvocation node) {
    node.setStaticElement(null);
    return super.visitRedirectingConstructorInvocation(node);
  }

  @Override
  public Void visitSimpleIdentifier(SimpleIdentifier node) {
    node.setStaticElement(null);
    node.setPropagatedElement(null);
    return super.visitSimpleIdentifier(node);
  }

  @Override
  public Void visitSuperConstructorInvocation(SuperConstructorInvocation node) {
    node.setStaticElement(null);
    return super.visitSuperConstructorInvocation(node);
  }
}
