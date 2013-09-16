/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.spelling;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.ui.texteditor.spelling.SpellingProblem;

/**
 * Spelling correction processor used to show quick fixes for spelling problems.
 * 
 * @since 2.0
 */
class SpellingQuickAssistProcessor implements IQuickAssistProcessor {
  private SpellingProblem fSpellingProblem = null;

  public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
    return false;
  }

  public boolean canFix(Annotation annotation) {
    return !annotation.isMarkedDeleted();
  }

  public ICompletionProposal[] computeQuickAssistProposals(
      IQuickAssistInvocationContext quickAssistContext) {
    ICompletionProposal[] proposals = null;

    if (fSpellingProblem != null) {
      proposals = fSpellingProblem.getProposals(quickAssistContext);
    }
    return proposals;
  }

  public String getErrorMessage() {
    return null;
  }

  void setSpellingProblem(SpellingProblem spellingProblem) {
    fSpellingProblem = spellingProblem;
  }
}
