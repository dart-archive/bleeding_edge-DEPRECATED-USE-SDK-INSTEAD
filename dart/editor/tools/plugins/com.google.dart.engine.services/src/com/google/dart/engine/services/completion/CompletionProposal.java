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

import com.google.dart.engine.element.Element;

public interface CompletionProposal {
  int RELEVANCE_LOW = 0;
  int RELEVANCE_DEFAULT = 10;
  int RELEVANCE_HIGH = 20;

  /**
   * This character is used to specify location of the cursor after completion.
   */
  char CURSOR_MARKER = 0x2758;

  void applyPartitionOffset(int partitionOffset);

  String getCompletion();

  String getDeclaringType();

  Element getElement();

  ProposalKind getKind();

  int getLocation();

  String getParameterName();

  String[] getParameterNames();

  String getParameterType();

  String[] getParameterTypes();

  int getPositionalParameterCount();

  int getRelevance();

  int getReplacementLength();

  int getReplacementLengthIdentifier();

  String getReturnType();

  boolean hasNamed();

  boolean hasPositional();

  CompletionProposal incRelevance();

  boolean isDeprecated();

  boolean isPotentialMatch();

  CompletionProposal setCompletion(String x);

  CompletionProposal setDeclaringType(String name);

  CompletionProposal setDeprecated(boolean deprecated);

  CompletionProposal setElement(Element element);

  CompletionProposal setKind(ProposalKind x);

  CompletionProposal setLocation(int x);

  CompletionProposal setParameterName(String paramName);

  CompletionProposal setParameterNames(String[] paramNames);

  CompletionProposal setParameterStyle(int count, boolean named, boolean positional);

  CompletionProposal setParameterType(String paramType);

  CompletionProposal setParameterTypes(String[] paramTypes);

  CompletionProposal setPotentialMatch(boolean isPotentialMatch);

  CompletionProposal setRelevance(int n);

  CompletionProposal setReplacementLength(int x);

  CompletionProposal setReplacementLengthIdentifier(int x);

  CompletionProposal setReturnType(String name);
}
