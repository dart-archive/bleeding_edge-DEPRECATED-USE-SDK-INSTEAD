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
package com.google.dart.engine.services.internal.completion;

import com.google.dart.engine.services.completion.CompletionProposal;
import com.google.dart.engine.services.completion.ProposalKind;

public class CompletionProposalImpl implements CompletionProposal {

  private String completion;
  private String name;
  private ProposalKind kind;
  private int location;
  private int replacementLength;

  @Override
  public String getCompletion() {
    return completion;
  }

  @Override
  public ProposalKind getKind() {
    return kind;
  }

  @Override
  public int getLocation() {
    return location;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getReplacementLength() {
    return replacementLength;
  }

  @Override
  public CompletionProposal setCompletion(String x) {
    completion = x;
    return this;
  }

  @Override
  public CompletionProposal setKind(ProposalKind x) {
    kind = x;
    return this;
  }

  @Override
  public CompletionProposal setLocation(int x) {
    location = x;
    return this;
  }

  @Override
  public CompletionProposal setName(String x) {
    name = x;
    return this;
  }

  @Override
  public CompletionProposal setReplacementLength(int x) {
    replacementLength = x;
    return this;
  }

}
