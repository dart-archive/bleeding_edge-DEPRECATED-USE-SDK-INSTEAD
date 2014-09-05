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
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.dart.server.GetFixesConsumer;
import com.google.dart.server.generated.types.AnalysisError;
import com.google.dart.server.generated.types.AnalysisErrorFixes;
import com.google.dart.server.generated.types.Location;
import com.google.dart.server.generated.types.SourceChange;
import com.google.dart.tools.core.DartCore;

import org.eclipse.jface.text.contentassist.ICompletionProposal;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * UI wrapper around {@link QuickFixProcessor_NEW} service.
 * 
 * @coverage dart.editor.ui.correction
 */
public class QuickFixProcessor_NEW {
  /**
   * Checks if the given {@link AnalysisError} might have a fix.
   */
  public static boolean hasFix(AnalysisError problem) {
    // TODO(scheglov) add API to check if the marker has a fixable problem
    return problem != null;
  }

  /**
   * Computes {@link ICompletionProposal}s which can fix some of the given {@link AnalysisError}s.
   * 
   * @return the {@link ICompletionProposal}s, may be empty, but not {@code null}.
   */
  public ICompletionProposal[] computeFix(AssistContextUI contextUI, AnalysisError problem) {
    List<ICompletionProposal> proposals = Lists.newArrayList();
    if (problem != null) {
      List<SourceChange> fixes = getFixes(problem);
      QuickAssistProcessor.addServerProposals(proposals, fixes);
    }
    return proposals.toArray(new ICompletionProposal[proposals.size()]);
  }

  private List<SourceChange> getFixes(AnalysisError problem) {
    final List<SourceChange> fixes = Lists.newArrayList();
    final String file = problem.getLocation().getFile();
    final int offset = problem.getLocation().getOffset();
    final CountDownLatch latch = new CountDownLatch(1);
    DartCore.getAnalysisServer().edit_getFixes(file, offset, new GetFixesConsumer() {
      @Override
      public void computedFixes(List<AnalysisErrorFixes> errorFixesArray) {
        for (AnalysisErrorFixes errorFixes : errorFixesArray) {
          Location errorLocation = errorFixes.getError().getLocation();
          if (errorLocation.getOffset() == offset) {
            fixes.addAll(errorFixes.getFixes());
            break;
          }
        }
        latch.countDown();
      }
    });
    Uninterruptibles.awaitUninterruptibly(latch, 100, TimeUnit.MILLISECONDS);
    return fixes;
  }
}
