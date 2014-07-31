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

  /**
   * The identifier to be inserted if the suggestion is selected. If the suggestion is for a method
   * or function, the client might want to additionally insert a template for the parameters. The
   * information required in order to do so is contained in other fields.
   */
  String getCompletion();

  /**
   * The class that declares the element being suggested. This field is omitted if the suggested
   * element is not a member of a class.
   */
  String getDeclaringType();

  String getElementDocDetails();

  String getElementDocSummary();

  /**
   * The kind of element being suggested.
   */
  CompletionSuggestionKind getKind();

  /**
   * The name of the optional parameter being suggested. This field is omitted if the suggestion is
   * not the addition of an optional argument within an argument list.
   */
  String getParameterName();

  /**
   * The names of the parameters of the function or method being suggested. This field is omitted if
   * the suggested element is not a setter, function or method.
   */
  String[] getParameterNames();

  /**
   * The type of the options parameter being suggested. This field is omitted if the parameterName
   * field is omitted.
   */
  String getParameterType();

  /**
   * The types of the parameters of the function or method being suggested. This field is omitted if
   * the parameterNames field is omitted.
   */
  String[] getParameterTypes();

  /**
   * The number of positional parameters for the function or method being suggested. This field is
   * omitted if the parameterNames field is omitted.
   */
  int getPositionalParameterCount();

  /**
   * The relevance of this completion suggestion.
   */
  CompletionRelevance getRelevance();

  /**
   * The number of required parameters for the function or method being suggested. This field is
   * omitted if the parameterNames field is omitted.
   */
  int getRequiredParameterCount();

  /**
   * The return type of the getter, function or method being suggested. This field is omitted if the
   * suggested element is not a getter, function or method.
   */
  String getReturnType();

  /**
   * The number of characters that should be selected after insertion.
   */
  int getSelectionLength();

  /**
   * The offset, relative to the beginning of the completion, of where the selection should be
   * placed after insertion.
   */
  int getSelectionOffset();

  boolean hasNamed();

  boolean hasPositional();

  /**
   * {@code true} if the suggested element is deprecated.
   */
  boolean isDeprecated();

  /**
   * {@code true} if the element is not known to be valid for the target. This happens if the type
   * of the target is dynamic.
   */
  boolean isPotential();
}
