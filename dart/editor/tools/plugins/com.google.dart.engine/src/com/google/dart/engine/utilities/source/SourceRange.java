/*
 * Copyright (c) 2013, the Dart project authors.
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
package com.google.dart.engine.utilities.source;

import com.google.dart.engine.element.Element;
import com.google.dart.engine.source.Source;

/**
 * A source range defines an {@link Element}'s source coordinates relative to its {@link Source}.
 */
public final class SourceRange {
  /**
   * The 0-based index of the first character of the source code for this element, relative to the
   * source buffer in which this element is contained.
   */
  private final int offset;

  /**
   * The number of characters of the source code for this element, relative to the source buffer in
   * which this element is contained.
   */
  private final int length;

  /**
   * Initialize a newly created source range using the given offset and the given length.
   * 
   * @param offset the given offset
   * @param length the given length
   */
  public SourceRange(int offset, int length) {
    this.offset = offset;
    this.length = length;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof SourceRange)) {
      return false;
    }
    SourceRange sourceRange = (SourceRange) obj;
    return sourceRange.getOffset() == offset && sourceRange.getLength() == length;
  }

  /**
   * Returns the number of characters of the source code for this element, relative to the source
   * buffer in which this element is contained.
   * 
   * @return the number of characters of the source code for this element, relative to the source
   *         buffer in which this element is contained
   */
  public int getLength() {
    return length;
  }

  /**
   * Returns the 0-based index of the first character of the source code for this element, relative
   * to the source buffer in which this element is contained.
   * 
   * @return the 0-based index of the first character of the source code for this element, relative
   *         to the source buffer in which this element is contained
   */
  public int getOffset() {
    return offset;
  }

  @Override
  public int hashCode() {
    return 31 * offset + length;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[offset=");
    builder.append(offset);
    builder.append(", length=");
    builder.append(length);
    builder.append("]");
    return builder.toString();
  }
}
