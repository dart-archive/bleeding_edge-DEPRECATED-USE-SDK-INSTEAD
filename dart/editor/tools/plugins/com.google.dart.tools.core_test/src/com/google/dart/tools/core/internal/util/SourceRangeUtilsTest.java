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

import junit.framework.TestCase;

/**
 * Test for {@link SourceRangeUtils}.
 */
public class SourceRangeUtilsTest extends TestCase {
  /**
   * Test for {@link SourceRangeUtils#contains(SourceRange, int)}
   */
  public void test_contains() throws Exception {
    SourceRange r = new SourceRangeImpl(0, 3);
    assertFalse(SourceRangeUtils.contains(r, -1));
    assertTrue(SourceRangeUtils.contains(r, 0));
    assertTrue(SourceRangeUtils.contains(r, 1));
    assertTrue(SourceRangeUtils.contains(r, 2));
    assertFalse(SourceRangeUtils.contains(r, 3));
  }

  /**
   * Test for {@link SourceRangeUtils#getEnd(SourceRange)}.
   */
  public void test_getEnd() throws Exception {
    assertEquals(8, SourceRangeUtils.getEnd(new SourceRangeImpl(5, 3)));
  }

  /**
   * Test for {@link SourceRangeUtils#getExpanded(SourceRange, int)}.
   */
  public void test_getExpanded() throws Exception {
    assertEquals(
        new SourceRangeImpl(5, 3),
        SourceRangeUtils.getExpanded(new SourceRangeImpl(5, 3), 0));
    assertEquals(
        new SourceRangeImpl(3, 7),
        SourceRangeUtils.getExpanded(new SourceRangeImpl(5, 3), 2));
    assertEquals(
        new SourceRangeImpl(6, 1),
        SourceRangeUtils.getExpanded(new SourceRangeImpl(5, 3), -1));
  }

  /**
   * Test for {@link SourceRangeUtils#intersects(SourceRange, SourceRange)}
   */
  public void test_intersects() throws Exception {
    SourceRange a = new SourceRangeImpl(5, 3);
    // ends before
    assertFalse(SourceRangeUtils.intersects(a, new SourceRangeImpl(0, 5)));
    // begins after
    assertFalse(SourceRangeUtils.intersects(a, new SourceRangeImpl(8, 5)));
    // begins on same offset
    assertTrue(SourceRangeUtils.intersects(a, new SourceRangeImpl(5, 1)));
    // begins inside, ends inside
    assertTrue(SourceRangeUtils.intersects(a, new SourceRangeImpl(6, 1)));
    // begins inside, ends after
    assertTrue(SourceRangeUtils.intersects(a, new SourceRangeImpl(6, 10)));
    // begins before, ends after
    assertTrue(SourceRangeUtils.intersects(a, new SourceRangeImpl(0, 10)));
  }
}
