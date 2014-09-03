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

import com.google.dart.server.GetRefactoringConsumer;
import com.google.dart.server.generated.types.RefactoringMethodParameter;
import com.google.dart.server.generated.types.RefactoringProblem;
import com.google.dart.server.generated.types.SourceChange;
import com.google.dart.server.utilities.general.JsonUtilities;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Instances of {@code GetRefactoringProcessor} translate JSON result objects for a given
 * {@link GetRefactoringConsumer}.
 * 
 * @coverage dart.server.remote
 */
public class GetRefactoringProcessor extends ResultProcessor {

  private final GetRefactoringConsumer consumer;

  public GetRefactoringProcessor(GetRefactoringConsumer consumer) {
    this.consumer = consumer;
  }

  public void process(JsonObject resultObject) {
    // problems
    List<RefactoringProblem> problems = RefactoringProblem.fromJsonArray(resultObject.get(
        "problems").getAsJsonArray());

    // change
    SourceChange change = SourceChange.fromJson(resultObject.get("change") != null
        ? resultObject.get("change").getAsJsonObject() : null);

    // potential edits
    List<String> potentialEdits = JsonUtilities.decodeStringList(resultObject.get("potentialEdits") != null
        ? resultObject.get("potentialEdits").getAsJsonArray() : null);

    //
    // Compute all refactoring-kind specific "Options" and put them into the feedback map
    //
    JsonObject feedbackObject = resultObject.get("feedback") != null
        ? resultObject.get("feedback").getAsJsonObject() : null;
    Map<String, Object> feedback = new HashMap<String, Object>();
    if (feedbackObject != null) {
      // names: List<String>
      JsonElement namesElt = feedbackObject.get("names");
      if (namesElt != null) {
        feedback.put("names", JsonUtilities.decodeStringList(namesElt.getAsJsonArray()));
      }
      // offsets: List<int>
      JsonElement offsetsElt = feedbackObject.get("offsets");
      if (offsetsElt != null) {
        feedback.put("offsets", JsonUtilities.decodeIntegerArray(offsetsElt.getAsJsonArray()));
      }
      // lengths: List<int>
      JsonElement lengthsElt = feedbackObject.get("lengths");
      if (lengthsElt != null) {
        feedback.put("lengths", JsonUtilities.decodeIntegerArray(lengthsElt.getAsJsonArray()));
      }
      // offset: int
      JsonElement offsetElt = feedbackObject.get("offset");
      if (offsetElt != null) {
        feedback.put("offset", offsetElt.getAsInt());
      }
      // length: int
      JsonElement lengthElt = feedbackObject.get("length");
      if (lengthElt != null) {
        feedback.put("length", lengthElt.getAsInt());
      }
      // returnType: String
      JsonElement returnTypeElt = feedbackObject.get("returnType");
      if (returnTypeElt != null) {
        feedback.put("returnType", returnTypeElt.getAsString());
      }
      // canCreateGetter: boolean
      JsonElement canCreateGetterElt = feedbackObject.get("canCreateGetter");
      if (canCreateGetterElt != null) {
        boolean canCreateGetter = canCreateGetterElt.getAsBoolean();
        feedback.put("canCreateGetter", canCreateGetter);
      }
      // parameters: List<Parameter>
      JsonElement parametersElt = feedbackObject.get("parameters");
      if (parametersElt instanceof JsonArray) {
        List<RefactoringMethodParameter> parameters = RefactoringMethodParameter.fromJsonArray((JsonArray) parametersElt);
        feedback.put("parameters", parameters);
      }
      // occurrences: int
      JsonElement occurrencesElt = feedbackObject.get("occurrences");
      if (occurrencesElt != null) {
        feedback.put("occurrences", occurrencesElt.getAsInt());
      }
    }

    consumer.computedRefactorings(problems, feedback, change, potentialEdits);
  }
}
