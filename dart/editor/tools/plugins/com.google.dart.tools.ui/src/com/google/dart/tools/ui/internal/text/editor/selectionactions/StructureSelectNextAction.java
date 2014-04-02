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

public class StructureSelectNextAction extends StructureSelectionAction {

  private static class NextNodeAnalyzer extends GeneralizingAstVisitor<Void> {

    public static AstNode perform(int offset, AstNode lastCoveringNode) {
      NextNodeAnalyzer analyzer = new NextNodeAnalyzer(offset);
      lastCoveringNode.accept(analyzer);
      return analyzer.nextNode;
    }

    private int offset;
    private AstNode nextNode;

    private NextNodeAnalyzer(int offset) {
      super();
      this.offset = offset;
    }

    @Override
    public Void visitNode(AstNode node) {
      int start = node.getOffset();
      int end = start + node.getLength();
      if (start == offset) {
        nextNode = node;
        super.visitNode(node);
      } else if (start < offset && offset < end) {
        super.visitNode(node);
      }
      return null;
    }
  }

  private static AstNode getNextNode(AstNode parent, AstNode node) {
    List<AstNode> siblingNodes = StructureSelectionAction.getSiblingNodes(node);
    if (siblingNodes.size() == 0) {
      return parent;
    }
    if (node == siblingNodes.get(siblingNodes.size() - 1)) {
      return parent;
    } else {
      return siblingNodes.get(siblingNodes.indexOf(node) + 1);
    }
  }

  public StructureSelectNextAction(DartEditor editor, SelectionHistory history) {
    super(SelectionActionMessages.StructureSelectNext_label, editor, history);
    setToolTipText(SelectionActionMessages.StructureSelectNext_tooltip);
    setDescription(SelectionActionMessages.StructureSelectNext_description);
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
      AstNode previousNode = NextNodeAnalyzer.perform(
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
    AstNode lastSelectedNode = selAnalyzer.getLastSelectedNode();
    AstNode nextNode = getNextNode(parent, lastSelectedNode);
    if (nextNode == parent) {
      return getSelectedNodeSourceRange(node, first.getParent());
    }
    int offset = oldSourceRange.getOffset();
    int end = Math.min(node.getLength(), nextNode.getOffset() + nextNode.getLength());
    return StructureSelectionAction.createSourceRange(offset, end);
  }
}
