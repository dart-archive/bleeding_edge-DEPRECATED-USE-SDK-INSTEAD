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
package com.google.dart.engine.internal.element;

import com.google.dart.engine.EngineTestCase;

public class ElementLocationImplTest extends EngineTestCase {
  public void test_create_encoding() {
    String encoding = "a;b;c";
    ElementLocationImpl location = new ElementLocationImpl(encoding);
    assertEquals(encoding, location.getEncoding());
  }

  /**
   * For example unnamed constructor.
   */
  public void test_create_encoding_emptyLast() {
    String encoding = "a;b;c;";
    ElementLocationImpl location = new ElementLocationImpl(encoding);
    assertEquals(encoding, location.getEncoding());
  }

  public void test_equals_equal() {
    String encoding = "a;b;c";
    ElementLocationImpl first = new ElementLocationImpl(encoding);
    ElementLocationImpl second = new ElementLocationImpl(encoding);
    assertTrue(first.equals(second));
  }

  public void test_equals_notEqual_differentLengths() {
    ElementLocationImpl first = new ElementLocationImpl("a;b;c");
    ElementLocationImpl second = new ElementLocationImpl("a;b;c;d");
    assertFalse(first.equals(second));
  }

  public void test_equals_notEqual_notLocation() {
    ElementLocationImpl first = new ElementLocationImpl("a;b;c");
    assertFalse(first.equals("a;b;d"));
  }

  public void test_equals_notEqual_sameLengths() {
    ElementLocationImpl first = new ElementLocationImpl("a;b;c");
    ElementLocationImpl second = new ElementLocationImpl("a;b;d");
    assertFalse(first.equals(second));
  }

  public void test_getComponents() {
    String encoding = "a;b;c";
    ElementLocationImpl location = new ElementLocationImpl(encoding);
    String[] components = location.getComponents();
    assertLength(3, components);
    assertEquals("a", components[0]);
    assertEquals("b", components[1]);
    assertEquals("c", components[2]);
  }

  public void test_getEncoding() {
    String encoding = "a;b;c;;d";
    ElementLocationImpl location = new ElementLocationImpl(encoding);
    assertEquals(encoding, location.getEncoding());
  }

  public void test_hashCode_equal() {
    String encoding = "a;b;c";
    ElementLocationImpl first = new ElementLocationImpl(encoding);
    ElementLocationImpl second = new ElementLocationImpl(encoding);
    assertTrue(first.hashCode() == second.hashCode());
  }
}
