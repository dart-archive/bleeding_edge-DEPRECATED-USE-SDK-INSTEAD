/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.tools.core.completion.CompletionProposal;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.ui.DartX;
import com.google.dart.tools.ui.PreferenceConstants;
import com.google.dart.tools.ui.text.dart.CompletionProposalCollector;
import com.google.dart.tools.ui.text.dart.ContentAssistInvocationContext;
import com.google.dart.tools.ui.text.dart.DartContentAssistInvocationContext;
import com.google.dart.tools.ui.text.dart.IDartCompletionProposalComputer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationExtension;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Computes Java completion proposals and context infos.
 */
public class DartCompletionProposalComputer implements IDartCompletionProposalComputer {

  private static final class ContextInformationWrapper implements IContextInformation,
      IContextInformationExtension {

    private final IContextInformation fContextInformation;
    private int fPosition;

    public ContextInformationWrapper(IContextInformation contextInformation) {
      fContextInformation = contextInformation;
    }

    /*
     * @see org.eclipse.jface.text.contentassist.IContextInformation#equals(java. lang.Object)
     */
    @Override
    public boolean equals(Object object) {
      if (object instanceof ContextInformationWrapper) {
        return fContextInformation.equals(((ContextInformationWrapper) object).fContextInformation);
      } else {
        return fContextInformation.equals(object);
      }
    }

    /*
     * @see IContextInformation#getContextDisplayString()
     */
    @Override
    public String getContextDisplayString() {
      return fContextInformation.getContextDisplayString();
    }

    /*
     * @see IContextInformationExtension#getContextInformationPosition()
     */
    @Override
    public int getContextInformationPosition() {
      return fPosition;
    }

    /*
     * @see IContextInformation#getImage()
     */
    @Override
    public Image getImage() {
      return fContextInformation.getImage();
    }

    /*
     * @see IContextInformation#getInformationDisplayString()
     */
    @Override
    public String getInformationDisplayString() {
      return fContextInformation.getInformationDisplayString();
    }

    public void setContextInformationPosition(int position) {
      fPosition = position;
    }
  }

  private String fErrorMessage;

  public DartCompletionProposalComputer() {
  }

  /*
   * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#
   * computeCompletionProposals
   * (org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext,
   * org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public List computeCompletionProposals(ContentAssistInvocationContext context,
      IProgressMonitor monitor) {
    if (context instanceof DartContentAssistInvocationContext) {
      DartContentAssistInvocationContext javaContext = (DartContentAssistInvocationContext) context;
      return internalComputeCompletionProposals(context.getInvocationOffset(), javaContext, monitor);
    }
    return Collections.EMPTY_LIST;
  }

  /*
   * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer#
   * computeContextInformation
   * (org.eclipse.jface.text.contentassist.TextContentAssistInvocationContext,
   * org.eclipse.core.runtime.IProgressMonitor)
   */
  @Override
  public List computeContextInformation(ContentAssistInvocationContext context,
      IProgressMonitor monitor) {
    if (context instanceof DartContentAssistInvocationContext) {
      DartContentAssistInvocationContext javaContext = (DartContentAssistInvocationContext) context;

      int contextInformationPosition = guessContextInformationPosition(javaContext);
      List result = addContextInformations(javaContext, contextInformationPosition, monitor);
      return result;
    }
    return Collections.EMPTY_LIST;
  }

  /*
   * @see org.eclipse.jface.text.contentassist.ICompletionProposalComputer# getErrorMessage()
   */
  @Override
  public String getErrorMessage() {
    return fErrorMessage;
  }

  /*
   * @see com.google.dart.tools.ui.text.dart.IDartCompletionProposalComputer#sessionEnded ()
   */
  @Override
  public void sessionEnded() {
    fErrorMessage = null;
  }

  /*
   * @see com.google.dart.tools.ui.text.dart.IDartCompletionProposalComputer# sessionStarted()
   */
  @Override
  public void sessionStarted() {
  }

  /**
   * Creates the collector used to get proposals from core.
   */
  protected CompletionProposalCollector createCollector(DartContentAssistInvocationContext context) {
    CompletionProposalCollector collector = new CompletionProposalCollector(
        context.getCompilationUnit());
    collector.setInvocationContext(context);
    return collector;
  }

  protected int guessContextInformationPosition(ContentAssistInvocationContext context) {
    return context.getInvocationOffset();
  }

  private List addContextInformations(DartContentAssistInvocationContext context, int offset,
      IProgressMonitor monitor) {
    List proposals = internalComputeCompletionProposals(offset, context, monitor);
    List result = new ArrayList(proposals.size());

    for (Iterator it = proposals.iterator(); it.hasNext();) {
      ICompletionProposal proposal = (ICompletionProposal) it.next();
      IContextInformation contextInformation = proposal.getContextInformation();
      if (contextInformation != null) {
        ContextInformationWrapper wrapper = new ContextInformationWrapper(contextInformation);
        wrapper.setContextInformationPosition(offset);
        result.add(wrapper);
      }
    }
    return result;
  }

  /**
   * Returns the array with favorite static members.
   * 
   * @return the <code>String</code> array with with favorite static members
   * @see org.eclipse.wst.jsdt.core.CompletionRequestor#setFavoriteReferences(String[])
   */
  private String[] getFavoriteStaticMembers() {
    String serializedFavorites = PreferenceConstants.getPreferenceStore().getString(
        PreferenceConstants.CODEASSIST_FAVORITE_STATIC_MEMBERS);
    if (serializedFavorites != null && serializedFavorites.length() > 0) {
      return serializedFavorites.split(";"); //$NON-NLS-1$
    }
    return new String[0];
  }

  private List internalComputeCompletionProposals(int offset,
      DartContentAssistInvocationContext context, IProgressMonitor monitor) {
    CompilationUnit unit = context.getCompilationUnit();
    if (unit == null) {
      return Collections.EMPTY_LIST;
    }

    ITextViewer viewer = context.getViewer();

    CompletionProposalCollector collector = createCollector(context);
    collector.setInvocationContext(context);

    // Allow completions for unresolved types - since 3.3
    collector.setAllowsRequiredProposals(CompletionProposal.FIELD_REF, CompletionProposal.TYPE_REF,
        true);
    collector.setAllowsRequiredProposals(CompletionProposal.FIELD_REF,
        CompletionProposal.TYPE_IMPORT, true);
    collector.setAllowsRequiredProposals(CompletionProposal.FIELD_REF,
        CompletionProposal.FIELD_IMPORT, true);

    collector.setAllowsRequiredProposals(CompletionProposal.METHOD_REF,
        CompletionProposal.TYPE_REF, true);
    collector.setAllowsRequiredProposals(CompletionProposal.METHOD_REF,
        CompletionProposal.TYPE_IMPORT, true);
    collector.setAllowsRequiredProposals(CompletionProposal.METHOD_REF,
        CompletionProposal.METHOD_IMPORT, true);

    // Set the favorite list to propose static members - since 3.3
    collector.setFavoriteReferences(getFavoriteStaticMembers());

    try {
      Point selection = viewer.getSelectedRange();
      if (selection.y > 0) {
        collector.setReplacementLength(selection.y);
      }

      unit.codeComplete(offset, collector);
    } catch (DartModelException x) {
      Shell shell = viewer.getTextWidget().getShell();
      DartX.todo();
      if (x.getMessage().startsWith("Failed to parse")) {
        // TODO remove this clause
        return Collections.EMPTY_LIST;
      }
//			if (x.isDoesNotExist() && !unit.getDartProject().isOnIncludepath(unit))
//				MessageDialog.openInformation(shell, DartTextMessages.CompletionProcessor_error_notOnBuildPath_title, DartTextMessages.CompletionProcessor_error_notOnBuildPath_message);
//			else
      ErrorDialog.openError(shell, DartTextMessages.CompletionProcessor_error_accessing_title,
          DartTextMessages.CompletionProcessor_error_accessing_message, x.getStatus());
    }

    ICompletionProposal[] javaProposals = collector.getJavaCompletionProposals();
    int contextInformationOffset = guessContextInformationPosition(context);
    if (contextInformationOffset != offset) {
      for (int i = 0; i < javaProposals.length; i++) {
        if (javaProposals[i] instanceof DartMethodCompletionProposal) {
          DartMethodCompletionProposal jmcp = (DartMethodCompletionProposal) javaProposals[i];
          jmcp.setContextInformationPosition(contextInformationOffset);
        }
      }
    }

    List proposals = new ArrayList(Arrays.asList(javaProposals));
    if (proposals.size() == 0) {
      String error = collector.getErrorMessage();
      if (error.length() > 0) {
        fErrorMessage = error;
      }
    }
    return proposals;
  }
}
