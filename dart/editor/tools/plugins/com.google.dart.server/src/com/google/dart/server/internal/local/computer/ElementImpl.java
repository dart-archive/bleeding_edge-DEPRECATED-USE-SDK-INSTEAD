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

package com.google.dart.server.internal.local.computer;

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.MethodElement;
import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.general.ObjectUtilities;
import com.google.dart.server.Element;
import com.google.dart.server.ElementKind;

/**
 * A concrete implementation of {@link Element}.
 * 
 * @coverage dart.server.local
 */
public class ElementImpl implements Element {
  /**
   * Creates an {@link ElementImpl} instance for the given
   * {@link com.google.dart.engine.element.Element}.
   */
  public static ElementImpl create(com.google.dart.engine.element.Element element) {
    if (element == null) {
      return null;
    }
    // prepare name
    String name = element.getDisplayName();
    int nameOffset = element.getNameOffset();
    int nameLength = name != null ? name.length() : 0;
    // prepare element kind specific information
    ElementKind outlineKind;
    boolean isAbstract = false;
    boolean isStatic = false;
    boolean isPrivate = element.isPrivate();
    switch (element.getKind()) {
      case CLASS:
        outlineKind = ElementKind.CLASS;
        isAbstract = ((ClassElement) element).isAbstract();
        break;
      case COMPILATION_UNIT:
        outlineKind = ElementKind.COMPILATION_UNIT;
        nameOffset = -1;
        nameLength = 0;
        break;
      case CONSTRUCTOR:
        outlineKind = ElementKind.CONSTRUCTOR;
        String className = element.getEnclosingElement().getName();
        if (name.length() != 0) {
          name = className + "." + name;
        } else {
          name = className;
        }
        break;
      case FUNCTION:
        outlineKind = ElementKind.FUNCTION;
        break;
      case GETTER:
        outlineKind = ElementKind.GETTER;
        break;
      case FUNCTION_TYPE_ALIAS:
        outlineKind = ElementKind.FUNCTION_TYPE_ALIAS;
        break;
      case LIBRARY:
        outlineKind = ElementKind.LIBRARY;
        break;
      case METHOD:
        outlineKind = ElementKind.METHOD;
        isAbstract = ((MethodElement) element).isAbstract();
        break;
      case SETTER:
        outlineKind = ElementKind.SETTER;
        break;
      default:
        outlineKind = ElementKind.UNKNOWN;
        break;
    }
    // extract return type and parameters from toString()
    // TODO(scheglov) we need a way to get this information directly from an Element
    String parameters;
    String returnType;
    {
      String str = element.toString();
      // return type
      String rightArrow = com.google.dart.engine.element.Element.RIGHT_ARROW;
      int returnIndex = str.lastIndexOf(rightArrow);
      if (returnIndex != -1) {
        returnType = str.substring(returnIndex + rightArrow.length());
        str = str.substring(0, returnIndex);
      } else {
        returnType = null;
      }
      // parameters
      int parametersIndex = str.indexOf("(");
      if (parametersIndex != -1) {
        parameters = str.substring(parametersIndex);
      } else {
        parameters = null;
      }
    }
    // new element
    return new ElementImpl(
        createId(element),
        element.getSource(),
        outlineKind,
        name,
        nameOffset,
        nameLength,
        parameters,
        returnType,
        isAbstract,
        isStatic,
        isPrivate);
  }

  /**
   * Returns an identifier of the given {@link Element}, maybe {@code null} if {@code null} given.
   */
  public static String createId(com.google.dart.engine.element.Element element) {
    if (element == null) {
      return null;
    }
    return element.getLocation().getEncoding();
  }

  private final String id;
  private final Source source;
  private final ElementKind kind;
  private final String name;
  private final int offset;
  private final int length;
  private final String parameters;
  private final String returnType;
  private final boolean isAbstract;
  private final boolean isPrivate;
  private final boolean isStatic;

  public ElementImpl(String id, Source source, ElementKind kind, String name, int offset,
      int length, String parameters, String returnType, boolean isAbstract, boolean isStatic,
      boolean isPrivate) {
    this.id = id;
    this.source = source;
    this.kind = kind;
    this.name = name;
    this.offset = offset;
    this.length = length;
    this.parameters = parameters;
    this.returnType = returnType;
    this.isAbstract = isAbstract;
    this.isStatic = isStatic;
    this.isPrivate = isPrivate;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof ElementImpl)) {
      return false;
    }
    ElementImpl other = (ElementImpl) obj;
    return other.kind == kind && ObjectUtilities.equals(other.source, source)
        && ObjectUtilities.equals(name, other.name);
  }

  @Override
  public String getId() {
    return id;
  }

  @Override
  public ElementKind getKind() {
    return kind;
  }

  @Override
  public int getLength() {
    return length;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public String getParameters() {
    return parameters;
  }

  @Override
  public String getReturnType() {
    return returnType;
  }

  @Override
  public Source getSource() {
    return source;
  }

  @Override
  public int hashCode() {
    if (name == null) {
      return source.hashCode();
    }
    return ObjectUtilities.combineHashCodes(source.hashCode(), name.hashCode());
  }

  @Override
  public boolean isAbstract() {
    return isAbstract;
  }

  @Override
  public boolean isPrivate() {
    return isPrivate;
  }

  @Override
  public boolean isStatic() {
    return isStatic;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[name=");
    builder.append(name);
    builder.append(", kind=");
    builder.append(kind);
    builder.append(", offset=");
    builder.append(offset);
    builder.append(", length=");
    builder.append(length);
    builder.append(", parameters=");
    builder.append(parameters);
    builder.append(", return=");
    builder.append(returnType);
    builder.append("]");
    return builder.toString();
  }
}
