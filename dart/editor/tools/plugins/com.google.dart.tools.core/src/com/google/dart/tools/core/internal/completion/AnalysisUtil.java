package com.google.dart.tools.core.internal.completion;

import com.google.dart.engine.services.completion.CompletionProposal;
import com.google.dart.engine.services.completion.CompletionRequestor;

/**
 * DEBUG: This class is only intended to facilitate using the new analysis engine for code
 * completion until support for the new engine is fully-plumbed into the editor.
 */
public class AnalysisUtil implements CompletionRequestor {

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
  public void endReporting() {
    requestor.endReporting();
  }

  public void setRequestor(com.google.dart.tools.core.completion.CompletionRequestor requestor) {
    this.requestor = requestor;
    requestor.acceptContext(new InternalCompletionContext());
  }

}
