/*******************************************************************************
 * Copyright (c) 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.contentassist;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;

import java.util.List;

/**
 * <p>
 * Computes completions and context information displayed by the SSE editor content assistant.
 * Contributions to the <tt>org.eclipse.wst.sse.ui.completionProposal</tt> extension point must
 * implement this interface.
 * </p>
 * 
 * @base org.eclipse.jdt.ui.text.java.IJavaCompletionProposalComputer
 */
public interface ICompletionProposalComputer {
  /**
   * Informs the computer that a content assist session has started. This call will always be
   * followed by a {@link #sessionEnded()} call, but not necessarily by calls to
   * {@link #computeCompletionProposals(CompletionProposalInvocationContext, IProgressMonitor)
   * computeCompletionProposals} or
   * {@link #computeContextInformation(CompletionProposalInvocationContext, IProgressMonitor)
   * computeContextInformation}.
   */
  void sessionStarted();

  /**
   * Returns a list of completion proposals valid at the given invocation context.
   * 
   * @param context the context of the content assist invocation
   * @param monitor a progress monitor to report progress. The monitor is private to this
   *          invocation, i.e. there is no need for the receiver to spawn a sub monitor.
   * @return a list of completion proposals (element type: {@link ICompletionProposal})
   */
  List computeCompletionProposals(CompletionProposalInvocationContext context,
      IProgressMonitor monitor);

  /**
   * Returns context information objects valid at the given invocation context.
   * 
   * @param context the context of the content assist invocation
   * @param monitor a progress monitor to report progress. The monitor is private to this
   *          invocation, i.e. there is no need for the receiver to spawn a sub monitor.
   * @return a list of context information objects (element type: {@link IContextInformation})
   */
  List computeContextInformation(CompletionProposalInvocationContext context,
      IProgressMonitor monitor);

  /**
   * Returns the reason why this computer was unable to produce any completion proposals or context
   * information.
   * 
   * @return an error message or <code>null</code> if no error occurred
   */
  String getErrorMessage();

  /**
   * Informs the computer that a content assist session has ended. This call will always be after
   * any calls to
   * {@linkplain #computeCompletionProposals(CompletionProposalInvocationContext, IProgressMonitor)
   * computeCompletionProposals} and
   * {@linkplain #computeContextInformation(CompletionProposalInvocationContext, IProgressMonitor)
   * computeContextInformation}.
   */
  void sessionEnded();
}
