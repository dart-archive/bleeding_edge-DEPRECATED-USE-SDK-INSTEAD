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
import com.google.dart.server.AnalysisOptions;
import com.google.dart.server.AnalysisService;
import com.google.dart.server.ContentChange;
import com.google.dart.server.Parameter;
import com.google.dart.server.generated.types.AnalysisError;
import com.google.dart.server.generated.types.Location;
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
  private static final String OFFSET = "offset";
  private static final String LENGTH = "length";

  // Server domain
  private static final String METHOD_SERVER_GET_VERSION = "server.getVersion";
  private static final String METHOD_SERVER_SHUTDOWN = "server.shutdown";
  private static final String METHOD_SERVER_SET_SUBSCRIPTIONS = "server.setSubscriptions";

  // Analysis domain
  private static final String METHOD_ANALYSIS_GET_ERRORS = "analysis.getErrors";
  private static final String METHOD_ANALYSIS_GET_HOVER = "analysis.getHover";
  private static final String METHOD_ANALYSIS_REANALYZE = "analysis.reanalyze";
  private static final String METHOD_ANALYSIS_SET_ROOTS = "analysis.setAnalysisRoots";
  private static final String METHOD_ANALYSIS_SET_PRIORITY_FILES = "analysis.setPriorityFiles";
  private static final String METHOD_ANALYSIS_SET_SUBSCRIPTIONS = "analysis.setSubscriptions";
  private static final String METHOD_ANALYSIS_UPDATE_CONTENT = "analysis.updateContent";
  private static final String METHOD_ANALYSIS_UPDATE_OPTIONS = "analysis.updateOptions";

  // Edit domain
  private static final String METHOD_EDIT_APPLY_REFACTORING = "edit.applyRefactoring";
  private static final String METHOD_EDIT_CREATE_REFACTORING = "edit.createRefactoring";
  private static final String METHOD_EDIT_DELETE_REFACTORING = "edit.deleteRefactoring";
  private static final String METHOD_EDIT_GET_ASSISTS = "edit.getAssists";
  private static final String METHOD_EDIT_GET_FIXES = "edit.getFixes";
  private static final String METHOD_EDIT_GET_REFACTORING = "edit.getRefactorings";
  private static final String METHOD_EDIT_SET_REFACTORING_OPTIONS = "edit.setRefactoringOptions";

  // Code Completion domain
  private static final String METHOD_COMPLETION_GET_SUGGESTIONS = "completion.getSuggestions";

  // Search domain
  private static final String METHOD_SEARCH_FIND_ELEMENT_REFERENCES = "search.findElementReferences";
  private static final String METHOD_SEARCH_FIND_MEMBER_DECLARATIONS = "search.findMemberDeclarations";
  private static final String METHOD_SEARCH_FIND_MEMBER_REFERENCES = "search.findMemberReferences";
  private static final String METHOD_SEARCH_FIND_TOP_LEVEL_DECLARATIONS = "search.findTopLevelDeclarations";
  private static final String METHOD_SEARCH_GET_TYPE_HIERARCHY = "search.getTypeHierarchy";

  // Debug domain
//  private static final String METHOD_DEBUG_CREATE_CONTEXT = "debug.createContext";
//  private static final String METHOD_DEBUG_DELETE_CONTEXT = "debug.deleteContext";
//  private static final String METHOD_DEBUG_MAP_URI = "debug.mapUri";
//  private static final String METHOD_DEBUG_SET_SUBSCRIPTIONS = "debug.setSubscriptions";

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
    } else if (object instanceof Location) {
      return buildJsonObjectLocation((Location) object);
    }
    throw new IllegalArgumentException("Unable to convert to JSON: " + object);
  }

  /**
   * Generate and return a {@value #METHOD_ANALYSIS_GET_ERRORS} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "analysis.getErrors"
   *   "params": {
   *     "file": FilePath
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateAnalysisGetErrors(String idValue, String file) {
    JsonObject params = new JsonObject();
    params.addProperty(FILE, file);
    return buildJsonObjectRequest(idValue, METHOD_ANALYSIS_GET_ERRORS, params);
  }

  /**
   * Generate and return a {@value #METHOD_ANALYSIS_GET_HOVER} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "analysis.getHover"
   *   "params": {
   *     "file": FilePath
   *     "offset": int
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateAnalysisGetHover(String idValue, String file, int offset) {
    JsonObject params = new JsonObject();
    params.addProperty(FILE, file);
    params.addProperty(OFFSET, offset);
    return buildJsonObjectRequest(idValue, METHOD_ANALYSIS_GET_HOVER, params);
  }

  /**
   * Generate and return a {@value #METHOD_ANALYSIS_REANALYZE} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "analysis.reanalyze"
   * }
   * </pre>
   */
  public static JsonObject generateAnalysisReanalyze(String id) {
    return buildJsonObjectRequest(id, METHOD_ANALYSIS_REANALYZE);
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
    params.addProperty(OFFSET, offset);
    return buildJsonObjectRequest(idValue, METHOD_COMPLETION_GET_SUGGESTIONS, params);
  }

  /**
   * Generate and return a {@value #METHOD_EDIT_APPLY_REFACTORING} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "edit.applyRefactoring"
   *   "params": {
   *     "id": RefactoringId
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateEditApplyRefactoring(String idValue, String refactoringId) {
    JsonObject params = new JsonObject();
    params.addProperty("id", refactoringId);
    return buildJsonObjectRequest(idValue, METHOD_EDIT_APPLY_REFACTORING, params);
  }

  /**
   * Generate and return a {@value #METHOD_EDIT_CREATE_REFACTORING} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "edit.createRefactoring"
   *   "params": {
   *     "kind": RefactoringKind
   *     "file": FilePath
   *     "offset": int
   *     "length": int
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateEditCreateRefactoring(String idValue, String refactoringKind,
      String file, int offset, int length) {
    JsonObject params = new JsonObject();
    params.addProperty("kind", refactoringKind);
    params.addProperty(FILE, file);
    params.addProperty(OFFSET, offset);
    params.addProperty(LENGTH, length);
    return buildJsonObjectRequest(idValue, METHOD_EDIT_CREATE_REFACTORING, params);
  }

  /**
   * Generate and return a {@value #METHOD_EDIT_DELETE_REFACTORING} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "edit.deleteRefactoring"
   *   "params": {
   *     "id": RefactoringId
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateEditDeleteRefactoring(String idValue, String refactoringId) {
    JsonObject params = new JsonObject();
    params.addProperty("id", refactoringId);
    return buildJsonObjectRequest(idValue, METHOD_EDIT_DELETE_REFACTORING, params);
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
    params.addProperty(OFFSET, offset);
    params.addProperty(LENGTH, length);
    return buildJsonObjectRequest(idValue, METHOD_EDIT_GET_ASSISTS, params);
  }

  /**
   * Generate and return a {@value #METHOD_EDIT_GET_FIXES} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "analysis.getFixes"
   *   "params": {
   *     "file": FilePath
   *     "offset": int
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateEditGetFixes(String idValue, String file, int offset) {
    JsonObject params = new JsonObject();
    params.addProperty(FILE, file);
    params.addProperty(OFFSET, offset);
    return buildJsonObjectRequest(idValue, METHOD_EDIT_GET_FIXES, params);
  }

  /**
   * Generate and return a {@value #METHOD_EDIT_GET_REFACTORING} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "edit.getRefactorings"
   *   "params": {
   *     "file": FilePath
   *     "offset": int
   *     "length": int
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateEditGetRefactorings(String idValue, String file, int offset,
      int length) {
    JsonObject params = new JsonObject();
    params.addProperty(FILE, file);
    params.addProperty(OFFSET, offset);
    params.addProperty(LENGTH, length);
    return buildJsonObjectRequest(idValue, METHOD_EDIT_GET_REFACTORING, params);
  }

  /**
   * Generate and return a {@value #METHOD_EDIT_SET_REFACTORING_OPTIONS} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "edit.getRefactorings"
   *   "params": {
   *     "id": refactoringId
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateEditSetRefactoringOptions(String idValue, String refactoringId,
      Map<String, Object> refactoringOptions) {
    JsonObject params = new JsonObject();
    params.addProperty(ID, refactoringId);
    JsonObject options = new JsonObject();
    if (refactoringOptions != null && !refactoringOptions.isEmpty()) {
      // name: String
      Object name = refactoringOptions.get("name");
      if (name != null) {
        options.addProperty("name", (String) name);
      }
      // extractAll: Boolean
      Object extractAll = refactoringOptions.get("extractAll");
      if (extractAll != null) {
        options.addProperty("extractAll", (Boolean) extractAll);
      }
      // returnType: String
      Object returnType = refactoringOptions.get("returnType");
      if (returnType != null) {
        options.addProperty("returnType", (String) returnType);
      }
      // createGetter: Boolean
      Object createGetter = refactoringOptions.get("createGetter");
      if (createGetter != null) {
        options.addProperty("createGetter", (Boolean) createGetter);
      }
      // parameters: List<Parameter>
      Object parameterListOb = refactoringOptions.get("parameters");
      if (parameterListOb != null) {
        JsonArray parameterArray = new JsonArray();
        if (parameterListOb instanceof Parameter[]) {
          Parameter[] parameterList = (Parameter[]) parameterListOb;
          for (Parameter parameter : parameterList) {
            JsonObject parameterJsonObject = new JsonObject();
            parameterJsonObject.addProperty("type", parameter.getType());
            parameterJsonObject.addProperty("name", parameter.getName());
            parameterArray.add(parameterJsonObject);
          }
        }
        options.add("parameters", parameterArray);
      }
      // deleteSource: Boolean
      Object deleteSource = refactoringOptions.get("deleteSource");
      if (deleteSource != null) {
        options.addProperty("deleteSource", (Boolean) deleteSource);
      }
      // inlineAll: Boolean
      Object inlineAll = refactoringOptions.get("inlineAll");
      if (inlineAll != null) {
        options.addProperty("inlineAll", (Boolean) inlineAll);
      }
      // newName: String
      Object newName = refactoringOptions.get("newName");
      if (newName != null) {
        options.addProperty("newName", (String) newName);
      }
    }
    params.add("options", options);
    return buildJsonObjectRequest(idValue, METHOD_EDIT_SET_REFACTORING_OPTIONS, params);
  }

  /**
   * Generate and return a {@value #METHOD_SEARCH_FIND_ELEMENT_REFERENCES} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "search.findElementReferences"
   *   "params": {
   *     "file": FilePath
   *     "offset": int
   *     "includePotential": boolean
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateSearchFindElementReferences(String idValue, String file,
      int offset, boolean includePotential) {
    JsonObject params = new JsonObject();
    params.addProperty(FILE, file);
    params.addProperty(OFFSET, offset);
    params.addProperty("includePotential", includePotential);
    return buildJsonObjectRequest(idValue, METHOD_SEARCH_FIND_ELEMENT_REFERENCES, params);
  }

  /**
   * Generate and return a {@value #METHOD_SEARCH_FIND_MEMBER_DECLARATIONS} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "search.findMemberDeclarations"
   *   "params": {
   *     "name": String
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateSearchFindMemberDeclarations(String idValue, String name) {
    JsonObject params = new JsonObject();
    params.addProperty("name", name);
    return buildJsonObjectRequest(idValue, METHOD_SEARCH_FIND_MEMBER_DECLARATIONS, params);
  }

  /**
   * Generate and return a {@value #METHOD_SEARCH_FIND_MEMBER_REFERENCES} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "search.findMemberReferences"
   *   "params": {
   *     "name": String
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateSearchFindMemberReferences(String idValue, String name) {
    JsonObject params = new JsonObject();
    params.addProperty("name", name);
    return buildJsonObjectRequest(idValue, METHOD_SEARCH_FIND_MEMBER_REFERENCES, params);
  }

  /**
   * Generate and return a {@value #METHOD_SEARCH_FIND_TOP_LEVEL_DECLARATIONS} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "search.findTopLevelDeclarations"
   *   "params": {
   *     "name": String
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateSearchFindTopLevelDeclarations(String idValue, String pattern) {
    JsonObject params = new JsonObject();
    params.addProperty("pattern", pattern);
    return buildJsonObjectRequest(idValue, METHOD_SEARCH_FIND_TOP_LEVEL_DECLARATIONS, params);
  }

  /**
   * Generate and return a {@value #METHOD_SEARCH_GET_TYPE_HIERARCHY} request.
   * 
   * <pre>
   * request: {
   *   "id": String
   *   "method": "search.getTypeHierarchy"
   *   "params": {
   *     "file": FilePath
   *     "offset": int
   *   }
   * }
   * </pre>
   */
  public static JsonObject generateSearchGetTypeHierarchy(String id, String file, int offset) {
    JsonObject params = new JsonObject();
    params.addProperty(FILE, file);
    params.addProperty(OFFSET, offset);
    return buildJsonObjectRequest(id, METHOD_SEARCH_GET_TYPE_HIERARCHY, params);
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
  public static JsonObject generateServerSetSubscriptions(String idValue, List<String> subscriptions) {
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
    errorJsonObject.addProperty("severity", error.getSeverity());
    errorJsonObject.addProperty("type", error.getType());
    errorJsonObject.add("location", buildJsonObjectLocation(error.getLocation()));
    errorJsonObject.addProperty("message", error.getMessage());
    String correction = error.getCorrection();
    if (correction != null) {
      errorJsonObject.addProperty("correction", correction);
    }
    return errorJsonObject;
  }

  private static JsonElement buildJsonObjectAnalysisOptions(AnalysisOptions options) {
    JsonObject optionsJsonObject = new JsonObject();
    Boolean analyzeAngular = options.getAnalyzeAngular();
    Boolean analyzePolymer = options.getAnalyzePolymer();
    Boolean enableAsync = options.getEnableAsync();
    Boolean enableDeferredLoading = options.getEnableDeferredLoading();
    Boolean enableEnums = options.getEnableEnums();
    Boolean generateDart2jsHints = options.getGenerateDart2jsHints();
    Boolean generateHints = options.getGenerateHints();

    if (analyzeAngular != null) {
      optionsJsonObject.addProperty("analyzeAngular", options.getAnalyzeAngular());
    }
    if (analyzePolymer != null) {
      optionsJsonObject.addProperty("analyzePolymer", options.getAnalyzePolymer());
    }
    if (enableAsync != null) {
      optionsJsonObject.addProperty("enableAsync", options.getEnableAsync());
    }
    if (enableDeferredLoading != null) {
      optionsJsonObject.addProperty("enableDeferredLoading", options.getEnableDeferredLoading());
    }
    if (enableEnums != null) {
      optionsJsonObject.addProperty("enableEnums", options.getEnableEnums());
    }
    if (generateDart2jsHints != null) {
      optionsJsonObject.addProperty("generateDart2jsHints", options.getGenerateDart2jsHints());
    }
    if (generateHints != null) {
      optionsJsonObject.addProperty("generateHints", options.getGenerateHints());
    }
    return optionsJsonObject;
  }

  private static JsonObject buildJsonObjectContentChange(ContentChange change) {
    JsonObject errorJsonObject = new JsonObject();
    errorJsonObject.addProperty("content", change.getContent());
    if (change.isIncremental()) {
      errorJsonObject.addProperty(OFFSET, change.getOffset());
      errorJsonObject.addProperty("oldLength", change.getOldLength());
      errorJsonObject.addProperty("newLength", change.getNewLength());
    }
    return errorJsonObject;
  }

  private static JsonObject buildJsonObjectLocation(Location location) {
    JsonObject locationJsonObject = new JsonObject();
    locationJsonObject.addProperty("file", location.getFile());
    locationJsonObject.addProperty(OFFSET, location.getOffset());
    locationJsonObject.addProperty(LENGTH, location.getLength());
    locationJsonObject.addProperty("startLine", location.getStartLine());
    locationJsonObject.addProperty("startColumn", location.getStartColumn());
    return locationJsonObject;
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
