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
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.correction.CorrectionProcessors;
import com.google.dart.engine.services.correction.CorrectionProposal;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;
import com.google.dart.tools.ui.actions.ConvertGetterToMethodAction;
import com.google.dart.tools.ui.actions.ConvertMethodToGetterAction;
import com.google.dart.tools.ui.actions.DartEditorActionDefinitionIds;
import com.google.dart.tools.ui.internal.refactoring.ServiceUtils;
import com.google.dart.tools.ui.internal.refactoring.actions.RenameDartElementAction;
import com.google.dart.tools.ui.internal.text.correction.proposals.ConvertGetterToMethodRefactoringProposal;
import com.google.dart.tools.ui.internal.text.correction.proposals.ConvertMethodToGetterRefactoringProposal;
import com.google.dart.tools.ui.internal.text.correction.proposals.FormatProposal;
import com.google.dart.tools.ui.internal.text.correction.proposals.RenameRefactoringProposal;
import com.google.dart.tools.ui.internal.text.correction.proposals.SortMembersProposal;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;
import com.google.dart.tools.ui.text.dart.IQuickAssistProcessor;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.source.ISourceViewer;

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
      ICompletionProposal uiProposal = ServiceUtils.toUI(serviceProposal);
      if (uiProposal != null) {
        proposals.add(uiProposal);
      }
    }
  }

  private AssistContext context;
  private DartEditor editor;
  private ISourceViewer viewer;
  private DartSelection selection;
  private List<ICompletionProposal> proposals;

  public synchronized ICompletionProposal[] getAssists(AssistContextUI contextUI) {
    this.context = contextUI.getContext();
    this.editor = contextUI.getEditor();
    this.viewer = editor.getViewer();
    this.selection = (DartSelection) editor.getSelectionProvider().getSelection();
    proposals = Lists.newArrayList();
    // not resolved yet
    if (context == null) {
      ExecutionUtils.runLog(new RunnableEx() {
        @Override
        public void run() throws Exception {
          addUnresolvedProposals();
        }
      });
    }
    // use AssistContext
    if (context != null) {
      ExecutionUtils.runLog(new RunnableEx() {
        @Override
        public void run() throws Exception {
          if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
            final List<CorrectionProposal> proposalList = Lists.newArrayList();
            // TODO(scheglov) restore or remove for the new API
//            final CountDownLatch latch = new CountDownLatch(1);
//            DartCore.getAnalysisServer().computeMinorRefactorings(
//                context.getAnalysisContextId(),
//                context.getSource(),
//                context.getSelectionOffset(),
//                context.getSelectionLength(),
//                new MinorRefactoringsConsumer() {
//                  @Override
//                  public void computedProposals(CorrectionProposal[] proposals, boolean isLastResult) {
//                    Collections.addAll(proposalList, proposals);
//                    if (isLastResult) {
//                      latch.countDown();
//                    }
//                  }
//                });
//            Uninterruptibles.awaitUninterruptibly(latch, 2000, TimeUnit.MILLISECONDS);
            CorrectionProposal[] serviceProposals = proposalList.toArray(new CorrectionProposal[proposalList.size()]);
            addServiceProposals(proposals, serviceProposals);
          } else {
            // add refactoring proposals
            addProposal_convertGetterToMethodRefactoring();
            addProposal_convertMethodToGetterRefactoring();
            addProposal_renameRefactoring();
            addProposal_format();
            addProposal_sortMembers();
            // ask services
            com.google.dart.engine.services.correction.QuickAssistProcessor serviceProcessor;
            serviceProcessor = CorrectionProcessors.getQuickAssistProcessor();
            CorrectionProposal[] serviceProposals = serviceProcessor.getProposals(context);
            addServiceProposals(proposals, serviceProposals);
          }
        }
      });
    }
    // done
    this.context = null;
    this.editor = null;
    this.selection = null;
    try {
      return proposals.toArray(new ICompletionProposal[proposals.size()]);
    } finally {
      proposals = null;
    }
  }

  private void addProposal_convertGetterToMethodRefactoring() throws CoreException {
    ConvertGetterToMethodAction action = new ConvertGetterToMethodAction(editor);
    action.update(selection);
    if (action.isEnabled()) {
      proposals.add(new ConvertGetterToMethodRefactoringProposal(action, selection));
    }
  }

  private void addProposal_convertMethodToGetterRefactoring() throws CoreException {
    ConvertMethodToGetterAction action = new ConvertMethodToGetterAction(editor);
    action.update(selection);
    if (action.isEnabled()) {
      proposals.add(new ConvertMethodToGetterRefactoringProposal(action, selection));
    }
  }

  private void addProposal_format() {
    IAction action = editor.getAction(DartEditorActionDefinitionIds.QUICK_FORMAT);
    if (action != null && action.isEnabled()) {
      proposals.add(new FormatProposal(action));
    }
  }

  private void addProposal_renameRefactoring() throws CoreException {
    RenameDartElementAction action = new RenameDartElementAction(editor);
    action.update(selection);
    if (action.isEnabled()) {
      proposals.add(new RenameRefactoringProposal(action, selection));
    }
  }

  private void addProposal_sortMembers() {
    CompilationUnit unit = context.getCompilationUnit();
    proposals.add(new SortMembersProposal(viewer, unit));
  }

  /**
   * Adds proposals which can be produced for unresolved {@link CompilationUnit}.
   */
  private void addUnresolvedProposals() throws Exception {
    // prepare parsed CompilationUnit
    CompilationUnit unit = editor.getInputUnit();
    if (unit == null) {
      return;
    }
    // prepare Source
    Source inputSource = editor.getInputSource();
    if (inputSource == null) {
      return;
    }
    // prepare AnalysisContext
    AnalysisContext analysisContext = editor.getInputAnalysisContext();
    if (analysisContext == null) {
      return;
    }
    // ask for corrections
    CorrectionProposal[] serviceProposals = CorrectionProcessors.getQuickAssistProcessor().getProposals(
        analysisContext,
        inputSource,
        unit,
        selection.getOffset());
    addServiceProposals(proposals, serviceProposals);
  }
}
