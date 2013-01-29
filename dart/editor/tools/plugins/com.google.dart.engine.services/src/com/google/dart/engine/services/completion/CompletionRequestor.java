package com.google.dart.engine.services.completion;

/**
 * A pathway for reporting completion proposals back to the client.
 */
public interface CompletionRequestor {

  /**
   * Record the given completion proposal for eventual presentation to the user.
   */
  void accept(CompletionProposal proposal);

  void beginReporting();

  void endReporting();
}
