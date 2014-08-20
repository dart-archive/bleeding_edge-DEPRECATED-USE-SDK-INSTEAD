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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
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
   * An empty array of {@link CompletionSuggestion}s.
   */
  public static final CompletionSuggestion[] EMPTY_ARRAY = new CompletionSuggestion[0];

  /**
   * The kind of element being suggested.
   */
  private final String kind;

  /**
   * The relevance of this completion suggestion.
   */
  private final String relevance;

  /**
   * The identifier to be inserted if the suggestion is selected. If the suggestion is for a method
   * or function, the client might want to additionally insert a template for the parameters. The
   * information required in order to do so is contained in other fields.
   */
  private final String completion;

  /**
   * The offset, relative to the beginning of the completion, of where the selection should be placed
   * after insertion.
   */
  private final Integer selectionOffset;

  /**
   * The number of characters that should be selected after insertion.
   */
  private final Integer selectionLength;

  /**
   * True if the suggested element is deprecated.
   */
  private final Boolean isDeprecated;

  /**
   * True if the element is not known to be valid for the target. This happens if the type of the
   * target is dynamic.
   */
  private final Boolean isPotential;

  /**
   * An abbreviated version of the Dartdoc associated with the element being suggested, This field is
   * omitted if there is no Dartdoc associated with the element.
   */
  private final String docSummary;

  /**
   * The Dartdoc associated with the element being suggested, This field is omitted if there is no
   * Dartdoc associated with the element.
   */
  private final String docComplete;

  /**
   * The class that declares the element being suggested. This field is omitted if the suggested
   * element is not a member of a class.
   */
  private final String declaringType;

  /**
   * The return type of the getter, function or method being suggested. This field is omitted if the
   * suggested element is not a getter, function or method.
   */
  private final String returnType;

  /**
   * The names of the parameters of the function or method being suggested. This field is omitted if
   * the suggested element is not a setter, function or method.
   */
  private final List<String> parameterNames;

  /**
   * The types of the parameters of the function or method being suggested. This field is omitted if
   * the parameterNames field is omitted.
   */
  private final List<String> parameterTypes;

  /**
   * The number of required parameters for the function or method being suggested. This field is
   * omitted if the parameterNames field is omitted.
   */
  private final Integer requiredParameterCount;

  /**
   * The number of positional parameters for the function or method being suggested. This field is
   * omitted if the parameterNames field is omitted.
   */
  private final Integer positionalParameterCount;

  /**
   * The name of the optional parameter being suggested. This field is omitted if the suggestion is
   * not the addition of an optional argument within an argument list.
   */
  private final String parameterName;

  /**
   * The type of the options parameter being suggested. This field is omitted if the parameterName
   * field is omitted.
   */
  private final String parameterType;

  /**
   * Constructor for {@link CompletionSuggestion}.
   */
  public CompletionSuggestion(String kind, String relevance, String completion, Integer selectionOffset, Integer selectionLength, Boolean isDeprecated, Boolean isPotential, String docSummary, String docComplete, String declaringType, String returnType, List<String> parameterNames, List<String> parameterTypes, Integer requiredParameterCount, Integer positionalParameterCount, String parameterName, String parameterType) {
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
        ObjectUtilities.equals(other.isDeprecated, isDeprecated) &&
        ObjectUtilities.equals(other.isPotential, isPotential) &&
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
  public Boolean getIsDeprecated() {
    return isDeprecated;
  }

  /**
   * True if the element is not known to be valid for the target. This happens if the type of the
   * target is dynamic.
   */
  public Boolean getIsPotential() {
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
  public Integer getPositionalParameterCount() {
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
  public Integer getRequiredParameterCount() {
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
  public Integer getSelectionLength() {
    return selectionLength;
  }

  /**
   * The offset, relative to the beginning of the completion, of where the selection should be placed
   * after insertion.
   */
  public Integer getSelectionOffset() {
    return selectionOffset;
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("kind", kind);
    jsonObject.addProperty("relevance", relevance);
    jsonObject.addProperty("completion", completion);
    jsonObject.addProperty("selectionOffset", selectionOffset);
    jsonObject.addProperty("selectionLength", selectionLength);
    jsonObject.addProperty("isDeprecated", isDeprecated);
    jsonObject.addProperty("isPotential", isPotential);
    if (docSummary != null) {
      jsonObject.addProperty("docSummary", docSummary);
    }
    if (docComplete != null) {
      jsonObject.addProperty("docComplete", docComplete);
    }
    if (declaringType != null) {
      jsonObject.addProperty("declaringType", declaringType);
    }
    if (returnType != null) {
      jsonObject.addProperty("returnType", returnType);
    }
    if (parameterNames != null) {
      JsonArray jsonArrayParameterNames = new JsonArray();
      for(String elt : parameterNames) {
        jsonArrayParameterNames.add(new JsonPrimitive(elt));
      }
      jsonObject.add("parameterNames", jsonArrayParameterNames);
    }
    if (parameterTypes != null) {
      JsonArray jsonArrayParameterTypes = new JsonArray();
      for(String elt : parameterTypes) {
        jsonArrayParameterTypes.add(new JsonPrimitive(elt));
      }
      jsonObject.add("parameterTypes", jsonArrayParameterTypes);
    }
    if (requiredParameterCount != null) {
      jsonObject.addProperty("requiredParameterCount", requiredParameterCount);
    }
    if (positionalParameterCount != null) {
      jsonObject.addProperty("positionalParameterCount", positionalParameterCount);
    }
    if (parameterName != null) {
      jsonObject.addProperty("parameterName", parameterName);
    }
    if (parameterType != null) {
      jsonObject.addProperty("parameterType", parameterType);
    }
    return jsonObject;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("kind=");
    builder.append(kind + ", ");
    builder.append("relevance=");
    builder.append(relevance + ", ");
    builder.append("completion=");
    builder.append(completion + ", ");
    builder.append("selectionOffset=");
    builder.append(selectionOffset + ", ");
    builder.append("selectionLength=");
    builder.append(selectionLength + ", ");
    builder.append("isDeprecated=");
    builder.append(isDeprecated + ", ");
    builder.append("isPotential=");
    builder.append(isPotential + ", ");
    builder.append("docSummary=");
    builder.append(docSummary + ", ");
    builder.append("docComplete=");
    builder.append(docComplete + ", ");
    builder.append("declaringType=");
    builder.append(declaringType + ", ");
    builder.append("returnType=");
    builder.append(returnType + ", ");
    builder.append("parameterNames=");
    builder.append(StringUtils.join(parameterNames, ", ") + ", ");
    builder.append("parameterTypes=");
    builder.append(StringUtils.join(parameterTypes, ", ") + ", ");
    builder.append("requiredParameterCount=");
    builder.append(requiredParameterCount + ", ");
    builder.append("positionalParameterCount=");
    builder.append(positionalParameterCount + ", ");
    builder.append("parameterName=");
    builder.append(parameterName + ", ");
    builder.append("parameterType=");
    builder.append(parameterType);
    builder.append("]");
    return builder.toString();
  }

}
