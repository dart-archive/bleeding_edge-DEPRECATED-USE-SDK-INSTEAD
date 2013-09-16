/*******************************************************************************
 * Copyright (c) 2001, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.contentassist;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.contentassist.CompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension2;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.wst.sse.core.internal.util.Debug;

/**
 * An implementation of ICompletionProposal whose values can be read after creation.
 */
public class CustomCompletionProposal implements ICompletionProposal, ICompletionProposalExtension,
    ICompletionProposalExtension2, IRelevanceCompletionProposal {

  private String fAdditionalProposalInfo;

  private IContextInformation fContextInformation;

  private int fCursorPosition = 0;

  private String fDisplayString;

  private String fAlternateMatch;

  private Image fImage;

  private int fRelevance = IRelevanceConstants.R_NONE;

  private int fReplacementLength = 0;

  private int fReplacementOffset = 0;

  private String fReplacementString = null;

  private boolean fUpdateLengthOnValidate;

  private char[] fTriggers;

  /**
   * Constructor with relevance and replacement length update flag. If the
   * <code>updateReplacementLengthOnValidate</code> flag is true, then when the user types, the
   * replacement length will be incremented by the number of new characters inserted from the
   * original position. Otherwise the replacement length will not change on validate. ex. <tag
   * |name="attr"> - the replacement length is 4 <tag i|name="attr"> - the replacement length is now
   * 5 <tag id|name="attr"> - the replacement length is now 6 <tag |name="attr"> - the
   * replacementlength is now 4 again <tag |name="attr"> - the replacment length remains 4
   */
  public CustomCompletionProposal(String replacementString, int replacementOffset,
      int replacementLength, int cursorPosition, Image image, String displayString,
      IContextInformation contextInformation, String additionalProposalInfo, int relevance,
      boolean updateReplacementLengthOnValidate) {
    this(replacementString, replacementOffset, replacementLength, cursorPosition, image,
        displayString, null, contextInformation, additionalProposalInfo, relevance,
        updateReplacementLengthOnValidate);
  }

  public CustomCompletionProposal(String replacementString, int replacementOffset,
      int replacementLength, int cursorPosition, Image image, String displayString,
      IContextInformation contextInformation, String additionalProposalInfo, int relevance) {
    this(replacementString, replacementOffset, replacementLength, cursorPosition, image,
        displayString, contextInformation, additionalProposalInfo, relevance, true);
  }

  public CustomCompletionProposal(String replacementString, int replacementOffset,
      int replacementLength, int cursorPosition, Image image, String displayString,
      String alternateMatch, IContextInformation contextInformation, String additionalProposalInfo,
      int relevance, boolean updateReplacementLengthOnValidate) {
    fReplacementString = replacementString;
    fReplacementOffset = replacementOffset;
    fReplacementLength = replacementLength;
    fCursorPosition = cursorPosition;
    fImage = image;
    fDisplayString = displayString;
    fAlternateMatch = alternateMatch;
    fContextInformation = contextInformation;
    fAdditionalProposalInfo = additionalProposalInfo;
    fRelevance = relevance;
    fUpdateLengthOnValidate = updateReplacementLengthOnValidate;
  }

  public void apply(IDocument document) {
    CompletionProposal proposal = new CompletionProposal(getReplacementString(),
        getReplacementOffset(), getReplacementLength(), getCursorPosition(), getImage(),
        getDisplayString(), getContextInformation(), getAdditionalProposalInfo());
    proposal.apply(document);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.text.contentassist.ICompletionProposalExtension#apply(org.eclipse.jface.text
   * .IDocument, char, int)
   */
  public void apply(IDocument document, char trigger, int offset) {
    CompletionProposal proposal = new CompletionProposal(getReplacementString(),
        getReplacementOffset(), getReplacementLength(), getCursorPosition(), getImage(),
        getDisplayString(), getContextInformation(), getAdditionalProposalInfo());
    // we currently don't do anything special for which character
    // selected the proposal, and where the cursor offset is
    // but we might in the future...
    proposal.apply(document);
    // we want to ContextInformationPresenter.updatePresentation() here
  }

  public void apply(ITextViewer viewer, char trigger, int stateMask, int offset) {
    IDocument document = viewer.getDocument();
    // CMVC 252634 to compensate for "invisible" initial region
    int caretOffset = viewer.getTextWidget().getCaretOffset();
    if (viewer instanceof ITextViewerExtension5) {
      ITextViewerExtension5 extension = (ITextViewerExtension5) viewer;
      caretOffset = extension.widgetOffset2ModelOffset(caretOffset);
    } else {
      caretOffset = viewer.getTextWidget().getCaretOffset() + viewer.getVisibleRegion().getOffset();
    }

    if (caretOffset == getReplacementOffset()) {
      apply(document);
    } else {
      // replace the text without affecting the caret Position as this
      // causes the cursor to move on its own
      try {
        int endOffsetOfChanges = getReplacementString().length() + getReplacementOffset();
        // Insert the portion of the new text that comes after the
        // current caret position
        if (endOffsetOfChanges >= caretOffset) {
          int postCaretReplacementLength = getReplacementOffset() + getReplacementLength()
              - caretOffset;
          int preCaretReplacementLength = getReplacementString().length()
              - (endOffsetOfChanges - caretOffset);
          if (postCaretReplacementLength < 0) {
            if (Debug.displayWarnings) {
              System.out.println("** postCaretReplacementLength was negative: " + postCaretReplacementLength); //$NON-NLS-1$
            }
            // This is just a quick fix while I figure out what
            // replacement length is supposed to be
            // in each case, otherwise we'll get negative
            // replacment length sometimes
            postCaretReplacementLength = 0;
          }
          document.replace(caretOffset, postCaretReplacementLength,
              getReplacementString().substring(preCaretReplacementLength));
        }
        // Insert the portion of the new text that comes before the
        // current caret position
        // Done second since offsets would change for the post text
        // otherwise
        // Outright insertions are handled here
        if (caretOffset > getReplacementOffset()) {
          int preCaretTextLength = caretOffset - getReplacementOffset();
          document.replace(getReplacementOffset(), preCaretTextLength,
              getReplacementString().substring(0, preCaretTextLength));
        }
      } catch (BadLocationException x) {
        apply(document);
      } catch (StringIndexOutOfBoundsException e) {
        apply(document);
      }
    }
  }

  public String getAdditionalProposalInfo() {
    // return fProposal.getAdditionalProposalInfo();
    return fAdditionalProposalInfo;
  }

  public IContextInformation getContextInformation() {
    // return fProposal.getContextInformation();
    return fContextInformation;
  }

  public void setContextInformation(IContextInformation contextInfo) {
    fContextInformation = contextInfo;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.text.contentassist.ICompletionProposalExtension#getContextInformationPosition
   * ()
   */
  public int getContextInformationPosition() {
    // https://bugs.eclipse.org/bugs/show_bug.cgi?id=110355
    // return getCursorPosition();
    if (getContextInformation() == null)
      return getReplacementOffset() - 1;
    return getReplacementOffset() + getCursorPosition();
  }

  public int getCursorPosition() {
    return fCursorPosition;
  }

  public void setCursorPosition(int pos) {
    fCursorPosition = pos;
  }

  public void setDisplayString(String newDisplayString) {
    fDisplayString = newDisplayString;
  }

  public String getDisplayString() {
    // return fProposal.getDisplayString();
    return fDisplayString;
  }

  public Image getImage() {
    // return fProposal.getImage();
    return fImage;
  }

  public int getRelevance() {
    return fRelevance;
  }

  public void setReplacementLength(int newReplacementLength) {
    fReplacementLength = newReplacementLength;
  }

  public int getReplacementLength() {
    return fReplacementLength;
  }

  public int getReplacementOffset() {
    return fReplacementOffset;
  }

  public String getReplacementString() {
    return fReplacementString;
  }

  public Point getSelection(IDocument document) {
    // return fProposal.getSelection(document);
    CompletionProposal proposal = new CompletionProposal(getReplacementString(),
        getReplacementOffset(), getReplacementLength(), getCursorPosition(), getImage(),
        getDisplayString(), getContextInformation(), getAdditionalProposalInfo());
    return proposal.getSelection(document);
  }

  /**
   * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension#getTriggerCharacters()
   */

  public char[] getTriggerCharacters() {
    return fTriggers;
  }

  public void setTriggerCharacters(char[] triggers) {
    fTriggers = triggers;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.text.contentassist.ICompletionProposalExtension#isValidFor(org.eclipse.jface
   * .text.IDocument, int)
   */
  public boolean isValidFor(IDocument document, int offset) {
    return validate(document, offset, null);
  }

  /**
   * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#selected(org.eclipse.jface.text.ITextViewer,
   *      boolean)
   */
  public void selected(ITextViewer viewer, boolean smartToggle) {
  }

  // code is borrowed from JavaCompletionProposal
  protected boolean startsWith(IDocument document, int offset, String word) {

    int wordLength = word == null ? 0 : word.length();
    if (offset > fReplacementOffset + wordLength)
      return false;

    try {
      int length = offset - fReplacementOffset;
      String start = document.get(fReplacementOffset, length);

      return (word != null && word.substring(0, length).equalsIgnoreCase(start))
          || (fAlternateMatch != null && length <= fAlternateMatch.length() && fAlternateMatch.substring(
              0, length).equalsIgnoreCase(start));
    } catch (BadLocationException x) {
    }

    return false;
  }

  /**
   * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#unselected(org.eclipse.jface.text.ITextViewer)
   */
  public void unselected(ITextViewer viewer) {
  }

  /**
   * borrowed from JavaCompletionProposal
   * 
   * @see org.eclipse.jface.text.contentassist.ICompletionProposalExtension2#validate(org.eclipse.jface.text.IDocument,
   *      int, org.eclipse.jface.text.DocumentEvent)
   */
  public boolean validate(IDocument document, int offset, DocumentEvent event) {
    if (offset < fReplacementOffset)
      return false;
    boolean validated = startsWith(document, offset, fDisplayString);

    if (fUpdateLengthOnValidate && event != null) {
      fReplacementLength += event.fText.length() - event.fLength; //adjust the replacement length by the event's text replacement
    }
    return validated;
  }

  /**
   * @param replacementOffset The fReplacementOffset to set.
   */
  public void setReplacementOffset(int replacementOffset) {
    fReplacementOffset = replacementOffset;
  }

  /**
   * @param replacementString The fReplacementString to set.
   */
  public void setReplacementString(String replacementString) {
    fReplacementString = replacementString;
  }
}
