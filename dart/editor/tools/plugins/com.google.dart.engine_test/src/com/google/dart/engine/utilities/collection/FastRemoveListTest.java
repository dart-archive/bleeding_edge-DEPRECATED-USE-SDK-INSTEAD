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

import com.google.common.collect.Sets;

import junit.framework.TestCase;

import static org.fest.assertions.Assertions.assertThat;

import java.util.Iterator;
import java.util.Set;

public class FastRemoveListTest extends TestCase {
  public void test_add() {
    FastRemoveList<String> list = FastRemoveList.newInstance();
    // empty initially
    assertEquals(0, list.size());
    // add "A"
    list.add("A");
    assertEquals(1, list.size());
    assertThat(Sets.newHashSet(list)).containsOnly("A");
    // add "B"
    list.add("B");
    assertEquals(2, list.size());
    assertThat(Sets.newHashSet(list)).containsOnly("A", "B");
    // add "C"
    list.add("C");
    assertEquals(3, list.size());
    assertThat(Sets.newHashSet(list)).containsOnly("A", "B", "C");
  }

  public void test_add_afterFillRemove() {
    FastRemoveList<String> list = FastRemoveList.newInstance();
    // "A" and "B"
    int handleA = list.add("A");
    list.add("B");
    // remove "A"
    list.remove(handleA);
    assertEquals(1, list.size());
    assertThat(Sets.newHashSet(list)).containsOnly("B");
    // add "C"
    list.add("C");
    assertEquals(2, list.size());
    // values
    {
      Set<String> elements = Sets.newHashSet(list);
      assertThat(elements).containsOnly("B", "C");
    }
  }

  public void test_add_many() {
    FastRemoveList<String> list = FastRemoveList.newInstance();
    for (int i = 0; i < 50; i++) {
      list.add("V-" + i);
      assertEquals(i + 1, list.size());
    }
  }

  public void test_add_null() {
    FastRemoveList<String> list = FastRemoveList.newInstance();
    try {
      list.add(null);
    } catch (NullPointerException e) {
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
