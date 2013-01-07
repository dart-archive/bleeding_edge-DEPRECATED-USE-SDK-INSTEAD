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

import junit.framework.TestCase;

import java.util.EmptyStackException;

public class IntStackTest extends TestCase {
  public void test_IntStack_1() {
    IntStack stack = new IntStack(20);
    assertEquals(0, stack.size());
  }

  public void test_IntStack_clear() {
    IntStack stack = new IntStack();
    stack.push(1);
    stack.clear();
    assertEquals(0, stack.size());
  }

  public void test_IntStack_constructor() {
    IntStack stack = new IntStack();
    assertEquals(0, stack.size());
  }

  public void test_IntStack_increment_empty() {
    IntStack stack = new IntStack();
    try {
      stack.increment(1);
      fail("Expected EmptyStackException");
    } catch (EmptyStackException exception) {
      // Expected
    }
  }

  public void test_IntStack_increment_nonEmpty() {
    IntStack stack = new IntStack();
    stack.push(1);
    stack.push(2);
    stack.increment(1);
    assertEquals(2, stack.size());
    assertEquals(3, stack.peek());
  }

  public void test_IntStack_isEmpty_empty() {
    IntStack stack = new IntStack();
    assertTrue(stack.isEmpty());
  }

  public void test_IntStack_isEmpty_nonEmpty() {
    IntStack stack = new IntStack();
    stack.push(1);
    assertFalse(stack.isEmpty());
  }

  public void test_IntStack_peek_empty() {
    IntStack stack = new IntStack();
    try {
      stack.peek();
      fail("Expected EmptyStackException");
    } catch (EmptyStackException exception) {
      // Expected
    }
  }

  public void test_IntStack_peek_nonEmpty() {
    IntStack stack = new IntStack();
    stack.push(1);
    stack.push(2);
    assertEquals(2, stack.peek());
    assertEquals(2, stack.size());
    assertEquals(2, stack.peek());
  }

  public void test_IntStack_pop_empty() {
    IntStack stack = new IntStack();
    try {
      stack.pop();
      fail("Expected EmptyStackException");
    } catch (EmptyStackException exception) {
      // Expected
    }
  }

  public void test_IntStack_pop_nonEmpty() {
    IntStack stack = new IntStack();
    stack.push(1);
    stack.push(2);
    assertEquals(2, stack.pop());
    assertEquals(1, stack.size());
    assertEquals(1, stack.peek());
  }

  public void test_IntStack_push_1() {
    IntStack stack = new IntStack();
    stack.push(1);
    assertEquals(1, stack.size());
    assertEquals(1, stack.peek());
  }

  public void test_IntStack_push_2() {
    IntStack stack = new IntStack();
    stack.push(1);
    stack.push(2);
    assertEquals(2, stack.size());
    assertEquals(2, stack.pop());
    assertEquals(1, stack.pop());
  }

  public void test_IntStack_push_20() {
    IntStack stack = new IntStack();
    int count = 20;
    for (int i = 0; i < count; i++) {
      stack.push(i);
    }
    assertEquals(count, stack.size());
  }

  public void test_IntStack_replaceTop_empty() {
    IntStack stack = new IntStack();
    try {
      stack.replaceTop(1);
      fail("Expected EmptyStackException");
    } catch (EmptyStackException exception) {
      // Expected
    }
  }

  public void test_IntStack_replaceTop_nonEmpty() {
    IntStack stack = new IntStack();
    stack.push(1);
    stack.push(2);
    stack.replaceTop(1);
    assertEquals(2, stack.size());
    assertEquals(1, stack.peek());
  }

  public void test_IntStack_size() {
    IntStack stack = new IntStack();
    assertEquals(0, stack.size());
    stack.push(1);
    assertEquals(1, stack.size());
    stack.pop();
    assertEquals(0, stack.size());
  }

  public void test_IntStack_toString_empty() {
    IntStack stack = new IntStack();
    assertEquals("[]", stack.toString());
  }

  public void test_IntStack_toString_one() {
    IntStack stack = new IntStack();
    stack.push(1);
    assertEquals("[1]", stack.toString());
  }

  public void test_IntStack_toString_two() {
    IntStack stack = new IntStack();
    stack.push(1);
    stack.push(2);
    assertEquals("[1, 2]", stack.toString());
  }
}
