/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.core.dom.IBinding;

/**
 * Semantic token
 */
public final class SemanticToken {

  /** AST node */
  private DartIdentifier fNode;
  private DartExpression fLiteral;

  /** Binding */
  private IBinding fBinding;
  /** Is the binding resolved? */
  private boolean fIsBindingResolved = false;

  /** AST root */
  private DartUnit fRoot;
  private boolean fIsRootResolved = false;

  /**
   * @return Returns the binding, can be <code>null</code>.
   */
  public IBinding getBinding() {
    if (!fIsBindingResolved) {
      fIsBindingResolved = true;
      //TODO (pquitslund): implement/remove DartIdentifier.resolveBinding()
      //likely by replacing resolveBinding() with getTargetSymbol() and to replace IBinding with Element
//      if (fNode != null)
//        fBinding = fNode.resolveBinding();
    }

    return fBinding;
  }

  /**
   * @return the AST node (a <code>Boolean-, Character- or NumberLiteral</code>)
   */
  public DartExpression getLiteral() {
    return fLiteral;
  }

  /**
   * @return the AST node (a {@link SimpleName})
   */
  public DartIdentifier getNode() {
    return fNode;
  }

  /**
   * @return the AST root
   */
  public DartUnit getRoot() {
    if (!fIsRootResolved) {
      fIsRootResolved = true;
      fRoot = (DartUnit) (fNode != null ? fNode : fLiteral).getRoot();
    }

    return fRoot;
  }

  /**
   * Clears this token.
   * <p>
   * NOTE: Allowed to be used by {@link SemanticHighlightingReconciler} only.
   * </p>
   */
  void clear() {
    fNode = null;
    fLiteral = null;
    fBinding = null;
    fIsBindingResolved = false;
    fRoot = null;
    fIsRootResolved = false;
  }

  /**
   * Update this token with the given AST node.
   * <p>
   * NOTE: Allowed to be used by {@link SemanticHighlightingReconciler} only.
   * </p>
   * 
   * @param literal the AST literal
   */
  void update(DartExpression literal) {
    clear();
    fLiteral = literal;
  }

  /**
   * Update this token with the given AST node.
   * <p>
   * NOTE: Allowed to be used by {@link SemanticHighlightingReconciler} only.
   * </p>
   * 
   * @param node the AST simple name
   */
  void update(DartIdentifier node) {
    clear();
    fNode = node;
  }
}
