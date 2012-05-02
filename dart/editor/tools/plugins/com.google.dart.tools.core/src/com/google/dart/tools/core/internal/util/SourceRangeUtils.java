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
   * @return the expanded instance of {@link SourceRange}, which has the same center.
   */
  public static SourceRange getExpanded(SourceRange s, int delta) {
    return new SourceRangeImpl(s.getOffset() - delta, delta + s.getLength() + delta);
  }

  /**
   * @return <code>true</code> if two given {@link SourceRange}s are intersecting.
   */
  public static boolean intersects(SourceRange a, SourceRange b) {
    return contains(a, b.getOffset()) || contains(a, b.getOffset() + b.getLength() - 1)
        || contains(b, a.getOffset());
  }
}
