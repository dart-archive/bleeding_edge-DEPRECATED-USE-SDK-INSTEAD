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
import com.google.dart.engine.services.correction.CorrectionProcessors;
import com.google.dart.engine.services.correction.CorrectionProposal;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

import java.util.List;

/**
 * UI wrapper around {@link QuickFixProcessor} service.
 * 
 * @coverage dart.editor.ui.correction
 */
public class QuickFixProcessor {
  /**
   * @param problem the {@link AnalysisError} to analyze, not {@code null}.
   * @return {@code true} if {@link QuickFixProcessor} can produce {@link ICompletionProposal} to
   *         fix given problem.
   */
  public static boolean hasFix(AnalysisError problem) {
    return CorrectionProcessors.getQuickFixProcessor().hasFix(problem);
  }

  /**
   * Computes {@link ICompletionProposal}s which can fix some of the given {@link AnalysisError}s.
   * 
   * @return the {@link ICompletionProposal}s, may be empty, but not {@code null}.
   */
  public ICompletionProposal[] computeFix(final AssistContextUI contextUI,
      final AnalysisError problem) {
    final List<ICompletionProposal> proposals = Lists.newArrayList();
    ExecutionUtils.runLog(new RunnableEx() {
      @Override
      public void run() throws Exception {
        com.google.dart.engine.services.correction.QuickFixProcessor serviceProcessor;
        serviceProcessor = CorrectionProcessors.getQuickFixProcessor();
        CorrectionProposal[] serviceProposals = serviceProcessor.computeProposals(
            contextUI.getContext(),
            problem);
        QuickAssistProcessor.addServiceProposals(proposals, serviceProposals);
      }
    });
    return proposals.toArray(new ICompletionProposal[proposals.size()]);
  }
}
