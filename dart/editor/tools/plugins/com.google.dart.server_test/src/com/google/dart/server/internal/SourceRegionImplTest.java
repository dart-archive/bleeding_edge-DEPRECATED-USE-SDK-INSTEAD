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
 */

package com.google.dart.server.internal;

import com.google.dart.server.internal.SourceRegionImpl;

import junit.framework.TestCase;

public class SourceRegionImplTest extends TestCase {
  public void test_access() throws Exception {
    SourceRegionImpl sourceRegion = new SourceRegionImpl(10, 20);
    assertEquals(10, sourceRegion.getOffset());
    assertEquals(20, sourceRegion.getLength());
  }

  public void test_containsInclusive() throws Exception {
    SourceRegionImpl sourceRegion = new SourceRegionImpl(10, 20);
    assertFalse(sourceRegion.containsInclusive(0));
    assertFalse(sourceRegion.containsInclusive(9));
    assertTrue(sourceRegion.containsInclusive(10));
    assertTrue(sourceRegion.containsInclusive(15));
    assertTrue(sourceRegion.containsInclusive(29));
    assertTrue(sourceRegion.containsInclusive(30));
    assertFalse(sourceRegion.containsInclusive(31));
    assertFalse(sourceRegion.containsInclusive(50));
  }

  public void test_equals() throws Exception {
    SourceRegionImpl sourceRegionA = new SourceRegionImpl(1, 2);
    SourceRegionImpl sourceRegionA2 = new SourceRegionImpl(1, 2);
    SourceRegionImpl sourceRegionB = new SourceRegionImpl(10, 20);
    assertTrue(sourceRegionA.equals(sourceRegionA));
    assertTrue(sourceRegionA.equals(sourceRegionA2));
    assertFalse(sourceRegionA.equals(this));
    assertFalse(sourceRegionA.equals(sourceRegionB));
  }

  public void test_hashCode() throws Exception {
    SourceRegionImpl sourceRegion = new SourceRegionImpl(10, 20);
    sourceRegion.hashCode();
  }

  public void test_toString() throws Exception {
    SourceRegionImpl sourceRegion = new SourceRegionImpl(10, 20);
    assertEquals("[offset=10, length=20]", sourceRegion.toString());
  }
}
