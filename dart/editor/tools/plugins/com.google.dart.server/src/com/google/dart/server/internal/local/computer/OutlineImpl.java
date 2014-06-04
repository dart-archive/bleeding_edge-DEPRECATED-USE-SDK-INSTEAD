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

import java.util.Arrays;

/**
 * A concrete implementation of {@link Outline}.
 * 
 * @coverage dart.server.local
 */
public class OutlineImpl extends SourceRegionImpl implements Outline {
  private final Outline parent;
  private final ElementKind kind;
  private final String name;
  private final boolean isAbstract;
  private final boolean isStatic;
  private final String arguments;
  private final String returnType;
  private Outline[] children = Outline.EMPTY_ARRAY;

  public OutlineImpl(Outline parent, ElementKind kind, String name, int offset, int length,
      boolean isAbstract, boolean isStatic) {
    this(parent, kind, name, offset, length, isAbstract, isStatic, null, null);
  }

  public OutlineImpl(Outline parent, ElementKind kind, String name, int offset, int length,
      boolean isAbstract, boolean isStatic, String arguments, String returnType) {
    super(offset, length);
    this.parent = parent;
    this.kind = kind;
    this.name = name;
    this.isAbstract = isAbstract;
    this.isStatic = isStatic;
    this.arguments = arguments;
    this.returnType = returnType;
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
    if (ObjectUtilities.equals(other.parent, parent) && other.getOffset() == getOffset()
        && other.getLength() == getLength() && other.kind == kind && other.name.equals(name)
        && ObjectUtilities.equals(other.arguments, arguments)
        && ObjectUtilities.equals(other.returnType, returnType)
        && (other.children.length == children.length)) {
      for (int i = 0; i < other.children.length; i++) {
        if (!other.children[i].equals(children[i])) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public String getArguments() {
    return arguments;
  }

  @Override
  public Outline[] getChildren() {
    return children;
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
  public Outline getParent() {
    return parent;
  }

  @Override
  public String getReturnType() {
    return returnType;
  }

  @Override
  public int hashCode() {
    return ObjectUtilities.combineHashCodes(
        Arrays.hashCode(new int[] {
            parent == null ? 0 : parent.hashCode(), kind.hashCode(), name.hashCode(),
            arguments == null ? 0 : arguments.hashCode(),
            returnType == null ? 0 : returnType.hashCode()}),
        Arrays.hashCode(children));
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
    builder.append("[offset=");
    builder.append(getOffset());
    builder.append(", length=");
    builder.append(getLength());
    builder.append(", kind=");
    builder.append(kind.name());
    builder.append(", name=");
    builder.append(name);
    builder.append(", arguments=");
    builder.append(arguments == null ? "null" : arguments);
    builder.append(", returnType=");
    builder.append(returnType == null ? "null" : returnType);
    builder.append(", children=[");
    builder.append(StringUtils.join(children, ", "));
    builder.append("]]");
    return builder.toString();
  }
}
