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
package com.google.dart.tools.ui.internal.text.editor.selectionactions;

import com.google.dart.compiler.ast.DartContext;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.GenericVisitor;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.model.SourceReference;
import com.google.dart.tools.ui.internal.text.IJavaHelpContextIds;
import com.google.dart.tools.ui.internal.text.SelectionAnalyzer;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.ui.PlatformUI;

public class StructureSelectNextAction extends StructureSelectionAction {

  private static class NextNodeAnalyzer extends GenericVisitor {
    public static DartNode perform(int offset, DartNode lastCoveringNode) {
      NextNodeAnalyzer analyzer = new NextNodeAnalyzer(offset);
      analyzer.accept(lastCoveringNode);
      return analyzer.fNextNode;
    }

    private final int fOffset;

    private DartNode fNextNode;

    private NextNodeAnalyzer(int offset) {
      super();
      fOffset = offset;
    }

    @Override
    protected boolean visitNode(DartNode node, DartContext ctx) {
      int start = node.getStartPosition();
      int end = start + node.getLength();
      if (start == fOffset) {
        fNextNode = node;
        return true;
      } else {
        return (start < fOffset && fOffset < end);
      }
    }
  }

  private static DartNode getNextNode(DartNode parent, DartNode node) {
    DartNode[] siblingNodes = StructureSelectionAction.getSiblingNodes(node);
    if (siblingNodes == null || siblingNodes.length == 0) {
      return parent;
    }
    if (node == siblingNodes[siblingNodes.length - 1]) {
      return parent;
    } else {
      return siblingNodes[StructureSelectionAction.findIndex(siblingNodes, node) + 1];
    }
  }

  /*
   * This constructor is for testing purpose only.
   */
  public StructureSelectNextAction() {
  }

  public StructureSelectNextAction(DartEditor editor, SelectionHistory history) {
    super(SelectionActionMessages.StructureSelectNext_label, editor, history);
    setToolTipText(SelectionActionMessages.StructureSelectNext_tooltip);
    setDescription(SelectionActionMessages.StructureSelectNext_description);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
        IJavaHelpContextIds.STRUCTURED_SELECT_NEXT_ACTION);
  }

  /*
   * non java doc
   * 
   * @see StructureSelectionAction#internalGetNewSelectionRange(SourceRange, CompilationUnit,
   * SelectionAnalyzer)
   */
  @Override
  SourceRange internalGetNewSelectionRange(SourceRange oldSourceRange, SourceReference sr,
      SelectionAnalyzer selAnalyzer) throws DartModelException {
    if (oldSourceRange.getLength() == 0 && selAnalyzer.getLastCoveringNode() != null) {
      DartNode previousNode = NextNodeAnalyzer.perform(oldSourceRange.getOffset(),
          selAnalyzer.getLastCoveringNode());
      if (previousNode != null) {
        return getSelectedNodeSourceRange(sr, previousNode);
      }
    }
    DartNode first = selAnalyzer.getFirstSelectedNode();
    if (first == null) {
      return getLastCoveringNodeRange(oldSourceRange, sr, selAnalyzer);
    }

    DartNode parent = first.getParent();
    if (parent == null) {
      return getLastCoveringNodeRange(oldSourceRange, sr, selAnalyzer);
    }

    DartNode lastSelectedNode = selAnalyzer.getSelectedNodes()[selAnalyzer.getSelectedNodes().length - 1];
    DartNode nextNode = getNextNode(parent, lastSelectedNode);
    if (nextNode == parent) {
      return getSelectedNodeSourceRange(sr, first.getParent());
    }
    int offset = oldSourceRange.getOffset();
    int end = Math.min(sr.getSourceRange().getLength(),
        nextNode.getStartPosition() + nextNode.getLength() - 1);
    return StructureSelectionAction.createSourceRange(offset, end);
  }
}
