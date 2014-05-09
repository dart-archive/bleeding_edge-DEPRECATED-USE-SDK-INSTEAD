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
package com.google.dart.server.internal.remote.utilities;

import org.json.JSONException;
import org.json.JSONWriter;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A utilities class for generating the {@link String} analysis server json requests.
 */
public class AnalysisServerRequestUtilities {

  private static StringWriter stringWriter;
  private static JSONWriter jsonWriter;

  private static final String ID = "id";
  private static final String METHOD = "method";
  private static final String PARAMS = "params";

  private static final String METHOD_CREATE_CONTEXT = "server.createContext";
  private static final String METHOD_VERSION = "server.version";

  /**
   * Generate and return a {@value #METHOD_CREATE_CONTEXT} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "server.createContext"
   *   "params": {
   *     "contextId": ContextId
   *     "sdkDirectory": Path
   *     "packageMap": Map&lt;String, List&lt;String&gt;&gt;
   *   }
   * }
   * </pre>
   */
  public static String generateCreateContextRequest(String idValue, String contextIdValue,
      String sdkDirectoryValue, Map<String, List<String>> packageMap) {
    LinkedHashMap<String, Object> params = new LinkedHashMap<String, Object>();
    params.put("contextId", contextIdValue);
    params.put("sdkDirectory", sdkDirectoryValue);
    if (packageMap != null) {
      params.put("packageMap", packageMap);
    }
    return buildJSONRequest(idValue, METHOD_CREATE_CONTEXT, params);
  }

  /**
   * Generate and return a {@value #METHOD_VERSION} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "server.version"
   * }
   * </pre>
   */
  public static String generateVersionRequest(String idValue) {
    return buildJSONRequest(idValue, METHOD_VERSION);
  }

  private static String buildJSONRequest(String idValue, String methodValue) {
    return buildJSONRequest(idValue, methodValue, null);
  }

  private static String buildJSONRequest(String idValue, String methodValue,
      LinkedHashMap<String, Object> params) {
    stringWriter = new StringWriter();
    jsonWriter = new JSONWriter(stringWriter);
    try {
      jsonWriter.object().key(ID).value(idValue).key(METHOD).value(methodValue);
      if (params != null) {
        writeObject(jsonWriter, PARAMS, params);
      }
      jsonWriter.endObject();
    } catch (JSONException e) {
      e.printStackTrace();
    }
    return stringWriter.toString();
  }

  private static void writeArray(JSONWriter writer, String arrayKey, List<String> map)
      throws JSONException {
    writer.key(arrayKey);
    writer.array();
    for (String string : map) {
      writer.value(string);
    }
    writer.endArray();
  }

  @SuppressWarnings("unchecked")
  private static void writeObject(JSONWriter writer, String objectKey, Map<String, Object> map)
      throws JSONException {
    writer.key(objectKey);
    writer.object();
    for (Entry<String, Object> entry : map.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (value instanceof String) {
        writer.key(key);
        writer.value(value);
      } else if (value instanceof Map<?, ?>) {
        writeObject(writer, key, (Map<String, Object>) value);
      } else if (value instanceof List<?>) {
        writeArray(writer, key, (List<String>) value);
      }
    }
    writer.endObject();
  }

  private AnalysisServerRequestUtilities() {
  }
}
