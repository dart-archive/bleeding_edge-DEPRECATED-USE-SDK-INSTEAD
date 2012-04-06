package com.google.dart.tools.core.internal.util;

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
   * @return <code>true</code> if two given {@link SourceRange}s are intersecting.
   */
  public static boolean intersects(SourceRange a, SourceRange b) {
    return contains(a, b.getOffset()) || contains(a, b.getOffset() + b.getLength() - 1)
        || contains(b, a.getOffset());
  }
}
