/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.internal.util;

import com.google.dart.tools.core.internal.model.SourceRangeImpl;
import com.google.dart.tools.core.model.SourceRange;

/**
 * Utilities for {@link SourceRange} checks.
 */
public class SourceRangeUtils {
  /**
   * @return <code>true</code> if <code>x</code> is in [offset, offset + length) interval.
   */
  public static boolean contains(SourceRange r, int x) {
    return r.getOffset() <= x && x < r.getOffset() + r.getLength();
  }

  /**
   * @return <code>true</code> if <code>thisRange</code> covers <code>otherRange</code>.
   */
  public static boolean covers(SourceRange thisRange, SourceRange otherRange) {
    return thisRange.getOffset() <= otherRange.getOffset()
        && getEnd(thisRange) >= getEnd(otherRange);
  }

  /**
   * @return the exclusive end position of given {@link SourceRange}.
   */
  public static int getEnd(SourceRange r) {
    return r.getOffset() + r.getLength();
  }

  /**
   * @return the inclusive end position of given {@link SourceRange}.
   */
  public static int getEndInclusive(SourceRange r) {
    return r.getOffset() + r.getLength() + 1;
  }

  /**
   * @return the expanded instance of {@link SourceRange}, which has the same center.
   */
  public static SourceRange getExpanded(SourceRange s, int delta) {
    return new SourceRangeImpl(s.getOffset() - delta, delta + s.getLength() + delta);
  }

  /**
   * @return the instance of {@link SourceRange} with end moved on "delta".
   */
  public static SourceRange getMoveEnd(SourceRange s, int delta) {
    return new SourceRangeImpl(s.getOffset(), s.getLength() + delta);
  }

  /**
   * @return <code>true</code> if two given {@link SourceRange}s are intersecting.
   */
  public static boolean intersects(SourceRange a, SourceRange b) {
    if (getEnd(a) <= b.getOffset()) {
      return false;
    }
    if (a.getOffset() >= getEnd(b)) {
      return false;
    }
    return true;
  }

  /**
   * Helper method that answers whether a valid source range is available in the given
   * {@link SourceRange}. When an element has no associated source code, Dart Model APIs may return
   * either <code>null</code> or a range of [-1, 0] to indicate an invalid range. This utility
   * method can be used to detect that case.
   * 
   * @param range a source range, can be <code>null</code>
   * @return <code>true</code> iff range is not null and range.getOffset() is not -1
   */
  public static boolean isAvailable(SourceRange range) {
    return range != null && range.getOffset() != -1;
  }

}
