/*
 * Copyright (c) 2014, the Dart project authors.
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

package com.google.dart.server.internal;

import com.google.dart.server.HighlightType;
import com.google.dart.server.internal.HighlightRegionImpl;

import junit.framework.TestCase;

public class HighlightRegionImplTest extends TestCase {

  public void test_access() throws Exception {
    HighlightRegionImpl region = new HighlightRegionImpl(10, 20, HighlightType.KEYWORD);
    assertEquals(10, region.getOffset());
    assertEquals(20, region.getLength());
    assertSame(HighlightType.KEYWORD, region.getType());
  }

  public void test_toString() throws Exception {
    HighlightRegionImpl region = new HighlightRegionImpl(10, 20, HighlightType.KEYWORD);
    assertEquals("[offset=10, length=20, type=KEYWORD]", region.toString());
  }
}
