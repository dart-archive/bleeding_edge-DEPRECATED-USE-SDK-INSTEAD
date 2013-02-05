package com.google.dart.engine.services.completion;

import com.google.dart.engine.services.assist.AssistContext;

import static com.google.dart.engine.services.completion.ProposalKind.METHOD;

/**
 * The analysis engine for code completion.
 */
public class CompletionEngine {

  private CompletionRequestor requestor;
  private CompletionFactory factory;
  private AssistContext context;

  public CompletionEngine(CompletionRequestor requestor, CompletionFactory factory) {
    this.requestor = requestor;
    this.factory = factory;
  }

  /**
   * Analyze the source unit in the given context to determine completion proposals at the selection
   * offset of the context.
   */
  public void complete(AssistContext context) {
    this.context = context;
    requestor.accept(factory.createCompletionProposal(METHOD)); // TODO: silence warning
  }

  public AssistContext getContext() {
    return context;
  }
}
