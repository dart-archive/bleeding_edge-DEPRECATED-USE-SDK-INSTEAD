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

import com.google.common.annotations.VisibleForTesting;
import com.google.dart.server.AnalysisError;
import com.google.dart.server.AnalysisOptions;
import com.google.dart.server.AnalysisService;
import com.google.dart.server.ContentChange;
import com.google.dart.server.ServerService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

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
  private static final String FILE = "file";

  // Server domain
  private static final String METHOD_SERVER_GET_VERSION = "server.getVersion";
  private static final String METHOD_SERVER_SHUTDOWN = "server.shutdown";
  private static final String METHOD_SERVER_SET_SUBSCRIPTIONS = "server.setSubscriptions";

  // Analysis domain
  private static final String METHOD_ANALYSIS_SET_ROOTS = "analysis.setAnalysisRoots";
  private static final String METHOD_ANALYSIS_SET_PRIORITY_FILES = "analysis.setPriorityFiles";
  private static final String METHOD_ANALYSIS_SET_SUBSCRIPTIONS = "analysis.setSubscriptions";
  private static final String METHOD_ANALYSIS_UPDATE_CONTENT = "analysis.updateContent";
  private static final String METHOD_ANALYSIS_UPDATE_OPTIONS = "analysis.updateOptions";
  private static final String METHOD_ANALYSIS_UPDATE_SDKS = "analysis.updateSdks";

  // Edit domain
  private static final String METHOD_EDIT_GET_FIXES = "edit.getFixes";
  private static final String METHOD_EDIT_GET_ASSISTS = "edit.getAssists";

  // Code Completion domain
  private static final String METHOD_COMPLETION_GET_SUGGESTIONS = "completion.getSuggestions";

  @VisibleForTesting
  public static JsonElement buildJsonElement(Object object) {
    if (object instanceof Boolean) {
      return new JsonPrimitive((Boolean) object);
    } else if (object instanceof Number) {
      return new JsonPrimitive((Number) object);
    } else if (object instanceof String) {
      return new JsonPrimitive((String) object);
    } else if (object instanceof List<?>) {
      List<?> list = (List<?>) object;
      JsonArray jsonArray = new JsonArray();
      for (Object item : list) {
        JsonElement jsonItem = buildJsonElement(item);
        jsonArray.add(jsonItem);
      }
      return jsonArray;
    } else if (object instanceof Map<?, ?>) {
      Map<?, ?> map = (Map<?, ?>) object;
      JsonObject jsonObject = new JsonObject();
      for (Entry<?, ?> entry : map.entrySet()) {
        Object key = entry.getKey();
        // prepare string key
        String keyString;
        if (key instanceof String) {
          keyString = (String) key;
        } else if (key instanceof AnalysisService) {
          keyString = ((AnalysisService) key).name();
//        } else if (keyObject instanceof NotificationKind) {
//          key = "CONTEXT_" + ((NotificationKind) keyObject).name();
//        } else if (keyObject instanceof SourceSetKind) {
//          key = ((SourceSetKind) keyObject).name();
        } else {
          throw new IllegalArgumentException("Unable to convert to string: " + getClassName(key));
        }
        // prepare JsonElement value
        Object value = entry.getValue();
        JsonElement valueJson = buildJsonElement(value);
        // put a property into the JSON object
        if (keyString != null && valueJson != null) {
          jsonObject.add(keyString, valueJson);
        }
      }
      return jsonObject;
    } else if (object instanceof ContentChange) {
      return buildJsonObjectContentChange((ContentChange) object);
    } else if (object instanceof AnalysisError) {
      return buildJsonObjectAnalysisError((AnalysisError) object);
    } else if (object instanceof AnalysisOptions) {
      return buildJsonObjectAnalysisOptions((AnalysisOptions) object);
    } else if (object instanceof ServerService) {
      return new JsonPrimitive(((ServerService) object).name());
    }
    throw new IllegalArgumentException("Unable to convert to JSON: " + object);
  }

  /**
   * Generate and return a {@value #METHOD_ANALYSIS_SET_ROOTS} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "analysis.setAnalysisRoots"
   *   "params": {
   *     "included": List&lt;FilePath&gt;
   *     "excluded": List&lt;FilePath&gt;
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateAnalysisSetAnalysisRoots(String id, List<String> included,
      List<String> excluded) {
    JsonObject params = new JsonObject();
    params.add("included", buildJsonElement(included));
    params.add("excluded", buildJsonElement(excluded));
    return buildJsonObjectRequest(id, METHOD_ANALYSIS_SET_ROOTS, params);
  }

  /**
   * Generate and return a {@value #METHOD_ANALYSIS_SET_PRIORITY_FILES} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "analysis.setPriorityFiles"
   *   "params": {
   *     "files": List&lt;FilePath&gt;
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateAnalysisSetPriorityFiles(String id, List<String> files) {
    JsonObject params = new JsonObject();
    params.add("files", buildJsonElement(files));
    return buildJsonObjectRequest(id, METHOD_ANALYSIS_SET_PRIORITY_FILES, params);
  }

  /**
   * Generate and return a {@value #METHOD_ANALYSIS_SET_SUBSCRIPTIONS} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "analysis.setSubscriptions"
   *   "params": {
   *     "subscriptions": Map&gt;AnalysisService, List&lt;FilePath&gt;&gt;
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateAnalysisSetSubscriptions(String id,
      Map<AnalysisService, List<String>> subscriptions) {
    JsonObject params = new JsonObject();
    params.add("subscriptions", buildJsonElement(subscriptions));
    return buildJsonObjectRequest(id, METHOD_ANALYSIS_SET_SUBSCRIPTIONS, params);
  }

  /**
   * Generate and return a {@value #METHOD_ANALYSIS_UPDATE_CONTENT} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "analysis.updateContent"
   *   "params": {
   *     "files": Map&lt;FilePath, ContentChange&gt;
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateAnalysisUpdateContent(String idValue,
      Map<String, ContentChange> files) {
    JsonObject params = new JsonObject();
    params.add("files", buildJsonElement(files));
    return buildJsonObjectRequest(idValue, METHOD_ANALYSIS_UPDATE_CONTENT, params);
  }

  /**
   * Generate and return a {@value #METHOD_ANALYSIS_UPDATE_OPTIONS} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "analysis.updateOptions"
   *   "params": {
   *     "options": AnalysisOptions
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateAnalysisUpdateOptions(String idValue, AnalysisOptions options) {
    JsonObject params = new JsonObject();
    params.add("options", buildJsonElement(options));
    return buildJsonObjectRequest(idValue, METHOD_ANALYSIS_UPDATE_OPTIONS, params);
  }

  /**
   * Generate and return a {@value #METHOD_ANALYSIS_UPDATE_SDKS} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "analysis.updateSdks"
   *   "params": {
   *     "added": List&lt;FilePath&gt;
   *     "removed": List&lt;FilePath&gt;
   *     "default": FilePath
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateAnalysisUpdateSdks(String idValue, List<String> added,
      List<String> removed, String defaultSdk) {
    JsonObject params = new JsonObject();
    params.add("added", buildJsonElement(added));
    params.add("removed", buildJsonElement(removed));
    if (defaultSdk != null) {
      params.addProperty("default", defaultSdk);
    }
    return buildJsonObjectRequest(idValue, METHOD_ANALYSIS_UPDATE_SDKS, params);
  }

  /**
   * Generate and return a {@value #METHOD_COMPLETION_GET_SUGGESTIONS} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "completion.getSuggestions"
   *   "params": {
   *     "file": FilePath
   *     "offset": int
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateCompletionGetSuggestions(String idValue, String file, int offset) {
    JsonObject params = new JsonObject();
    params.addProperty(FILE, file);
    params.addProperty("offset", offset);
    return buildJsonObjectRequest(idValue, METHOD_COMPLETION_GET_SUGGESTIONS, params);
  }

  /**
   * Generate and return a {@value #METHOD_EDIT_GET_ASSISTS} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "edit.getAssists"
   *   "params": {
   *     "file": FilePath
   *     "offset": int
   *     "length": int
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateEditGetAssists(String idValue, String file, int offset,
      int length) {
    JsonObject params = new JsonObject();
    params.addProperty(FILE, file);
    params.addProperty("offset", offset);
    params.addProperty("length", length);
    return buildJsonObjectRequest(idValue, METHOD_EDIT_GET_ASSISTS, params);
  }

//  /**
//   * Generate and return a {@value #METHOD_CONTEXT_APPLY_ANALYSIS_DELTA} request.
//   * 
//   * <pre>
//   * request: {
//   *   "id": String
//   *   "method": "context.applyAnalysisDelta"
//   *   "params": {
//   *     "contextId": ContextId
//   *     "delta": AnalysisDelta
//   *   }
//   * }
//   * </pre>
//   */
//  public static JsonObject generateContextApplyAnalysisDeltaRequest(String idValue,
//      String contextIdValue, Map<String, AnalysisDelta.AnalysisLevel> analysisMap) {
//    JsonObject params = new JsonObject();
//    params.addProperty("contextId", contextIdValue);
//    params.add("delta", buildJsonObject(analysisMap));
//    return buildJsonObjectRequest(idValue, METHOD_CONTEXT_APPLY_ANALYSIS_DELTA, params);
//  }
//
//  /**
//   * Generate and return a {@value #METHOD_CONTEXT_APPLY_SOURCE_DELTA} request.
//   * 
//   * <pre>
//   * request: {
//   *   "id": String
//   *   "method": "context.applySourceDelta"
//   *   "params": {
//   *     "contextId": ContextId
//   *     "delta": SourceDelta
//   *   }
//   * }
//   * </pre>
//   */
//  public static JsonObject generateContextApplySourceDeltaRequest(String idValue,
//      String contextIdValue) {
//    // TODO(jwren) SourceDelta object TBD in Server API
////    LinkedHashMap<String, Object> params = new LinkedHashMap<String, Object>(2);
////    params.put("contextId", contextIdValue);
////    params.put("delta", analysisMap);
////    return buildJsonObjectRequest(idValue, METHOD_CONTEXT_APPLY_SOURCE_DELTA, params);
//    throw new Error("not yet implemented");
//  }
//
//  /**
//   * Generate and return a {@value #METHOD_CONTEXT_GET_FIXES} request.
//   * 
//   * <pre>
//   * request: {
//   *   "id": String
//   *   "method": "context.getFixes"
//   *   "params": {
//   *     "contextId": ContextId
//   *     "errors": List<AnalysisError>
//   *   }
//   * }
//   * </pre>
//   */
//  public static JsonObject generateContextGetFixesRequest() {
//    // TODO(jwren) Fix object TBD in Server API
//    throw new Error("not yet implemented");
//  }
//
//  /**
//   * Generate and return a {@value #METHOD_CONTEXT_SET_OPTIONS} request.
//   * 
//   * <pre>
//   * request: {
//   *   "id": String
//   *   "method": "context.setOptions"
//   *   "params": {
//   *     "contextId": ContextId
//   *     "options": AnalysisOptions
//   *   }
//   * }
//   * </pre>
//   */
//  public static JsonObject generateContextSetOptionsRequest(String idValue, String contextIdValue,
//      AnalysisOptions options) {
//    // Create the json object from the passed AnalysisOptions
//    JsonObject optionsJsonObject = new JsonObject();
//    optionsJsonObject.addProperty("analyzeAngular", options.getAnalyzeAngular());
//    optionsJsonObject.addProperty("analyzePolymer", options.getAnalyzePolymer());
//    optionsJsonObject.addProperty("cacheSize", options.getCacheSize());
//    optionsJsonObject.addProperty("enableDeferredLoading", options.getEnableDeferredLoading());
//    optionsJsonObject.addProperty("generateDart2jsHints", options.getDart2jsHint());
//    optionsJsonObject.addProperty("generateHints", options.getHint());
//
//    JsonObject params = new JsonObject();
//    params.addProperty("contextId", contextIdValue);
//    params.add("options", optionsJsonObject);
//    return buildJsonObjectRequest(idValue, METHOD_CONTEXT_SET_OPTIONS, params);
//  }
//
//  /**
//   * Generate and return a {@value #METHOD_CONTEXT_SET_PRIORITY_SOURCES} request.
//   * 
//   * <pre>
//   * request: {
//   *   "id": String
//   *   "method": "context.setPrioritySources"
//   *   "params": {
//   *     "contextId": ContextId
//   *     "sources": List&lt;Source&gt;
//   *   }
//   * }
//   * </pre>
//   */
//  public static JsonObject generateContextSetPrioritySourcesRequest(String idValue,
//      String contextIdValue, List<String> sources) {
//    // TODO (jwren) Should this method take a List<Source> instead? Where should the conversion from
//    // Sources to Strings take place?
//    JsonObject params = new JsonObject();
//    params.addProperty("contextId", contextIdValue);
//    params.add("sources", buildJsonArray(sources));
//    return buildJsonObjectRequest(idValue, METHOD_CONTEXT_SET_PRIORITY_SOURCES, params);
//  }
//
//  /**
//   * Generate and return a {@value #METHOD_CONTEXT_SUBSCRIBE} request.
//   * 
//   * <pre>
//   * request: {
//   *   "id": String
//   *   "method": "context.subscribe"
//   *   "params": {
//   *     "contextId": ContextId
//   *     "subscriptions": Map&lt;NotificationKind, SourceSet&gt;
//   *   }
//   * }
//
//   * </pre>
//   */
//  public static JsonObject generateContextSubscribeRequest() {
//    // TODO (jwren) implement
//    return null;
//  }
//
//  /**
//   * Generate and return a {@value #METHOD_SERVER_CREATE_CONTEXT} request.
//   * 
//   * <pre>
//   * request: {
//   *   "id": String
//   *   "method": "server.createContext"
//   *   "params": {
//   *     "contextId": ContextId
//   *     "sdkDirectory": Path
//   *     "packageMap": Map&lt;String, List&lt;String&gt;&gt;
//   *   }
//   * }
//   * </pre>
//   */
//  public static JsonObject generateServerCreateContextRequest(String idValue,
//      String contextIdValue, String sdkDirectoryValue, Map<String, List<String>> packageMap) {
//    JsonObject params = new JsonObject();
//    params.addProperty("contextId", contextIdValue);
//    params.addProperty("sdkDirectory", sdkDirectoryValue);
//    if (packageMap != null && !packageMap.isEmpty()) {
//      params.add("packageMap", buildJsonObject(packageMap));
//    }
//    return buildJsonObjectRequest(idValue, METHOD_SERVER_CREATE_CONTEXT, params);
//  }
//
//  /**
//   * Generate and return a {@value #METHOD_SERVER_DELETE_CONTEXT} request.
//   * 
//   * <pre>
//   * request: {
//   *   "id": String
//   *   "method": "server.deleteContext"
//   *   "params": {
//   *     "contextId": ContextId
//   *   }
//   * }
//   * </pre>
//   */
//  public static JsonObject generateServerDeleteContextRequest(String idValue, String contextIdValue) {
//    JsonObject params = new JsonObject();
//    params.addProperty("contextId", contextIdValue);
//    return buildJsonObjectRequest(idValue, METHOD_SERVER_DELETE_CONTEXT, params);
//  }

  /**
   * Generate and return a {@value #METHOD_EDIT_GET_FIXES} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "analysis.getFixes"
   *   "params": {
   *     "errors": List&lt;AnalysisError&gt;
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateEditGetFixes(String idValue, List<AnalysisError> errors) {
    JsonObject params = new JsonObject();
    params.add("errors", buildJsonElement(errors));
    return buildJsonObjectRequest(idValue, METHOD_EDIT_GET_FIXES, params);
  }

  /**
   * Generate and return a {@value #METHOD_SERVER_GET_VERSION} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "server.getVersion"
   * }
   * </pre>
   */
  public static JsonObject generateServerGetVersion(String idValue) {
    return buildJsonObjectRequest(idValue, METHOD_SERVER_GET_VERSION);
  }

  /**
   * Generate and return a {@value #METHOD_SERVER_SET_SUBSCRIPTIONS} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "server.setSubscriptions"
   *   "params": {
   *     "subscriptions": List&lt;ServerService&gt;
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateServerSetSubscriptions(String idValue,
      List<ServerService> subscriptions) {
    JsonObject params = new JsonObject();
    params.add("subscriptions", buildJsonElement(subscriptions));
    return buildJsonObjectRequest(idValue, METHOD_SERVER_SET_SUBSCRIPTIONS, params);
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
  public static JsonObject generateServerShutdown(String idValue) {
    return buildJsonObjectRequest(idValue, METHOD_SERVER_SHUTDOWN);
  }

  private static JsonObject buildJsonObjectAnalysisError(AnalysisError error) {
    JsonObject errorJsonObject = new JsonObject();
    errorJsonObject.addProperty(FILE, error.getFile());
    errorJsonObject.addProperty("errorCode", error.getErrorCode().getUniqueName());
    errorJsonObject.addProperty("offset", error.getOffset());
    errorJsonObject.addProperty("length", error.getLength());
    errorJsonObject.addProperty("message", error.getMessage());
    String correction = error.getCorrection();
    if (correction != null) {
      errorJsonObject.addProperty("correction", correction);
    }
    return errorJsonObject;
  }

  private static JsonElement buildJsonObjectAnalysisOptions(AnalysisOptions options) {
    JsonObject optionsJsonObject = new JsonObject();
    optionsJsonObject.addProperty("analyzeAngular", options.getAnalyzeAngular());
    optionsJsonObject.addProperty("analyzePolymer", options.getAnalyzePolymer());
    optionsJsonObject.addProperty("enableAsync", options.getEnableAsync());
    optionsJsonObject.addProperty("enableDeferredLoading", options.getEnableDeferredLoading());
    optionsJsonObject.addProperty("enableEnums", options.getEnableEnums());
    optionsJsonObject.addProperty("generateDart2jsHints", options.getGenerateDart2jsHints());
    optionsJsonObject.addProperty("generateHints", options.getGenerateHints());
    return optionsJsonObject;
  }

  private static JsonObject buildJsonObjectContentChange(ContentChange change) {
    JsonObject errorJsonObject = new JsonObject();
    errorJsonObject.addProperty("content", change.getContent());
    if (change.isIncremental()) {
      errorJsonObject.addProperty("offset", change.getOffset());
      errorJsonObject.addProperty("oldLength", change.getOldLength());
      errorJsonObject.addProperty("newLength", change.getNewLength());
    }
    return errorJsonObject;
  }

  private static JsonObject buildJsonObjectRequest(String idValue, String methodValue) {
    return buildJsonObjectRequest(idValue, methodValue, null);
  }

  private static JsonObject buildJsonObjectRequest(String idValue, String methodValue,
      JsonObject params) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty(ID, idValue);
    jsonObject.addProperty(METHOD, methodValue);
    if (params != null) {
      jsonObject.add(PARAMS, params);
    }
    return jsonObject;
  }

  /**
   * Return the name of the given object, may be {@code "null"} string.
   */
  private static String getClassName(Object object) {
    return object != null ? object.getClass().getName() : "null";
  }

  private RequestUtilities() {
  }
}
