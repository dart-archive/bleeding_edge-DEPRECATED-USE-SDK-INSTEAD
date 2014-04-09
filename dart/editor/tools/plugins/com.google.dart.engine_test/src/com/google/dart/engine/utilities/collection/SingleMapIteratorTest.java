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
import java.util.NoSuchElementException;

public class SingleMapIteratorTest extends EngineTestCase {
  public void test_empty() {
    HashMap<String, String> map = new HashMap<String, String>();
    SingleMapIterator<String, String> iterator = new SingleMapIterator<String, String>(map);
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
    assertFalse(iterator.moveNext());
  }

  public void test_multiple() {
    HashMap<String, String> map = new HashMap<String, String>();
    map.put("k1", "v1");
    map.put("k2", "v2");
    map.put("k3", "v3");
    SingleMapIterator<String, String> iterator = new SingleMapIterator<String, String>(map);
    assertTrue(iterator.moveNext());
    assertTrue(iterator.moveNext());
    assertTrue(iterator.moveNext());
    assertFalse(iterator.moveNext());
  }

  public void test_single() {
    String key = "key";
    String value = "value";
    HashMap<String, String> map = new HashMap<String, String>();
    map.put(key, value);
    SingleMapIterator<String, String> iterator = new SingleMapIterator<String, String>(map);
    assertTrue(iterator.moveNext());
    assertSame(key, iterator.getKey());
    assertSame(value, iterator.getValue());
    String newValue = "newValue";
    iterator.setValue(newValue);
    assertSame(newValue, iterator.getValue());
    assertFalse(iterator.moveNext());
  }
}
