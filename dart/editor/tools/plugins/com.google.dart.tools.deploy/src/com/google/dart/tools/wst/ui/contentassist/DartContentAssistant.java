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
import com.google.dart.engine.search.SearchEngineFactory;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.DartCoreDebug;
import com.google.dart.tools.ui.internal.text.dart.DartCompletionProposalComputer;
import com.google.dart.tools.ui.text.dart.ContentAssistInvocationContext;
import com.google.dart.tools.ui.text.dart.DartContentAssistInvocationContext;
import com.google.dart.tools.wst.ui.style.LineStyleProviderForDart;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer;

import java.util.List;

@SuppressWarnings("restriction")
public class DartContentAssistant implements ICompletionProposalComputer {

  private static class HtmlDartCompletionContext extends DartContentAssistInvocationContext {

    CompilationUnit unit;
    AnalysisContext context;

    HtmlDartCompletionContext(CompilationUnit unit, int offset, ITextViewer viewer,
        AnalysisContext context) {
      super(viewer, offset, null);
      this.unit = unit;
      this.context = context;
    }

    @Override
    public AssistContext getAssistContext() {
      return new AssistContext(
          SearchEngineFactory.createSearchEngine(DartCore.getProjectManager().getIndex()),
          context,
          unit,
          getInvocationOffset(),
          0);
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
    offset -= region.getStartOffset();
    CompilationUnit unit = LineStyleProviderForDart.LINE_HACK_UNIT;
    AnalysisContext analysisContext = LineStyleProviderForDart.LINE_HACK_CONTEXT;
    if (!DartCoreDebug.EXPERIMENTAL || unit == null) {
      return new ContentAssistInvocationContext(viewer, offset);
    }
    return new HtmlDartCompletionContext(unit, offset, viewer, analysisContext);
  }

}
