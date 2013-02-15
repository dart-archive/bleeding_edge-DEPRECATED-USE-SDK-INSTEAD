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

public interface CompletionProposal {

  String getCompletion();

  String getDeclaringType();

  ProposalKind getKind();

  int getLocation();

  String[] getParameterNames();

  String[] getParameterTypes();

  int getPositionalParameterCount();

  int getReplacementLength();

  String getReturnType();

  boolean hasNamed();

  boolean hasPositional();

  CompletionProposal setCompletion(String x);

  CompletionProposal setDeclaringType(String name);

  CompletionProposal setKind(ProposalKind x);

  CompletionProposal setLocation(int x);

  CompletionProposal setParameterNames(String[] paramNames);

  CompletionProposal setParameterStyle(int count, boolean named, boolean positional);

  CompletionProposal setParameterTypes(String[] paramTypes);

  CompletionProposal setReplacementLength(int x);

  CompletionProposal setReturnType(String name);
}
