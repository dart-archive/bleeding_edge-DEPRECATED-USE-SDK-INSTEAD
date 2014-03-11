package com.google.dart.tools.wst.ui.contentassist;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.dart.engine.ast.AstNode;
import com.google.dart.engine.ast.Expression;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.HtmlElement;
import com.google.dart.engine.html.ast.HtmlUnit;
import com.google.dart.engine.html.ast.HtmlUnitUtils;
import com.google.dart.engine.index.Index;
import com.google.dart.engine.search.SearchEngineFactory;
import com.google.dart.engine.services.assist.AssistContext;
import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.ui.internal.text.dart.DartCompletionProposalComputer;
import com.google.dart.tools.ui.text.dart.ContentAssistInvocationContext;
import com.google.dart.tools.ui.text.dart.DartContentAssistInvocationContext;
import com.google.dart.tools.wst.ui.HtmlReconcilerHook;
import com.google.dart.tools.wst.ui.HtmlReconcilerManager;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.wst.sse.ui.contentassist.CompletionProposalInvocationContext;
import org.eclipse.wst.sse.ui.contentassist.ICompletionProposalComputer;

import java.util.List;

/**
 * {@link ICompletionProposalComputer} for Angular HTML.
 */
public class AngularCompletionProposalComputer implements ICompletionProposalComputer {
  private static final List<ICompletionProposal> EMPTY_PROPOSALS = ImmutableList.of();
  private final DartCompletionProposalComputer proposalComputer = new DartCompletionProposalComputer();

  @Override
  public List<ICompletionProposal> computeCompletionProposals(
      CompletionProposalInvocationContext context, IProgressMonitor monitor) {
    // prepare resolved HtmlUnit
    HtmlUnit htmlUnit = getResolvedHtmlUnit(context);
    if (htmlUnit == null) {
      return EMPTY_PROPOSALS;
    }
    // prepare HtmlElement
    final HtmlElement htmlElement = htmlUnit.getElement();
    if (htmlElement == null) {
      return EMPTY_PROPOSALS;
    }
    // find Expression
    int offset = context.getInvocationOffset();
    final Expression expression = HtmlUnitUtils.getExpression(htmlUnit, offset);
    if (expression == null) {
      return EMPTY_PROPOSALS;
    }
    // prepare AssistContext
    final AssistContext assistContext;
    {
      AnalysisContext analysisContext = htmlElement.getContext();
      Index index = DartCore.getProjectManager().getIndex();
      assistContext = new AssistContext(
          SearchEngineFactory.createSearchEngine(index),
          analysisContext,
          null,
          offset,
          0) {
        @Override
        public CompilationUnitElement getCompilationUnitElement() {
          return htmlElement.getAngularCompilationUnit();
        }

        @Override
        public AstNode getCoveredNode() {
          return expression;
        }
      };
    }
    //
    ITextViewer viewer = context.getViewer();
    ContentAssistInvocationContext completionContext = new DartContentAssistInvocationContext(
        viewer,
        offset,
        null) {
      @Override
      public AssistContext getAssistContext() {
        return assistContext;
      }
    };
    return proposalComputer.computeCompletionProposals(completionContext, monitor);
  }

  @Override
  public List<ICompletionProposal> computeContextInformation(
      CompletionProposalInvocationContext context, IProgressMonitor monitor) {
    List<ICompletionProposal> proposals = Lists.newArrayList();
    return proposals;
  }

  @Override
  public String getErrorMessage() {
    return null;
  }

  @Override
  public void sessionEnded() {
  }

  @Override
  public void sessionStarted() {
  }

  private HtmlUnit getResolvedHtmlUnit(CompletionProposalInvocationContext context) {
    IDocument document = context.getDocument();
    HtmlReconcilerHook reconciler = HtmlReconcilerManager.getInstance().reconcilerFor(document);
    return reconciler.getResolvedUnit();
  }
}
