/*
 * Copyright (c) 2014, the Dart project authors.
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
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.server.generated.types.AnalysisError;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.DartToolsPlugin;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.text.dart.CompletionProposalComparator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistInvocationContext;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.ui.texteditor.MarkerAnnotation;

import java.util.Collections;
import java.util.List;

/**
 * {@link IQuickAssistProcessor} for Dart.
 * 
 * @coverage dart.editor.ui.correction
 */
public class DartCorrectionProcessor_NEW implements
    org.eclipse.jface.text.quickassist.IQuickAssistProcessor {

  private final DartCorrectionAssistant_NEW assistant;
  private String errorMessage;

  public DartCorrectionProcessor_NEW(DartCorrectionAssistant_NEW assistant) {
    this.assistant = assistant;
  }

  @Override
  public boolean canAssist(IQuickAssistInvocationContext invocationContext) {
    return false;
  }

  @Override
  public boolean canFix(Annotation annotation) {
    if (!(annotation instanceof MarkerAnnotation)) {
      return false;
    }
    IMarker marker = ((MarkerAnnotation) annotation).getMarker();
    if (marker == null) {
      return false;
    }
    // TODO(scheglov) add API to check if the marker has a fixable problem
    try {
      String type = marker.getType();
      return DartCore.DART_PROBLEM_MARKER_TYPE.equals(type);
    } catch (CoreException e) {
      return false;
    }
  }

  @Override
  public ICompletionProposal[] computeQuickAssistProposals(
      IQuickAssistInvocationContext invocationContext) {
    // prepare AssistContextUI
    AssistContextUI contextUI;
    {
      DartEditor editor = assistant.getEditor();
      AssistContext context = editor.getAssistContext();
      contextUI = new AssistContextUI(context, editor);
    }
    // prepare proposals
    List<ICompletionProposal> proposals = Lists.newArrayList();
    // add Quick Fixes
    try {
      QuickFixProcessor_NEW qfProcessor = new QuickFixProcessor_NEW();
      AnalysisError problemToFix = assistant.getProblemToFix();
      ICompletionProposal[] fixProposals = qfProcessor.computeFix(contextUI, problemToFix);
      Collections.addAll(proposals, fixProposals);
      // show problem only if there is are fixes
      if (fixProposals.length != 0) {
        assistant.showProblemToFix();
      }
    } catch (Throwable e) {
      DartToolsPlugin.log(e);
    }
    // add Quick Assists
    if (proposals.isEmpty()) {
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
