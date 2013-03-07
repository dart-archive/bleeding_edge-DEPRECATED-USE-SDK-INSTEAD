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

import junit.framework.TestCase;

public class SourceRangeTest extends TestCase {
  public void test_access() throws Exception {
    SourceRange r = new SourceRange(10, 1);
    assertEquals(10, r.getOffset());
    assertEquals(1, r.getLength());
    assertEquals(10 + 1, r.getEnd());
    // to check
    r.hashCode();
  }

  public void test_contains() throws Exception {
    SourceRange r = new SourceRange(5, 10);
    assertTrue(r.contains(5));
    assertTrue(r.contains(10));
    assertTrue(r.contains(14));
    assertFalse(r.contains(0));
    assertFalse(r.contains(15));
  }

  public void test_containsExclusive() throws Exception {
    SourceRange r = new SourceRange(5, 10);
    assertFalse(r.containsExclusive(5));
    assertTrue(r.containsExclusive(10));
    assertTrue(r.containsExclusive(14));
    assertFalse(r.containsExclusive(0));
    assertFalse(r.containsExclusive(15));
  }

  public void test_coveredBy() throws Exception {
    SourceRange r = new SourceRange(5, 10);
    // ends before
    assertFalse(r.coveredBy(new SourceRange(20, 10)));
    // starts after
    assertFalse(r.coveredBy(new SourceRange(0, 3)));
    // only intersects
    assertFalse(r.coveredBy(new SourceRange(0, 10)));
    assertFalse(r.coveredBy(new SourceRange(10, 10)));
    // covered
    assertTrue(r.coveredBy(new SourceRange(0, 20)));
    assertTrue(r.coveredBy(new SourceRange(5, 10)));
  }

  public void test_covers() throws Exception {
    SourceRange r = new SourceRange(5, 10);
    // ends before
    assertFalse(r.covers(new SourceRange(0, 3)));
    // starts after
    assertFalse(r.covers(new SourceRange(20, 3)));
    // only intersects
    assertFalse(r.covers(new SourceRange(0, 10)));
    assertFalse(r.covers(new SourceRange(10, 10)));
    // covers
    assertTrue(r.covers(new SourceRange(5, 10)));
    assertTrue(r.covers(new SourceRange(6, 9)));
    assertTrue(r.covers(new SourceRange(6, 8)));
  }

  public void test_endsIn() throws Exception {
    SourceRange r = new SourceRange(5, 10);
    // ends before
    assertFalse(r.endsIn(new SourceRange(20, 10)));
    // starts after
    assertFalse(r.endsIn(new SourceRange(0, 3)));
    // ends
    assertTrue(r.endsIn(new SourceRange(10, 20)));
    assertTrue(r.endsIn(new SourceRange(0, 20)));
  }

  public void test_equals() throws Exception {
    SourceRange r = new SourceRange(10, 1);
    assertFalse(r.equals(null));
    assertFalse(r.equals(this));
    assertFalse(r.equals(new SourceRange(20, 2)));
    assertTrue(r.equals(new SourceRange(10, 1)));
    assertTrue(r.equals(r));
  }

  public void test_getExpanded() throws Exception {
    SourceRange r = new SourceRange(5, 3);
    assertEquals(r, r.getExpanded(0));
    assertEquals(new SourceRange(3, 7), r.getExpanded(2));
    assertEquals(new SourceRange(6, 1), r.getExpanded(-1));
  }

  public void test_getMoveEnd() throws Exception {
    SourceRange r = new SourceRange(5, 3);
    assertEquals(r, r.getMoveEnd(0));
    assertEquals(new SourceRange(5, 6), r.getMoveEnd(3));
    assertEquals(new SourceRange(5, 2), r.getMoveEnd(-1));
  }

  public void test_intersects() throws Exception {
    SourceRange r = new SourceRange(5, 3);
    // null
    assertFalse(r.intersects(null));
    // ends before
    assertFalse(r.intersects(new SourceRange(0, 5)));
    // begins after
    assertFalse(r.intersects(new SourceRange(8, 5)));
    // begins on same offset
    assertTrue(r.intersects(new SourceRange(5, 1)));
    // begins inside, ends inside
    assertTrue(r.intersects(new SourceRange(6, 1)));
    // begins inside, ends after
    assertTrue(r.intersects(new SourceRange(6, 10)));
    // begins before, ends after
    assertTrue(r.intersects(new SourceRange(0, 10)));
  }

  public void test_startsIn() throws Exception {
    SourceRange r = new SourceRange(5, 10);
    // ends before
    assertFalse(r.startsIn(new SourceRange(20, 10)));
    // starts after
    assertFalse(r.startsIn(new SourceRange(0, 3)));
    // starts
    assertTrue(r.startsIn(new SourceRange(5, 1)));
    assertTrue(r.startsIn(new SourceRange(0, 20)));
  }

  public void test_toString() throws Exception {
    SourceRange r = new SourceRange(10, 1);
    assertEquals("[offset=10, length=1]", r.toString());
  }
}
