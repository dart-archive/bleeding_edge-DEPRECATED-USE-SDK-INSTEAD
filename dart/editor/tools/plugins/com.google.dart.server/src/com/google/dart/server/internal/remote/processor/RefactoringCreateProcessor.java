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

import com.google.dart.server.Parameter;
import com.google.dart.server.RefactoringCreateConsumer;
import com.google.dart.server.RefactoringProblem;
import com.google.dart.server.internal.ParameterImpl;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.Iterator;
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
    Map<String, Object> feedback = new HashMap<String, Object>();
    // names: List<String>
    JsonElement namesElt = resultObject.get("names");
    if (namesElt != null) {
      String[] names = constructStringArray(namesElt.getAsJsonArray());
      feedback.put("names", names);
    }
    // offsets: List<int>
    JsonElement offsetsElt = resultObject.get("offsets");
    if (offsetsElt != null) {
      int[] offsets = constructIntArray(offsetsElt.getAsJsonArray());
      feedback.put("offsets", offsets);
    }
    // lengths: List<int>
    JsonElement lengthsElt = resultObject.get("lengths");
    if (lengthsElt != null) {
      int[] lengths = constructIntArray(lengthsElt.getAsJsonArray());
      feedback.put("lengths", lengths);
    }
    // offset: int
    JsonElement offsetElt = resultObject.get("offset");
    if (offsetElt != null) {
      int offset = offsetElt.getAsInt();
      feedback.put("offset", offset);
    }
    // length: int
    JsonElement lengthElt = resultObject.get("length");
    if (lengthElt != null) {
      int length = lengthElt.getAsInt();
      feedback.put("length", length);
    }
    // returnType: String
    JsonElement returnTypeElt = resultObject.get("returnType");
    if (returnTypeElt != null) {
      String returnType = returnTypeElt.getAsString();
      feedback.put("returnType", returnType);
    }
    // canCreateGetter: boolean
    JsonElement canCreateGetterElt = resultObject.get("canCreateGetter");
    if (canCreateGetterElt != null) {
      boolean canCreateGetter = canCreateGetterElt.getAsBoolean();
      feedback.put("canCreateGetter", canCreateGetter);
    }
    // parameters: List<Parameter>
    JsonElement parametersElt = resultObject.get("parameters");
    if (parametersElt != null) {
      Parameter[] parameters = constructParameterArray(parametersElt.getAsJsonArray());
      feedback.put("parameters", parameters);
    }
    // occurrences: int
    JsonElement occurrencesElt = resultObject.get("occurrences");
    if (occurrencesElt != null) {
      int occurrences = occurrencesElt.getAsInt();
      feedback.put("occurrences", occurrences);
    }
    //
    // Pass the response back to the consumer.
    //
    consumer.computedStatus(refactoringId, status, feedback);
  }

  private Parameter constructParameter(JsonObject parameterObject) {
    String type = parameterObject.get("type").getAsString();
    String name = parameterObject.get("name").getAsString();
    return new ParameterImpl(type, name);
  }

  private Parameter[] constructParameterArray(JsonArray jsonArray) {
    if (jsonArray == null) {
      return new Parameter[] {};
    }
    int i = 0;
    Parameter[] parameters = new Parameter[jsonArray.size()];
    Iterator<JsonElement> iterator = jsonArray.iterator();
    while (iterator.hasNext()) {
      parameters[i] = constructParameter(iterator.next().getAsJsonObject());
      ++i;
    }
    return parameters;
  }
}
