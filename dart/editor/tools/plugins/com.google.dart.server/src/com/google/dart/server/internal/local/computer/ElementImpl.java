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

  public ElementImpl(Source source, ElementKind kind, String name, int offset, int length,
      String parameters, String returnType, boolean isAbstract, boolean isPrivate, boolean isStatic) {
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
