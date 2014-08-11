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
import com.google.dart.server.CompletionSuggestion;
import com.google.dart.server.CompletionSuggestionKind;
import com.google.dart.tools.core.completion.CompletionProposal;

import org.eclipse.core.runtime.IProgressMonitor;

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
    String detailsText = suggestion.getElementDocDetails();
    return DartDocUtilities.getDartDocAsHtml(detailsText);
  }

  @Override
  public String getElementDocSummary() {
    return suggestion.getElementDocSummary();
  }

  @Override
  public int getKind() {
    switch (suggestion.getKind()) {
      case ARGUMENT_LIST:
        return CompletionProposal.ARGUMENT_LIST;
      case CLASS:
        return CompletionProposal.TYPE_REF;
      case CLASS_ALIAS:
        return CompletionProposal.TYPE_REF;
      case CONSTRUCTOR:
        return CompletionProposal.METHOD_REF;
      case FIELD:
        return CompletionProposal.FIELD_REF;
      case FUNCTION:
        return CompletionProposal.METHOD_REF;
      case FUNCTION_TYPE_ALIAS:
        return CompletionProposal.TYPE_REF;
      case GETTER:
        return CompletionProposal.FIELD_REF;
      case IMPORT:
        return CompletionProposal.TYPE_IMPORT;
      case LIBRARY_PREFIX:
        return CompletionProposal.LIBRARY_PREFIX;
      case LOCAL_VARIABLE:
        return CompletionProposal.LOCAL_VARIABLE_REF;
      case METHOD:
        return CompletionProposal.METHOD_REF;
      case METHOD_NAME:
        return CompletionProposal.METHOD_NAME_REFERENCE;
      case OPTIONAL_ARGUMENT:
        return CompletionProposal.OPTIONAL_ARGUMENT;
      case NAMED_ARGUMENT:
        return CompletionProposal.NAMED_ARGUMENT;
      case PARAMETER:
        return CompletionProposal.LOCAL_VARIABLE_REF;
      case SETTER:
        return CompletionProposal.FIELD_REF;
      case TYPE_PARAMETER:
        return CompletionProposal.TYPE_REF;
      default:
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
      return suggestion.getRelevance().ordinal();
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
    return suggestion.hasNamed();
  }

  @Override
  public boolean hasOptionalParameters() {
    return suggestion.hasPositional() || suggestion.hasNamed();
  }

  @Override
  public boolean isConstructor() {
    return suggestion.getKind() == CompletionSuggestionKind.CONSTRUCTOR;
  }

  @Override
  public boolean isDeprecated() {
    return suggestion.isDeprecated();
  }

  public boolean isGetter() {
    return suggestion.getKind() == CompletionSuggestionKind.GETTER;
  }

  @Override
  public boolean isPotentialMatch() {
    return suggestion.isPotential();
  }

  public boolean isSetter() {
    return suggestion.getKind() == CompletionSuggestionKind.SETTER;
  }

  private char[][] copyStrings(String[] strings) {
    char[][] chars = new char[strings.length][];
    for (int i = 0; i < strings.length; i++) {
      chars[i] = strings[i].toCharArray();
    }
    return chars;
  }
}
