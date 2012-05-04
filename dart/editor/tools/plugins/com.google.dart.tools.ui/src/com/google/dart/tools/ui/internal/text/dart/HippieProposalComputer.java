/*
 * Copyright (c) 2012, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.tools.ui.text.dart.ContentAssistInvocationContext;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposalComputer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.ui.texteditor.HippieProposalProcessor;

import java.util.Arrays;
import java.util.List;

/**
 * A computer wrapper for the hippie processor.
 */
public final class HippieProposalComputer implements IDartCompletionProposalComputer {
  /** The wrapped processor. */
  private final HippieProposalProcessor fProcessor = new HippieProposalProcessor();

  /**
   * Default ctor to make it instantiatable via the extension mechanism.
   */
  public HippieProposalComputer() {
  }

  /*
   * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#
   * computeCompletionProposals
   * (org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext,
   * org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public List<ICompletionProposal> computeCompletionProposals(ContentAssistInvocationContext context,
      IProgressMonitor monitor) {
    return Arrays.asList(fProcessor.computeCompletionProposals(context.getViewer(),
        context.getInvocationOffset()));
  }

  /*
   * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#
   * computeContextInformation
   * (org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext,
   * org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public List<IContextInformation> computeContextInformation(ContentAssistInvocationContext context,
      IProgressMonitor monitor) {
    return Arrays.asList(fProcessor.computeContextInformation(context.getViewer(),
        context.getInvocationOffset()));
  }

  /*
   * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer# getErrorMessage()
   */
  @Override
  public String getErrorMessage() {
    return fProcessor.getErrorMessage();
  }

  /*
   * @see com.google.dart.tools.ui.text.dart.IDartCompletionProposalComputer#sessionEnded ()
   */
  @Override
  public void sessionEnded() {
  }

  /*
   * @see com.google.dart.tools.ui.text.dart.IDartCompletionProposalComputer# sessionStarted()
   */
  @Override
  public void sessionStarted() {
  }
}
