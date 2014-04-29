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

import com.google.dart.server.Outline;
import com.google.dart.server.OutlineKind;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class OutlineImplTest extends TestCase {
  public void test_access() throws Exception {
    Outline childA = mock(Outline.class);
    Outline childB = mock(Outline.class);
    when(childA.toString()).thenReturn("childA");
    when(childB.toString()).thenReturn("childB");
    Outline parent = childA;
    Outline[] children = new Outline[] {childA, childB};
    OutlineImpl outline = new OutlineImpl(
        parent,
        new SourceRegionImpl(1, 2),
        OutlineKind.METHOD,
        "foo",
        10,
        20,
        "(int i, String s)",
        "Map<String, int>",
        true,
        true,
        true);
    assertSame(parent, outline.getParent());
    assertEquals(new SourceRegionImpl(1, 2), outline.getSourceRegion());
    assertSame(OutlineKind.METHOD, outline.getKind());
    assertEquals("foo", outline.getName());
    assertEquals(10, outline.getOffset());
    assertEquals(20, outline.getLength());
    assertEquals("(int i, String s)", outline.getParameters());
    assertEquals("Map<String, int>", outline.getReturnType());
    assertTrue(outline.isAbstract());
    assertTrue(outline.isPrivate());
    assertTrue(outline.isStatic());
    // children
    outline.setChildren(children);
    assertEquals(children, outline.getChildren());
    // toString
    assertEquals("[name=foo, kind=METHOD, offset=10, length=20, parameters=(int i, String s), "
        + "return=Map<String, int>, children=[childA, childB]]", outline.toString());
  }

  public void test_equals() throws Exception {
    OutlineImpl outlineA = new OutlineImpl(
        null,
        new SourceRegionImpl(1, 2),
        OutlineKind.METHOD,
        "aaa",
        1,
        2,
        "()",
        "",
        false,
        false,
        false);
    OutlineImpl outlineA2 = new OutlineImpl(
        null,
        new SourceRegionImpl(1, 2),
        OutlineKind.METHOD,
        "aaa",
        10,
        2,
        "()",
        "",
        false,
        false,
        false);
    OutlineImpl outlineB = new OutlineImpl(
        null,
        new SourceRegionImpl(1, 2),
        OutlineKind.METHOD,
        "bbb",
        10,
        20,
        "()",
        "",
        false,
        false,
        false);
    assertTrue(outlineA.equals(outlineA));
    assertTrue(outlineA.equals(outlineA2));
    assertFalse(outlineA.equals(this));
    assertFalse(outlineA.equals(outlineB));
  }

  public void test_hashCode() throws Exception {
    OutlineImpl unitOutline = new OutlineImpl(
        null,
        new SourceRegionImpl(1, 2),
        OutlineKind.METHOD,
        "foo",
        10,
        20,
        "(int i, String s)",
        "Map<String, int>",
        true,
        true,
        true);
    unitOutline.hashCode();
    OutlineImpl outline = new OutlineImpl(
        unitOutline,
        new SourceRegionImpl(1, 2),
        OutlineKind.METHOD,
        "foo",
        10,
        20,
        "(int i, String s)",
        "Map<String, int>",
        true,
        true,
        true);
    outline.hashCode();
  }
}
