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
import org.apache.commons.lang3.StringUtils;

/**
 * A description of a parameter.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class Parameter {

  /**
   * An empty array of {@link Parameter}s.
   */
  public static final Parameter[] EMPTY_ARRAY = new Parameter[0];

  /**
   * The type that should be given to the parameter.
   */
  private final String type;

  /**
   * The name that should be given to the parameter.
   */
  private final String name;

  /**
   * Constructor for {@link Parameter}.
   */
  public Parameter(String type, String name) {
    this.type = type;
    this.name = name;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Parameter) {
      Parameter other = (Parameter) obj;
      return
        ObjectUtilities.equals(other.type, type) &&
        ObjectUtilities.equals(other.name, name);
    }
    return false;
  }

  /**
   * The name that should be given to the parameter.
   */
  public String getName() {
    return name;
  }

  /**
   * The type that should be given to the parameter.
   */
  public String getType() {
    return type;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("type=");
    builder.append(type + ", ");
    builder.append("name=");
    builder.append(name);
    builder.append("]");
    return builder.toString();
  }

}
