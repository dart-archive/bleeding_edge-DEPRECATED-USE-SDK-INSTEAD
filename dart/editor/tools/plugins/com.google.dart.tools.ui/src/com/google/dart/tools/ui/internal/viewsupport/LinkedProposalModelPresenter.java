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
package com.google.dart.tools.ui.internal.viewsupport;

import com.google.dart.tools.internal.corext.fix.LinkedProposalModel;
import com.google.dart.tools.internal.corext.fix.LinkedProposalPositionGroup;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUI;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.EditorHighlightingSynchronizer;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposal;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.ILinkedModeListener;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedModeUI.ExitFlags;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.jface.text.link.ProposalPosition;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.text.edits.MalformedTreeException;
import org.eclipse.text.edits.TextEdit;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;

import java.util.Iterator;

/**
 * Does the setup of the linked mode from a {@link LinkedProposalModel}
 */
public class LinkedProposalModelPresenter {

  private static class LinkedModeExitPolicy implements LinkedModeUI.IExitPolicy {
    @Override
    public ExitFlags doExit(LinkedModeModel model, VerifyEvent event, int offset, int length) {
      if (event.character == '=') {
        return new ExitFlags(ILinkedModeListener.EXIT_ALL, true);
      }
      return null;
    }
  }

  private static class LinkedPositionProposalImpl implements ICompletionProposalExtension2,
      IDartCompletionProposal {

    private final LinkedProposalPositionGroup.Proposal fProposal;
    private final LinkedModeModel fLinkedPositionModel;

    public LinkedPositionProposalImpl(LinkedProposalPositionGroup.Proposal proposal,
        LinkedModeModel model) {
      fProposal = proposal;
      fLinkedPositionModel = model;
    }

    @Override
    public void apply(IDocument document) {
      // not called
    }

    @Override
    public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
      IDocument doc = viewer.getDocument();
      LinkedPosition position = fLinkedPositionModel.findPosition(new LinkedPosition(doc, offset, 0));
      if (position != null) {
        try {
          try {
            TextEdit edit = fProposal.computeEdits(
                offset,
                position,
                trigger,
                stateMask,
                fLinkedPositionModel);
            if (edit != null) {
              edit.apply(position.getDocument(), 0);
            }
          } catch (MalformedTreeException e) {
            throw new CoreException(new Status(
                IStatus.ERROR,
                DartUI.ID_PLUGIN,
                IStatus.ERROR,
                "Unexpected exception applying edit", e)); //$NON-NLS-1$
          } catch (BadLocationException e) {
            throw new CoreException(new Status(
                IStatus.ERROR,
                DartUI.ID_PLUGIN,
                IStatus.ERROR,
                "Unexpected exception applying edit", e)); //$NON-NLS-1$
          }
        } catch (CoreException e) {
          DartToolsPlugin.log(e);
        }
      }
    }

    @Override
    public String getAdditionalProposalInfo() {
      return fProposal.getAdditionalProposalInfo();
    }

    @Override
    public IContextInformation getContextInformation() {
      return null;
    }

    @Override
    public String getDisplayString() {
      return fProposal.getDisplayString();
    }

    @Override
    public Image getImage() {
      return fProposal.getImage();
    }

    @Override
    public int getRelevance() {
      return fProposal.getRelevance();
    }

    @Override
    public Point getSelection(IDocument document) {
      return null;
    }

    @Override
    public void selected(ITextViewer viewer, boolean smartToggle) {
    }

    @Override
    public void unselected(ITextViewer viewer) {
    }

    @Override
    public boolean validate(IDocument document, int offset, DocumentEvent event) {
      // ignore event
      String insert = getDisplayString();

      int off;
      LinkedPosition pos = fLinkedPositionModel.findPosition(new LinkedPosition(document, offset, 0));
      if (pos != null) {
        off = pos.getOffset();
      } else {
        off = Math.max(0, offset - insert.length());
      }
      int length = offset - off;

      if (offset <= document.getLength()) {
        try {
          String content = document.get(off, length);
          if (insert.startsWith(content)) {
            return true;
          }
        } catch (BadLocationException e) {
          DartToolsPlugin.log(e);
          // and ignore and return false
        }
      }
      return false;
    }
  }

  public LinkedProposalModelPresenter() {
  }

  public void enterLinkedMode(
      ITextViewer viewer,
      IEditorPart editor,
      boolean switchedEditor,
      LinkedProposalModel linkedProposalModel) throws BadLocationException {
    IDocument document = viewer.getDocument();

    LinkedModeModel model = new LinkedModeModel();
    boolean added = false;

    Iterator<LinkedProposalPositionGroup> iterator = linkedProposalModel.getPositionGroupIterator();
    while (iterator.hasNext()) {
      LinkedProposalPositionGroup curr = iterator.next();

      LinkedPositionGroup group = new LinkedPositionGroup();

      LinkedProposalPositionGroup.PositionInformation[] positions = curr.getPositions();
      if (positions.length > 0) {
        LinkedProposalPositionGroup.Proposal[] linkedModeProposals = curr.getProposals();
        if (linkedModeProposals.length <= 1) {
          for (int i = 0; i < positions.length; i++) {
            LinkedProposalPositionGroup.PositionInformation pos = positions[i];
            if (pos.getOffset() != -1) {
              group.addPosition(new LinkedPosition(
                  document,
                  pos.getOffset(),
                  pos.getLength(),
                  pos.getSequenceRank()));
            }
          }
        } else {
          LinkedPositionProposalImpl[] proposalImpls = new LinkedPositionProposalImpl[linkedModeProposals.length];
          for (int i = 0; i < linkedModeProposals.length; i++) {
            proposalImpls[i] = new LinkedPositionProposalImpl(linkedModeProposals[i], model);
          }

          for (int i = 0; i < positions.length; i++) {
            LinkedProposalPositionGroup.PositionInformation pos = positions[i];
            if (pos.getOffset() != -1) {
              group.addPosition(new ProposalPosition(
                  document,
                  pos.getOffset(),
                  pos.getLength(),
                  pos.getSequenceRank(),
                  proposalImpls));
            }
          }
        }
        model.addGroup(group);
        added = true;
      }
    }

    model.forceInstall();

    if (editor instanceof DartEditor) {
      model.addLinkingListener(new EditorHighlightingSynchronizer((DartEditor) editor));
    }

    if (added) { // only set up UI if there are any positions set
      LinkedModeUI ui = new EditorLinkedModeUI(model, viewer);
      LinkedProposalPositionGroup.PositionInformation endPosition = linkedProposalModel.getEndPosition();
      if (endPosition != null && endPosition.getOffset() != -1) {
        ui.setExitPosition(
            viewer,
            endPosition.getOffset() + endPosition.getLength(),
            0,
            Integer.MAX_VALUE);
      } else if (!switchedEditor) {
        int cursorPosition = viewer.getSelectedRange().x;
        if (cursorPosition != 0) {
          ui.setExitPosition(viewer, cursorPosition, 0, Integer.MAX_VALUE);
        }
      }
      ui.setExitPolicy(new LinkedModeExitPolicy());
      ui.enter();

      IRegion region = ui.getSelectedRegion();
      viewer.setSelectedRange(region.getOffset(), region.getLength());
      viewer.revealRange(region.getOffset(), region.getLength());
    }
  }

}
