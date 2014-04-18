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

import com.google.dart.server.Outline;
import com.google.dart.server.OutlineKind;

import org.apache.commons.lang3.StringUtils;

/**
 * A concrete implementation of {@link Outline}.
 * 
 * @coverage dart.server.local
 */
public class OutlineImpl implements Outline {
  private final Outline parent;
  private final OutlineKind kind;
  private final String name;
  private final int offset;
  private final int length;
  private final String arguments;
  private final String returnType;
  private Outline[] children = Outline.EMPTY_ARRAY;

  public OutlineImpl(Outline parent, OutlineKind kind, String name, int offset, int length,
      String arguments, String returnType) {
    this.parent = parent;
    this.kind = kind;
    this.name = name;
    this.offset = offset;
    this.length = length;
    this.arguments = arguments;
    this.returnType = returnType;
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
  public OutlineKind getKind() {
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
  public Outline getParent() {
    return parent;
  }

  @Override
  public String getReturnType() {
    return returnType;
  }

  public void setChildren(Outline[] children) {
    this.children = children;
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
    builder.append(", arguments=");
    builder.append(arguments);
    builder.append(", return=");
    builder.append(returnType);
    builder.append(", children=[");
    builder.append(StringUtils.join(children, ", "));
    builder.append("]]");
    return builder.toString();
  }
}
