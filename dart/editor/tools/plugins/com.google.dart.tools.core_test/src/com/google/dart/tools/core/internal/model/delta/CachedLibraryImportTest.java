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
package com.google.dart.tools.core.internal.model.delta;

import junit.framework.TestCase;

/**
 * Test for {@link CachedLibraryImport}.
 */
public class CachedLibraryImportTest extends TestCase {
  public void test_access() throws Exception {
    CachedLibraryImport imp = new CachedLibraryImport("libA.dart", "aaa");
    assertEquals("libA.dart", imp.getPath());
    assertEquals("aaa", imp.getPrefix());
    assertEquals("CachedLibraryImport{path=libA.dart, prefix=aaa}", imp.toString());
  }

  public void test_equals() throws Exception {
    CachedLibraryImport importA = new CachedLibraryImport("pathA", "aaa");
    CachedLibraryImport importB = new CachedLibraryImport("pathB", "bbb");
    assertFalse(importA.equals(null));
    assertFalse(importA.equals(this));
    assertTrue(importA.equals(importA));
    assertFalse(importA.equals(importB));
    assertFalse(importA.equals(new CachedLibraryImport("pathA", null)));
    assertFalse(importA.equals(new CachedLibraryImport(null, "aaa")));
    assertFalse(importA.equals(new CachedLibraryImport(null, null)));
  }

  public void test_hasCode() throws Exception {
    new CachedLibraryImport("path", "prefix").hashCode();
    new CachedLibraryImport("path", null).hashCode();
  }
}
