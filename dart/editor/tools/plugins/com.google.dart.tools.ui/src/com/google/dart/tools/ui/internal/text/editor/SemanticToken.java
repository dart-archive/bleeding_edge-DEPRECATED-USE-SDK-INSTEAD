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
import com.google.dart.compiler.common.SourceInfo;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/**
 * Semantic token
 */
public final class SemanticToken {

  private DartNode node;
  private IDocument document;

  /**
   * Attach source to this token (in case the AST is insufficient).
   * 
   * @param the source
   */
  public void attachSource(IDocument source) {
    this.document = source;
  }

  /**
   * @return the {@link DartNode}.
   */
  public DartNode getNode() {
    return node;
  }

  /**
   * @return the {@link DartIdentifier}.
   */
  public DartIdentifier getNodeIdentifier() {
    return (DartIdentifier) node;
  }

  /**
   * @return the source associated with this token
   */
  public String getSource() {
    SourceInfo sourceInfo = node.getSourceInfo();
    try {
      return document.get(sourceInfo.getOffset(), sourceInfo.getLength());
    } catch (BadLocationException e) {
      return null;
    }
  }

  /**
   * Clears this token.
   * <p>
   * NOTE: Allowed to be used by {@link SemanticHighlightingReconciler} only.
   * </p>
   */
  void clear() {
    node = null;
    document = null;
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
    this.node = node;
  }
}
