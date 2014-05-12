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

import com.google.dart.engine.context.AnalysisDelta;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * A utilities class for generating the {@link String} analysis server json requests.
 * 
 * @coverage dart.server.remote
 */
public class RequestUtilities {

  private static final String ID = "id";
  private static final String METHOD = "method";
  private static final String PARAMS = "params";

  // Server domain
  private static final String METHOD_SERVER_CREATE_CONTEXT = "server.createContext";
  private static final String METHOD_SERVER_DELETE_CONTEXT = "server.deleteContext";
  private static final String METHOD_SERVER_SHUTDOWN = "server.shutdown";
  private static final String METHOD_SERVER_VERSION = "server.version";

  // Context domain
  private static final String METHOD_CONTEXT_APPLY_ANALYSIS_DELTA = "context.applyAnalysisDelta";
  private static final String METHOD_CONTEXT_APPLY_SOURCE_DELTA = "context.applySourceDelta";
  private static final String METHOD_CONTEXT_GET_FIXES = "context.getFixes";
  private static final String METHOD_CONTEXT_GET_MINOR_REFACTORINGS = "context.getMinorRefactorings";
  private static final String METHOD_CONTEXT_SET_OPTIONS = "context.setOptions";
  private static final String METHOD_CONTEXT_PRIORITY_SOURCES = "context.setPrioritySources";
  private static final String METHOD_CONTEXT_SUBSCRIBE = "context.subscribe";

  /**
   * Generate and return a {@value #METHOD_CONTEXT_APPLY_ANALYSIS_DELTA} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "context.applyAnalysisDelta"
   *   "params": {
   *     "contextId": ContextId
   *     "delta": AnalysisDelta
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateContextApplyAnalysisDeltaRequest(String idValue,
      String contextIdValue, Map<String, AnalysisDelta.AnalysisLevel> analysisMap) {
    LinkedHashMap<String, Object> params = new LinkedHashMap<String, Object>(2);
    params.put("contextId", contextIdValue);
    params.put("delta", analysisMap);
    return buildJsonObjectRequest(idValue, METHOD_CONTEXT_APPLY_ANALYSIS_DELTA, params);
  }

  /**
   * Generate and return a {@value #METHOD_CONTEXT_APPLY_SOURCE_DELTA} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "context.applySourceDelta"
   *   "params": {
   *     "contextId": ContextId
   *     "delta": SourceDelta
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateContextApplySourceDeltaRequest(String idValue,
      String contextIdValue) {
    // TODO(jwren) SourceDelta object TBD in Server API
//    LinkedHashMap<String, Object> params = new LinkedHashMap<String, Object>(2);
//    params.put("contextId", contextIdValue);
//    params.put("delta", analysisMap);
//    return buildJsonObjectRequest(idValue, METHOD_CONTEXT_APPLY_SOURCE_DELTA, params);
    throw new Error("not yet implemented");
  }

  /**
   * Generate and return a {@value #METHOD_CONTEXT_GET_FIXES} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "context.getFixes"
   *   "params": {
   *     "contextId": ContextId
   *     "errors": List<AnalysisError>
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateContextGetFixesRequest() {
    // TODO(jwren) Fix object TBD in Server API
    throw new Error("not yet implemented");
  }

  /**
   * Generate and return a {@value #METHOD_CONTEXT_GET_MINOR_REFACTORINGS} request.
   * 
   * <pre>
   * </pre>
   */
  public static JsonObject generateContextGetMinorRefactoringsRequest(String idValue,
      String contextIdValue, String source, int offset, int length) {
    LinkedHashMap<String, Object> params = new LinkedHashMap<String, Object>(4);
    params.put("contextId", contextIdValue);
    params.put("source", source);
    params.put("offset", offset);
    params.put("length", length);
    return buildJsonObjectRequest(idValue, METHOD_CONTEXT_GET_MINOR_REFACTORINGS, params);
  }

  /**
   * Generate and return a {@value #METHOD_CONTEXT_PRIORITY_SOURCES} request.
   * 
   * <pre>
   * </pre>
   */
  public static JsonObject generateContextPrioritySourcesRequest() {
    throw new Error("not yet implemented");
  }

  /**
   * Generate and return a {@value #METHOD_CONTEXT_SET_OPTIONS} request.
   * 
   * <pre>
   * </pre>
   */
  public static JsonObject generateContextSetOptionsRequest() {
    throw new Error("not yet implemented");
  }

  /**
   * Generate and return a {@value #METHOD_CONTEXT_SUBSCRIBE} request.
   * 
   * <pre>
   * </pre>
   */
  public static JsonObject generateContextSubscribeRequest() {
    throw new Error("not yet implemented");
  }

  /**
   * Generate and return a {@value #METHOD_SERVER_CREATE_CONTEXT} request.
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
  public static JsonObject generateServerCreateContextRequest(String idValue,
      String contextIdValue, String sdkDirectoryValue, Map<String, List<String>> packageMap) {
    LinkedHashMap<String, Object> params = new LinkedHashMap<String, Object>(3);
    params.put("contextId", contextIdValue);
    params.put("sdkDirectory", sdkDirectoryValue);
    if (packageMap != null) {
      params.put("packageMap", packageMap);
    }
    return buildJsonObjectRequest(idValue, METHOD_SERVER_CREATE_CONTEXT, params);
  }

  /**
   * Generate and return a {@value #METHOD_SERVER_DELETE_CONTEXT} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "server.deleteContext"
   *   "params": {
   *     "contextId": ContextId
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateServerDeleteContextRequest(String idValue, String contextIdValue) {
    LinkedHashMap<String, Object> params = new LinkedHashMap<String, Object>(1);
    params.put("contextId", contextIdValue);
    return buildJsonObjectRequest(idValue, METHOD_SERVER_DELETE_CONTEXT, params);
  }

  /**
   * Generate and return a {@value #METHOD_SERVER_SHUTDOWN} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "server.shutdown"
   * }
   * </pre>
   */
  public static JsonObject generateServerShutdownRequest(String idValue) {
    return buildJsonObjectRequest(idValue, METHOD_SERVER_SHUTDOWN);
  }

  /**
   * Generate and return a {@value #METHOD_SERVER_VERSION} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "server.version"
   * }
   * </pre>
   */
  public static JsonObject generateServerVersionRequest(String idValue) {
    return buildJsonObjectRequest(idValue, METHOD_SERVER_VERSION);
  }

  private static JsonElement buildJsonArray(List<String> list) {
    JsonArray jsonArray = new JsonArray();
    for (String string : list) {
      jsonArray.add(new JsonPrimitive(string));
    }
    return jsonArray;
  }

  @SuppressWarnings("unchecked")
  private static JsonElement buildJsonObject(LinkedHashMap<String, Object> map) {
    JsonObject jsonObject = new JsonObject();
    for (Entry<String, Object> entry : map.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();
      if (value instanceof String) {
        jsonObject.addProperty(key, (String) value);
      } else if (value instanceof Integer) {
        jsonObject.addProperty(key, (Integer) value);
      } else if (value instanceof AnalysisDelta.AnalysisLevel) {
        jsonObject.addProperty(key, ((AnalysisDelta.AnalysisLevel) value).name());
      } else if (value instanceof LinkedHashMap<?, ?>) {
        jsonObject.add(key, buildJsonObject((LinkedHashMap<String, Object>) value));
      } else if (value instanceof List<?>) {
        jsonObject.add(key, buildJsonArray((List<String>) value));
      }
    }
    return jsonObject;
  }

  private static JsonObject buildJsonObjectRequest(String idValue, String methodValue) {
    return buildJsonObjectRequest(idValue, methodValue, null);
  }

  private static JsonObject buildJsonObjectRequest(String idValue, String methodValue,
      LinkedHashMap<String, Object> params) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(ID, idValue);
    jsonObject.addProperty(METHOD, methodValue);
    if (params != null) {
      jsonObject.add(PARAMS, buildJsonObject(params));
    }
    return jsonObject;
  }

  private RequestUtilities() {
  }
}
