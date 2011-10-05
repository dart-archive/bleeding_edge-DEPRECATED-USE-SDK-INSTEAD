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

public class StructureSelectPreviousAction extends StructureSelectionAction {

  private static class PreviousNodeAnalyzer extends GenericVisitor {
    public static DartNode perform(int offset, DartNode lastCoveringNode) {
      PreviousNodeAnalyzer analyzer = new PreviousNodeAnalyzer(offset);
      analyzer.accept(lastCoveringNode);
      return analyzer.fPreviousNode;
    }

    private final int fOffset;

    private DartNode fPreviousNode;

    private PreviousNodeAnalyzer(int offset) {
      super();
      fOffset = offset;
    }

    @Override
    protected boolean visitNode(DartNode node, DartContext ctx) {
      int start = node.getStartPosition();
      int end = start + node.getLength();
      if (end == fOffset) {
        fPreviousNode = node;
        return true;
      } else {
        return (start < fOffset && fOffset < end);
      }
    }
  }

  private static DartNode getPreviousNode(DartNode parent, DartNode node) {
    DartNode[] siblingNodes = StructureSelectionAction.getSiblingNodes(node);
    if (siblingNodes == null || siblingNodes.length == 0) {
      return parent;
    }
    if (node == siblingNodes[0]) {
      return parent;
    } else {
      int index = StructureSelectionAction.findIndex(siblingNodes, node);
      if (index < 1) {
        return parent;
      }
      return siblingNodes[index - 1];
    }
  }

  /*
   * This constructor is for testing purpose only.
   */
  public StructureSelectPreviousAction() {
  }

  public StructureSelectPreviousAction(DartEditor editor, SelectionHistory history) {
    super(SelectionActionMessages.StructureSelectPrevious_label, editor, history);
    setToolTipText(SelectionActionMessages.StructureSelectPrevious_tooltip);
    setDescription(SelectionActionMessages.StructureSelectPrevious_description);
    PlatformUI.getWorkbench().getHelpSystem().setHelp(this,
        IJavaHelpContextIds.STRUCTURED_SELECT_PREVIOUS_ACTION);
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
      DartNode previousNode = PreviousNodeAnalyzer.perform(oldSourceRange.getOffset(),
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

    DartNode previousNode = getPreviousNode(parent, selAnalyzer.getSelectedNodes()[0]);
    if (previousNode == parent) {
      return getSelectedNodeSourceRange(sr, parent);
    }

    int offset = previousNode.getStartPosition();
    int end = oldSourceRange.getOffset() + oldSourceRange.getLength() - 1;
    return StructureSelectionAction.createSourceRange(offset, end);
  }
}
