/*
 * Copyright 2011, the Dart project authors.
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

public class StringUtilsTest extends TestCase {
  private static final String[] DATA = {"a", "b", "c", "d"};

  public void test_StringUtils_indent() {
    for (int i = -2; i < 6; i++) {
      String indent = StringUtils.indent(i);
      assertNotNull(indent);
      if (i < 0) {
        assertEquals(0, indent.length());
      } else {
        assertEquals(i * 2, indent.length());
      }
    }
  }

  public void test_StringUtils_join1() {
    assertEquals("a//b//c//d", StringUtils.join(DATA));
  }

  public void test_StringUtils_join2() {
    assertEquals("a:b:c:d", StringUtils.join(DATA, ":"));
  }

  public void test_StringUtils_join4_all() {
    assertEquals("a:b:c:d", StringUtils.join(DATA, 0, -1, ":"));
  }

  public void test_StringUtils_join4_empty() {
    assertEquals("", StringUtils.join(DATA, 2, 1, ":"));
  }

  public void test_StringUtils_join4_middle() {
    assertEquals("b:c", StringUtils.join(DATA, 1, 3, ":"));
  }
}
