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
import com.google.dart.engine.utilities.general.StringUtilities;
import com.google.dart.server.ElementKind;
import com.google.dart.server.Outline;

import org.apache.commons.lang3.StringUtils;

/**
 * A concrete implementation of {@link Outline}.
 * 
 * @coverage dart.server.local
 */
public class OutlineImpl implements Outline {
  private final Outline parent;
  private final ElementKind kind;
  private final String name;
  private final int nameOffset;
  private final int nameLength;
  private final int elementOffset;
  private final int elementLength;
  private final boolean isAbstract;
  private final boolean isStatic;
  private final String parameters;
  private final String returnType;
  private Outline[] children = Outline.EMPTY_ARRAY;

  public OutlineImpl(Outline parent, ElementKind kind, String name, int nameOffset, int nameLength,
      int elementOffset, int elementLength, boolean isAbstract, boolean isStatic,
      String parameters, String returnType) {
    this.parent = parent;
    this.kind = kind;
    this.name = name;
    this.nameOffset = nameOffset;
    this.nameLength = nameLength;
    this.elementOffset = elementOffset;
    this.elementLength = elementLength;
    this.isAbstract = isAbstract;
    this.isStatic = isStatic;
    this.parameters = parameters;
    this.returnType = returnType;
  }

  @Override
  public boolean containsInclusive(int offset) {
    return elementOffset <= offset && offset <= elementOffset + elementLength;
  }

  @Override
  public boolean equals(Object object) {
    if (object == this) {
      return true;
    }
    if (!(object instanceof OutlineImpl)) {
      return false;
    }
    OutlineImpl other = (OutlineImpl) object;
    if (other.kind == kind && other.name.equals(name) && other.getNameOffset() == getNameOffset()
        && other.getNameLength() == getNameLength()) {
      return true;
    }
    return false;
  }

  @Override
  public Outline[] getChildren() {
    return children;
  }

  @Override
  public int getElementLength() {
    return elementLength;
  }

  @Override
  public int getElementOffset() {
    return elementOffset;
  }

  @Override
  public ElementKind getKind() {
    return kind;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public int getNameLength() {
    return nameLength;
  }

  @Override
  public int getNameOffset() {
    return nameOffset;
  }

  @Override
  public String getParameters() {
    return parameters;
  }

  @Override
  public Outline getParent() {
    return parent;
  }

  @Override
  public String getReturnType() {
    return returnType;
  }

  @Override
  public int hashCode() {
    return ObjectUtilities.combineHashCodes(kind.hashCode(), name.hashCode());
  }

  @Override
  public boolean isAbstract() {
    return isAbstract;
  }

  @Override
  public boolean isPrivate() {
    if (kind == ElementKind.COMPILATION_UNIT) {
      return false;
    }
    if (kind == ElementKind.CONSTRUCTOR) {
      return name.contains("._");
    }
    return StringUtilities.startsWithChar(name, '_');
  }

  @Override
  public boolean isStatic() {
    return isStatic;
  }

  public void setChildren(Outline[] children) {
    this.children = children;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[kind=");
    builder.append(kind.name());
    builder.append(", name=");
    builder.append(name);
    builder.append(", nameOffset=");
    builder.append(nameOffset);
    builder.append(", nameLength=");
    builder.append(nameLength);
    builder.append(", elementOffset=");
    builder.append(elementOffset);
    builder.append(", elementLength=");
    builder.append(elementLength);
    builder.append(", arguments=");
    builder.append(parameters == null ? "null" : parameters);
    builder.append(", returnType=");
    builder.append(returnType == null ? "null" : returnType);
    builder.append(", children=[");
    builder.append(StringUtils.join(children, ", "));
    builder.append("]]");
    return builder.toString();
  }
}
