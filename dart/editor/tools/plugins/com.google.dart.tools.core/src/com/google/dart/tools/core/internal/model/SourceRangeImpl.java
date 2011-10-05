/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.internal.model;

import com.google.dart.compiler.ast.DartNode;
import com.google.dart.tools.core.model.SourceRange;

/**
 * Instances of the class <code>SourceRangeImpl</code> implement a source range that defines an
 * element's source coordinates relative to its source buffer.
 */
public class SourceRangeImpl implements SourceRange {
  /**
   * Return <code>true</code> if a valid source range is available in the given SourceRange. When an
   * element has no associated source code, Dart Model APIs may return either <code>null</code> or a
   * range of [-1, 0] to indicate an invalid range. This utility method can be used to detect that
   * case.
   * 
   * @param range a source range, can be <code>null</code>
   * @return <code>true</code> iff range is not null and range.getOffset() is not -1
   */
  public static boolean isAvailable(SourceRange range) {
    // see https://bugs.eclipse.org/bugs/show_bug.cgi?id=130161
    return range != null && range.getOffset() != -1;
  }

  /**
   * The 0-based index of the first character of the source code for this element, relative to the
   * source buffer in which this element is contained.
   */
  private int offset;

  /**
   * The number of characters of the source code for this element, relative to the source buffer in
   * which this element is contained.
   */
  private int length;

  /**
   * Initialize a newly created source range using the offset and length from the given node.
   * 
   * @param the node whose source position is to be used to initialize the source range
   */
  public SourceRangeImpl(DartNode node) {
    this.offset = node.getSourceStart();
    this.length = node.getSourceLength();
  }

  /**
   * Initialize a newly created source range using the given offset and the given length.
   * 
   * @param offset the given offset
   * @param length the given length
   */
  public SourceRangeImpl(int offset, int length) {
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

  @Override
  public int getLength() {
    return length;
  }

  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public int hashCode() {
    return length ^ offset;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("[offset="); //$NON-NLS-1$
    builder.append(offset);
    builder.append(", length="); //$NON-NLS-1$
    builder.append(length);
    builder.append("]"); //$NON-NLS-1$
    return builder.toString();
  }
}
