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
package com.google.dart.engine.internal.index.file;

import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.index.Location;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LocationDataTest extends TestCase {
  private AnalysisContext context = mock(AnalysisContext.class);
  private ElementCodec elementCodec = mock(ElementCodec.class);

  public void test_newFromData() throws Exception {
    Element element = mock(Element.class);
    when(elementCodec.decode(context, 0)).thenReturn(element);
    LocationData locationData = new LocationData(0, 1, 2);
    Location location = locationData.getLocation(context, elementCodec);
    assertEquals(element, location.getElement());
    assertEquals(1, location.getOffset());
    assertEquals(2, location.getLength());
  }

  public void test_newFromObjects() throws Exception {
    // prepare Element
    Element element = mock(Element.class);
    when(elementCodec.encode(element, false)).thenReturn(42);
    when(elementCodec.decode(context, 42)).thenReturn(element);
    // create
    Location location = new Location(element, 1, 2);
    LocationData locationData = new LocationData(elementCodec, location);
    // touch hashCode()
    locationData.hashCode();
    // equals()
    assertFalse(locationData.equals(null));
    assertTrue(locationData.equals(locationData));
    // getLocation()
    {
      Location newLocation = locationData.getLocation(context, elementCodec);
      assertEquals(element, newLocation.getElement());
      assertEquals(1, newLocation.getOffset());
      assertEquals(2, newLocation.getLength());
    }
    // no Element - no Location
    {
      when(elementCodec.decode(context, 42)).thenReturn(null);
      Location newLocation = locationData.getLocation(context, elementCodec);
      assertNull(newLocation);
    }
  }
}
