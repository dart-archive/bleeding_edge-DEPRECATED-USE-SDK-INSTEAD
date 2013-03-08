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
  public void test_equals() throws Exception {
    Element elementA = mock(Element.class);
    Element elementB = mock(Element.class);
    Location location = new Location(elementA, 1, 2, null);
    // not Location
    assertFalse(location.equals(null));
    assertFalse(location.equals(this));
    // true
    assertTrue(location.equals(location));
    assertTrue(location.equals(new Location(elementA, 1, 2, null)));
    // not equal attribute
    assertFalse(location.equals(new Location(elementB, 1, 2, null)));
    assertFalse(location.equals(new Location(elementA, 10, 2, null)));
    assertFalse(location.equals(new Location(elementA, 1, 20, null)));
    assertFalse(location.equals(new Location(elementA, 1, 2, "pref")));
  }

  public void test_hashCode() throws Exception {
    Element element = mock(Element.class);
    Location location = new Location(element, 1, 2, null);
    // just ask
    location.hashCode();
  }

  public void test_new() throws Exception {
    Element element = mock(Element.class);
    when(element.toString()).thenReturn("myElement");
    // test Location
    Location location = new Location(element, 1, 2, null);
    assertSame(element, location.getElement());
    assertEquals(1, location.getOffset());
    assertEquals(2, location.getLength());
    assertSame(null, location.getImportPrefix());
    assertEquals("[1 - 3) in myElement", location.toString());
  }

  public void test_new_nullElement() throws Exception {
    try {
      new Location(null, 1, 2, null);
      fail();
    } catch (IllegalArgumentException e) {
    }
  }

  public void test_new_withPrefix() throws Exception {
    Element element = mock(Element.class);
    when(element.toString()).thenReturn("myElement");
    when(element.toString()).thenReturn("myElement");
    // test Location
    Location location = new Location(element, 1, 2, "pref");
    assertSame(element, location.getElement());
    assertEquals(1, location.getOffset());
    assertEquals(2, location.getLength());
    assertEquals("pref", location.getImportPrefix());
    assertEquals("[1 - 3) in myElement with prefix 'pref'", location.toString());
  }
}
