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

  CompletionRequestor getRequestor() {
    return requestor;
  }
}
