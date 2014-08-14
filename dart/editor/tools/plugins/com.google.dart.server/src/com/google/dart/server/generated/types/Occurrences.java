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
 * A description of the references to a single element within a single file.
 *
 * @coverage dart.server.generated.types
 */
@SuppressWarnings("unused")
public class Occurrences {

  /**
   * An empty array of {@link Occurrences}s.
   */
  public static final Occurrences[] EMPTY_ARRAY = new Occurrences[0];

  /**
   * The element that was referenced.
   */
  private final Element element;

  /**
   * The offsets of the name of the referenced element within the file.
   */
  private final int[] offsets;

  /**
   * The length of the name of the referenced element.
   */
  private final int length;

  /**
   * Constructor for {@link Occurrences}.
   */
  public Occurrences(Element element, int[] offsets, int length) {
    this.element = element;
    this.offsets = offsets;
    this.length = length;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof Occurrences) {
      Occurrences other = (Occurrences) obj;
      return
        ObjectUtilities.equals(other.element, element) &&
        Arrays.equals(other.offsets, offsets) &&
        other.length == length;
    }
    return false;
  }

  /**
   * The element that was referenced.
   */
  public Element getElement() {
    return element;
  }

  /**
   * The length of the name of the referenced element.
   */
  public int getLength() {
    return length;
  }

  /**
   * The offsets of the name of the referenced element within the file.
   */
  public int[] getOffsets() {
    return offsets;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[");
    builder.append("element=");
    builder.append(element + ", ");
    builder.append("offsets=");
    builder.append(StringUtils.join(offsets, ", ") + ", ");
    builder.append("length=");
    builder.append(length);
    builder.append("]");
    return builder.toString();
  }

}
