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
package com.google.dart.engine.timing;

import junit.framework.TestCase;

import java.util.HashMap;

/**
 * The class {@code AccessTimings} defines a test that compares the time it takes to access an
 * instance field through an accessor method and the time it takes to access an entry in a HashMap.
 */
public class AccessTimings extends TestCase {
  private static class TestObject {
    public static TestObject generateList(int count) {
      TestObject head = new TestObject();
      TestObject previous = head;
      for (int i = 0; i < count; i++) {
        TestObject next = new TestObject();
        previous.next = next;
        previous = next;
      }
      return head;
    }

    private TestObject next;

    public TestObject getNext() {
      return next;
    }
  }

  public void test_access() {
    final TestObject list = TestObject.generateList(1000);
    final HashMap<TestObject, TestObject> map = buildMap(list);
    //
    // Measure field access via method invocation.
    //
    // Warm-up
    for (int i = 0; i < 1000; i++) {
      TestObject current = list;
      while (current != null) {
        current = current.getNext();
      }
    }
    // Measured
    long directTime = 0L;
    for (int i = 0; i < 1000; i++) {
      TestObject current = list;
      long start = System.nanoTime();
      while (current != null) {
        current = current.getNext();
      }
      long end = System.nanoTime();
      directTime += (end - start);
    }
    //
    // Measure access via HashMap lookup.
    //
    // Warm-up
    for (int i = 0; i < 1000; i++) {
      TestObject current = list;
      while (current != null) {
        current = map.get(current);
      }
    }
    // Measured
    long lookupTime = 0L;
    for (int i = 0; i < 1000; i++) {
      TestObject current = list;
      long start = System.nanoTime();
      while (current != null) {
        current = map.get(current);
      }
      long end = System.nanoTime();
      lookupTime += (end - start);
    }
    //
    // Print the results.
    //
    System.out.print("Direct = ");
    System.out.print(directTime);
    System.out.println(" ns");
    System.out.print("Lookup = ");
    System.out.print(lookupTime);
    System.out.println(" ns");
  }

  private HashMap<TestObject, TestObject> buildMap(TestObject list) {
    HashMap<TestObject, TestObject> map = new HashMap<TestObject, TestObject>();
    TestObject current = list;
    while (current != null) {
      TestObject next = current.getNext();
      map.put(current, next);
      current = next;
    }
    return map;
  }
}
