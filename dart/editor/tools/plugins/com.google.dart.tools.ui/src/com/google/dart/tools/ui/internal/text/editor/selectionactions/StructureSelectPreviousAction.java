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
package com.google.dart.tools.ui.internal.text.editor.selectionactions;

import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.visitor.GeneralizingAstVisitor;
import com.google.dart.engine.services.util.SelectionAnalyzer;
import com.google.dart.engine.utilities.source.SourceRange;
import com.google.dart.tools.ui.internal.text.DartHelpContextIds;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.ui.PlatformUI;

import java.util.List;

public class StructureSelectPreviousAction extends StructureSelectionAction {

  private static class PreviousNodeAnalyzer extends GeneralizingAstVisitor<Void> {

    public static AstNode perform(int offset, AstNode lastCoveringNode) {
      PreviousNodeAnalyzer analyzer = new PreviousNodeAnalyzer(offset);
      lastCoveringNode.accept(analyzer);
      return analyzer.previousNode;
    }

    private int offset;
    private AstNode previousNode;

    private PreviousNodeAnalyzer(int offset) {
      super();
      this.offset = offset;
    }

    @Override
    public Void visitNode(AstNode node) {
      int start = node.getOffset();
      int end = start + node.getLength();
      if (end == offset) {
        previousNode = node;
        node.visitChildren(this);
      } else if (start < offset && offset < end) {
        node.visitChildren(this);
      }
      return null;
    }
  }

  private static AstNode getPreviousNode(AstNode parent, AstNode node) {
    List<AstNode> siblingNodes = StructureSelectionAction.getSiblingNodes(node);
    if (siblingNodes.size() == 0) {
      return parent;
    }
    if (node == siblingNodes.get(0)) {
      return parent;
    } else {
      int index = siblingNodes.indexOf(node);
      if (index < 1) {
        return parent;
      }
      return siblingNodes.get(index - 1);
    }
  }

  public StructureSelectPreviousAction(DartEditor editor, SelectionHistory history) {
    super(SelectionActionMessages.StructureSelectPrevious_label, editor, history);
    setToolTipText(SelectionActionMessages.StructureSelectPrevious_tooltip);
    setDescription(SelectionActionMessages.StructureSelectPrevious_description);
    try {
      PlatformUI.getWorkbench().getHelpSystem().setHelp(
          this,
          DartHelpContextIds.STRUCTURED_SELECT_ENCLOSING_ACTION);
    } catch (IllegalStateException ex) {
      // ignore workbench-not-created error thrown during testing
    }
  }

  @Override
  SourceRange internalGetNewSelectionRange(SourceRange oldSourceRange, AstNode node,
      SelectionAnalyzer selAnalyzer) {
    if (oldSourceRange.getLength() == 0 && selAnalyzer.getCoveringNode() != null) {
      AstNode previousNode = PreviousNodeAnalyzer.perform(
          oldSourceRange.getOffset(),
          selAnalyzer.getCoveringNode());
      if (previousNode != null) {
        return getSelectedNodeSourceRange(node, previousNode);
      }
    }
    AstNode first = selAnalyzer.getFirstSelectedNode();
    if (first == null) {
      return getLastCoveringNodeRange(oldSourceRange, node, selAnalyzer);
    }
    AstNode parent = first.getParent();
    if (parent == null) {
      return getLastCoveringNodeRange(oldSourceRange, node, selAnalyzer);
    }
    AstNode previousNode = getPreviousNode(parent, selAnalyzer.getFirstSelectedNode());
    if (previousNode == parent) {
      return getSelectedNodeSourceRange(node, parent);
    }
    int offset = previousNode.getOffset();
    int end = oldSourceRange.getOffset() + oldSourceRange.getLength();
    return StructureSelectionAction.createSourceRange(offset, end);
  }
}
