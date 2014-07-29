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

import com.google.dart.server.ErrorFixes;
import com.google.dart.server.FixesConsumer;
import com.google.dart.server.internal.ErrorFixesImpl;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Iterator;

/**
 * Instances of {@code FixesProcessor} translate JSON result objects for a given
 * {@link FixesConsumer}.
 * 
 * @coverage dart.server.remote
 */
public class FixesProcessor extends ResultProcessor {

  private final FixesConsumer consumer;

  public FixesProcessor(FixesConsumer consumer) {
    this.consumer = consumer;
  }

  public void process(JsonObject resultObject) {
    ErrorFixes[] errorFixesArray = constructErrorFixesArray(resultObject.get("fixes").getAsJsonArray());
    consumer.computedFixes(errorFixesArray);
  }

  private ErrorFixes constructErrorFixes(JsonObject jsonObject) {
    // TODO (jwren) "fixes" property after we have a data type in the spec
    return new ErrorFixesImpl(constructAnalysisError(jsonObject.get("error").getAsJsonObject()));
  }

  private ErrorFixes[] constructErrorFixesArray(JsonArray jsonArray) {
    if (jsonArray == null) {
      return ErrorFixes.EMPTY_ARRAY;
    }
    int i = 0;
    ErrorFixes[] errorFixesArray = new ErrorFixes[jsonArray.size()];
    Iterator<JsonElement> iterator = jsonArray.iterator();
    while (iterator.hasNext()) {
      errorFixesArray[i] = constructErrorFixes(iterator.next().getAsJsonObject());
      ++i;
    }
    return errorFixesArray;
  }
}
