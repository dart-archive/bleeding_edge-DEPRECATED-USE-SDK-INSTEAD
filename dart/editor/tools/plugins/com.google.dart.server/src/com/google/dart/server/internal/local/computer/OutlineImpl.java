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
import com.google.dart.server.SourceRegion;

import org.apache.commons.lang3.StringUtils;

/**
 * A concrete implementation of {@link Outline}.
 * 
 * @coverage dart.server.local
 */
public class OutlineImpl implements Outline {
  private final Outline parent;
  private final Element element;
  private final SourceRegion sourceRegion;
  private Outline[] children = Outline.EMPTY_ARRAY;

  public OutlineImpl(Outline parent, Element element, SourceRegion sourceRegion) {
    this.parent = parent;
    this.element = element;
    this.sourceRegion = sourceRegion;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof OutlineImpl)) {
      return false;
    }
    OutlineImpl other = (OutlineImpl) obj;
    return ObjectUtilities.equals(other.element, element)
        && ObjectUtilities.equals(other.parent, parent);
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
  public Outline getParent() {
    return parent;
  }

  @Override
  public SourceRegion getSourceRegion() {
    return sourceRegion;
  }

  @Override
  public int hashCode() {
    if (parent == null) {
      return element.hashCode();
    }
    return ObjectUtilities.combineHashCodes(parent.hashCode(), element.hashCode());
  }

  public void setChildren(Outline[] children) {
    this.children = children;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[element=");
    builder.append(element);
    builder.append(", children=[");
    builder.append(StringUtils.join(children, ", "));
    builder.append("]]");
    return builder.toString();
  }
}
