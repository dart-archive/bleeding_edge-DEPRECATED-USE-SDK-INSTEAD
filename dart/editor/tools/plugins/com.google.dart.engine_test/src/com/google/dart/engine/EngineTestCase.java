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
package com.google.dart.engine;

import com.google.dart.engine.ast.NodeList;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

/**
 * The class {@code EngineTestCase} defines utility methods for making assertions.
 */
public class EngineTestCase extends TestCase {
  /**
   * Assert that the given list is empty.
   * 
   * @param elementType a textual description of the kind of nodes in the list
   * @param list the list being tested
   * @throws AssertionFailedError if the list is not empty
   */
  public static void assertEmpty(String elementType, NodeList<?> list) {
    if (!list.isEmpty()) {
      fail("Expected " + elementType + " list to be empty; contained " + list.size() + " elements");
    }
  }

  /**
   * Assert that the given object is an instance of the expected class.
   * 
   * @param expectedClass the class that the object is expected to be an instance of
   * @param object the object being tested
   * @return the object that was being tested
   * @throws Exception if the object is not an instance of the expected class
   */
  @SuppressWarnings("unchecked")
  public static <E> E assertInstanceOf(Class<E> expectedClass, Object object) {
    if (!expectedClass.isInstance(object)) {
      fail("Expected instance of " + expectedClass.getName() + ", found "
          + (object == null ? "null" : object.getClass().getName()));
    }
    return (E) object;
  }
}
