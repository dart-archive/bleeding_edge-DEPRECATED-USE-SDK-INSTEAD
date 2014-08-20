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
 *
 * This file has been automatically generated.  Please do not edit it manually.
 * To regenerate the file, use the script "pkg/analysis_server/spec/generate_files".
 */
package com.google.dart.server.generated.types;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import com.google.dart.server.utilities.general.ObjectUtilities;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.StringUtils;

/**
 * A description of a parameter in a method refactoring.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class RefactoringMethodParameter {

  /**
   * An empty array of {@link RefactoringMethodParameter}s.
   */
  public static final RefactoringMethodParameter[] EMPTY_ARRAY = new RefactoringMethodParameter[0];

  /**
   * The unique identifier of the parameter. Clients may omit this field for the parameters they want
   * to add.
   */
  private final String id;

  /**
   * The kind of the parameter.
   */
  private final String kind;

  /**
   * The type that should be given to the parameter, or the return type of the parameter's function
   * type.
   */
  private final String type;

  /**
   * The name that should be given to the parameter.
   */
  private final String name;

  /**
   * The parameter list of the parameter's function type. If the parameter is not of a function type,
   * this field will not be defined. If the function type has zero parameters, this field will have a
   * value of "()".
   */
  private final String parameters;

  /**
   * Constructor for {@link RefactoringMethodParameter}.
   */
  public RefactoringMethodParameter(String id, String kind, String type, String name, String parameters) {
    this.id = id;
    this.kind = kind;
    this.type = type;
    this.name = name;
    this.parameters = parameters;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof RefactoringMethodParameter) {
      RefactoringMethodParameter other = (RefactoringMethodParameter) obj;
      return
        ObjectUtilities.equals(other.id, id) &&
        ObjectUtilities.equals(other.kind, kind) &&
        ObjectUtilities.equals(other.type, type) &&
        ObjectUtilities.equals(other.name, name) &&
        ObjectUtilities.equals(other.parameters, parameters);
    }
    return false;
  }

  /**
   * The unique identifier of the parameter. Clients may omit this field for the parameters they want
   * to add.
   */
  public String getId() {
    return id;
  }

  /**
   * The kind of the parameter.
   */
  public String getKind() {
    return kind;
  }

  /**
   * The name that should be given to the parameter.
   */
  public String getName() {
    return name;
  }

  /**
   * The parameter list of the parameter's function type. If the parameter is not of a function type,
   * this field will not be defined. If the function type has zero parameters, this field will have a
   * value of "()".
   */
  public String getParameters() {
    return parameters;
  }

  /**
   * The type that should be given to the parameter, or the return type of the parameter's function
   * type.
   */
  public String getType() {
    return type;
  }

  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    if (id != null) {
      jsonObject.addProperty("id", id);
    }
    jsonObject.addProperty("kind", kind);
    jsonObject.addProperty("type", type);
    jsonObject.addProperty("name", name);
    if (parameters != null) {
      jsonObject.addProperty("parameters", parameters);
    }
    return jsonObject;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("id=");
    builder.append(id + ", ");
    builder.append("kind=");
    builder.append(kind + ", ");
    builder.append("type=");
    builder.append(type + ", ");
    builder.append("name=");
    builder.append(name + ", ");
    builder.append("parameters=");
    builder.append(parameters);
    builder.append("]");
    return builder.toString();
  }

}
