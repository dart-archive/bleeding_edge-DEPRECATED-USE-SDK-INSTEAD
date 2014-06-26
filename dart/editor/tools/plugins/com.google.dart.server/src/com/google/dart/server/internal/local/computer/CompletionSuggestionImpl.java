/*
 * Copyright (c) 2014, the Dart project authors.
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
package com.google.dart.server.internal.local.computer;

import com.google.dart.server.CompletionRelevance;
import com.google.dart.server.CompletionSuggestion;
import com.google.dart.server.CompletionSuggestionKind;

/**
 * A concrete implementation of {@link CompletionSuggestion}.
 * 
 * @coverage dart.server.local
 */
public class CompletionSuggestionImpl implements CompletionSuggestion {
  private final CompletionSuggestionKind kind;
  private final CompletionRelevance relevance;
  private final String completion;
  private final int replacementOffset;
  private final int replacementLength;
  private final int insertionLength;
  private final int offset;
  private final int selectionOffset;
  private final int selectionLength;
  private final boolean isDeprecated;
  private final boolean isPotential;
  private final String elementDocSummary;
  private final String elementDocDetails;
  private final String declaringType;
  private final String returnType;
  private final String[] parameterNames;
  private final String[] parameterTypes;
  private final int requiredParameterCount;
  private final int positionalParameterCount;
  private final String parameterName;
  private final String parameterType;

  public CompletionSuggestionImpl(CompletionSuggestionKind kind, CompletionRelevance relevance,
      String completion, int replacementOffset, int replacementLength, int insertionLength,
      int offset, int selectionOffset, int selectionLength, boolean isDeprecated,
      boolean isPotential, String elementDocSummary, String elementDocDetails,
      String declaringType, String returnType, String[] parameterNames, String[] parameterTypes,
      int requiredParameterCount, int positionalParameterCount, String parameterName,
      String parameterType) {
    this.kind = kind;
    this.relevance = relevance;
    this.completion = completion;
    this.replacementOffset = replacementOffset;
    this.replacementLength = replacementLength;
    this.insertionLength = insertionLength;
    this.offset = offset;
    this.selectionOffset = selectionOffset;
    this.selectionLength = selectionLength;
    this.isDeprecated = isDeprecated;
    this.isPotential = isPotential;
    this.elementDocSummary = elementDocSummary;
    this.elementDocDetails = elementDocDetails;
    this.declaringType = declaringType;
    this.returnType = returnType;
    this.parameterNames = parameterNames;
    this.parameterTypes = parameterTypes;
    this.requiredParameterCount = requiredParameterCount;
    this.positionalParameterCount = positionalParameterCount;
    this.parameterName = parameterName;
    this.parameterType = parameterType;
  }

  @Override
  public String getCompletion() {
    return completion;
  }

  @Override
  public String getDeclaringType() {
    return declaringType;
  }

  @Override
  public String getElementDocDetails() {
    return elementDocDetails;
  }

  @Override
  public String getElementDocSummary() {
    return elementDocSummary;
  }

  @Override
  public int getInsertionLength() {
    return insertionLength;
  }

  @Override
  public CompletionSuggestionKind getKind() {
    return kind;
  }

  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public String getParameterName() {
    return parameterName;
  }

  @Override
  public String[] getParameterNames() {
    return parameterNames;
  }

  @Override
  public String getParameterType() {
    return parameterType;
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
  public CompletionRelevance getRelevance() {
    return relevance;
  }

  @Override
  public int getReplacementLength() {
    return replacementLength;
  }

  @Override
  public int getReplacementOffset() {
    return replacementOffset;
  }

  @Override
  public int getRequiredParameterCount() {
    return requiredParameterCount;
  }

  @Override
  public String getReturnType() {
    return returnType;
  }

  @Override
  public int getSelectionLength() {
    return selectionLength;
  }

  @Override
  public int getSelectionOffset() {
    return selectionOffset;
  }

  @Override
  public boolean hasNamed() {
    int numOfParams = parameterNames != null ? parameterNames.length : 0;
    if (numOfParams == 0) {
      return false;
    }
    return (numOfParams - requiredParameterCount - positionalParameterCount) > 0;
  }

  @Override
  public boolean hasPositional() {
    return positionalParameterCount > 0;
  }

  @Override
  public boolean isDeprecated() {
    return isDeprecated;
  }

  @Override
  public boolean isPotential() {
    return isPotential;
  }
}
