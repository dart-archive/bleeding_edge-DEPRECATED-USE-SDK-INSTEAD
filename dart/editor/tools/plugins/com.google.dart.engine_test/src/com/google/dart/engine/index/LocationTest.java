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
package com.google.dart.engine.index;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.element.Element;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LocationTest extends EngineTestCase {
  public void test_new() throws Exception {
    Element element = mock(Element.class);
    when(element.toString()).thenReturn("myElement");
    // test Location
    Location location = new Location(element, 1, 2);
    assertSame(element, location.getElement());
    assertEquals(1, location.getOffset());
    assertEquals(2, location.getLength());
    assertEquals("[1 - 3) in myElement", location.toString());
  }

  public void test_new_nullElement() throws Exception {
    try {
      new Location(null, 1, 2);
      fail();
    } catch (IllegalArgumentException e) {
    }
  }
}
