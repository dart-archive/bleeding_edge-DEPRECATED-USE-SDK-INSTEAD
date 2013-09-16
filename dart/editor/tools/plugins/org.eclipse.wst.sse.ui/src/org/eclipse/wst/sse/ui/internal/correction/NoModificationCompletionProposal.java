/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.correction;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.wst.sse.ui.internal.SSEUIMessages;

public class NoModificationCompletionProposal implements ICompletionProposal {

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.text.contentassist.ICompletionProposal#apply(org.eclipse.jface.text.IDocument
   * )
   */
  public void apply(IDocument document) {
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getAdditionalProposalInfo()
   */
  public String getAdditionalProposalInfo() {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getContextInformation()
   */
  public IContextInformation getContextInformation() {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getDisplayString()
   */
  public String getDisplayString() {
    return SSEUIMessages.NoModificationCompletionProposal_0; //$NON-NLS-1$
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.text.contentassist.ICompletionProposal#getImage()
   */
  public Image getImage() {
    return null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.text.contentassist.ICompletionProposal#getSelection(org.eclipse.jface.text
   * .IDocument)
   */
  public Point getSelection(IDocument document) {
    return null;
  }

}
