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
import com.google.dart.server.RefactoringProblem;
import com.google.dart.server.RefactoringProblemSeverity;
import com.google.dart.server.SourceChange;
import com.google.dart.server.SourceEdit;
import com.google.dart.server.SourceFileEdit;
import com.google.dart.server.generated.types.AnalysisError;
import com.google.dart.server.generated.types.Element;
import com.google.dart.server.generated.types.Location;
import com.google.dart.server.internal.RefactoringProblemImpl;
import com.google.dart.server.internal.SourceChangeImpl;
import com.google.dart.server.internal.SourceEditImpl;
import com.google.dart.server.internal.SourceFileEditImpl;
import com.google.dart.server.utilities.general.StringUtilities;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Abstract processor class with common behavior for {@link NotificationProcessor} and
 * {@link ResultProcessor}.
 * 
 * @coverage dart.server.remote
 */
public abstract class JsonProcessor {

  protected AnalysisError constructAnalysisError(JsonObject errorObject) {
    String errorSeverity = errorObject.get("severity").getAsString();
    String errorType = errorObject.get("type").getAsString();
    Location location = constructLocation(errorObject.get("location").getAsJsonObject());
    String message = errorObject.get("message").getAsString();
    String correction = safelyGetAsString(errorObject, "correction");
    return new AnalysisError(errorSeverity, errorType, location, message, correction);
  }

  protected Element constructElement(JsonObject elementObject) {
    String kind = elementObject.get("kind").getAsString();
    String name = elementObject.get("name").getAsString();
    Location location = constructLocation(elementObject.get("location").getAsJsonObject());
    int flags = elementObject.get("flags").getAsInt();
    String parameters = safelyGetAsString(elementObject, "parameters");
    String returnType = safelyGetAsString(elementObject, "returnType");
    return new Element(kind, name, location, flags, parameters, returnType);
  }

  protected Element[] constructElementArray(JsonArray jsonArray) {
    if (jsonArray == null) {
      return new Element[] {};
    }
    int i = 0;
    Element[] elements = new Element[jsonArray.size()];
    Iterator<JsonElement> iterator = jsonArray.iterator();
    while (iterator.hasNext()) {
      elements[i] = constructElement(iterator.next().getAsJsonObject());
      ++i;
    }
    return elements;
  }

  /**
   * Given some {@link JsonArray} and of {@code int} primitives, return the {@code int[]}.
   * 
   * @param intJsonArray some {@link JsonArray} of {@code int}s
   * @return the {@code int[]}
   */
  protected int[] constructIntArray(JsonArray intJsonArray) {
    if (intJsonArray == null) {
      return new int[] {};
    }
    int i = 0;
    int[] ints = new int[intJsonArray.size()];
    Iterator<JsonElement> iterator = intJsonArray.iterator();
    while (iterator.hasNext()) {
      ints[i] = iterator.next().getAsInt();
      i++;
    }
    return ints;
  }

  protected Location constructLocation(JsonObject locationObject) {
    String file = locationObject.get("file").getAsString();
    int offset = locationObject.get("offset").getAsInt();
    int length = locationObject.get("length").getAsInt();
    int startLine = locationObject.get("startLine").getAsInt();
    int startColumn = locationObject.get("startColumn").getAsInt();
    return new Location(file, offset, length, startLine, startColumn);
  }

  protected RefactoringProblem[] constructRefactoringProblemArray(JsonArray problemsArray) {
    ArrayList<RefactoringProblem> problems = new ArrayList<RefactoringProblem>();
    Iterator<JsonElement> iter = problemsArray.iterator();
    while (iter.hasNext()) {
      JsonElement problemElement = iter.next();
      if (problemElement instanceof JsonObject) {
        JsonObject problemObject = (JsonObject) problemElement;
        problems.add(new RefactoringProblemImpl(
            RefactoringProblemSeverity.valueOf(problemObject.get("severity").getAsString()),
            problemObject.get("message").getAsString(),
            constructLocation(problemObject.get("location").getAsJsonObject())));
      }
    }
    return problems.toArray(new RefactoringProblem[problems.size()]);
  }

  protected SourceChange constructSourceChange(JsonObject sourceChangeObject) {
    String message = sourceChangeObject.get("message").getAsString();
    ArrayList<SourceFileEdit> sourceFileEdits = new ArrayList<SourceFileEdit>();
    Iterator<JsonElement> iter = sourceChangeObject.get("edits").getAsJsonArray().iterator();
    while (iter.hasNext()) {
      JsonElement sourceFileEditElement = iter.next();
      if (sourceFileEditElement instanceof JsonObject) {
        sourceFileEdits.add(constructSourceFileEdit((JsonObject) sourceFileEditElement));
      }
    }
    return new SourceChangeImpl(
        message,
        sourceFileEdits.toArray(new SourceFileEdit[sourceFileEdits.size()]));
  }

  protected SourceChange[] constructSourceChangeArray(JsonArray jsonArray) {
    if (jsonArray == null) {
      return SourceChange.EMPTY_ARRAY;
    }
    int i = 0;
    SourceChange[] sourceChanges = new SourceChange[jsonArray.size()];
    Iterator<JsonElement> iterator = jsonArray.iterator();
    while (iterator.hasNext()) {
      sourceChanges[i] = constructSourceChange(iterator.next().getAsJsonObject());
      ++i;
    }
    return sourceChanges;
  }

  /**
   * Given some {@link JsonArray} and of string primitives, return the {@link String} array.
   * 
   * @param strJsonArray some {@link JsonArray} of {@link String}s
   * @return the {@link String} array
   */
  protected String[] constructStringArray(JsonArray strJsonArray) {
    if (strJsonArray == null) {
      return StringUtilities.EMPTY_ARRAY;
    }
    List<String> strings = Lists.newArrayList();
    Iterator<JsonElement> iterator = strJsonArray.iterator();
    while (iterator.hasNext()) {
      strings.add(iterator.next().getAsString());
    }
    return strings.toArray(new String[strings.size()]);
  }

  /**
   * Safely get some member off of the passed {@link JsonObject} and return the {@code int}. Instead
   * of calling {@link JsonObject#has(String)} before {@link JsonObject#get(String)}, only one call
   * to the {@link JsonObject} is made in order to be faster. The result will be the passed default
   * value if the member is not on the {@link JsonObject}. This is used for optional json
   * parameters.
   * 
   * @param jsonObject the {@link JsonObject}
   * @param memberName the member name
   * @param defaultValue the default value if the member is not in the {@link JsonObject}
   * @return the looked up {@link JsonArray}, or the default value
   */
  protected int safelyGetAsInt(JsonObject jsonObject, String memberName, int defaultValue) {
    JsonElement jsonElement = jsonObject.get(memberName);
    if (jsonElement == null) {
      return defaultValue;
    } else {
      return jsonElement.getAsInt();
    }
  }

  /**
   * Safely get some member off of the passed {@link JsonObject} and return the {@link JsonArray}.
   * Instead of calling {@link JsonObject#has(String)} before {@link JsonObject#get(String)}, only
   * one call to the {@link JsonObject} is made in order to be faster. The result will be
   * {@code null} if the member is not on the {@link JsonObject}. This is used for optional json
   * parameters.
   * 
   * @param jsonObject the {@link JsonObject}
   * @param memberName the member name
   * @return the looked up {@link JsonArray}, or {@code null}
   */
  protected JsonArray safelyGetAsJsonArray(JsonObject jsonObject, String memberName) {
    JsonElement jsonElement = jsonObject.get(memberName);
    if (jsonElement == null) {
      return null;
    } else {
      return jsonElement.getAsJsonArray();
    }
  }

  /**
   * Safely get some member off of the passed {@link JsonObject} and return the {@code JsonObject}.
   * Instead of calling {@link JsonObject#has(String)} before {@link JsonObject#get(String)}, only
   * one call to the {@link JsonObject} is made in order to be faster. The result will be the passed
   * default value if the member is not on the {@link JsonObject}. This is used for optional json
   * parameters.
   * 
   * @param jsonObject the {@link JsonObject}
   * @param memberName the member name
   * @return the looked up {@link JsonObject}, or {@code null}
   */
  protected JsonObject safelyGetAsJsonObject(JsonObject jsonObject, String memberName) {
    JsonElement jsonElement = jsonObject.get(memberName);
    if (jsonElement == null) {
      return null;
    } else {
      return jsonElement.getAsJsonObject();
    }
  }

  /**
   * Safely get some member off of the passed {@link JsonObject} and return the {@link String}.
   * Instead of calling {@link JsonObject#has(String)} before {@link JsonObject#get(String)}, only
   * one call to the {@link JsonObject} is made in order to be faster. The result will be
   * {@code null} if the member is not on the {@link JsonObject}. This is used for optional json
   * parameters.
   * 
   * @param jsonObject the {@link JsonObject}
   * @param memberName the member name
   * @return the looked up {@link String}, or {@code null}
   */
  protected String safelyGetAsString(JsonObject jsonObject, String memberName) {
    JsonElement jsonElement = jsonObject.get(memberName);
    if (jsonElement == null) {
      return null;
    } else {
      return jsonElement.getAsString();
    }
  }

  private SourceEdit constructSourceEdit(JsonObject sourceEditObject) {
    return new SourceEditImpl(sourceEditObject.get("offset").getAsInt(), sourceEditObject.get(
        "length").getAsInt(), sourceEditObject.get("replacement").getAsString());
  }

  private SourceFileEdit constructSourceFileEdit(JsonObject sourceFileEditObject) {
    String file = sourceFileEditObject.get("file").getAsString();
    ArrayList<SourceEdit> sourceEdits = new ArrayList<SourceEdit>();
    Iterator<JsonElement> iter = sourceFileEditObject.get("edits").getAsJsonArray().iterator();
    while (iter.hasNext()) {
      JsonElement sourceEditElement = iter.next();
      if (sourceEditElement instanceof JsonObject) {
        sourceEdits.add(constructSourceEdit((JsonObject) sourceEditElement));
      }
    }
    return new SourceFileEditImpl(file, sourceEdits.toArray(new SourceEdit[sourceEdits.size()]));
  }
}
