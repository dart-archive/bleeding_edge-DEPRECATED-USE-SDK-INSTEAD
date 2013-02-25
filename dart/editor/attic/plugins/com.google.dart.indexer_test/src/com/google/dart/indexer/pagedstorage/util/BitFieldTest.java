/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.indexer.pagedstorage.util;

import junit.framework.TestCase;

public class BitFieldTest extends TestCase {
  public void test_BitField() {
    BitField bitField = new BitField();
    assertNotNull(bitField);
  }

  public void test_BitField_clear() {
    BitField bitField = new BitField();
    int index = 3;
    bitField.clear(index);
    assertFalse(bitField.get(index));
  }

  public void test_BitField_get() {
    BitField bitField = new BitField();
    int index = 7;
    assertFalse(bitField.get(index));
  }

  public void test_BitField_getByte() {
    BitField bitField = new BitField();
    int index = 8;
    assertEquals(0, bitField.getByte(index));
    bitField.set(11);
    bitField.set(13);
    assertEquals(0x28, bitField.getByte(index)); // 0010 1000
  }

  public void test_BitField_getLastSetBit() {
    BitField bitField = new BitField();
    assertEquals(-1, bitField.getLastSetBit());
    int index = 7;
    bitField.set(index);
    assertEquals(index, bitField.getLastSetBit());
    index = 91;
    bitField.set(index);
    assertEquals(index, bitField.getLastSetBit());
    bitField.set(23);
    assertEquals(index, bitField.getLastSetBit());
  }

  public void test_BitField_getLong() {
    BitField bitField = new BitField();
    int index = 64;
    assertEquals(0, bitField.getLong(index));
    bitField.set(75);
    bitField.set(77);
    assertEquals(0x0000000000002800, bitField.getLong(index));
  }

  public void test_BitField_nextClearBit() {
    BitField bitField = new BitField();
    bitField.set(7);
    bitField.set(23);
    assertEquals(0, bitField.nextClearBit(0));
    assertEquals(6, bitField.nextClearBit(6));
    assertEquals(8, bitField.nextClearBit(7));
  }

  public void test_BitField_nextSetBit() {
    BitField bitField = new BitField();
    bitField.set(7);
    bitField.set(23);
    assertEquals(7, bitField.nextSetBit(0));
    assertEquals(7, bitField.nextSetBit(7));
    assertEquals(23, bitField.nextSetBit(8));
  }

  public void test_BitField_set() {
    BitField bitField = new BitField();
    int index = 7;
    bitField.set(index);
    assertTrue(bitField.get(index));
  }

  public void test_BitField_setRange() {
    BitField bitField = new BitField();
    int index = 7;
    assertFalse(bitField.get(6));
    assertFalse(bitField.get(7));
    assertFalse(bitField.get(8));
    assertFalse(bitField.get(9));
    assertFalse(bitField.get(10));
    bitField.setRange(index, 3, true);
    assertFalse(bitField.get(6));
    assertTrue(bitField.get(7));
    assertTrue(bitField.get(8));
    assertTrue(bitField.get(9));
    assertFalse(bitField.get(10));
  }
}
