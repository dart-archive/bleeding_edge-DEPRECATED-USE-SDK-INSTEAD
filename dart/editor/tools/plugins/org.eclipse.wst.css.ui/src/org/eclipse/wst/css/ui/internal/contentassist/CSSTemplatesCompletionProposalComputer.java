/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.contentassist;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.wst.css.ui.internal.templates.TemplateContextTypeIdsCSS;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer;
import org.eclipse.wst.sse.ui.internal.contentassist.ContentAssistUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * Completion computer for CSS templates
 * </p>
 */
public class CSSTemplatesCompletionProposalComputer implements ICompletionProposalComputer {
  /**
   * <p>
   * The template processor used to create the proposals
   * </p>
   */
  private CSSTemplateCompletionProcessor fTemplateProcessor = null;

  /**
   * <p>
   * Create the computer
   * </p>
   */
  public CSSTemplatesCompletionProposalComputer() {
    fTemplateProcessor = new CSSTemplateCompletionProcessor();
  }

  /**
   * @see org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer#computeCompletionProposals(org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext,
   *      org.eclipse.core.runtime.IProgressMonitor)
   */
  public List computeCompletionProposals(CompletionProposalInvocationContext context,
      IProgressMonitor monitor) {

    List proposals = new ArrayList();

    boolean isEmptyDocument = ContentAssistUtils.isViewerEmpty(context.getViewer());
    if (isEmptyDocument) {
      proposals.addAll(getTemplates(TemplateContextTypeIdsCSS.NEW, context));
    }
    proposals.addAll(getTemplates(TemplateContextTypeIdsCSS.ALL, context));

    return proposals;
  }

  /**
   * @see org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer#computeContextInformation(org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext,
   *      org.eclipse.core.runtime.IProgressMonitor)
   */
  public List computeContextInformation(CompletionProposalInvocationContext context,
      IProgressMonitor monitor) {
    // TODO Auto-generated method stub
    return null;
  }

  /**
   * @see org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer#getErrorMessage()
   */
  public String getErrorMessage() {
    return null;
  }

  /**
   * @see org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer#sessionStarted()
   */
  public void sessionStarted() {
    //default is to do nothing
  }

  /**
   * @see org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer#sessionEnded()
   */
  public void sessionEnded() {
    //default is to do nothing
  }

  /**
   * <p>
   * Gets template proposals for the given template and proposal contexts
   * </p>
   * 
   * @param templateContext the template context
   * @param context the proposal context
   * @return {@link List} of template proposals for the given contexts
   */
  private List getTemplates(String templateContext, CompletionProposalInvocationContext context) {

    fTemplateProcessor.setContextType(templateContext);
    ICompletionProposal[] proposals = fTemplateProcessor.computeCompletionProposals(
        context.getViewer(), context.getInvocationOffset());

    return Arrays.asList(proposals);
  }

}
