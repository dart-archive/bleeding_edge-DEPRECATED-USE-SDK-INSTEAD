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

import com.google.dart.engine.services.internal.completion.CompletionProposalImpl;

/**
 * The factory class used to create completion proposals.
 * 
 * @coverage com.google.dart.engine.services.completion
 */
public class CompletionFactory {

  /**
   * Create a completion proposal of the given kind.
   */
  public CompletionProposal createCompletionProposal(ProposalKind kind, int insertionPoint) {
    CompletionProposalImpl prop = new CompletionProposalImpl();
    prop.setKind(kind);
    prop.setLocation(insertionPoint);
    return prop;
  }

}
