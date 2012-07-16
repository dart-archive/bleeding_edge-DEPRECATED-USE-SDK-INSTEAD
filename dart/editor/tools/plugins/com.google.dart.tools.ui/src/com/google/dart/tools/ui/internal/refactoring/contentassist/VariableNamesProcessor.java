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
package com.google.dart.tools.ui.internal.refactoring.contentassist;

import com.google.dart.tools.ui.DartPluginImages;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.DartUIMessages;
import com.google.dart.tools.ui.internal.text.dart.DartCompletionProposal;
import com.google.dart.tools.ui.internal.viewsupport.ImageDescriptorRegistry;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.contentassist.IContentAssistSubjectControl;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.swt.graphics.Image;

import java.util.ArrayList;
import java.util.Arrays;

@SuppressWarnings("deprecation")
public class VariableNamesProcessor implements IContentAssistProcessor,
    org.eclipse.jface.contentassist.ISubjectControlContentAssistProcessor {

  private String fErrorMessage;

  private String[] fTempNameProposals;

  private ImageDescriptorRegistry fImageRegistry;
  private ImageDescriptor fProposalImageDescriptor;

  public VariableNamesProcessor(String[] tempNameProposals) {
    fTempNameProposals = tempNameProposals.clone();
    Arrays.sort(fTempNameProposals);
    fImageRegistry = DartToolsPlugin.getImageDescriptorRegistry();
    fProposalImageDescriptor = DartPluginImages.DESC_OBJS_LOCAL_VARIABLE;

  }

  @Override
  public ICompletionProposal[] computeCompletionProposals(
      IContentAssistSubjectControl contentAssistSubject, int documentOffset) {
    if (fTempNameProposals.length == 0) {
      return null;
    }
    String input = contentAssistSubject.getDocument().get();

    ArrayList<DartCompletionProposal> proposals = new ArrayList<DartCompletionProposal>();
    String prefix = input.substring(0, documentOffset);
    Image image = fImageRegistry.get(fProposalImageDescriptor);
    for (int i = 0; i < fTempNameProposals.length; i++) {
      String tempName = fTempNameProposals[i];
      if (tempName.length() == 0 || !tempName.startsWith(prefix)) {
        continue;
      }
      DartCompletionProposal proposal = new DartCompletionProposal(
          tempName,
          0,
          input.length(),
          image,
          tempName,
          0);
      proposals.add(proposal);
    }
    fErrorMessage = proposals.size() > 0 ? null
        : DartUIMessages.JavaEditor_codeassist_noCompletions;
    return proposals.toArray(new ICompletionProposal[proposals.size()]);
  }

  @Override
  public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int documentOffset) {
    Assert.isTrue(false, "ITextViewer not supported"); //$NON-NLS-1$
    return null;
  }

  @Override
  public IContextInformation[] computeContextInformation(
      IContentAssistSubjectControl contentAssistSubject, int documentOffset) {
    return null; //no context
  }

  @Override
  public IContextInformation[] computeContextInformation(ITextViewer viewer, int documentOffset) {
    Assert.isTrue(false, "ITextViewer not supported"); //$NON-NLS-1$
    return null;
  }

  @Override
  public char[] getCompletionProposalAutoActivationCharacters() {
    return null;
  }

  @Override
  public char[] getContextInformationAutoActivationCharacters() {
    return null; //no context
  }

  @Override
  public IContextInformationValidator getContextInformationValidator() {
    return null; //no context
  }

  @Override
  public String getErrorMessage() {
    return fErrorMessage;
  }

  public void setProposalImageDescriptor(ImageDescriptor proposalImageDescriptor) {
    fProposalImageDescriptor = proposalImageDescriptor;
  }

}
