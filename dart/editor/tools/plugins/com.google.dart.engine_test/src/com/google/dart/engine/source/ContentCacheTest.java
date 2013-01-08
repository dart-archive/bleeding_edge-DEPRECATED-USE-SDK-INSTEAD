/*
 * Copyright 2013 Dart project authors.
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
package com.google.dart.engine.source;

import junit.framework.TestCase;

public class ContentCacheTest extends TestCase {

  public void test_setContents() {
    Source source = new TestSource();
    ContentCache cache = new ContentCache();
    assertNull(cache.getContents(source));
    String contents = "library lib;";
    cache.setContents(source, contents);
    assertEquals(contents, cache.getContents(source));
    cache.setContents(source, null);
    assertNull(cache.getContents(source));
  }
}
