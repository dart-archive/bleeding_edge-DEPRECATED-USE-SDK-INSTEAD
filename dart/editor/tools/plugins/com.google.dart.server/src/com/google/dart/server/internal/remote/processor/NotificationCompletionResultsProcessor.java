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
package com.google.dart.server.internal.remote.processor;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.CompletionRelevance;
import com.google.dart.server.CompletionSuggestion;
import com.google.dart.server.CompletionSuggestionKind;
import com.google.dart.server.internal.CompletionSuggestionImpl;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Iterator;
import java.util.List;

/**
 * Processor for "completion.results" notification.
 * 
 * @coverage dart.server.remote
 */
public class NotificationCompletionResultsProcessor extends NotificationProcessor {

  /**
   * Return the {@link CompletionSuggestionKind} code for the given name. If the passed name cannot
   * be found, an {@link IllegalArgumentException} is thrown.
   */
  @VisibleForTesting
  public static CompletionSuggestionKind getCompletionSuggestionKind(String kindName) {
    return CompletionSuggestionKind.valueOf(kindName);
  }

  public NotificationCompletionResultsProcessor(AnalysisServerListener listener) {
    super(listener);
  }

  /**
   * Process the given {@link JsonObject} notification and notify {@link #listener}.
   */
  @Override
  public void process(JsonObject response) throws Exception {
    JsonObject paramsObject = response.get("params").getAsJsonObject();
    String completionId = paramsObject.get("id").getAsString();
    JsonArray resultsArray = paramsObject.get("results").getAsJsonArray();
    int replacementOffset = paramsObject.get("replacementOffset").getAsInt();
    int replacementLength = paramsObject.get("replacementLength").getAsInt();
    boolean last = paramsObject.get("last").getAsBoolean();
    // compute outline and notify listener
    getListener().computedCompletion(
        completionId,
        replacementOffset,
        replacementLength,
        constructCompletions(resultsArray),
        last);
  }

  private CompletionSuggestion[] constructCompletions(JsonArray resultsArray) {
    Iterator<JsonElement> completionObjectIterator = resultsArray.iterator();
    List<CompletionSuggestion> completions = Lists.newArrayList();
    while (completionObjectIterator.hasNext()) {
      JsonObject completionObject = completionObjectIterator.next().getAsJsonObject();
      CompletionSuggestionKind kind = getCompletionSuggestionKind(completionObject.get("kind").getAsString());
      CompletionRelevance relevance = CompletionRelevance.valueOf(completionObject.get("relevance").getAsString());
      String completion = completionObject.get("completion").getAsString();
      int selectionOffset = completionObject.get("selectionOffset").getAsInt();
      int selectionLength = completionObject.get("selectionLength").getAsInt();
      boolean isDeprecated = completionObject.get("isDeprecated").getAsBoolean();
      boolean isPotential = completionObject.get("isPotential").getAsBoolean();
      // optional parameters
      String docSummary = safelyGetAsString(completionObject, "docSummary");
      String docComplete = safelyGetAsString(completionObject, "docComplete");
      String declaringType = safelyGetAsString(completionObject, "declaringType");
      String returnType = safelyGetAsString(completionObject, "returnType");
      String[] parameterNames = constructStringArray(safelyGetAsJsonArray(
          completionObject,
          "parameterNames"));
      String[] parameterTypes = constructStringArray(safelyGetAsJsonArray(
          completionObject,
          "parameterTypes"));
      int requiredParameterCount = safelyGetAsInt(completionObject, "requiredParameterCount", 0);
      int positionalParameterCount = safelyGetAsInt(completionObject, "positionalParameterCount", 0);
      String parameterName = safelyGetAsString(completionObject, "parameterName");
      String parameterType = safelyGetAsString(completionObject, "parameterType");
      completions.add(new CompletionSuggestionImpl(
          kind,
          relevance,
          completion,
          selectionOffset,
          selectionLength,
          isDeprecated,
          isPotential,
          docSummary,
          docComplete,
          declaringType,
          returnType,
          parameterNames,
          parameterTypes,
          requiredParameterCount,
          positionalParameterCount,
          parameterName,
          parameterType));
    }
    return completions.toArray(new CompletionSuggestion[completions.size()]);
  }
}
