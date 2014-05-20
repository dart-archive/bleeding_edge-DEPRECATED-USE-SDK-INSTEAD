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

import com.google.dart.server.CompletionSuggestion;
import com.google.dart.server.CompletionSuggestionKind;

/**
 * A concrete implementation of {@link CompletionSuggestion}.
 * 
 * @coverage dart.server.local
 */
public class CompletionSuggestionImpl implements CompletionSuggestion {
  private final String completion;
  private final String declaringType;
  private final String elementDocSummary;
  private final String elementDocDetails;
  private final CompletionSuggestionKind kind;
  private final int location;
  private final String parameterName;
  private final String[] parameterNames;
  private final String parameterType;
  private final String[] parameterTypes;
  private final int positionalParameterCount;
  private final int relevance;
  private final int replacementLength;
  private final int replacementLengthIdentifier;
  private final String returnType;
  private final boolean hasNamed;
  private final boolean hasPositional;
  private final boolean isDeprecated;
  private final boolean isPotentialMatch;

  public CompletionSuggestionImpl(String elementDocSummary, String elementDocDetails,
      String completion, String declaringType, CompletionSuggestionKind kind, int location,
      String parameterName, String[] parameterNames, String parameterType, String[] parameterTypes,
      int positionalParameterCount, int relevance, int replacementLength,
      int replacementLengthIdentifier, String returnType, boolean hasNamed, boolean hasPositional,
      boolean isDeprecated, boolean isPotentialMatch) {
    this.completion = completion;
    this.declaringType = declaringType;
    this.elementDocSummary = elementDocSummary;
    this.elementDocDetails = elementDocDetails;
    this.kind = kind;
    this.location = location;
    this.parameterName = parameterName;
    this.parameterNames = parameterNames;
    this.parameterType = parameterType;
    this.parameterTypes = parameterTypes;
    this.positionalParameterCount = positionalParameterCount;
    this.relevance = relevance;
    this.replacementLength = replacementLength;
    this.replacementLengthIdentifier = replacementLengthIdentifier;
    this.returnType = returnType;
    this.hasNamed = hasNamed;
    this.hasPositional = hasPositional;
    this.isDeprecated = isDeprecated;
    this.isPotentialMatch = isPotentialMatch;
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
  public CompletionSuggestionKind getKind() {
    return kind;
  }

  @Override
  public int getLocation() {
    return location;
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
  public int getRelevance() {
    return relevance;
  }

  @Override
  public int getReplacementLength() {
    return replacementLength;
  }

  @Override
  public int getReplacementLengthIdentifier() {
    return replacementLengthIdentifier;
  }

  @Override
  public String getReturnType() {
    return returnType;
  }

  @Override
  public boolean hasNamed() {
    return hasNamed;
  }

  @Override
  public boolean hasPositional() {
    return hasPositional;
  }

  @Override
  public boolean isDeprecated() {
    return isDeprecated;
  }

  @Override
  public boolean isPotentialMatch() {
    return isPotentialMatch;
  }
}
