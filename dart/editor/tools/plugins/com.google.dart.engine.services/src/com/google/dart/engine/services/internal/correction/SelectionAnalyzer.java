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
package com.google.dart.engine.services.internal.correction;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.dart.engine.ast.ASTNode;
import com.google.dart.engine.ast.visitor.GeneralizingASTVisitor;
import com.google.dart.engine.utilities.source.SourceRange;

import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeNode;
import static com.google.dart.engine.utilities.source.SourceRangeFactory.rangeStartEnd;

import java.util.List;

/**
 * Abstract visitor for visiting {@link ASTNode}s covered by the selection {@link SourceRange}.
 */
public class SelectionAnalyzer extends GeneralizingASTVisitor<Void> {
  protected final SourceRange selection;
  private ASTNode coveringNode;
  private List<ASTNode> selectedNodes;

  public SelectionAnalyzer(SourceRange selection) {
    assert selection != null;
    this.selection = selection;
  }

  /**
   * @return the {@link ASTNode} with the shortest length which completely covers the specified
   *         selection.
   */
  public ASTNode getCoveringNode() {
    return coveringNode;
  }

  /**
   * @return the first selected {@link ASTNode}, may be <code>null</code>.
   */
  public ASTNode getFirstSelectedNode() {
    if (selectedNodes == null || selectedNodes.isEmpty()) {
      return null;
    }
    return selectedNodes.get(0);
  }

  /**
   * @return the last selected {@link ASTNode}, may be <code>null</code>.
   */
  public ASTNode getLastSelectedNode() {
    if (selectedNodes == null || selectedNodes.isEmpty()) {
      return null;
    }
    return selectedNodes.get(selectedNodes.size() - 1);
  }

  /**
   * @return the {@link SourceRange} which covers selected {@link ASTNode}s, may be
   *         <code>null</code> if no {@link ASTNode}s under selection.
   */
  public SourceRange getSelectedNodeRange() {
    if (selectedNodes == null || selectedNodes.isEmpty()) {
      return null;
    }
    ASTNode firstNode = selectedNodes.get(0);
    ASTNode lastNode = selectedNodes.get(selectedNodes.size() - 1);
    return rangeStartEnd(firstNode, lastNode);
  }

  /**
   * @return the {@link ASTNode}s fully covered by the selection {@link SourceRange}.
   */
  public List<ASTNode> getSelectedNodes() {
    if (selectedNodes == null || selectedNodes.isEmpty()) {
      return ImmutableList.of();
    }
    return selectedNodes;
  }

  /**
   * @return <code>true</code> if there are {@link ASTNode} fully covered by the selection
   *         {@link SourceRange}.
   */
  public boolean hasSelectedNodes() {
    return selectedNodes != null && !selectedNodes.isEmpty();
  }

  @Override
  public Void visitNode(ASTNode node) {
    SourceRange nodeRange = rangeNode(node);
    if (selection.covers(nodeRange)) {
      if (isFirstNode()) {
        handleFirstSelectedNode(node);
      } else {
        handleNextSelectedNode(node);
      }
      return null;
    } else if (selection.coveredBy(nodeRange)) {
      coveringNode = node;
      node.visitChildren(this);
      return null;
    } else if (selection.startsIn(nodeRange)) {
      handleSelectionStartsIn(node);
      node.visitChildren(this);
      return null;
    } else if (selection.endsIn(nodeRange)) {
      handleSelectionEndsIn(node);
      node.visitChildren(this);
      return null;
    }
    // no intersection
    return null;
  }

  /**
   * Adds first selected {@link ASTNode}.
   */
  protected void handleFirstSelectedNode(ASTNode node) {
    selectedNodes = Lists.newArrayList();
    selectedNodes.add(node);
  }

  /**
   * Adds second or more selected {@link ASTNode}.
   */
  protected void handleNextSelectedNode(ASTNode node) {
    if (getFirstSelectedNode().getParent() == node.getParent()) {
      selectedNodes.add(node);
    }
  }

  /**
   * Notifies that selection ends in given {@link ASTNode}.
   */
  protected void handleSelectionEndsIn(ASTNode node) {
  }

  /**
   * Notifies that selection starts in given {@link ASTNode}.
   */
  protected void handleSelectionStartsIn(ASTNode node) {
  }

  /**
   * Resets selected nodes.
   */
  protected void reset() {
    selectedNodes = null;
  }

  /**
   * @return <code>true</code> if there was no selected nodes yet.
   */
  private boolean isFirstNode() {
    return selectedNodes == null;
  }
}
