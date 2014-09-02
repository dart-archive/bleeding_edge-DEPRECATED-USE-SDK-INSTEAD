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

import com.google.dart.server.RefactoringCreateConsumer;
import com.google.dart.server.RefactoringProblem;
import com.google.dart.server.generated.types.RefactoringMethodParameter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Instances of {@code RefactoringCreateProcessor} translate JSON result objects for a given
 * {@link RefactoringCreateConsumer}.
 * 
 * @coverage dart.server.remote
 */
public class RefactoringCreateProcessor extends ResultProcessor {

  private final RefactoringCreateConsumer consumer;

  public RefactoringCreateProcessor(RefactoringCreateConsumer consumer) {
    this.consumer = consumer;
  }

  public void process(JsonObject resultObject) {
    // // Compute the String refactoringId
    String refactoringId = resultObject.get("id").getAsString();

    // Compute the RefactoringProblem[] status
    RefactoringProblem[] status = constructRefactoringProblemArray(resultObject.get("status").getAsJsonArray());

    //
    // Compute all refactoring-kind specific "Options" and put them into the feedback map
    //
    JsonObject feedbackObject = resultObject.get("feedback").getAsJsonObject();
    Map<String, Object> feedback = new HashMap<String, Object>();
    // names: List<String>
    JsonElement namesElt = feedbackObject.get("names");
    if (namesElt != null) {
      String[] names = constructStringArray(namesElt.getAsJsonArray());
      feedback.put("names", names);
    }
    // offsets: List<int>
    JsonElement offsetsElt = feedbackObject.get("offsets");
    if (offsetsElt != null) {
      int[] offsets = constructIntArray(offsetsElt.getAsJsonArray());
      feedback.put("offsets", offsets);
    }
    // lengths: List<int>
    JsonElement lengthsElt = feedbackObject.get("lengths");
    if (lengthsElt != null) {
      int[] lengths = constructIntArray(lengthsElt.getAsJsonArray());
      feedback.put("lengths", lengths);
    }
    // offset: int
    JsonElement offsetElt = feedbackObject.get("offset");
    if (offsetElt != null) {
      int offset = offsetElt.getAsInt();
      feedback.put("offset", offset);
    }
    // length: int
    JsonElement lengthElt = feedbackObject.get("length");
    if (lengthElt != null) {
      int length = lengthElt.getAsInt();
      feedback.put("length", length);
    }
    // returnType: String
    JsonElement returnTypeElt = feedbackObject.get("returnType");
    if (returnTypeElt != null) {
      String returnType = returnTypeElt.getAsString();
      feedback.put("returnType", returnType);
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
      int occurrences = occurrencesElt.getAsInt();
      feedback.put("occurrences", occurrences);
    }
    //
    // Pass the response back to the consumer.
    //
    consumer.computedStatus(refactoringId, status, feedback);
  }

}
