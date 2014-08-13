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
 *
 * This file has been automatically generated.  Please do not edit it manually.
 * To regenerate the file, use the script "pkg/analysis_server/spec/generate_files".
 */
package com.google.dart.server.generated.types;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.google.dart.server.utilities.general.ObjectUtilities;
import org.apache.commons.lang3.StringUtils;

/**
 * A suggestion for how to complete partially entered text. Many of the fields are optional,
 * depending on the kind of element being suggested.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class CompletionSuggestion {

  /**
   * The identifier to be inserted if the suggestion is selected. If the suggestion is for a method
   * or function, the client might want to additionally insert a template for the parameters. The
   * information required in order to do so is contained in other fields.
   */
  private final String completion;

  /**
   * The class that declares the element being suggested. This field is omitted if the suggested
   * element is not a member of a class.
   */
  private final String declaringType;

  /**
   * The Dartdoc associated with the element being suggested, This field is omitted if there is no
   * Dartdoc associated with the element.
   */
  private final String docComplete;

  /**
   * An abbreviated version of the Dartdoc associated with the element being suggested, This field is
   * omitted if there is no Dartdoc associated with the element.
   */
  private final String docSummary;

  /**
   * True if the suggested element is deprecated.
   */
  private final boolean isDeprecated;

  /**
   * True if the element is not known to be valid for the target. This happens if the type of the
   * target is dynamic.
   */
  private final boolean isPotential;

  /**
   * The kind of element being suggested.
   */
  private final String kind;

  /**
   * The name of the optional parameter being suggested. This field is omitted if the suggestion is
   * not the addition of an optional argument within an argument list.
   */
  private final String parameterName;

  /**
   * The names of the parameters of the function or method being suggested. This field is omitted if
   * the suggested element is not a setter, function or method.
   */
  private final List<String> parameterNames;

  /**
   * The type of the options parameter being suggested. This field is omitted if the parameterName
   * field is omitted.
   */
  private final String parameterType;

  /**
   * The types of the parameters of the function or method being suggested. This field is omitted if
   * the parameterNames field is omitted.
   */
  private final List<String> parameterTypes;

  /**
   * The number of positional parameters for the function or method being suggested. This field is
   * omitted if the parameterNames field is omitted.
   */
  private final int positionalParameterCount;

  /**
   * The relevance of this completion suggestion.
   */
  private final String relevance;

  /**
   * The number of required parameters for the function or method being suggested. This field is
   * omitted if the parameterNames field is omitted.
   */
  private final int requiredParameterCount;

  /**
   * The return type of the getter, function or method being suggested. This field is omitted if the
   * suggested element is not a getter, function or method.
   */
  private final String returnType;

  /**
   * The number of characters that should be selected after insertion.
   */
  private final int selectionLength;

  /**
   * The offset, relative to the beginning of the completion, of where the selection should be placed
   * after insertion.
   */
  private final int selectionOffset;

  /**
   * Constructor for {@link CompletionSuggestion}.
   */
  public CompletionSuggestion(String kind, String relevance, String completion, int selectionOffset, int selectionLength, boolean isDeprecated, boolean isPotential, String docSummary, String docComplete, String declaringType, String returnType, List<String> parameterNames, List<String> parameterTypes, int requiredParameterCount, int positionalParameterCount, String parameterName, String parameterType) {
    this.kind = kind;
    this.relevance = relevance;
    this.completion = completion;
    this.selectionOffset = selectionOffset;
    this.selectionLength = selectionLength;
    this.isDeprecated = isDeprecated;
    this.isPotential = isPotential;
    this.docSummary = docSummary;
    this.docComplete = docComplete;
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
  public boolean equals(Object obj) {
    if (obj instanceof CompletionSuggestion) {
      CompletionSuggestion other = (CompletionSuggestion) obj;
      return
        ObjectUtilities.equals(other.kind, kind) &&
        ObjectUtilities.equals(other.relevance, relevance) &&
        ObjectUtilities.equals(other.completion, completion) &&
        other.selectionOffset == selectionOffset &&
        other.selectionLength == selectionLength &&
        other.isDeprecated == isDeprecated &&
        other.isPotential == isPotential &&
        ObjectUtilities.equals(other.docSummary, docSummary) &&
        ObjectUtilities.equals(other.docComplete, docComplete) &&
        ObjectUtilities.equals(other.declaringType, declaringType) &&
        ObjectUtilities.equals(other.returnType, returnType) &&
        ObjectUtilities.equals(other.parameterNames, parameterNames) &&
        ObjectUtilities.equals(other.parameterTypes, parameterTypes) &&
        other.requiredParameterCount == requiredParameterCount &&
        other.positionalParameterCount == positionalParameterCount &&
        ObjectUtilities.equals(other.parameterName, parameterName) &&
        ObjectUtilities.equals(other.parameterType, parameterType);
    }
    return false;
  }

  /**
   * The identifier to be inserted if the suggestion is selected. If the suggestion is for a method
   * or function, the client might want to additionally insert a template for the parameters. The
   * information required in order to do so is contained in other fields.
   */
  public String getCompletion() {
    return completion;
  }

  /**
   * The class that declares the element being suggested. This field is omitted if the suggested
   * element is not a member of a class.
   */
  public String getDeclaringType() {
    return declaringType;
  }

  /**
   * The Dartdoc associated with the element being suggested, This field is omitted if there is no
   * Dartdoc associated with the element.
   */
  public String getDocComplete() {
    return docComplete;
  }

  /**
   * An abbreviated version of the Dartdoc associated with the element being suggested, This field is
   * omitted if there is no Dartdoc associated with the element.
   */
  public String getDocSummary() {
    return docSummary;
  }

  /**
   * True if the suggested element is deprecated.
   */
  public boolean getIsDeprecated() {
    return isDeprecated;
  }

  /**
   * True if the element is not known to be valid for the target. This happens if the type of the
   * target is dynamic.
   */
  public boolean getIsPotential() {
    return isPotential;
  }

  /**
   * The kind of element being suggested.
   */
  public String getKind() {
    return kind;
  }

  /**
   * The name of the optional parameter being suggested. This field is omitted if the suggestion is
   * not the addition of an optional argument within an argument list.
   */
  public String getParameterName() {
    return parameterName;
  }

  /**
   * The names of the parameters of the function or method being suggested. This field is omitted if
   * the suggested element is not a setter, function or method.
   */
  public List<String> getParameterNames() {
    return parameterNames;
  }

  /**
   * The type of the options parameter being suggested. This field is omitted if the parameterName
   * field is omitted.
   */
  public String getParameterType() {
    return parameterType;
  }

  /**
   * The types of the parameters of the function or method being suggested. This field is omitted if
   * the parameterNames field is omitted.
   */
  public List<String> getParameterTypes() {
    return parameterTypes;
  }

  /**
   * The number of positional parameters for the function or method being suggested. This field is
   * omitted if the parameterNames field is omitted.
   */
  public int getPositionalParameterCount() {
    return positionalParameterCount;
  }

  /**
   * The relevance of this completion suggestion.
   */
  public String getRelevance() {
    return relevance;
  }

  /**
   * The number of required parameters for the function or method being suggested. This field is
   * omitted if the parameterNames field is omitted.
   */
  public int getRequiredParameterCount() {
    return requiredParameterCount;
  }

  /**
   * The return type of the getter, function or method being suggested. This field is omitted if the
   * suggested element is not a getter, function or method.
   */
  public String getReturnType() {
    return returnType;
  }

  /**
   * The number of characters that should be selected after insertion.
   */
  public int getSelectionLength() {
    return selectionLength;
  }

  /**
   * The offset, relative to the beginning of the completion, of where the selection should be placed
   * after insertion.
   */
  public int getSelectionOffset() {
    return selectionOffset;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("kind=");
    builder.append(kind.toString() + ", ");
    builder.append("relevance=");
    builder.append(relevance.toString() + ", ");
    builder.append("completion=");
    builder.append(completion.toString() + ", ");
    builder.append("selectionOffset=");
    builder.append(selectionOffset + ", ");
    builder.append("selectionLength=");
    builder.append(selectionLength + ", ");
    builder.append("isDeprecated=");
    builder.append(isDeprecated + ", ");
    builder.append("isPotential=");
    builder.append(isPotential + ", ");
    builder.append("docSummary=");
    builder.append(docSummary.toString() + ", ");
    builder.append("docComplete=");
    builder.append(docComplete.toString() + ", ");
    builder.append("declaringType=");
    builder.append(declaringType.toString() + ", ");
    builder.append("returnType=");
    builder.append(returnType.toString() + ", ");
    builder.append("parameterNames=");
    builder.append(StringUtils.join(parameterNames, ", ") + ", ");
    builder.append("parameterTypes=");
    builder.append(StringUtils.join(parameterTypes, ", ") + ", ");
    builder.append("requiredParameterCount=");
    builder.append(requiredParameterCount + ", ");
    builder.append("positionalParameterCount=");
    builder.append(positionalParameterCount + ", ");
    builder.append("parameterName=");
    builder.append(parameterName.toString() + ", ");
    builder.append("parameterType=");
    builder.append(parameterType.toString() + ", ");
    builder.append("]");
    return builder.toString();
  }

}
