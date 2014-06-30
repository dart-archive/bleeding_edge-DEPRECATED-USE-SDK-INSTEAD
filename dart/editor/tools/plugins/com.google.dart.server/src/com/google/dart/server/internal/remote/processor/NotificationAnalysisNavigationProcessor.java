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
import com.google.dart.server.NavigationRegion;
import com.google.dart.server.internal.local.computer.NavigationRegionImpl;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Iterator;
import java.util.List;

/**
 * Processor for "analysis.navigation" notification.
 * 
 * @coverage dart.server.remote
 */
public class NotificationAnalysisNavigationProcessor extends NotificationProcessor {
  public NotificationAnalysisNavigationProcessor(AnalysisServerListener listener) {
    super(listener);
  }

  /**
   * Process the given {@link JsonObject} notification and notify {@link #listener}.
   */
  @Override
  public void process(JsonObject response) throws Exception {
    JsonObject paramsObject = response.get("params").getAsJsonObject();
    String file = paramsObject.get("file").getAsString();
    // prepare region objects iterator
    JsonElement regionsElement = paramsObject.get("regions");
    Iterator<JsonElement> regionObjectIterator = regionsElement.getAsJsonArray().iterator();
    // convert regions
    List<NavigationRegion> regions = Lists.newArrayList();
    while (regionObjectIterator.hasNext()) {
      JsonObject regionObject = regionObjectIterator.next().getAsJsonObject();
      int offset = regionObject.get("offset").getAsInt();
      int length = regionObject.get("length").getAsInt();
      // prepare region objects iterator
      JsonElement targetsElement = regionObject.get("targets");
      Iterator<JsonElement> targetObjectIterator = targetsElement.getAsJsonArray().iterator();
      // convert targets
      List<Element> targets = Lists.newArrayList();
      while (targetObjectIterator.hasNext()) {
        JsonObject elementObject = targetObjectIterator.next().getAsJsonObject();
        Element element = computeElement(elementObject);
        targets.add(element);
      }
      regions.add(new NavigationRegionImpl(
          offset,
          length,
          targets.toArray(new Element[targets.size()])));
    }
    // notify listener
    getListener().computedNavigation(file, regions.toArray(new NavigationRegion[regions.size()]));
  }
}
