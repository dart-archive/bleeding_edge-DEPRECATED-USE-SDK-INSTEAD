/******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 ******************************************************************************/
package org.eclipse.wst.css.ui.internal.contentassist;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.internal.text.html.BrowserInformationControl;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.AbstractReusableInformationControlCreator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension3;
import org.eclipse.jface.text.contentassist.ICompletionProposalExtension5;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.wst.css.core.internal.metamodel.CSSMMNode;

public class CompletionProposal implements ICompletionProposal, ICompletionProposalExtension3,
    ICompletionProposalExtension5 {

  /** The string to be displayed in the completion proposal popup. */
  private String fDisplayString;
  /** The replacement string. */
  private String fReplacementString;
  /** The replacement offset. */
  private int fReplacementOffset;
  /** The replacement length. */
  private int fReplacementLength;
  /** The cursor position after this proposal has been applied. */
  private int fCursorPosition;
  /** The image to be displayed in the completion proposal popup. */
  private Image fImage;
  /** The context information of this proposal. */
  private IContextInformation fContextInformation;
  /** The additional info of this proposal. */
  private String fAdditionalProposalInfo;
  private CSSMMNode fNode;
  /** The information control creator */
  private IInformationControlCreator fCreator;

  /**
   * Creates a new completion proposal based on the provided information. The replacement string is
   * considered being the display string too. All remaining fields are set to <code>null</code>.
   * 
   * @param replacementString the actual string to be inserted into the document
   * @param replacementOffset the offset of the text to be replaced
   * @param replacementLength the length of the text to be replaced
   * @param cursorPosition the position of the cursor following the insert relative to
   *          replacementOffset
   */
  public CompletionProposal(String replacementString, int replacementOffset, int replacementLength,
      int cursorPosition) {
    this(replacementString, replacementOffset, replacementLength, cursorPosition, null, null, null,
        null);
  }

  /**
   * Creates a new completion proposal. All fields are initialized based on the provided
   * information.
   * 
   * @param replacementString the actual string to be inserted into the document
   * @param replacementOffset the offset of the text to be replaced
   * @param replacementLength the length of the text to be replaced
   * @param cursorPosition the position of the cursor following the insert relative to
   *          replacementOffset
   * @param image the image to display for this proposal
   * @param displayString the string to be displayed for the proposal
   * @param contextInformation the context information associated with this proposal
   * @param additionalProposalInfo the additional information associated with this proposal
   */
  public CompletionProposal(String replacementString, int replacementOffset, int replacementLength,
      int cursorPosition, Image image, String displayString,
      IContextInformation contextInformation, String additionalProposalInfo) {
    this(replacementString, replacementOffset, replacementLength, cursorPosition, null, null, null,
        null, null);
  }

  public CompletionProposal(String replacementString, int replacementOffset, int replacementLength,
      int cursorPosition, Image image, String displayString,
      IContextInformation contextInformation, String additionalProposalInfo, CSSMMNode node) {
    Assert.isNotNull(replacementString);
    Assert.isTrue(replacementOffset >= 0);
    Assert.isTrue(replacementLength >= 0);
    Assert.isTrue(cursorPosition >= 0);

    fReplacementString = replacementString;
    fReplacementOffset = replacementOffset;
    fReplacementLength = replacementLength;
    fCursorPosition = cursorPosition;
    fImage = image;
    fDisplayString = displayString;
    fContextInformation = contextInformation;
    fAdditionalProposalInfo = additionalProposalInfo;
    fNode = node;
  }

  /*
   * @see ICompletionProposal#apply(IDocument)
   */
  public void apply(IDocument document) {
    try {
      document.replace(fReplacementOffset, fReplacementLength, fReplacementString);
    } catch (BadLocationException x) {
      // ignore
    }
  }

  /*
   * @see ICompletionProposal#getSelection(IDocument)
   */
  public Point getSelection(IDocument document) {
    return new Point(fReplacementOffset + fCursorPosition, 0);
  }

  /*
   * @see ICompletionProposal#getContextInformation()
   */
  public IContextInformation getContextInformation() {
    return fContextInformation;
  }

  /*
   * @see ICompletionProposal#getImage()
   */
  public Image getImage() {
    return fImage;
  }

  /*
   * @see ICompletionProposal#getDisplayString()
   */
  public String getDisplayString() {
    if (fDisplayString != null)
      return fDisplayString;
    return fReplacementString;
  }

  /*
   * @see ICompletionProposal#getAdditionalProposalInfo()
   */
  public String getAdditionalProposalInfo() {
    return fAdditionalProposalInfo;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.text.contentassist.ICompletionProposalExtension5#getAdditionalProposalInfo
   * (org.eclipse.core.runtime.IProgressMonitor)
   */
  public Object getAdditionalProposalInfo(IProgressMonitor monitor) {
    return ProposalInfoFactory.getProposalInfo(fNode);
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.text.contentassist.ICompletionProposalExtension3#getInformationControlCreator
   * ()
   */
  public IInformationControlCreator getInformationControlCreator() {
    if (fCreator == null) {
      fCreator = new AbstractReusableInformationControlCreator() {

        protected IInformationControl doCreateInformationControl(Shell parent) {
          if (BrowserInformationControl.isAvailable(parent)) {
            BrowserInformationControl control = new BrowserInformationControl(parent,
                JFaceResources.DIALOG_FONT, false);
            return control;
          } else {
            return new DefaultInformationControl(parent, true);
          }
        }
      };
    }
    return fCreator;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.text.contentassist.ICompletionProposalExtension3#getPrefixCompletionText(
   * org.eclipse.jface.text.IDocument, int)
   */
  public CharSequence getPrefixCompletionText(IDocument document, int completionOffset) {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.text.contentassist.ICompletionProposalExtension3#getPrefixCompletionStart
   * (org.eclipse.jface.text.IDocument, int)
   */
  public int getPrefixCompletionStart(IDocument document, int completionOffset) {
    return 0;
  }

}
