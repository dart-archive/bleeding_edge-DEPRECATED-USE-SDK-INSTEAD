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
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.services.correction.CorrectionProcessors;
import com.google.dart.engine.services.correction.CorrectionProposal;
import com.google.dart.engine.source.Source;
import com.google.dart.server.GetAssistsConsumer;
import com.google.dart.server.generated.types.SourceChange;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.internal.corext.refactoring.util.ExecutionUtils;
import com.google.dart.tools.internal.corext.refactoring.util.RunnableEx;
import com.google.dart.tools.ui.actions.ConvertGetterToMethodAction_NEW;
import com.google.dart.tools.ui.actions.ConvertGetterToMethodAction_OLD;
import com.google.dart.tools.ui.actions.ConvertMethodToGetterAction_NEW;
import com.google.dart.tools.ui.actions.ConvertMethodToGetterAction_OLD;
import com.google.dart.tools.ui.actions.DartEditorActionDefinitionIds;
import com.google.dart.tools.ui.internal.refactoring.ServiceUtils_NEW;
import com.google.dart.tools.ui.internal.refactoring.ServiceUtils_OLD;
import com.google.dart.tools.ui.internal.refactoring.actions.RenameDartElementAction_OLD;
import com.google.dart.tools.ui.internal.text.correction.proposals.ConvertGetterToMethodRefactoringProposal_NEW;
import com.google.dart.tools.ui.internal.text.correction.proposals.ConvertGetterToMethodRefactoringProposal_OLD;
import com.google.dart.tools.ui.internal.text.correction.proposals.ConvertMethodToGetterRefactoringProposal_NEW;
import com.google.dart.tools.ui.internal.text.correction.proposals.ConvertMethodToGetterRefactoringProposal_OLD;
import com.google.dart.tools.ui.internal.text.correction.proposals.FormatProposal;
import com.google.dart.tools.ui.internal.text.correction.proposals.RenameRefactoringProposal;
import com.google.dart.tools.ui.internal.text.correction.proposals.SortMembersProposal;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.quickassist.IQuickAssistProcessor;
import org.eclipse.jface.text.source.ISourceViewer;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Standard {@link IQuickAssistProcessor} for Dart.
 * 
 * @coverage dart.editor.ui.correction
 */
public class QuickAssistProcessor {
  /**
   * Adds the given server's {@link SourceChange}s as LTK proposals.
   */
  static void addServerProposals(List<ICompletionProposal> proposals, List<SourceChange> changes) {
    for (SourceChange change : changes) {
      ICompletionProposal uiProposal = ServiceUtils_NEW.toUI(change);
      if (uiProposal != null) {
        proposals.add(uiProposal);
      }
    }
  }

  /**
   * Adds service {@link CorrectionProposal} to the Eclipse {@link ICompletionProposal}s.
   */
  static void addServiceProposals(List<ICompletionProposal> proposals,
      CorrectionProposal[] serviceProposals) {
    for (CorrectionProposal serviceProposal : serviceProposals) {
      ICompletionProposal uiProposal = ServiceUtils_OLD.toUI(serviceProposal);
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
    // add proposals
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      // add refactoring proposals
      addProposal_convertGetterToMethodRefactoring();
      addProposal_convertMethodToGetterRefactoring();
      // TODO(scheglov) add other proposals
//      addProposal_renameRefactoring();
      addProposal_format();
//      addProposal_sortMembers();
      // ask server
      ExecutionUtils.runLog(new RunnableEx() {
        @Override
        public void run() throws Exception {
          final List<SourceChange> changes = Lists.newArrayList();
          final CountDownLatch latch = new CountDownLatch(1);
          String file = context.getFile();
          DartCore.getAnalysisServer().edit_getAssists(
              file,
              context.getSelectionOffset(),
              context.getSelectionLength(),
              new GetAssistsConsumer() {
                @Override
                public void computedSourceChanges(List<SourceChange> _changes) {
                  changes.addAll(_changes);
                  latch.countDown();
                }
              });
          Uninterruptibles.awaitUninterruptibly(latch, 2000, TimeUnit.MILLISECONDS);
          addServerProposals(proposals, changes);
        }
      });
    } else {
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
        });
      }
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

  private void addProposal_convertGetterToMethodRefactoring() {
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      ConvertGetterToMethodAction_NEW action = new ConvertGetterToMethodAction_NEW(editor);
      action.selectionChanged(selection);
      if (action.isEnabled()) {
        proposals.add(new ConvertGetterToMethodRefactoringProposal_NEW(action));
      }
    } else {
      ConvertGetterToMethodAction_OLD action = new ConvertGetterToMethodAction_OLD(editor);
      action.update(selection);
      if (action.isEnabled()) {
        proposals.add(new ConvertGetterToMethodRefactoringProposal_OLD(action, selection));
      }
    }
  }

  private void addProposal_convertMethodToGetterRefactoring() {
    if (DartCoreDebug.ENABLE_ANALYSIS_SERVER) {
      ConvertMethodToGetterAction_NEW action = new ConvertMethodToGetterAction_NEW(editor);
      action.selectionChanged(selection);
      if (action.isEnabled()) {
        proposals.add(new ConvertMethodToGetterRefactoringProposal_NEW(action));
      }
    } else {
      ConvertMethodToGetterAction_OLD action = new ConvertMethodToGetterAction_OLD(editor);
      action.update(selection);
      if (action.isEnabled()) {
        proposals.add(new ConvertMethodToGetterRefactoringProposal_OLD(action, selection));
      }
    }
  }

  private void addProposal_format() {
    IAction action = editor.getAction(DartEditorActionDefinitionIds.QUICK_FORMAT);
    if (action != null && action.isEnabled()) {
      proposals.add(new FormatProposal(action));
    }
  }

  private void addProposal_renameRefactoring() {
    RenameDartElementAction_OLD action = new RenameDartElementAction_OLD(editor);
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
