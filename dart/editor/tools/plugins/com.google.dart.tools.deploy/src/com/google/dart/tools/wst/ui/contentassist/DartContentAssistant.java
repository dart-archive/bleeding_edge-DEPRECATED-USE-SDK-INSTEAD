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
package com.google.dart.tools.wst.ui.contentassist;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.search.SearchEngineFactory;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.internal.text.dart.DartCompletionProposalComputer;
import com.google.dart.tools.ui.text.dart.ContentAssistInvocationContext;
import com.google.dart.tools.ui.text.dart.DartContentAssistInvocationContext;
import com.google.dart.tools.wst.ui.DartReconcilerManager;
import com.google.dart.tools.wst.ui.EmbeddedDartReconcilerHook;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer;

import java.util.List;

/**
 * Content assist for Dart code embedded in HTML.
 */
@SuppressWarnings("restriction")
public class DartContentAssistant implements ICompletionProposalComputer {

  private static class HtmlDartCompletionContext extends DartContentAssistInvocationContext {

    private CompilationUnit unit;
    private AnalysisContext context;
    private int partitionOffset;

    HtmlDartCompletionContext(CompilationUnit unit, int offset, ITextViewer viewer,
        AnalysisContext context, int partitionOffset) {
      super(viewer, offset, null, null);
      this.unit = unit;
      this.context = context;
      this.partitionOffset = partitionOffset;
    }

    @Override
    public AssistContext getAssistContext() {
      Source source = null;
      if (unit != null) {
        CompilationUnitElement unitElement = unit.getElement();
        if (unitElement != null) {
          source = unitElement.getSource();
        }
      }
      return new AssistContext(
          SearchEngineFactory.createSearchEngine(DartCore.getProjectManager().getIndex()),
          context,
          null,
          source,
          unit,
          super.getInvocationOffset(),
          0);
    }

    /**
     * The invocation offset needs to be adjusted to account for completion analysis beginning at
     * the start of the Dart script partition, not the beginning of the file.
     */
    @Override
    public int getInvocationOffset() {
      return super.getInvocationOffset() + partitionOffset;
    }

    /**
     * Return the offset of the beginning of the Dart script partition.
     */
    @Override
    public int getPartitionOffset() {
      return partitionOffset;
    }
  }

  DartCompletionProposalComputer proposalComputer = new DartCompletionProposalComputer();

  @Override
  public List<ICompletionProposal> computeCompletionProposals(
      CompletionProposalInvocationContext context, IProgressMonitor monitor) {
    return proposalComputer.computeCompletionProposals(convertContext(context), monitor);
  }

  @Override
  public List<IContextInformation> computeContextInformation(
      CompletionProposalInvocationContext context, IProgressMonitor monitor) {
    return proposalComputer.computeContextInformation(convertContext(context), monitor);
  }

  @Override
  public String getErrorMessage() {
    return proposalComputer.getErrorMessage();
  }

  @Override
  public void sessionEnded() {
    proposalComputer.sessionEnded();
  }

  @Override
  public void sessionStarted() {
    proposalComputer.sessionStarted();
  }

  private ContentAssistInvocationContext convertContext(CompletionProposalInvocationContext context) {
    ITextViewer viewer = context.getViewer();
    IStructuredDocument document = (IStructuredDocument) context.getDocument();
    int offset = context.getInvocationOffset();
    IStructuredDocumentRegion region = document.getRegionAtCharacterOffset(offset);
    int delta = offset - region.getStartOffset();
    int length = region.getLength();
    EmbeddedDartReconcilerHook reconciler = DartReconcilerManager.getInstance().reconcilerFor(
        document);
    if (reconciler != null) {
      CompilationUnit unit = reconciler.getResolvedUnit(region.getStartOffset(), length, document);
      AnalysisContext ac = reconciler.getInputAnalysisContext();
      if (unit != null) {
        return new HtmlDartCompletionContext(unit, delta, viewer, ac, region.getStartOffset());
      }
    }
    return new ContentAssistInvocationContext(viewer, delta);
  }

}
