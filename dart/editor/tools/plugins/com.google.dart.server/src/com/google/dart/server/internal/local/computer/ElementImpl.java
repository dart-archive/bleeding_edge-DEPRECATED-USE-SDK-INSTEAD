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

import com.google.dart.engine.utilities.general.ObjectUtilities;
import com.google.dart.server.Element;
import com.google.dart.server.ElementKind;
import com.google.dart.server.Location;

/**
 * A concrete implementation of {@link Element}.
 * 
 * @coverage dart.server.local
 */
public class ElementImpl implements Element {

  private static final int ABSTRACT = 0x01;
  private static final int CONST = 0x02;
  private static final int FINAL = 0x04;
  private static final int TOP_LEVEL_STATIC = 0x08;
  private static final int PRIVATE = 0x10;
  private static final int DEPRECATED = 0x20;

  private final ElementKind kind;
  private final String name;
  private final Location location;
  private final int flags;
  private final String parameters;
  private final String returnType;

  public ElementImpl(ElementKind kind, String name, Location location, int flags,
      String parameters, String returnType) {
    this.kind = kind;
    this.name = name;
    this.location = location;
    this.flags = flags;
    this.parameters = parameters;
    this.returnType = returnType;
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
    return other.kind == kind && ObjectUtilities.equals(other.name, name)
        && ObjectUtilities.equals(other.location, location) && other.flags == flags
        && ObjectUtilities.equals(other.parameters, parameters)
        && ObjectUtilities.equals(other.returnType, returnType);
  }

  @Override
  public ElementKind getKind() {
    return kind;
  }

  @Override
  public Location getLocation() {
    return location;
  }

  @Override
  public String getName() {
    return name;
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
  public boolean isAbstract() {
    return (flags & ABSTRACT) != 0;
  }

  @Override
  public boolean isConst() {
    return (flags & CONST) != 0;
  }

  @Override
  public boolean isDeprecated() {
    return (flags & DEPRECATED) != 0;
  }

  @Override
  public boolean isFinal() {
    return (flags & FINAL) != 0;
  }

  @Override
  public boolean isPrivate() {
    return (flags & PRIVATE) != 0;
  }

  @Override
  public boolean isTopLevelOrStatic() {
    return (flags & TOP_LEVEL_STATIC) != 0;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[name=");
    builder.append(name);
    builder.append(", kind=");
    builder.append(kind);
    builder.append(", location=");
    builder.append(location.toString());
    builder.append(", flags=");
    builder.append(flags);
    builder.append(", parameters=");
    builder.append(parameters);
    builder.append(", returnType=");
    builder.append(returnType);
    builder.append("]");
    return builder.toString();
  }
}
