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
 * 
 * @coverage dart.engine.utilities
 */
public final class SourceRange {
  /**
   * An empty {@link SourceRange} with offset {@code 0} and length {@code 0}.
   */
  public static final SourceRange EMPTY = new SourceRange(0, 0);

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

  /**
   * @return {@code true} if <code>x</code> is in [offset, offset + length) interval.
   */
  public boolean contains(int x) {
    return offset <= x && x < offset + length;
  }

  /**
   * @return {@code true} if <code>x</code> is in (offset, offset + length) interval.
   */
  public boolean containsExclusive(int x) {
    return offset < x && x < offset + length;
  }

  /**
   * @return {@code true} if <code>otherRange</code> covers this {@link SourceRange}.
   */
  public boolean coveredBy(SourceRange otherRange) {
    return otherRange.covers(this);
  }

  /**
   * @return {@code true} if this {@link SourceRange} covers <code>otherRange</code>.
   */
  public boolean covers(SourceRange otherRange) {
    return getOffset() <= otherRange.getOffset() && otherRange.getEnd() <= getEnd();
  }

  /**
   * @return {@code true} if this {@link SourceRange} ends in <code>otherRange</code>.
   */
  public boolean endsIn(SourceRange otherRange) {
    int thisEnd = getEnd();
    return otherRange.contains(thisEnd);
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
   * @return the 0-based index of the after-last character of the source code for this element,
   *         relative to the source buffer in which this element is contained.
   */
  public int getEnd() {
    return offset + length;
  }

  /**
   * @return the expanded instance of {@link SourceRange}, which has the same center.
   */
  public SourceRange getExpanded(int delta) {
    return new SourceRange(offset - delta, delta + length + delta);
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
   * @return the instance of {@link SourceRange} with end moved on "delta".
   */
  public SourceRange getMoveEnd(int delta) {
    return new SourceRange(offset, length + delta);
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

  /**
   * @return the expanded translated of {@link SourceRange}, with moved start and the same length.
   */
  public SourceRange getTranslated(int delta) {
    return new SourceRange(offset + delta, length);
  }

  /**
   * @return the minimal {@link SourceRange} that cover this and the given {@link SourceRange}s.
   */
  public SourceRange getUnion(SourceRange other) {
    int newOffset = Math.min(offset, other.offset);
    int newEnd = Math.max(offset + length, other.offset + other.length);
    return new SourceRange(newOffset, newEnd - newOffset);
  }

  @Override
  public int hashCode() {
    return 31 * offset + length;
  }

  /**
   * @return {@code true} if this {@link SourceRange} intersects with given.
   */
  public boolean intersects(SourceRange other) {
    if (other == null) {
      return false;
    }
    if (getEnd() <= other.getOffset()) {
      return false;
    }
    if (getOffset() >= other.getEnd()) {
      return false;
    }
    return true;
  }

  /**
   * @return {@code true} if this {@link SourceRange} starts in <code>otherRange</code>.
   */
  public boolean startsIn(SourceRange otherRange) {
    return otherRange.contains(offset);
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
