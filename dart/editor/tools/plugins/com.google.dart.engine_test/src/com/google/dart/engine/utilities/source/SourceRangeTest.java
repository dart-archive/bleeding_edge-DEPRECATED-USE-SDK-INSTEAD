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
package com.google.dart.engine.utilities.source;

import junit.framework.TestCase;

public class SourceRangeTest extends TestCase {
  public void test_access() throws Exception {
    SourceRange r = new SourceRange(10, 1);
    assertEquals(10, r.getOffset());
    assertEquals(1, r.getLength());
    assertEquals(10 + 1, r.getEnd());
    // to check
    r.hashCode();
  }

  public void test_equals() throws Exception {
    SourceRange r = new SourceRange(10, 1);
    assertFalse(r.equals(null));
    assertFalse(r.equals(this));
    assertFalse(r.equals(new SourceRange(20, 2)));
    assertTrue(r.equals(new SourceRange(10, 1)));
    assertTrue(r.equals(r));
  }

  public void test_toString() throws Exception {
    SourceRange r = new SourceRange(10, 1);
    assertEquals("[offset=10, length=1]", r.toString());
  }
}
