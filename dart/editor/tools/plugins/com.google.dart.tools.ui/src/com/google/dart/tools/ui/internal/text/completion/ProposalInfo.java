/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text.completion;

import com.google.dart.tools.core.completion.CompletionProposal;

import org.eclipse.core.runtime.IProgressMonitor;

public class ProposalInfo {
  private final CompletionProposal proposal;
  private final String comment;

  public ProposalInfo(CompletionProposal proposal, String comment) {
    this.proposal = proposal;
    this.comment = comment;
  }

  /**
   * Gets the text for this proposal info formatted as HTML, or <code>null</code> if no text is
   * available.
   * 
   * @param monitor a progress monitor
   * @return the additional info text
   */
  public final String getInfo(IProgressMonitor monitor) {
    return comment;
  }

  public CompletionProposal getProposal() {
    return proposal;
  }
}
