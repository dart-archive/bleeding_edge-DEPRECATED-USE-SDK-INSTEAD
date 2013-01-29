package com.google.dart.engine.services.completion;

/**
 * The factory class used to create completion proposals.
 */
public interface CompletionFactory {

  /**
   * Create a completion proposal of the given kind.
   */
  CompletionProposal createCompletionProposal(ProposalKind kind);

}
