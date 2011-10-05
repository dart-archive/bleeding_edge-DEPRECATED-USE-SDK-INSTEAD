/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.dom.visitor;

import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFunctionExpression;
import com.google.dart.compiler.ast.DartFunctionTypeAlias;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartLabel;
import com.google.dart.compiler.ast.DartMapLiteralEntry;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartNodeTraverser;
import com.google.dart.compiler.ast.DartParameter;
import com.google.dart.compiler.ast.DartPlainVisitor;
import com.google.dart.compiler.ast.DartTypeParameter;
import com.google.dart.compiler.ast.DartVariable;

/**
 * Instances of the class <code>ChildVisitor</code> use a specified visitor to visit all of the
 * children of the node being visited by the <code>ChildVisitor</code>. Typically, that visitor will
 * then use the child visitor to visit all of the children of those nodes. This class is needed
 * because several nodes do not visit all of their children as part of the
 * {@link DartNode#visitChildren(DartPlainVisitor)} method.
 */
public class ChildVisitor<R> extends DartNodeTraverser<Void> {
  /**
   * The visitor used to visit the children of the node being visited by this visitor.
   */
  private DartPlainVisitor<R> baseVisitor;

  /**
   * Initialize a newly created visitor to use the given visitor to visit all of the children of
   * whichever node is being visited by this visitor.
   * 
   * @param baseVisitor the visitor used to visit the children of the node being visited by this
   *          visitor
   */
  public ChildVisitor(DartPlainVisitor<R> baseVisitor) {
    this.baseVisitor = baseVisitor;
  }

  @Override
  public Void visitClass(DartClass node) {
    node.getName().accept(baseVisitor);
    node.visitChildren(baseVisitor);
    return null;
  }

  @Override
  public Void visitField(DartField node) {
    node.getName().accept(baseVisitor);
    node.visitChildren(baseVisitor);
    return null;
  }

  @Override
  public Void visitFunctionExpression(DartFunctionExpression node) {
    DartIdentifier name = node.getName();
    if (name != null) {
      name.accept(baseVisitor);
    }
    node.visitChildren(baseVisitor);
    return null;
  }

  @Override
  public Void visitFunctionTypeAlias(DartFunctionTypeAlias node) {
    node.getName().accept(baseVisitor);
    node.visitChildren(baseVisitor);
    return null;
  }

  @Override
  public Void visitLabel(DartLabel node) {
    node.getLabel().accept(baseVisitor);
    node.visitChildren(baseVisitor);
    return null;
  }

  @Override
  public Void visitMapLiteralEntry(DartMapLiteralEntry node) {
    node.visitChildren(baseVisitor);
    return null;
  }

  @Override
  public Void visitMethodDefinition(DartMethodDefinition node) {
    node.getName().accept(baseVisitor);
    node.visitChildren(baseVisitor);
    return null;
  }

  @Override
  public Void visitNode(DartNode node) {
    node.visitChildren(baseVisitor);
    return null;
  }

  @Override
  public Void visitParameter(DartParameter node) {
    node.getName().accept(baseVisitor);
    node.visitChildren(baseVisitor);
    return null;
  }

  @Override
  public Void visitTypeParameter(DartTypeParameter node) {
    node.getName().accept(baseVisitor);
    node.visitChildren(baseVisitor);
    return null;
  }

  @Override
  public Void visitVariable(DartVariable node) {
    node.getName().accept(baseVisitor);
    node.visitChildren(baseVisitor);
    return null;
  }
}
