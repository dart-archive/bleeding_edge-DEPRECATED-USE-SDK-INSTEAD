/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.contentassist;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.link.LinkedModeModel;
import org.eclipse.jface.text.link.LinkedModeUI;
import org.eclipse.jface.text.link.LinkedPosition;
import org.eclipse.jface.text.link.LinkedPositionGroup;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.texteditor.link.EditorLinkedModeUI;
import org.eclipse.wst.sse.ui.internal.contentassist.CustomCompletionProposal;
import org.eclipse.wst.xml.ui.internal.Logger;

/**
 * A completion proposal that is capable of establishing linked positions within inserted markup.
 */
public class MarkupCompletionProposal extends CustomCompletionProposal {

  private IRegion fSelectedRegion = null;

  public MarkupCompletionProposal(String replacementString, int replacementOffset,
      int replacementLength, int cursorPosition, Image image, String displayString,
      IContextInformation contextInformation, String additionalProposalInfo, int relevance) {
    super(replacementString, replacementOffset, replacementLength, cursorPosition, image,
        displayString, contextInformation, additionalProposalInfo, relevance);
  }

  public MarkupCompletionProposal(String replacementString, int replacementOffset,
      int replacementLength, int cursorPosition, Image image, String displayString,
      IContextInformation contextInformation, String additionalProposalInfo, int relevance,
      boolean updateReplacementLengthOnValidate) {
    super(replacementString, replacementOffset, replacementLength, cursorPosition, image,
        displayString, null, contextInformation, additionalProposalInfo, relevance,
        updateReplacementLengthOnValidate);
  }

  public MarkupCompletionProposal(String replacementString, int replacementOffset,
      int replacementLength, int cursorPosition, Image image, String displayString,
      String alternateMatch, IContextInformation contextInformation, String additionalProposalInfo,
      int relevance, boolean updateReplacementLengthOnValidate) {
    super(replacementString, replacementOffset, replacementLength, cursorPosition, image,
        displayString, alternateMatch, contextInformation, additionalProposalInfo, relevance,
        updateReplacementLengthOnValidate);
  }

  public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
    super.apply(viewer, trigger, stateMask, offset);

    getLinkedPositions(viewer);
  }

  /**
   * Sets up linked positions and installs them on the viewer.
   */
  protected void getLinkedPositions(ITextViewer viewer) {
    final String replacement = getReplacementString();
    final IDocument document = viewer.getDocument();
    final int length = replacement.length();
    boolean inAttribute = false, hasGroup = false;
    int offset = 0;
    char attType = 0;
    int exitPosition = -1;
    LinkedModeModel model = new LinkedModeModel();

    try {
      for (int i = 0; i < length; i++) {
        final char c = replacement.charAt(i);
        switch (c) {
          case '=':
            break;
          case '\'':
          case '\"':
            if (!inAttribute) {
              offset = i;
              attType = c;
              inAttribute = true;
            } else {
              // Found matching quotes establishing an attribute value region
              if (attType == c && replacement.charAt(i - 1) != '\\') {
                inAttribute = false; // Record position length
                addPosition(model, document, getReplacementOffset() + offset + 1, i - offset - 1);
                hasGroup = true;
              }
            }
            break;
          case '>':
            if (!inAttribute && exitPosition == -1) {
              exitPosition = getReplacementOffset() + i + 1;
            }
            break;
        }
      }
      if (hasGroup) {
        model.forceInstall();
        final LinkedModeUI ui = new EditorLinkedModeUI(model, viewer);
        ui.setExitPosition(viewer, exitPosition < 0 ? getReplacementOffset()
            + getReplacementLength() + replacement.length() - 1 : exitPosition, 0,
            Integer.MAX_VALUE);
        ui.setCyclingMode(LinkedModeUI.CYCLE_WHEN_NO_PARENT);
        ui.setDoContextInfo(true);
        ui.enter();
        fSelectedRegion = ui.getSelectedRegion();
      }
    } catch (BadLocationException e) {
      Logger.logException(e);
    }
  }

  /**
   * Adds a {@link LinkedPosition} to its own position group. This group is then added to the model
   * 
   * @param model the linked model for this proposal
   * @param document the document the content assist is operating upon
   * @param offset the offset to establish the {@link LinkedPosition}
   * @param length the length of the {@link LinkedPosition}
   * @throws BadLocationException
   */
  private void addPosition(LinkedModeModel model, IDocument document, int offset, int length)
      throws BadLocationException {
    final LinkedPositionGroup group = new LinkedPositionGroup();
    group.addPosition(new LinkedPosition(document, offset, length, LinkedPositionGroup.NO_STOP));
    model.addGroup(group);
  }

  public Point getSelection(IDocument document) {
    // Attempt to return the selection based on the selected region from the LinkedModeUI
    if (fSelectedRegion == null)
      return super.getSelection(document);
    return new Point(fSelectedRegion.getOffset(), fSelectedRegion.getLength());
  }
}
