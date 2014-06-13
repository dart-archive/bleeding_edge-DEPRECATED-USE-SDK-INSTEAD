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
import com.google.dart.server.Element;
import com.google.dart.server.Outline;
import com.google.dart.server.internal.local.computer.OutlineImpl;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Iterator;
import java.util.List;

/**
 * Processor for "analysis.outline" notification.
 * 
 * @coverage dart.server.remote
 */
public class NotificationAnalysisOutlineProcessor extends NotificationProcessor {

  public NotificationAnalysisOutlineProcessor(AnalysisServerListener listener) {
    super(listener);
  }

  /**
   * Process the given {@link JsonObject} notification and notify {@link #listener}.
   */
  @Override
  public void process(JsonObject response) throws Exception {
    JsonObject paramsObject = response.get("params").getAsJsonObject();
    String file = paramsObject.get("file").getAsString();
    JsonObject outlineObject = paramsObject.get("outline").getAsJsonObject();
    // compute outline and notify listener
    getListener().computedOutline(file, computeOutline(null, outlineObject));
  }

  private Outline computeOutline(Outline parent, JsonObject outlineObject) {
    JsonObject elementObject = outlineObject.get("element").getAsJsonObject();
    int offset = outlineObject.get("offset").getAsInt();
    int length = outlineObject.get("length").getAsInt();
    Element element = computeElement(elementObject);

    // create outline object
    OutlineImpl outline = new OutlineImpl(parent, element, offset, length);

    // compute children recursively
    List<Outline> childrenList = Lists.newArrayList();
    if (outlineObject.has("children")) {
      JsonElement childrenJsonArray = outlineObject.get("children");
      if (childrenJsonArray instanceof JsonArray) {
        Iterator<JsonElement> childrenElementIterator = ((JsonArray) childrenJsonArray).iterator();
        while (childrenElementIterator.hasNext()) {
          JsonObject childObject = childrenElementIterator.next().getAsJsonObject();
          childrenList.add(computeOutline(outline, childObject));
        }
      }
    }

    // set children onto outline
    outline.setChildren(childrenList.toArray(new Outline[childrenList.size()]));
    return outline;
  }
}
