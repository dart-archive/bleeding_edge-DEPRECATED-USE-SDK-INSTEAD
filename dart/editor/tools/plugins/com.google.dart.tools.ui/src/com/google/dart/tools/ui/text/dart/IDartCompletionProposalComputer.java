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
package com.google.dart.tools.ui.text.dart;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;

import java.util.List;

/**
 * Computes completions and context information displayed by the Dart editor content assistant.
 * Contributions to the <tt>org.eclipse.wst.jsdt.ui.javaCompletionProposalComputer</tt> extension
 * point must implement this interface. Provisional API: This class/interface is part of an interim
 * API that is still under development and expected to change significantly before reaching
 * stability. It is being made available at this early stage to solicit feedback from pioneering
 * adopters on the understanding that any code that uses this API will almost certainly be broken
 * (repeatedly) as the API evolves.
 */
public interface IDartCompletionProposalComputer {
  /**
   * Returns a list of completion proposals valid at the given invocation context.
   * 
   * @param context the context of the content assist invocation
   * @param monitor a progress monitor to report progress. The monitor is private to this
   *          invocation, i.e. there is no need for the receiver to spawn a sub monitor.
   * @return a list of completion proposals (element type:
   *         {@link org.eclipse.jface.text.contentassist.ICompletionProposal})
   */
  List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext context,
      IProgressMonitor monitor);

  /**
   * Returns context information objects valid at the given invocation context.
   * 
   * @param context the context of the content assist invocation
   * @param monitor a progress monitor to report progress. The monitor is private to this
   *          invocation, i.e. there is no need for the receiver to spawn a sub monitor.
   * @return a list of context information objects (element type:
   *         {@link org.eclipse.jface.text.contentassist.IContextInformation})
   */
  List<IContextInformation> computeContextInformation(ContentAssistInvocationContext context,
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
   * {@linkplain #computeCompletionProposals(ContentAssistInvocationContext, IProgressMonitor)
   * computeCompletionProposals} and
   * {@linkplain #computeContextInformation(ContentAssistInvocationContext, IProgressMonitor)
   * computeContextInformation}.
   */
  void sessionEnded();

  /**
   * Informs the computer that a content assist session has started. This call will always be
   * followed by a {@link #sessionEnded()} call, but not necessarily by calls to
   * {@linkplain #computeCompletionProposals(ContentAssistInvocationContext, IProgressMonitor)
   * computeCompletionProposals} or
   * {@linkplain #computeContextInformation(ContentAssistInvocationContext, IProgressMonitor)
   * computeContextInformation}.
   */
  void sessionStarted();
}
