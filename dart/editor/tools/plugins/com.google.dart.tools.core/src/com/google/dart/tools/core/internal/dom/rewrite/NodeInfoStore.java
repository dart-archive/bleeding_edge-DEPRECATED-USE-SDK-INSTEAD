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
package com.google.dart.tools.core.internal.dom.rewrite;

import com.google.dart.compiler.ast.DartBlock;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartTryStatement;
import com.google.dart.compiler.ast.DartVariable;
import com.google.dart.compiler.ast.DartVariableStatement;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.dom.AST;
import com.google.dart.tools.core.internal.dom.rewrite.RewriteEventStore.CopySourceInfo;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Instances of the class <code>NodeInfoStore</code> store information about AST nodes.
 */
public class NodeInfoStore {
  protected static final class CopyPlaceholderData extends PlaceholderData {
    public CopySourceInfo copySource;

    @Override
    public String toString() {
      return "[placeholder " + copySource + "]"; //$NON-NLS-1$//$NON-NLS-2$
    }
  }

  protected static final class StringPlaceholderData extends PlaceholderData {
    public String code;

    @Override
    public String toString() {
      return "[placeholder string: " + code + "]"; //$NON-NLS-1$ //$NON-NLS-2$
    }
  }
  static class PlaceholderData {
    // base class
  }

  private AST ast;

  private Map<DartNode, PlaceholderData> placeholderNodes;

  private Set<DartBlock> collapsedNodes;

  public NodeInfoStore(AST ast) {
    super();
    this.ast = ast;
    placeholderNodes = null;
    collapsedNodes = null;
  }

  /**
   *
   */
  public void clear() {
    placeholderNodes = null;
    collapsedNodes = null;
  }

  // collapsed nodes: in source: use one node that represents many; to be used
  // as
  // copy/move source or to replace at once.
  // in the target: one block node that is not flattened.

  public DartBlock createCollapsePlaceholder() {
    DartBlock placeHolder = ast.newBlock();
    if (collapsedNodes == null) {
      collapsedNodes = new HashSet<DartBlock>();
    }
    collapsedNodes.add(placeHolder);
    return placeHolder;
  }

  public Object getPlaceholderData(DartNode node) {
    if (placeholderNodes != null) {
      return placeholderNodes.get(node);
    }
    return null;
  }

  public boolean isCollapsed(DartNode node) {
    if (collapsedNodes != null) {
      return collapsedNodes.contains(node);
    }
    return false;
  }

  /**
   * Marks a node as a copy or move target. The copy target represents a copied node at the target
   * (copied) site.
   * 
   * @param target The node at the target site. Can be a placeholder node but also the source node
   *          itself.
   * @param copySource The info at the source site.
   */
  public final void markAsCopyTarget(DartNode target, CopySourceInfo copySource) {
    CopyPlaceholderData data = new CopyPlaceholderData();
    data.copySource = copySource;
    setPlaceholderData(target, data);
  }

  /**
   * Marks a node as a placehoder for a plain string content. The type of the node should correspond
   * to the code's code content.
   * 
   * @param placeholder The placeholder node that acts for the string content.
   * @param code The string content.
   */
  public final void markAsStringPlaceholder(DartNode placeholder, String code) {
    StringPlaceholderData data = new StringPlaceholderData();
    data.code = code;
    setPlaceholderData(placeholder, data);
  }

  /**
   * Creates a placeholder node of the given type, or <code>null</code> if the type is not
   * supported.
   * 
   * @param nodeType the type of the node to create.
   * @return a place holder node
   */
  public final DartNode newPlaceholderNode(Class<? extends DartNode> nodeClass) {
    DartNode node = ast.createInstance(nodeClass);
    if (node instanceof DartVariableStatement) {
      ((DartVariableStatement) node).getVariables().add(ast.createInstance(DartVariable.class));
    } else if (node instanceof DartTryStatement) {
      // have to set at least a finally block to be legal code
      DartCore.notYetImplemented();
      // ((DartTryStatement) node).setFinally(ast.newBlock());
    }
    return node;
  }

  private void setPlaceholderData(DartNode node, PlaceholderData data) {
    if (placeholderNodes == null) {
      placeholderNodes = new IdentityHashMap<DartNode, PlaceholderData>();
    }
    placeholderNodes.put(node, data);
  }
}
