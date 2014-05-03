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

package com.google.dart.server.internal.local.computer;

import com.google.dart.server.Element;
import com.google.dart.server.Outline;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OutlineImplTest extends TestCase {
  private Element elementA = mock(Element.class);
  private Element elementB = mock(Element.class);

  public void test_access() throws Exception {
    Outline childA = mock(Outline.class);
    Outline childB = mock(Outline.class);
    when(childA.toString()).thenReturn("childA");
    when(childB.toString()).thenReturn("childB");
    Outline parent = childA;
    Outline[] children = new Outline[] {childA, childB};
    OutlineImpl outline = new OutlineImpl(parent, elementA, new SourceRegionImpl(1, 2));
    assertSame(parent, outline.getParent());
    assertEquals(new SourceRegionImpl(1, 2), outline.getSourceRegion());
    assertSame(elementA, outline.getElement());
    // children
    outline.setChildren(children);
    assertEquals(children, outline.getChildren());
    // toString
    assertEquals("[element=elementA, children=[childA, childB]]", outline.toString());
  }

  public void test_equals() throws Exception {
    OutlineImpl outlineA = new OutlineImpl(null, elementA, new SourceRegionImpl(1, 2));
    OutlineImpl outlineA2 = new OutlineImpl(null, elementA, new SourceRegionImpl(1, 2));
    OutlineImpl outlineB = new OutlineImpl(null, elementB, new SourceRegionImpl(1, 2));
    assertTrue(outlineA.equals(outlineA));
    assertTrue(outlineA.equals(outlineA2));
    assertFalse(outlineA.equals(this));
    assertFalse(outlineA.equals(outlineB));
  }

  public void test_hashCode() throws Exception {
    OutlineImpl unitOutline = new OutlineImpl(null, elementA, new SourceRegionImpl(1, 2));
    unitOutline.hashCode();
    OutlineImpl outline = new OutlineImpl(unitOutline, elementB, new SourceRegionImpl(10, 20));
    outline.hashCode();
  }

  @Override
  protected void setUp() throws Exception {
    when(elementA.toString()).thenReturn("elementA");
    when(elementB.toString()).thenReturn("elementB");
    super.setUp();
  }
}
