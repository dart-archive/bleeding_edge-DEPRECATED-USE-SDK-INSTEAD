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

import com.google.dart.server.MapUriConsumer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Instances of the class {@code MapUriProcessor} process the result of an {@code execution.mapUri}
 * request.
 */
public class MapUriProcessor extends ResultProcessor {
  /**
   * The consumer that will be notified when a result is processed.
   */
  private MapUriConsumer consumer;

  /**
   * Initialize a newly create result processor to process the result of an {@code execution.mapUri}
   * request.
   * 
   * @param consumer the consumer that will be notified when a result is processed
   */
  public MapUriProcessor(MapUriConsumer consumer) {
    this.consumer = consumer;
  }

  /**
   * Process the given result.
   * 
   * @param resultObject the result object to be processed
   */
  public void process(JsonObject resultObject) {
    String file = getOptionalString(resultObject, "file");
    String uri = getOptionalString(resultObject, "uri");
    consumer.computedFileOrUri(file, uri);
  }

  /**
   * Return the value of the given optional member in the given object as a string, or {@code null}
   * if the member is not defined.
   * 
   * @param resultObject the object containing the member
   * @param memberName the name of the member to be returned
   * @return the value of the optional member
   */
  private String getOptionalString(JsonObject resultObject, String memberName) {
    JsonElement element = resultObject.get(memberName);
    if (element == null) {
      return null;
    }
    return element.getAsString();
  }
}
