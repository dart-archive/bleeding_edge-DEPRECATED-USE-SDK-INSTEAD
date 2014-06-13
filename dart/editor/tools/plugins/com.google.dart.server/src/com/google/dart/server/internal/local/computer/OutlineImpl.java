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
import com.google.dart.server.Outline;

import org.apache.commons.lang3.StringUtils;

/**
 * A concrete implementation of {@link Outline}.
 * 
 * @coverage dart.server.local
 */
public class OutlineImpl implements Outline {
  private final Outline parent;
  private final Element element;
  private final int offset;
  private final int length;
  private Outline[] children = Outline.EMPTY_ARRAY;

  public OutlineImpl(Outline parent, Element element, int offset, int length) {
    this.parent = parent;
    this.element = element;
    this.offset = offset;
    this.length = length;
  }

  @Override
  public boolean containsInclusive(int offsetValue) {
    return offset <= offsetValue && offsetValue <= offset + length;
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
    if (ObjectUtilities.equals(other.parent, parent)
        && ObjectUtilities.equals(other.element, element)
        && other.children.length == children.length) {
      for (int i = 0; i < children.length; i++) {
        if (!ObjectUtilities.equals(other.children[i], children)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  @Override
  public Outline[] getChildren() {
    return children;
  }

  @Override
  public Element getElement() {
    return element;
  }

  @Override
  public int getLength() {
    return length;
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
  public int hashCode() {
    return ObjectUtilities.combineHashCodes(parent.hashCode(), element.getName().hashCode());
  }

  public void setChildren(Outline[] children) {
    this.children = children;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[element=");
    builder.append(element.toString());
    builder.append(", children=[");
    builder.append(StringUtils.join(children, ", "));
    builder.append("]]");
    return builder.toString();
  }
}
