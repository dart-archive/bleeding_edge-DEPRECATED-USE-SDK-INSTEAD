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

import static org.fest.assertions.Assertions.assertThat;

public class IntToIntSetMapTest extends TestCase {
  private static int HASH_32_KEY_1 = 0x60000;
  private static int HASH_32_KEY_2 = 0x90000;
  private static int HASH_32_KEY_3 = 0x100000;
  private IntToIntSetMap map = new IntToIntSetMap(16, 0.75f);

  public void test_add_duplicate() throws Exception {
    map.add(HASH_32_KEY_1, 1);
    map.add(HASH_32_KEY_1, 1);
    map.add(HASH_32_KEY_1, 10);
    assertThat(map.get(HASH_32_KEY_1)).containsOnly(1, 10);
  }

  public void test_add_hasCollision() throws Exception {
    map = new IntToIntSetMap(33, 0.75f);
    map.add(HASH_32_KEY_1, 10);
    map.add(HASH_32_KEY_1, 11);
    map.add(HASH_32_KEY_2, 20);
    map.add(HASH_32_KEY_2, 21);
    map.add(HASH_32_KEY_2, 22);
    map.add(HASH_32_KEY_3, 30);
    assertThat(map.get(HASH_32_KEY_1)).containsOnly(10, 11);
    assertThat(map.get(HASH_32_KEY_2)).containsOnly(20, 21, 22);
    assertThat(map.get(HASH_32_KEY_3)).containsOnly(30);
  }

  public void test_add_negativeKey() throws Exception {
    try {
      map.add(-1, 10);
      fail();
    } catch (IllegalArgumentException e) {
    }
  }

  public void test_add_noCollision() throws Exception {
    map.add(HASH_32_KEY_1, 1);
    map.add(HASH_32_KEY_1, 2);
    map.add(HASH_32_KEY_1, 3);
    assertThat(map.get(HASH_32_KEY_1)).containsOnly(1, 2, 3);
  }

  public void test_clear() throws Exception {
    map.add(HASH_32_KEY_1, 1);
    assertEquals(1, map.size());
    // clear
    map.clear();
    assertEquals(0, map.size());
  }

  public void test_get_no() throws Exception {
    assertThat(map.get(HASH_32_KEY_1)).isEmpty();
  }

  public void test_size() throws Exception {
    // empty
    assertEquals(0, map.size());
    // 1 key, 1 value
    map.add(HASH_32_KEY_1, 1);
    assertEquals(1, map.size());
    // 1 key, 2 values
    map.add(HASH_32_KEY_1, 2);
    assertEquals(1, map.size());
    // 2 keys
    map.add(HASH_32_KEY_2, 3);
    assertEquals(2, map.size());
  }

  public void test_stress() throws Exception {
    int count = 1000;
    // fill map
    for (int i = 0; i < count; i++) {
      int key = i << 16;
      map.add(key, i * 10);
      map.add(key, i * 100);
      map.add(key, i * 1000);
    }
    // check map
    assertEquals(count, map.size());
    for (int i = 0; i < count; i++) {
      int key = i << 16;
      assertThat(map.get(key)).containsOnly(i * 10, i * 100, i * 1000);
    }
  }
}
