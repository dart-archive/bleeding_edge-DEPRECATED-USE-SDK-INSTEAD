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
package com.google.dart.engine.utilities.collection;

import junit.framework.TestCase;

public class IntListTest extends TestCase {
  public void test_IntList() {
    IntList firstList = new IntList();
    assertNotNull(firstList);
    assertEquals(0, firstList.size());

    IntList secondList = new IntList(20);
    assertNotNull(secondList);
    assertEquals(0, secondList.size());
  }

  public void test_IntList_add_grow() {
    IntList list = new IntList(2);
    list.add(1);
    list.add(2);
    list.add(3);
    assertEquals(3, list.size());
  }

  public void test_IntList_add_noGrow() {
    IntList list = new IntList(20);
    list.add(1);
    assertEquals(1, list.size());
  }

  public void test_IntList_toArray_empty() {
    IntList list = new IntList(20);
    int[] result = list.toArray();
    assertNotNull(result);
    assertEquals(0, result.length);
  }

  public void test_IntList_toArray_nonEmpty() {
    IntList list = new IntList(20);
    list.add(1);
    list.add(2);
    list.add(3);
    int[] result = list.toArray();
    assertNotNull(result);
    assertEquals(3, result.length);
  }
}
