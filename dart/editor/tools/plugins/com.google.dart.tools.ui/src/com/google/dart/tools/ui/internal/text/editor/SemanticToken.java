/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.SimpleIdentifier;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * Semantic token
 */
public final class SemanticToken {

  private DartNode nodeOld;
  private AstNode node;
  private IDocument document;

  /**
   * Attach {@link IDocument} to this token (in case the AST is insufficient).
   * 
   * @param the source
   */
  public void attachSource(IDocument document) {
    this.document = document;
  }

  /**
   * Clears this token.
   */
  public void clear() {
    nodeOld = null;
    document = null;
  }

  /**
   * @return the {@link ASNode}.
   */
  public AstNode getNode() {
    return node;
  }

  /**
   * @return the {@link SimpleIdentifier}.
   */
  public SimpleIdentifier getNodeIdentifier() {
    return (SimpleIdentifier) node;
  }

  /**
   * @return the {@link DartIdentifier}.
   */
  public DartIdentifier getNodeIdentifierOld() {
    return (DartIdentifier) nodeOld;
  }

  /**
   * @return the {@link DartNode}.
   */
  public DartNode getNodeOld() {
    return nodeOld;
  }

  /**
   * @return the source associated with this token
   */
  public String getSource() {
    if (node != null) {
      try {
        return document.get(node.getOffset(), node.getLength());
      } catch (BadLocationException e) {
        return null;
      }
    }
    return null;
  }

  /**
   * Update this token with the given AST node.
   * 
   * @param node the {@link AstNode}
   */
  public void update(AstNode node) {
    clear();
    this.node = node;
  }

  /**
   * Update this token with the given AST node.
   * <p>
   * NOTE: Allowed to be used by {@link SemanticHighlightingReconciler} only.
   * </p>
   * 
   * @param node the {@link DartNode}
   */
  void update(DartNode node) {
    clear();
    this.nodeOld = node;
  }
}
