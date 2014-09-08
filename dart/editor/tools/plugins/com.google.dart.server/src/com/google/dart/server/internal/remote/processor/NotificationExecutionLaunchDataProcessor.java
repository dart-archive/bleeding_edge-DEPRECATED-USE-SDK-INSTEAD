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

import com.google.dart.server.AnalysisServerListener;
import com.google.dart.server.utilities.general.JsonUtilities;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Instances of the class {@code NotificationExecutionLaunchDataProcessor} process
 * "execution.launchData" notifications.
 * 
 * @coverage dart.server.remote
 */
public class NotificationExecutionLaunchDataProcessor extends NotificationProcessor {
  /**
   * Initialize a newly created processor to report to the given listener.
   * 
   * @param listener the listener that should be notified when the notification is received
   */
  public NotificationExecutionLaunchDataProcessor(AnalysisServerListener listener) {
    super(listener);
  }

  @Override
  public void process(JsonObject response) throws Exception {
    JsonObject paramsObject = response.get("params").getAsJsonObject();
    JsonArray executables = paramsObject.get("executables").getAsJsonArray();
    JsonObject dartToHtml = paramsObject.get("dartToHtml").getAsJsonObject();
    JsonObject htmlToDart = paramsObject.get("htmlToDart").getAsJsonObject();
    getListener().computedLaunchData(
        JsonUtilities.decodeStringList(executables),
        toMap(dartToHtml),
        toMap(htmlToDart));
  }

  /**
   * Convert the given JSON object to a map of strings to lists of strings.
   * 
   * @param object the object to be converted
   * @return the result of converting the given JSON object to a map of strings to lists of strings
   */
  private Map<String, List<String>> toMap(JsonObject object) {
    Map<String, List<String>> map = new HashMap<String, List<String>>();
    for (Entry<String, JsonElement> entry : object.entrySet()) {
      JsonElement value = entry.getValue();
      if (value instanceof JsonArray) {
        map.put(entry.getKey(), JsonUtilities.decodeStringList((JsonArray) value));
      }
    }
    return map;
  }
}
