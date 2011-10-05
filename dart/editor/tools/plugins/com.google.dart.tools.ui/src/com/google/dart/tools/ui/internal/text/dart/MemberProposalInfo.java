/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.dart;

import com.google.dart.tools.core.completion.CompletionProposal;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.core.model.DartProject;
import com.google.dart.tools.core.model.TypeMember;

import org.eclipse.core.runtime.Assert;

/**
 * Proposal info that computes the javadoc lazily when it is queried.
 */
public abstract class MemberProposalInfo extends ProposalInfo {
  /* configuration */
  protected final DartProject fJavaProject;
  protected final CompletionProposal fProposal;

  /* cache filled lazily */
  private boolean fJavaElementResolved = false;

  /**
   * Creates a new proposal info.
   * 
   * @param project the java project to reference when resolving types
   * @param proposal the proposal to generate information for
   */
  public MemberProposalInfo(DartProject project, CompletionProposal proposal) {
    Assert.isNotNull(project);
    Assert.isNotNull(proposal);
    fJavaProject = project;
    fProposal = proposal;
  }

  /**
   * Returns the java element that this computer corresponds to, possibly <code>null</code>.
   * 
   * @return the java element that this computer corresponds to, possibly <code>null</code>
   * @throws DartModelException
   */
  @Override
  public DartElement getJavaElement() throws DartModelException {
    if (!fJavaElementResolved) {
      fJavaElementResolved = true;
      fElement = resolveMember();
    }
    return fElement;
  }

  /**
   * Resolves the member described by the receiver and returns it if found. Returns
   * <code>null</code> if no corresponding member can be found.
   * 
   * @return the resolved member or <code>null</code> if none is found
   * @throws DartModelException if accessing the java model fails
   */
  protected abstract TypeMember resolveMember() throws DartModelException;

}
