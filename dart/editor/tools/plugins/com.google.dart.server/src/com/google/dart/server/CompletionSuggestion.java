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
package com.google.dart.server;

/**
 * The interface {@code CompletionSuggestion} defines the behavior of objects representing a
 * completion suggestions.
 * 
 * @coverage dart.server
 */
public interface CompletionSuggestion {
  /**
   * An empty array of suggestions.
   */
  CompletionSuggestion[] EMPTY_ARRAY = new CompletionSuggestion[0];

  /**
   * This character is used to specify location of the cursor after completion.
   */
  char CURSOR_MARKER = 0x2758;

  String getCompletion();

  String getDeclaringType();

  String getElementDocDetails();

  String getElementDocSummary();

  int getInsertionLength();

  CompletionSuggestionKind getKind();

  int getOffset();

  String getParameterName();

  String[] getParameterNames();

  String getParameterType();

  String[] getParameterTypes();

  int getPositionalParameterCount();

  CompletionRelevance getRelevance();

  int getReplacementLength();

  int getReplacementOffset();

  int getRequiredParameterCount();

  String getReturnType();

  int getSelectionLength();

  int getSelectionOffset();

  boolean hasNamed();

  boolean hasPositional();

  boolean isDeprecated();

  boolean isPotential();
}
