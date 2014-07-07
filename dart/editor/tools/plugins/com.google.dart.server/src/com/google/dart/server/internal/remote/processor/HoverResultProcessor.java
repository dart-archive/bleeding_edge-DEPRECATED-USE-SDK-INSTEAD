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

import com.google.dart.server.HoverConsumer;
import com.google.dart.server.HoverInformation;
import com.google.dart.server.internal.HoverInformationImpl;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Instances of {@code HoverResultProcessor} translate JSON result objects for a given
 * {@link HoverConsumer}.
 * 
 * @coverage dart.server.remote
 */
public class HoverResultProcessor extends ResultProcessor {

  private final HoverConsumer consumer;

  public HoverResultProcessor(HoverConsumer consumer) {
    this.consumer = consumer;
  }

  public void process(JsonObject resultObject) {
    ArrayList<HoverInformation> hovers = new ArrayList<HoverInformation>();
    Iterator<JsonElement> iter = resultObject.get("hovers").getAsJsonArray().iterator();
    while (iter.hasNext()) {
      JsonElement hoverElem = iter.next();
      if (hoverElem instanceof JsonObject) {
        JsonObject hoverObj = (JsonObject) hoverElem;
        hovers.add(new HoverInformationImpl( //
            hoverObj.get("offset").getAsInt(),
            hoverObj.get("length").getAsInt(),
            safelyGetAsString(hoverObj, "containingLibraryName"),
            safelyGetAsString(hoverObj, "containingLibraryPath"),
            safelyGetAsString(hoverObj, "dartdoc"),
            safelyGetAsString(hoverObj, "elementDescription"),
            safelyGetAsString(hoverObj, "elementKind"),
            safelyGetAsString(hoverObj, "parameter"),
            safelyGetAsString(hoverObj, "propagatedType"),
            safelyGetAsString(hoverObj, "staticType")));
      }
    }
    consumer.computedHovers(hovers.toArray(new HoverInformation[hovers.size()]));
  }
}
