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

import com.google.dart.server.FindElementReferencesConsumer;
import com.google.dart.server.generated.types.Element;
import com.google.dart.server.generated.types.RequestError;
import com.google.gson.JsonObject;

/**
 * Instances of {@code FindElementReferencesProcessor} translate JSON result objects for a given
 * {@link FindElementReferencesConsumer}.
 * 
 * @coverage dart.server.remote
 */
public class FindElementReferencesProcessor extends ResultProcessor {

  private final FindElementReferencesConsumer consumer;

  public FindElementReferencesProcessor(FindElementReferencesConsumer consumer) {
    this.consumer = consumer;
  }

  public void process(JsonObject resultObject, RequestError requestError) {
    if (resultObject != null) {
      try {
        String searchId = resultObject.has("id") ? resultObject.get("id").getAsString() : null;
        Element element = resultObject.has("element")
            ? Element.fromJson(resultObject.get("element").getAsJsonObject()) : null;
        consumer.computedElementReferences(searchId, element);
      } catch (Exception exception) {
        // catch any exceptions in the formatting of this response
        requestError = generateRequestError(exception);
      }
    }
    if (requestError != null) {
      consumer.onError(requestError);
    }
  }
}
