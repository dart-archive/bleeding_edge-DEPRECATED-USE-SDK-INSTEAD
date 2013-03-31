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
package com.google.dart.engine.utilities.collection;

import com.google.dart.engine.utilities.collection.FastRemoveList.Handle;

import junit.framework.TestCase;

import java.util.Iterator;

public class FastRemoveListTest extends TestCase {
  public void test_add() {
    FastRemoveList<String> list = FastRemoveList.newInstance();
    // empty initially
    assertEquals(0, list.size());
    // add "A"
    list.add("A");
    assertEquals(1, list.size());
    // add "B"
    list.add("B");
    assertEquals(2, list.size());
  }

  public void test_Handle_remove() {
    FastRemoveList<String> list = FastRemoveList.newInstance();
    Handle handleA = list.add("A");
    Handle handleB = list.add("B");
    assertEquals(2, list.size());
    // values
    {
      Iterator<String> iter = list.iterator();
      assertTrue(iter.hasNext());
      assertEquals("B", iter.next());
      assertTrue(iter.hasNext());
      assertEquals("A", iter.next());
      assertFalse(iter.hasNext());
    }
    // remove "B"
    handleB.remove();
    {
      Iterator<String> iter = list.iterator();
      assertTrue(iter.hasNext());
      assertEquals("A", iter.next());
      assertFalse(iter.hasNext());
    }
    // remove "A"
    handleA.remove();
    {
      Iterator<String> iter = list.iterator();
      assertFalse(iter.hasNext());
    }
  }

  public void test_Iterator_remove() {
    FastRemoveList<String> list = FastRemoveList.newInstance();
    Iterator<String> iter = list.iterator();
    try {
      iter.remove();
      fail();
    } catch (AbstractMethodError e) {
    }
  }
}
