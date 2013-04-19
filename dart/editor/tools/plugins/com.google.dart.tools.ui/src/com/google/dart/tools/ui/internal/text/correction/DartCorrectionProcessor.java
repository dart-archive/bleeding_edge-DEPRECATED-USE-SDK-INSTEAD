/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text.correction;

import com.google.common.collect.Lists;
import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.text.dart.CompletionProposalComparator;

import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;

import java.util.Collections;
import java.util.List;

/**
 * {@link IQuickAssistProcessor} for Dart.
 * 
 * @coverage dart.editor.ui.correction
 */
public class DartCorrectionProcessor implements
    org.eclipse.jface.text.quickassist.IQuickAssistProcessor {

  private final DartCorrectionAssistant assistant;
  private String errorMessage;

  public DartCorrectionProcessor(DartCorrectionAssistant assistant) {
    this.assistant = assistant;
  }

  @Override
  public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
    return false;
  }

  @Override
  public boolean canFix(Annotation annotation) {
    AnalysisError problem = assistant.getAnalysisError(annotation);
    if (problem == null) {
      return false;
    }
    return QuickFixProcessor.hasFix(problem);
  }

  @Override
  public ICompletionProposal[] computeQuickAssistProposals(
      IQuickAssistInvocationContext invocationContext) {
    // prepare AssistContextUI
    AssistContextUI contextUI;
    {
      DartEditor editor = assistant.getEditor();
      AssistContext context = editor.getAssistContext();
      if (context == null) {
        return new ICompletionProposal[0];
      }
      contextUI = new AssistContextUI(context, editor);
    }
    // prepare proposals
    List<ICompletionProposal> proposals = Lists.newArrayList();
    // add Quick Fixes
    AnalysisError problemToFix = assistant.getProblemToFix();
    try {
      QuickFixProcessor qfProcessor = new QuickFixProcessor();
      ICompletionProposal[] fixProposals = qfProcessor.computeFix(contextUI, problemToFix);
      Collections.addAll(proposals, fixProposals);
    } catch (Throwable e) {
      DartToolsPlugin.log(e);
    }
    // add Quick Assists
    if (problemToFix == null) {
      QuickAssistProcessor qaProcessor = new QuickAssistProcessor();
      ICompletionProposal[] assistProposals = qaProcessor.getAssists(contextUI);
      Collections.addAll(proposals, assistProposals);
    }
    // done
    Collections.sort(proposals, new CompletionProposalComparator());
    return proposals.toArray(new ICompletionProposal[proposals.size()]);
  }

  @Override
  public String getErrorMessage() {
    return errorMessage;
  }
}
