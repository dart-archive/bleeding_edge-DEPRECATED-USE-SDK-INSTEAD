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
package com.google.dart.tools.ui.internal.text;

import com.google.dart.compiler.ast.DartExpression;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartNodeTraverser;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

import java.util.ArrayList;
import java.util.List;

/**
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 */
public class SelectionAnalyzer extends DartNodeTraverser<Void> {

  private Selection fSelection;
  private boolean fTraverseSelectedNode;
  private DartNode fLastCoveringNode;

  // Selected nodes
  private List<DartNode> fSelectedNodes;

  public SelectionAnalyzer(Selection selection, boolean traverseSelectedNode) {
    Assert.isNotNull(selection);
    fSelection = selection;
    fTraverseSelectedNode = traverseSelectedNode;
  }

  public DartNode getFirstSelectedNode() {
    if (fSelectedNodes == null || fSelectedNodes.isEmpty()) {
      return null;
    }
    return fSelectedNodes.get(0);
  }

  public DartNode getLastCoveringNode() {
    return fLastCoveringNode;
  }

  public DartNode getLastSelectedNode() {
    if (fSelectedNodes == null || fSelectedNodes.isEmpty()) {
      return null;
    }
    return fSelectedNodes.get(fSelectedNodes.size() - 1);
  }

  public IRegion getSelectedNodeRange() {
    if (fSelectedNodes == null || fSelectedNodes.isEmpty()) {
      return null;
    }
    DartNode firstNode = fSelectedNodes.get(0);
    DartNode lastNode = fSelectedNodes.get(fSelectedNodes.size() - 1);
    int start = firstNode.getSourceStart();
    return new Region(start, lastNode.getSourceStart() + lastNode.getSourceLength() - start);
  }

  public DartNode[] getSelectedNodes() {
    if (fSelectedNodes == null || fSelectedNodes.isEmpty()) {
      return new DartNode[0];
    }
    return fSelectedNodes.toArray(new DartNode[fSelectedNodes.size()]);
  }

  public boolean hasSelectedNodes() {
    return fSelectedNodes != null && !fSelectedNodes.isEmpty();
  }

  public boolean isExpressionSelected() {
    if (!hasSelectedNodes()) {
      return false;
    }
    return fSelectedNodes.get(0) instanceof DartExpression;
  }

  @Override
  public Void visitNode(DartNode node) {
    // The selection lies behind the node.
    if (fSelection.liesOutside(node)) {
      return null;
    } else if (fSelection.covers(node)) {
      if (isFirstNode()) {
        handleFirstSelectedNode(node);
      } else {
        handleNextSelectedNode(node);
      }
      if (fTraverseSelectedNode) {
        node.visitChildren(this);
      }
      return null;
    } else if (fSelection.coveredBy(node)) {
      fLastCoveringNode = node;
      node.visitChildren(this);
      return null;
    } else if (fSelection.endsIn(node)) {
      if (handleSelectionEndsIn(node)) {
        node.visitChildren(this);
      }
      return null;
    }
    // There is a possibility that the user has selected trailing semicolons that don't belong to
    // the statement. So dive into it to check if sub nodes are fully covered.
    node.visitChildren(this);
    return null;
  }

  protected Selection getSelection() {
    return fSelection;
  }

  protected void handleFirstSelectedNode(DartNode node) {
    fSelectedNodes = new ArrayList<DartNode>(5);
    fSelectedNodes.add(node);
  }

  protected void handleNextSelectedNode(DartNode node) {
    if (getFirstSelectedNode().getParent() == node.getParent()) {
      fSelectedNodes.add(node);
    }
  }

  protected boolean handleSelectionEndsIn(DartNode node) {
    return false;
  }

  protected List<DartNode> internalGetSelectedNodes() {
    return fSelectedNodes;
  }

  protected void reset() {
    fSelectedNodes = null;
  }

  private boolean isFirstNode() {
    return fSelectedNodes == null;
  }
}
