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
package com.google.dart.engine.utilities.general;

import junit.framework.TestCase;

public class StringUtilitiesTest extends TestCase {
  public void test_EMPTY() {
    assertEquals("", StringUtilities.EMPTY);
    assertTrue(StringUtilities.EMPTY.isEmpty());
  }

  public void test_EMPTY_ARRAY() {
    assertEquals(0, StringUtilities.EMPTY_ARRAY.length);
  }

  public void test_isAlpha() throws Exception {
    assertFalse(StringUtilities.isAlpha(null));
    assertFalse(StringUtilities.isAlpha(""));
    assertFalse(StringUtilities.isAlpha("-"));
    assertFalse(StringUtilities.isAlpha("0"));
    assertFalse(StringUtilities.isAlpha("0a"));
    assertFalse(StringUtilities.isAlpha("a0"));
    assertFalse(StringUtilities.isAlpha("a b"));
    assertTrue(StringUtilities.isAlpha("a"));
    assertTrue(StringUtilities.isAlpha("ab"));
  }

  public void test_isEmpty() {
    assertTrue(StringUtilities.isEmpty(""));
    assertFalse(StringUtilities.isEmpty(" "));
    assertFalse(StringUtilities.isEmpty("a"));
    assertTrue(StringUtilities.isEmpty(StringUtilities.EMPTY));
  }

  public void test_isTagName() throws Exception {
    assertFalse(StringUtilities.isTagName(null));
    assertFalse(StringUtilities.isTagName(""));
    assertFalse(StringUtilities.isTagName("-"));
    assertFalse(StringUtilities.isTagName("0"));
    assertFalse(StringUtilities.isTagName("0a"));
    assertFalse(StringUtilities.isTagName("a b"));
    assertTrue(StringUtilities.isTagName("a0"));
    assertTrue(StringUtilities.isTagName("a"));
    assertTrue(StringUtilities.isTagName("ab"));
    assertTrue(StringUtilities.isTagName("a-b"));
  }

  public void test_substringBefore() {
    assertEquals(null, StringUtilities.substringBefore(null, ""));
    assertEquals(null, StringUtilities.substringBefore(null, "a"));
    assertEquals("", StringUtilities.substringBefore("", "a"));
    assertEquals("", StringUtilities.substringBefore("abc", "a"));
    assertEquals("a", StringUtilities.substringBefore("abcba", "b"));
    assertEquals("ab", StringUtilities.substringBefore("abc", "c"));
    assertEquals("abc", StringUtilities.substringBefore("abc", "d"));
    assertEquals("", StringUtilities.substringBefore("abc", ""));
    assertEquals("abc", StringUtilities.substringBefore("abc", null));
  }
}
