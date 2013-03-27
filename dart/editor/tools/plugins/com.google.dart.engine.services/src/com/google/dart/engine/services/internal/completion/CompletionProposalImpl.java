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

/**
 * Completion proposal data.
 * 
 * @coverage com.google.dart.engine.services.completion
 */
import com.google.dart.engine.services.completion.CompletionProposal;
import com.google.dart.engine.services.completion.ProposalKind;
import com.google.dart.engine.utilities.general.StringUtilities;

public class CompletionProposalImpl implements CompletionProposal {

  // All fields must be initialized to ensure getters never return null.
  private String completion = "";
  private String returnType = "";
  private String declaringType = "";
  private String[] parameterNames = StringUtilities.EMPTY_ARRAY;
  private String[] parameterTypes = StringUtilities.EMPTY_ARRAY;
  private ProposalKind kind = ProposalKind.NONE;
  private int location = 0;
  private int replacementLength = 0;
  private int positionalParameterCount = 0;
  private boolean named = false;
  private boolean positional = false;

  @Override
  public String getCompletion() {
    return completion;
  }

  @Override
  public String getDeclaringType() {
    return declaringType;
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
  public String[] getParameterNames() {
    return parameterNames;
  }

  @Override
  public String[] getParameterTypes() {
    return parameterTypes;
  }

  @Override
  public int getPositionalParameterCount() {
    return positionalParameterCount;
  }

  @Override
  public int getReplacementLength() {
    return replacementLength;
  }

  @Override
  public String getReturnType() {
    return returnType;
  }

  @Override
  public boolean hasNamed() {
    return named;
  }

  @Override
  public boolean hasPositional() {
    return positional;
  }

  @Override
  public CompletionProposal setCompletion(String x) {
    completion = x;
    if (replacementLength == 0) {
      setReplacementLength(x.length());
    }
    return this;
  }

  @Override
  public CompletionProposal setDeclaringType(String name) {
    declaringType = name;
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
  public CompletionProposal setParameterNames(String[] paramNames) {
    parameterNames = paramNames;
    return this;
  }

  @Override
  public CompletionProposal setParameterStyle(int count, boolean named, boolean positional) {
    this.named = named;
    this.positional = positional;
    this.positionalParameterCount = count;
    return this;
  }

  @Override
  public CompletionProposal setParameterTypes(String[] paramTypes) {
    parameterTypes = paramTypes;
    return this;
  }

  @Override
  public CompletionProposal setReplacementLength(int x) {
    replacementLength = x;
    return this;
  }

  @Override
  public CompletionProposal setReturnType(String name) {
    returnType = name;
    return this;
  }

}
