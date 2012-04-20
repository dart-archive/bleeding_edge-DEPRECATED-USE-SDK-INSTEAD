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
package com.google.dart.tools.ui.internal.text.correction.proposals;

import com.google.dart.compiler.ast.DartIdentifier;
import com.google.dart.compiler.ast.DartNode;
import com.google.dart.compiler.ast.DartUnit;
import com.google.dart.tools.core.dom.NodeFinder;
import com.google.dart.tools.internal.corext.dom.LinkedNodeFinder;
import com.google.dart.tools.internal.corext.refactoring.util.Messages;
import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.correction.AssistContext;
import com.google.dart.tools.ui.internal.text.correction.CorrectionCommandHandler;
import com.google.dart.tools.ui.internal.text.correction.CorrectionMessages;
import com.google.dart.tools.ui.internal.text.correction.ICommandAccess;
import com.google.dart.tools.ui.internal.text.editor.ASTProvider;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.EditorHighlightingSynchronizer;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;
import com.google.dart.tools.ui.text.dart.IInvocationContext;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension6;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.jface.text.link.LinkedModeUI.IExitPolicy;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

import java.util.Arrays;
import java.util.Comparator;

/**
 * A template proposal.
 */
public class LinkedNamesAssistProposal implements IDartCompletionProposal,
    ICompletionProposalExtension2, ICompletionProposalExtension6, ICommandAccess {

  /**
   * An exit policy that skips Backspace and Delete at the beginning and at the end of a linked
   * position, respectively. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=183925 .
   */
  public static class DeleteBlockingExitPolicy implements IExitPolicy {
    private final IDocument fDocument;

    public DeleteBlockingExitPolicy(IDocument document) {
      fDocument = document;
    }

    @Override
    public ExitFlags doExit(LinkedModeModel model, VerifyEvent event, int offset, int length) {
      if (length == 0 && (event.character == SWT.BS || event.character == SWT.DEL)) {
        LinkedPosition position = model.findPosition(new LinkedPosition(fDocument, offset, 0,
            LinkedPositionGroup.NO_STOP));
        if (position != null) {
          if (event.character == SWT.BS) {
            if (offset - 1 < position.getOffset()) {
              //skip backspace at beginning of linked position
              event.doit = false;
            }
          } else /* event.character == SWT.DEL */{
            if (offset + 1 > position.getOffset() + position.getLength()) {
              //skip delete at end of linked position
              event.doit = false;
            }
          }
        }
      }

      return null; // don't change behavior
    }
  }

  public static final String ASSIST_ID = "org.eclipse.jdt.ui.correction.renameInFile.assist"; //$NON-NLS-1$

  private final DartIdentifier fNode;
  private final IInvocationContext fContext;
  private final String fLabel;
  private final String fValueSuggestion;
  private int fRelevance;

  public LinkedNamesAssistProposal(IInvocationContext context, DartIdentifier node) {
    this(CorrectionMessages.LinkedNamesAssistProposal_description, context, node, null);
  }

  public LinkedNamesAssistProposal(String label, IInvocationContext context, DartIdentifier node,
      String valueSuggestion) {
    fLabel = label;
    fNode = node;
    fContext = context;
    fValueSuggestion = valueSuggestion;
    fRelevance = 8;
  }

  @Override
  public void apply(IDocument document) {
    // can't do anything
  }

  @Override
  public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
    try {
      Point seletion = viewer.getSelectedRange();

      // get full ast
      DartUnit root = ASTProvider.getASTProvider().getAST(fContext.getCompilationUnit(),
          ASTProvider.WAIT_YES, null);

      DartNode nameNode = NodeFinder.perform(root, fNode.getSourceInfo().getOffset(),
          fNode.getSourceInfo().getLength());
      final int pos = fNode.getSourceInfo().getOffset();

      DartNode[] sameNodes;
      if (nameNode instanceof DartIdentifier) {
        sameNodes = LinkedNodeFinder.findByNode(root, (DartIdentifier) nameNode);
      } else {
        sameNodes = new DartNode[] {nameNode};
      }

      // sort for iteration order, starting with the node @ offset
      Arrays.sort(sameNodes, new Comparator<DartNode>() {

        @Override
        public int compare(DartNode o1, DartNode o2) {
          return rank(o1) - rank(o2);
        }

        /**
         * Returns the absolute rank of an <code>DartNode</code>. Nodes preceding
         * <code>offset</code> are ranked last.
         * 
         * @param node the node to compute the rank for
         * @return the rank of the node with respect to the invocation offset
         */
        private int rank(DartNode node) {
          int relativeRank = node.getSourceInfo().getOffset() + node.getSourceInfo().getLength()
              - pos;
          if (relativeRank < 0) {
            return Integer.MAX_VALUE + relativeRank;
          } else {
            return relativeRank;
          }
        }

      });

      IDocument document = viewer.getDocument();
      LinkedPositionGroup group = new LinkedPositionGroup();
      for (int i = 0; i < sameNodes.length; i++) {
        DartNode elem = sameNodes[i];
        group.addPosition(new LinkedPosition(document, elem.getSourceInfo().getOffset(),
            elem.getSourceInfo().getLength(), i));
      }

      LinkedModeModel model = new LinkedModeModel();
      model.addGroup(group);
      model.forceInstall();
      if (fContext instanceof AssistContext) {
        IEditorPart editor = ((AssistContext) fContext).getEditor();
        if (editor instanceof DartEditor) {
          model.addLinkingListener(new EditorHighlightingSynchronizer((DartEditor) editor));
        }
      }

      LinkedModeUI ui = new EditorLinkedModeUI(model, viewer);
      ui.setExitPolicy(new DeleteBlockingExitPolicy(document));
      ui.setExitPosition(viewer, offset, 0, LinkedPositionGroup.NO_STOP);
      ui.enter();

      if (fValueSuggestion != null) {
        document.replace(nameNode.getSourceInfo().getOffset(),
            nameNode.getSourceInfo().getLength(), fValueSuggestion);
        IRegion selectedRegion = ui.getSelectedRegion();
        seletion = new Point(selectedRegion.getOffset(), fValueSuggestion.length());
      }

      viewer.setSelectedRange(seletion.x, seletion.y); // by default full word is selected, restore original selection

    } catch (BadLocationException e) {
      DartToolsPlugin.log(e);
    }
  }

  @Override
  public String getAdditionalProposalInfo() {
    return CorrectionMessages.LinkedNamesAssistProposal_proposalinfo;
  }

  @Override
  public String getCommandId() {
    return ASSIST_ID;
  }

  @Override
  public IContextInformation getContextInformation() {
    return null;
  }

  @Override
  public String getDisplayString() {
    String shortCutString = CorrectionCommandHandler.getShortCutString(getCommandId());
    if (shortCutString != null) {
      return Messages.format(CorrectionMessages.ChangeCorrectionProposal_name_with_shortcut,
          new String[] {fLabel, shortCutString});
    }
    return fLabel;
  }

  @Override
  public Image getImage() {
    return DartPluginImages.get(DartPluginImages.IMG_CORRECTION_LINKED_RENAME);
  }

  @Override
  public int getRelevance() {
    return fRelevance;
  }

  @Override
  public Point getSelection(IDocument document) {
    return null;
  }

  @Override
  public StyledString getStyledDisplayString() {
    StyledString str = new StyledString(fLabel);

    String shortCutString = CorrectionCommandHandler.getShortCutString(getCommandId());
    if (shortCutString != null) {
      String decorated = Messages.format(
          CorrectionMessages.ChangeCorrectionProposal_name_with_shortcut, new String[] {
              fLabel, shortCutString});
      return StyledCellLabelProvider.styleDecoratedString(decorated, StyledString.QUALIFIER_STYLER,
          str);
    }
    return str;
  }

  @Override
  public void selected(ITextViewer textViewer, boolean smartToggle) {
  }

  public void setRelevance(int relevance) {
    fRelevance = relevance;
  }

  @Override
  public void unselected(ITextViewer textViewer) {
  }

  @Override
  public boolean validate(IDocument document, int offset, DocumentEvent event) {
    return false;
  }
}
