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
package com.google.dart.engine.internal.index.file;

import junit.framework.TestCase;

public class IntArrayToIntMapTest extends TestCase {
  private static int[] HASH_32_KEY_1 = {1};
  private static int[] HASH_32_KEY_2 = {33};
  private static int[] HASH_32_KEY_3 = {65};
  private static int[] HASH_33_KEY = {2};
  private IntArrayToIntMap map = new IntArrayToIntMap(16, 0.75f);

  public void test_get_no() throws Exception {
    assertEquals(-1, map.get(HASH_32_KEY_1, -1));
  }

  public void test_put_conflict_3() throws Exception {
    map.put(HASH_32_KEY_1, 10);
    map.put(HASH_32_KEY_2, 20);
    map.put(HASH_32_KEY_3, 30);
    assertEquals(10, map.get(HASH_32_KEY_1, -1));
    assertEquals(20, map.get(HASH_32_KEY_2, -1));
    assertEquals(30, map.get(HASH_32_KEY_3, -1));
    // update exiting Entry
    map.put(HASH_32_KEY_2, 200);
    assertEquals(10, map.get(HASH_32_KEY_1, -1));
    assertEquals(200, map.get(HASH_32_KEY_2, -1));
    assertEquals(30, map.get(HASH_32_KEY_3, -1));
  }

  public void test_put_noConflict() throws Exception {
    map.put(HASH_32_KEY_1, 10);
    map.put(HASH_33_KEY, 20);
    assertEquals(10, map.get(HASH_32_KEY_1, -1));
    assertEquals(20, map.get(HASH_33_KEY, -1));
  }

  public void test_remove_hasConflict() throws Exception {
    // put
    assertEquals(-1, map.remove(HASH_32_KEY_1, -1));
    assertEquals(-1, map.remove(HASH_32_KEY_2, -1));
    assertEquals(-1, map.remove(HASH_32_KEY_3, -1));
    map.put(HASH_32_KEY_1, 10);
    map.put(HASH_32_KEY_2, 20);
    map.put(HASH_32_KEY_3, 30);
    assertEquals(3, map.size());
    // remove
    assertEquals(10, map.remove(HASH_32_KEY_1, -1));
    assertEquals(2, map.size());
    assertEquals(20, map.remove(HASH_32_KEY_2, -1));
    assertEquals(1, map.size());
    assertEquals(30, map.remove(HASH_32_KEY_3, -1));
    assertEquals(0, map.size());
    // nothing to remove
    assertEquals(-1, map.remove(HASH_32_KEY_1, -1));
    assertEquals(-1, map.remove(HASH_32_KEY_2, -1));
    assertEquals(-1, map.remove(HASH_32_KEY_3, -1));
    assertEquals(0, map.size());
  }

  public void test_remove_noConflict() throws Exception {
    assertEquals(-1, map.remove(HASH_32_KEY_1, -1));
    // put
    map.put(HASH_32_KEY_1, 10);
    assertEquals(1, map.size());
    // remove
    assertEquals(10, map.remove(HASH_32_KEY_1, -1));
    assertEquals(-1, map.remove(HASH_32_KEY_1, -1));
    assertEquals(0, map.size());
  }

  public void test_size() throws Exception {
    // empty
    assertEquals(0, map.size());
    // 1 key
    map.put(new int[] {1}, 10);
    assertEquals(1, map.size());
    // 2 keys
    map.put(new int[] {1, 2}, 20);
    assertEquals(2, map.size());
    // same key
    map.put(new int[] {1, 2}, 200);
    assertEquals(2, map.size());
    // remove
    map.remove(new int[] {1, 2}, -1);
    assertEquals(1, map.size());
  }

  public void test_stress() throws Exception {
    int count = 1000;
    // fill map
    for (int i = 0; i < count; i++) {
      int[] key = new int[] {i << 5, i << 3};
      map.put(key, i);
    }
    // check map
    assertEquals(count, map.size());
    for (int i = 0; i < count; i++) {
      int[] key = new int[] {i << 5, i << 3};
      assertEquals(i, map.get(key, -1));
    }
  }
}
