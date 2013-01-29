package com.google.dart.tools.core.internal.completion;

import com.google.dart.tools.core.completion.CompletionProposal;

public class ProxyProposal extends CompletionProposal {

  private com.google.dart.engine.services.completion.CompletionProposal proposal;

  public ProxyProposal(com.google.dart.engine.services.completion.CompletionProposal proposal) {
    this.proposal = proposal;
  }

}
