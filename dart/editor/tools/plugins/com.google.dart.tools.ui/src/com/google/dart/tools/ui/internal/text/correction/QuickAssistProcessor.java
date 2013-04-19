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
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.change.SourceChange;
import com.google.dart.engine.services.correction.CorrectionKind;
import com.google.dart.engine.services.correction.CorrectionProcessors;
import com.google.dart.engine.services.correction.CorrectionProposal;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;
import com.google.dart.tools.ui.internal.refactoring.ServiceUtils;
import com.google.dart.tools.ui.internal.refactoring.actions.RenameDartElementAction;
import com.google.dart.tools.ui.internal.text.correction.proposals.CUCorrectionProposal;
import com.google.dart.tools.ui.internal.text.correction.proposals.RenameRefactoringProposal;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;
import com.google.dart.tools.ui.text.dart.IQuickAssistProcessor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ltk.core.refactoring.TextChange;
import org.eclipse.swt.graphics.Image;

import java.util.List;

/**
 * Standard {@link IQuickAssistProcessor} for Dart.
 * 
 * @coverage dart.editor.ui.correction
 */
public class QuickAssistProcessor {
  /**
   * Adds service {@link CorrectionProposal} to the Eclipse {@link ICompletionProposal}s.
   */
  static void addServiceProposals(List<ICompletionProposal> proposals,
      CorrectionProposal[] serviceProposals) {
    for (CorrectionProposal serviceProposal : serviceProposals) {
      CorrectionKind kind = serviceProposal.getKind();
      Image image = ServiceUtils.toLTK(kind.getImage());
      // TODO(scheglov) why do we have several SourceChange-s in CorrectionProposal? 
      List<SourceChange> serviceChanges = serviceProposal.getChanges();
      if (serviceChanges.size() == 1) {
        SourceChange sourceChange = serviceChanges.get(0);
        TextChange textChange = ServiceUtils.toLTK(sourceChange);
        proposals.add(new CUCorrectionProposal(
            kind.getName(),
            sourceChange.getSource(),
            textChange,
            kind.getRelevance(),
            image));
      }
    }
  }

  private AssistContext context;
  private DartEditor editor;
  private DartSelection selection;

  private List<ICompletionProposal> proposals;

  public synchronized ICompletionProposal[] getAssists(AssistContextUI contextUI) {
    this.context = contextUI.getContext();
    this.editor = contextUI.getEditor();
    this.selection = (DartSelection) editor.getSelectionProvider().getSelection();
    proposals = Lists.newArrayList();
    ExecutionUtils.runLog(new RunnableEx() {
      @Override
      public void run() throws Exception {
        // add refactoring proposals
        addProposal_renameRefactoring();
        // ask services
        com.google.dart.engine.services.correction.QuickAssistProcessor serviceProcessor;
        serviceProcessor = CorrectionProcessors.getQuickAssistProcessor();
        CorrectionProposal[] serviceProposals = serviceProcessor.getProposals(context);
        addServiceProposals(proposals, serviceProposals);
      }
    });
    this.context = null;
    this.editor = null;
    this.selection = null;
    try {
      return proposals.toArray(new ICompletionProposal[proposals.size()]);
    } finally {
      proposals = null;
    }
  }

  public boolean hasAssists(AssistContextUI contextUI) throws CoreException {
    return contextUI.getContext() != null;
  }

  void addProposal_renameRefactoring() throws CoreException {
    RenameDartElementAction action = new RenameDartElementAction(editor);
    action.update(selection);
    if (action.isEnabled()) {
      proposals.add(new RenameRefactoringProposal(action, selection));
    }
  }
}
