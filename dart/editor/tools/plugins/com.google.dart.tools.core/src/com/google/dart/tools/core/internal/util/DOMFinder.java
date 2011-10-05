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
package com.google.dart.tools.core.internal.util;

import com.google.dart.compiler.ast.DartClass;
import com.google.dart.compiler.ast.DartField;
import com.google.dart.compiler.ast.DartFunction;
import com.google.dart.compiler.ast.DartMethodDefinition;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartNodeTraverser;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.compiler.common.Symbol;
import com.google.dart.tools.core.internal.model.SourceReferenceImpl;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.SourceReference;

/**
 * Instances of the class <code>DOMFinder</code> locate the {@link DartNode AST node} associated
 * with a {@link DartElement}.
 */
public class DOMFinder extends DartNodeTraverser<Void> {
  /**
   * The compilation unit containing the node being searched for.
   */
  private DartUnit ast;

  /**
   * The element corresponding to the node being searched for.
   */
  private SourceReferenceImpl element;

  /**
   * A flag indicating whether the compilation unit has been resolved and the symbol associated with
   * the node should be computed.
   */
  private boolean resolveBinding;

  /**
   * The starting location of the node being searched for.
   */
  private int rangeStart = -1;

  /**
   * The length of the node being searched for.
   */
  private int rangeLength = 0;

  /**
   * The node that was found, or <code>null</code> if no matching node was found.
   */
  private DartNode foundNode = null;

  /**
   * The binding information associated with the node that was found, or <code>null</code> if no
   * matching node was found.
   */
  private Symbol foundBinding = null;

  /**
   * Initialize a newly created node finder to find the node within the given compilation unit that
   * corresponds to the given element.
   * 
   * @param ast the compilation unit containing the node being searched for
   * @param element the element corresponding to the node being searched for
   * @param resolveBinding <code>true</code> if the compilation unit has been resolved and the
   *          symbol associated with the node should be computed
   */
  public DOMFinder(DartUnit ast, SourceReferenceImpl element, boolean resolveBinding) {
    this.ast = ast;
    this.element = element;
    this.resolveBinding = resolveBinding;
  }

  /**
   * Return the binding information associated with the node that was found, or <code>null</code> if
   * no matching node was found.
   * 
   * @return the binding information associated with the node that was found
   */
  public Symbol getFoundBinding() {
    return foundBinding;
  }

  /**
   * Return the node that was found, or <code>null</code> if no matching node was found.
   * 
   * @return the node that was found
   */
  public DartNode getFoundNode() {
    return foundNode;
  }

  public DartNode search() throws DartModelException {
    SourceRange range = null;
    if (element instanceof SourceReference) {
      range = ((SourceReference) element).getNameRange();
    } else {
      range = element.getSourceRange();
    }
    this.rangeStart = range.getOffset();
    this.rangeLength = range.getLength();
    this.ast.accept(this);
    return this.foundNode;
  }

  @Override
  public Void visitClass(DartClass node) {
    if (!found(node, node.getName())) {
      node.visitChildren(this);
    }
    return null;
  }

  @Override
  public Void visitField(DartField node) {
    if (!found(node, node.getName())) {
      node.visitChildren(this);
    }
    return null;
  }

  @Override
  public Void visitFunction(DartFunction node) {
    if (!found(node, node)) {
      node.visitChildren(this);
    }
    return null;
  }

  @Override
  public Void visitMethodDefinition(DartMethodDefinition node) {
    if (!found(node, node.getName())) {
      node.visitChildren(this);
    }
    return null;
  }

  private boolean found(DartNode node, DartNode name) {
    if (name.getSourceStart() == rangeStart && name.getSourceLength() == rangeLength) {
      foundNode = node;
      if (resolveBinding) {
        foundBinding = node.getSymbol();
      }
      return true;
    }
    return false;
  }
}
