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
package com.google.dart.engine.utilities.collection;

import com.google.dart.engine.EngineTestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public class MultipleMapIteratorTest extends EngineTestCase {
  public void test_multipleMaps_firstEmpty() {
    HashMap<String, String> map1 = new HashMap<String, String>();
    HashMap<String, String> map2 = new HashMap<String, String>();
    map2.put("k2", "v2");
    HashMap<String, String> map3 = new HashMap<String, String>();
    map3.put("k3", "v3");
    MultipleMapIterator<String, String> iterator = iterator(map1, map2, map3);
    assertTrue(iterator.moveNext());
    assertTrue(iterator.moveNext());
    assertFalse(iterator.moveNext());
  }

  public void test_multipleMaps_lastEmpty() {
    HashMap<String, String> map1 = new HashMap<String, String>();
    map1.put("k1", "v1");
    HashMap<String, String> map2 = new HashMap<String, String>();
    map2.put("k2", "v2");
    HashMap<String, String> map3 = new HashMap<String, String>();
    MultipleMapIterator<String, String> iterator = iterator(map1, map2, map3);
    assertTrue(iterator.moveNext());
    assertTrue(iterator.moveNext());
    assertFalse(iterator.moveNext());
  }

  public void test_multipleMaps_middleEmpty() {
    HashMap<String, String> map1 = new HashMap<String, String>();
    map1.put("k1", "v1");
    HashMap<String, String> map2 = new HashMap<String, String>();
    HashMap<String, String> map3 = new HashMap<String, String>();
    map3.put("k3", "v3");
    MultipleMapIterator<String, String> iterator = iterator(map1, map2, map3);
    assertTrue(iterator.moveNext());
    assertTrue(iterator.moveNext());
    assertFalse(iterator.moveNext());
  }

  public void test_multipleMaps_nonEmpty() {
    HashMap<String, String> map1 = new HashMap<String, String>();
    map1.put("k1", "v1");
    HashMap<String, String> map2 = new HashMap<String, String>();
    map2.put("k2", "v2");
    HashMap<String, String> map3 = new HashMap<String, String>();
    map3.put("k3", "v3");
    MultipleMapIterator<String, String> iterator = iterator(map1, map2, map3);
    assertTrue(iterator.moveNext());
    assertTrue(iterator.moveNext());
    assertTrue(iterator.moveNext());
    assertFalse(iterator.moveNext());
  }

  public void test_noMap() {
    MultipleMapIterator<String, String> iterator = iterator();
    assertFalse(iterator.moveNext());
    assertFalse(iterator.moveNext());
  }

  public void test_singleMap_empty() {
    HashMap<String, String> map = new HashMap<String, String>();
    MultipleMapIterator<String, String> iterator = iterator(new Map[] {map});
    assertFalse(iterator.moveNext());
    try {
      iterator.getKey();
      fail("Expected NoSuchElementException");
    } catch (NoSuchElementException exception) {
      // Expected
    }
    try {
      iterator.getValue();
      fail("Expected NoSuchElementException");
    } catch (NoSuchElementException exception) {
      // Expected
    }
    try {
      iterator.setValue("x");
      fail("Expected NoSuchElementException");
    } catch (NoSuchElementException exception) {
      // Expected
    }
  }

  public void test_singleMap_multiple() {
    HashMap<String, String> map = new HashMap<String, String>();
    map.put("k1", "v1");
    map.put("k2", "v2");
    map.put("k3", "v3");
    MultipleMapIterator<String, String> iterator = iterator(map);
    assertTrue(iterator.moveNext());
    assertTrue(iterator.moveNext());
    assertTrue(iterator.moveNext());
    assertFalse(iterator.moveNext());
  }

  public void test_singleMap_single() {
    String key = "key";
    String value = "value";
    HashMap<String, String> map = new HashMap<String, String>();
    map.put(key, value);
    MultipleMapIterator<String, String> iterator = iterator(map);
    assertTrue(iterator.moveNext());
    assertSame(key, iterator.getKey());
    assertSame(value, iterator.getValue());
    String newValue = "newValue";
    iterator.setValue(newValue);
    assertSame(newValue, iterator.getValue());
    assertFalse(iterator.moveNext());
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private MultipleMapIterator<String, String> iterator(Map... maps) {
    return new MultipleMapIterator<String, String>(maps);
  }
}
