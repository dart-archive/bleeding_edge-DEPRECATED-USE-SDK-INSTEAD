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
package com.google.dart.tools.ui.text.dart;

import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.tools.core.completion.CompletionContext;
import com.google.dart.tools.core.completion.CompletionProposal;
import com.google.dart.tools.ui.internal.text.completion.DartServerProposalCollector;
import com.google.dart.tools.ui.internal.text.dart.ContentAssistHistory;
import com.google.dart.tools.ui.internal.text.dart.ContentAssistHistory.RHSHistory;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;

import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.IEditorPart;

/**
 * Describes the context of a content assist invocation in a Dart editor.
 * <p>
 * Clients may use but not subclass this class.
 * </p>
 * Provisional API: This class/interface is part of an interim API that is still under development
 * and expected to change significantly before reaching stability. It is being made available at
 * this early stage to solicit feedback from pioneering adopters on the understanding that any code
 * that uses this API will almost certainly be broken (repeatedly) as the API evolves.
 * Implementation note: There is no need to override hashCode and equals, as we only add cached
 * values shared across one assist invocation.
 */
public class DartContentAssistInvocationContext extends ContentAssistInvocationContext {
  private final IEditorPart fEditor;

  private CompletionProposalLabelProvider fLabelProvider;
  private CompletionProposalCollector fCollector;
  private RHSHistory fRHSHistory;

  private IDartCompletionProposal[] fKeywordProposals = null;
  private CompletionContext fCoreContext = null;
  private AssistContext assistContext;

  private DartServerProposalCollector collector;

  /**
   * Creates a new context.
   */
  public DartContentAssistInvocationContext() {
    super();
    fEditor = null;
  }

  /**
   * Creates a new context.
   * 
   * @param viewer the viewer used by the editor
   * @param offset the invocation offset
   * @param editor the editor that content assist is invoked in
   * @param assistContext the context or {@code null} if unknown
   * @param collector the completion collector or {@code null} if unknown
   */
  public DartContentAssistInvocationContext(ITextViewer viewer, int offset, IEditorPart editor,
      AssistContext assistContext, DartServerProposalCollector collector) {
    super(viewer, offset);
    fEditor = editor;
    this.assistContext = assistContext;
    this.collector = collector;
  }

  public AssistContext getAssistContext() {
    if (assistContext == null) {
      assistContext = ((DartEditor) fEditor).getAssistContext();
    }
    return assistContext;
  }

  public DartServerProposalCollector getCollector() {
    return collector;
  }

  /**
   * Returns the {@link CompletionContext core completion context} if available, <code>null</code>
   * otherwise.
   * <p>
   * <strong>Note:</strong> This method may run
   * {@linkplain org.eclipse.wst.jsdt.core.ICodeAssist#codeComplete(int, org.eclipse.wst.jsdt.core.CompletionRequestor)
   * codeComplete} on the compilation unit.
   * </p>
   * 
   * @return the core completion context if available, <code>null</code> otherwise
   */
  public CompletionContext getCoreContext() {
    if (fCoreContext == null) {
      // use the context from the existing collector if it exists, retrieve one
      // ourselves otherwise
      if (fCollector != null) {
        fCoreContext = fCollector.getContext();
      }
      if (fCoreContext == null) {
        computeKeywordsAndContext();
      }
    }
    return fCoreContext;
  }

  /**
   * Returns an float in [0.0,&nbsp;1.0] based on whether the type has been recently used as a right
   * hand side for the type expected in the current context. 0 signals that the
   * <code>qualifiedTypeName</code> does not match the expected type, while 1.0 signals that
   * <code>qualifiedTypeName</code> has most recently been used in a similar context.
   * <p>
   * <strong>Note:</strong> This method may run
   * {@linkplain org.eclipse.wst.jsdt.core.ICodeAssist#codeComplete(int, org.eclipse.wst.jsdt.core.CompletionRequestor)
   * codeComplete} on the compilation unit.
   * </p>
   * 
   * @param qualifiedTypeName the type name of the type of interest
   * @return a relevance in [0.0,&nbsp;1.0] based on previous content assist invocations
   */
  public float getHistoryRelevance(String qualifiedTypeName) {
    return getRHSHistory().getRank(qualifiedTypeName);
  }

  public com.google.dart.engine.ast.CompilationUnit getInputUnit() {
    return ((DartEditor) fEditor).getInputUnit();
  }

  /**
   * Returns the keyword proposals that are available in this context, possibly none.
   * <p>
   * <strong>Note:</strong> This method may run
   * {@linkplain org.eclipse.wst.jsdt.core.ICodeAssist#codeComplete(int, org.eclipse.wst.jsdt.core.CompletionRequestor)
   * codeComplete} on the compilation unit.
   * </p>
   * 
   * @return the available keyword proposals
   */
  public IDartCompletionProposal[] getKeywordProposals() {
    if (fKeywordProposals == null) {
      if (fCollector != null && !fCollector.isIgnored(CompletionProposal.KEYWORD)
          && fCollector.getContext() != null) {
        // use the existing collector if it exists, collects keywords, and has
        // already been invoked
        fKeywordProposals = fCollector.getKeywordCompletionProposals();
      } else {
        // otherwise, retrieve keywords ourselves
        computeKeywordsAndContext();
      }
    }

    return fKeywordProposals;
  }

  /**
   * Returns a label provider that can be used to compute proposal labels.
   * 
   * @return a label provider that can be used to compute proposal labels
   */
  public CompletionProposalLabelProvider getLabelProvider() {
    if (fLabelProvider == null) {
      if (fCollector != null) {
        fLabelProvider = fCollector.getLabelProvider();
      } else {
        fLabelProvider = new CompletionProposalLabelProvider();
      }
    }

    return fLabelProvider;
  }

  public int getPartitionOffset() {
    return 0;
  }

  /**
   * Sets the collector, which is used to access the compilation unit, the core context and the
   * label provider. This is a performance optimization: {@link IDartCompletionProposalComputer}s
   * may instantiate a {@link CompletionProposalCollector} and set this invocation context via
   * {@link CompletionProposalCollector#setInvocationContext(DartContentAssistInvocationContext)} ,
   * which in turn calls this method. This allows the invocation context to retrieve the core
   * context and keyword proposals from the existing collector, instead of computing theses values
   * itself via {@link #computeKeywordsAndContext()}.
   * 
   * @param collector the collector
   */
  protected void setCollector(CompletionProposalCollector collector) {
    fCollector = collector;
  }

  /**
   * Fallback to retrieve a core context and keyword proposals when no collector is available. Runs
   * code completion on the cu and collects keyword proposals. {@link #fKeywordProposals} is non-
   * <code>null</code> after this call.
   */
  private void computeKeywordsAndContext() {
    if (fKeywordProposals == null) {
      fKeywordProposals = new IDartCompletionProposal[0];
    }
  }

  /**
   * Returns the content assist type history for the expected type.
   * 
   * @return the content assist type history for the expected type
   */
  private RHSHistory getRHSHistory() {
    if (fRHSHistory == null) {
      fRHSHistory = ContentAssistHistory.EMPTY_HISTORY;
    }
    return fRHSHistory;
  }
}
