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
 * Information about an element (something that can be declared in code).
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class Element {

  /**
   * A bit-map containing the following flags:
   *
   * - 0x01 - set if the element is explicitly or implicitly abstract
   * - 0x02 - set if the element was declared to be ‘const’
   * - 0x04 - set if the element was declared to be ‘final’
   * - 0x08 - set if the element is a static member of a class or is a top-level function or field
   * - 0x10 - set if the element is private
   * - 0x20 - set if the element is deprecated
   */
  private final int flags;

  /**
   * The kind of the element.
   */
  private final String kind;

  /**
   * The location of the name in the declaration of the element.
   */
  private final Location location;

  /**
   * The name of the element. This is typically used as the label in the outline.
   */
  private final String name;

  /**
   * The parameter list for the element. If the element is not a method or function this field will
   * not be defined. If the element has zero parameters, this field will have a value of "()".
   */
  private final String parameters;

  /**
   * The return type of the element. If the element is not a method or function this field will not
   * be defined. If the element does not have a declared return type, this field will contain an
   * empty string.
   */
  private final String returnType;

  /**
   * Constructor for {@link Element}.
   */
  public Element(String kind, String name, Location location, int flags, String parameters, String returnType) {
    this.kind = kind;
    this.name = name;
    this.location = location;
    this.flags = flags;
    this.parameters = parameters;
    this.returnType = returnType;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Element) {
      Element other = (Element) obj;
      return
        ObjectUtilities.equals(other.kind, kind) &&
        ObjectUtilities.equals(other.name, name) &&
        ObjectUtilities.equals(other.location, location) &&
        other.flags == flags &&
        ObjectUtilities.equals(other.parameters, parameters) &&
        ObjectUtilities.equals(other.returnType, returnType);
    }
    return false;
  }

  /**
   * A bit-map containing the following flags:
   *
   * - 0x01 - set if the element is explicitly or implicitly abstract
   * - 0x02 - set if the element was declared to be ‘const’
   * - 0x04 - set if the element was declared to be ‘final’
   * - 0x08 - set if the element is a static member of a class or is a top-level function or field
   * - 0x10 - set if the element is private
   * - 0x20 - set if the element is deprecated
   */
  public int getFlags() {
    return flags;
  }

  /**
   * The kind of the element.
   */
  public String getKind() {
    return kind;
  }

  /**
   * The location of the name in the declaration of the element.
   */
  public Location getLocation() {
    return location;
  }

  /**
   * The name of the element. This is typically used as the label in the outline.
   */
  public String getName() {
    return name;
  }

  /**
   * The parameter list for the element. If the element is not a method or function this field will
   * not be defined. If the element has zero parameters, this field will have a value of "()".
   */
  public String getParameters() {
    return parameters;
  }

  /**
   * The return type of the element. If the element is not a method or function this field will not
   * be defined. If the element does not have a declared return type, this field will contain an
   * empty string.
   */
  public String getReturnType() {
    return returnType;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("kind=");
    builder.append(kind.toString() + ", ");
    builder.append("name=");
    builder.append(name.toString() + ", ");
    builder.append("location=");
    builder.append(location.toString() + ", ");
    builder.append("flags=");
    builder.append(flags + ", ");
    builder.append("parameters=");
    builder.append(parameters.toString() + ", ");
    builder.append("returnType=");
    builder.append(returnType.toString() + ", ");
    builder.append("]");
    return builder.toString();
  }

}
