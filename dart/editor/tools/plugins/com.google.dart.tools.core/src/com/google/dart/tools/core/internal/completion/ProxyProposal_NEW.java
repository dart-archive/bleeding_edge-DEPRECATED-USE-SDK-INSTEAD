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
package com.google.dart.tools.core.internal.completion;

import com.google.dart.engine.services.util.DartDocUtilities;
import com.google.dart.server.generated.types.CompletionRelevance;
import com.google.dart.server.generated.types.CompletionSuggestion;
import com.google.dart.server.generated.types.CompletionSuggestionKind;
import com.google.dart.tools.core.completion.CompletionProposal;

import org.eclipse.core.runtime.IProgressMonitor;

import java.util.List;

/**
 * Wrap String-based completion proposals for use in legacy char[]-based client code.
 * 
 * @coverage dart.tools.core.completion
 */
public class ProxyProposal_NEW extends CompletionProposal {

  private final int replacementOffset;
  private final int replacementLength;
  private final CompletionSuggestion suggestion;
  private int partitionOffset;

  public ProxyProposal_NEW(int replacementOffset, int replacementLength,
      CompletionSuggestion suggestion) {
    this.replacementOffset = replacementOffset;
    this.replacementLength = replacementLength;
    this.suggestion = suggestion;
  }

  @Override
  public void applyPartitionOffset(int partitionOffset) {
    this.partitionOffset = partitionOffset;
  }

  @Override
  public char[][] findParameterNames(IProgressMonitor monitor) {
    return getParameterNames();
  }

  @Override
  public char[] getCompletion() {
    return suggestion.getCompletion().toCharArray();
  }

  @Override
  public int getCompletionLocation() {
    return partitionOffset + replacementOffset - 1;
  }

  @Override
  public char[] getDeclarationSignature() {
    return suggestion.getDeclaringType().toCharArray();
  }

  @Override
  public String getElementDocDetails() {
    return DartDocUtilities.getDartDocAsHtml(suggestion.getDocComplete());
  }

  @Override
  public String getElementDocSummary() {
    return suggestion.getDocSummary();
  }

  @Override
  public int getKind() {
    String kind = suggestion.getKind();
    if (kind.equals(CompletionSuggestionKind.ARGUMENT_LIST)) {
      return CompletionProposal.ARGUMENT_LIST;
    } else if (kind.equals(CompletionSuggestionKind.CLASS)) {
      return CompletionProposal.TYPE_REF;
    } else if (kind.equals(CompletionSuggestionKind.CLASS_ALIAS)) {
      return CompletionProposal.TYPE_REF;
    } else if (kind.equals(CompletionSuggestionKind.CONSTRUCTOR)) {
      return CompletionProposal.METHOD_REF;
    } else if (kind.equals(CompletionSuggestionKind.FIELD)) {
      return CompletionProposal.FIELD_REF;
    } else if (kind.equals(CompletionSuggestionKind.FUNCTION)) {
      return CompletionProposal.METHOD_REF;
    } else if (kind.equals(CompletionSuggestionKind.FUNCTION_TYPE_ALIAS)) {
      return CompletionProposal.TYPE_REF;
    } else if (kind.equals(CompletionSuggestionKind.GETTER)) {
      return CompletionProposal.FIELD_REF;
    } else if (kind.equals(CompletionSuggestionKind.IMPORT)) {
      return CompletionProposal.TYPE_IMPORT;
    } else if (kind.equals(CompletionSuggestionKind.LIBRARY_PREFIX)) {
      return CompletionProposal.LIBRARY_PREFIX;
    } else if (kind.equals(CompletionSuggestionKind.LOCAL_VARIABLE)) {
      return CompletionProposal.LOCAL_VARIABLE_REF;
    } else if (kind.equals(CompletionSuggestionKind.METHOD)) {
      return CompletionProposal.METHOD_REF;
    } else if (kind.equals(CompletionSuggestionKind.METHOD_NAME)) {
      return CompletionProposal.METHOD_NAME_REFERENCE;
    } else if (kind.equals(CompletionSuggestionKind.OPTIONAL_ARGUMENT)) {
      return CompletionProposal.OPTIONAL_ARGUMENT;
    } else if (kind.equals(CompletionSuggestionKind.NAMED_ARGUMENT)) {
      return CompletionProposal.NAMED_ARGUMENT;
    } else if (kind.equals(CompletionSuggestionKind.PARAMETER)) {
      return CompletionProposal.LOCAL_VARIABLE_REF;
    } else if (kind.equals(CompletionSuggestionKind.SETTER)) {
      return CompletionProposal.FIELD_REF;
    } else if (kind.equals(CompletionSuggestionKind.TYPE_PARAMETER)) {
      return CompletionProposal.TYPE_REF;
    } else {
      return 0;
    }
  }

  @Override
  public char[] getName() {
    return suggestion.getCompletion().toCharArray();
  }

  @Override
  public String getParameterName() {
    return suggestion.getParameterName();
  }

  @Override
  public char[][] getParameterNames() {
    return copyStrings(suggestion.getParameterNames());
  }

  @Override
  public String getParameterType() {
    return suggestion.getParameterType();
  }

  @Override
  public char[][] getParameterTypeNames() {
    return copyStrings(suggestion.getParameterTypes());
  }

  @Override
  public int getPositionalParameterCount() {
    return suggestion.getPositionalParameterCount();
  }

  @Override
  public int getRelevance() {
    if (suggestion.getCompletion().startsWith("$dom_")) {
      return -1;
    } else {
      String relevance = suggestion.getRelevance();
      if (relevance.equals(CompletionRelevance.LOW)) {
        return 0;
      } else if (relevance.equals(CompletionRelevance.DEFAULT)) {
        return 1;
      } else {
        return 2;
      }
    }
  }

  @Override
  public int getReplaceEnd() {
    return replacementOffset + replacementLength;
  }

  @Override
  public int getReplaceEndIdentifier() {
    return replacementOffset + suggestion.getCompletion().length();
  }

  @Override
  public int getReplaceStart() {
    return replacementOffset;
  }

  @Override
  public char[] getReturnTypeName() {
    return suggestion.getReturnType().toCharArray();
  }

  @Override
  public char[] getSignature() {
    return suggestion.getCompletion().toCharArray();
  }

  @Override
  public boolean hasNamedParameters() {
    int numOfParams = suggestion.getParameterNames() != null
        ? suggestion.getParameterNames().size() : 0;
    if (numOfParams == 0) {
      return false;
    }
    return (numOfParams - suggestion.getRequiredParameterCount() - suggestion.getPositionalParameterCount()) > 0;

  }

  @Override
  public boolean hasOptionalParameters() {
    return suggestion.getPositionalParameterCount() > 0 || hasNamedParameters();
  }

  @Override
  public boolean isConstructor() {
    return suggestion.getKind().equals(CompletionSuggestionKind.CONSTRUCTOR);
  }

  @Override
  public boolean isDeprecated() {
    return suggestion.isDeprecated();
  }

  public boolean isGetter() {
    return suggestion.getKind().equals(CompletionSuggestionKind.GETTER);
  }

  @Override
  public boolean isPotentialMatch() {
    return suggestion.isPotential();
  }

  public boolean isSetter() {
    return suggestion.getKind().equals(CompletionSuggestionKind.SETTER);
  }

  private char[][] copyStrings(List<String> strings) {
    char[][] chars = new char[strings.size()][];
    int i = 0;
    for (String s : strings) {
      chars[i] = s.toCharArray();
      i++;
    }
    return chars;
  }
}
