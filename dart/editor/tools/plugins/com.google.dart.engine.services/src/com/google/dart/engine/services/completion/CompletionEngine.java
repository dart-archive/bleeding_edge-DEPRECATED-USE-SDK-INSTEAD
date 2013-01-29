package com.google.dart.engine.services.completion;

import com.google.dart.engine.ast.CompilationUnit;

import static com.google.dart.engine.services.completion.ProposalKind.METHOD;

/**
 * The analysis engine for code completion.
 */
public class CompletionEngine {

  private CompletionRequestor requestor;
  private CompletionFactory factory;

  public CompletionEngine(CompletionRequestor requestor, CompletionFactory factory) {
    this.requestor = requestor;
    this.factory = factory;
  }

  /**
   * Analyze the given source unit to determine completion proposals at the given completion
   * position.
   */
  public void complete(CompilationUnit sourceUnit, int completionPosition) {
    requestor.accept(factory.createCompletionProposal(METHOD)); // TODO: silence warning
  }
}
