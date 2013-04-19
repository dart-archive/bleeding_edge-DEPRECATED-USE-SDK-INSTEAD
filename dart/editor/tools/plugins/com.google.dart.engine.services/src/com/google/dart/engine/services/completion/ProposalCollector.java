package com.google.dart.engine.services.completion;

import java.util.ArrayList;
import java.util.List;

class ProposalCollector implements CompletionRequestor {

  private CompletionRequestor requestor;
  private List<CompletionProposal> proposals;

  ProposalCollector(CompletionRequestor requestor) {
    this.requestor = requestor;
    this.proposals = new ArrayList<CompletionProposal>();
  }

  @Override
  public void accept(CompletionProposal proposal) {
    proposals.add(proposal);
    requestor.accept(proposal);
  }

  @Override
  public void beginReporting() {
    requestor.beginReporting();
  }

  @Override
  public void endReporting() {
    requestor.endReporting();
  }

  List<CompletionProposal> getProposals() {
    return proposals;
  }
}
