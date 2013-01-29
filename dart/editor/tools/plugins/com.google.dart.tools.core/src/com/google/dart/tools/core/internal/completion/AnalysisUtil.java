package com.google.dart.tools.core.internal.completion;

import com.google.dart.engine.ast.CompilationUnit;
import com.google.dart.engine.services.completion.CompletionFactory;
import com.google.dart.engine.services.completion.CompletionProposal;
import com.google.dart.engine.services.completion.CompletionRequestor;
import com.google.dart.engine.services.completion.ProposalKind;

/**
 * DEBUG: This class is only intended to facilitate using the new analysis engine for code
 * completion until support for the new engine is fully-plumbed into the editor.
 */
public class AnalysisUtil implements CompletionFactory, CompletionRequestor {

  private com.google.dart.tools.core.completion.CompletionRequestor requestor;

  @Override
  public void accept(CompletionProposal proposal) {
    requestor.accept(new ProxyProposal(proposal));
  }

  @Override
  public void beginReporting() {
    requestor.beginReporting();
  }

  @Override
  public CompletionProposal createCompletionProposal(ProposalKind kind) {
    return null;
  }

  @Override
  public void endReporting() {
    requestor.endReporting();
  }

  public CompilationUnit parse(com.google.dart.tools.core.model.CompilationUnit sourceUnit) {
    return null;
  }

  public void setRequestor(com.google.dart.tools.core.completion.CompletionRequestor requestor) {
    this.requestor = requestor;
  }

}
