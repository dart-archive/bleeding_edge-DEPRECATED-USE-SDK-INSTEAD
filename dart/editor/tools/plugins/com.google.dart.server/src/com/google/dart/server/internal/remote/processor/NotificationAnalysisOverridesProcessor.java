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

import com.google.common.collect.Lists;
import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.OverrideMember;
import com.google.dart.server.generated.types.Element;
import com.google.dart.server.internal.OverrideMemberImpl;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Iterator;
import java.util.List;

/**
 * Processor for "analysis.overrides" notification.
 * 
 * @coverage dart.server.remote
 */
public class NotificationAnalysisOverridesProcessor extends NotificationProcessor {

  public NotificationAnalysisOverridesProcessor(AnalysisServerListener listener) {
    super(listener);
  }

  /**
   * Process the given {@link JsonObject} notification and notify {@link #listener}.
   */
  @Override
  public void process(JsonObject response) throws Exception {
    JsonObject paramsObject = response.get("params").getAsJsonObject();
    String file = paramsObject.get("file").getAsString();
    JsonArray occurrencesJsonArray = paramsObject.get("overrides").getAsJsonArray();
    // compute occurrences and notify listener
    getListener().computedOverrides(file, constructOverridesArray(occurrencesJsonArray));
  }

  private OverrideMember[] constructOverridesArray(JsonArray occurrencesJsonArray) {
    Iterator<JsonElement> overridesIterator = occurrencesJsonArray.iterator();
    List<OverrideMember> overridesList = Lists.newArrayList();
    while (overridesIterator.hasNext()) {
      JsonObject overridesObject = overridesIterator.next().getAsJsonObject();
      int offset = overridesObject.get("offset").getAsInt();
      int length = overridesObject.get("length").getAsInt();
      JsonObject superclassObject = safelyGetAsJsonObject(overridesObject, "superclassElement");
      Element superclassElement = null;
      if (superclassObject != null) {
        superclassElement = constructElement(superclassObject);
      }
      overridesList.add(new OverrideMemberImpl(offset, length, superclassElement));
    }

    // create overrides object
    return overridesList.toArray(new OverrideMember[overridesList.size()]);
  }
}
