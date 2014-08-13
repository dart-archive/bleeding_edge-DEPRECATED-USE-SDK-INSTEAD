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
 * An node in the outline structure of a file.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class Outline {

  /**
   * The children of the node. The field will be omitted if the node has no children.
   */
  private final List<Outline> children;

  /**
   * A description of the element represented by this node.
   */
  private final Element element;

  /**
   * The length of the element.
   */
  private final int length;

  /**
   * The offset of the first character of the element. This is different than the offset in the
   * Element, which if the offset of the name of the element. It can be used, for example, to map
   * locations in the file back to an outline.
   */
  private final int offset;

  /**
   * Constructor for {@link Outline}.
   */
  public Outline(Element element, int offset, int length, List<Outline> children) {
    this.element = element;
    this.offset = offset;
    this.length = length;
    this.children = children;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Outline) {
      Outline other = (Outline) obj;
      return
        ObjectUtilities.equals(other.element, element) &&
        other.offset == offset &&
        other.length == length &&
        ObjectUtilities.equals(other.children, children);
    }
    return false;
  }

  /**
   * The children of the node. The field will be omitted if the node has no children.
   */
  public List<Outline> getChildren() {
    return children;
  }

  /**
   * A description of the element represented by this node.
   */
  public Element getElement() {
    return element;
  }

  /**
   * The length of the element.
   */
  public int getLength() {
    return length;
  }

  /**
   * The offset of the first character of the element. This is different than the offset in the
   * Element, which if the offset of the name of the element. It can be used, for example, to map
   * locations in the file back to an outline.
   */
  public int getOffset() {
    return offset;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("element=");
    builder.append(element.toString() + ", ");
    builder.append("offset=");
    builder.append(offset + ", ");
    builder.append("length=");
    builder.append(length + ", ");
    builder.append("children=");
    builder.append(StringUtils.join(children, ", ") + ", ");
    builder.append("]");
    return builder.toString();
  }

}
