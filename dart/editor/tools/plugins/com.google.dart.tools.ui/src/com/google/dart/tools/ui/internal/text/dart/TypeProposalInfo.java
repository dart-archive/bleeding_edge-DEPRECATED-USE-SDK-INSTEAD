/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.tools.core.completion.CompletionProposal;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.Type;

/**
 * Proposal info that computes the Dart doc lazily when it is queried. TODO(pquitslund): this class
 * used to subclass {@link MemberProposalInfo} and needs to have more logic moved over
 */
public final class TypeProposalInfo extends ProposalInfo {

  protected final DartProject project;
  protected final CompletionProposal proposal;

  /**
   * Creates a new proposal info.
   * 
   * @param project the Dart project to reference when resolving types
   * @param proposal the proposal to generate information for
   */
  public TypeProposalInfo(DartProject project, CompletionProposal proposal) {
    this.project = project;
    this.proposal = proposal;
  }

  /**
   * Resolves the member described by the receiver and returns it if found. Returns
   * <code>null</code> if no corresponding member can be found.
   * 
   * @return the resolved member or <code>null</code> if none is found
   * @throws DartModelException if accessing the Dart model fails
   */
  @SuppressWarnings("deprecation")
  protected Type resolveType() throws DartModelException {
    String typeName = String.valueOf(proposal.getSignature());
    Type type = project.findType(typeName);
    if (type == null) {
      type = project.findType(new String(proposal.getCompletion()));
    }
    return type;
  }
}
