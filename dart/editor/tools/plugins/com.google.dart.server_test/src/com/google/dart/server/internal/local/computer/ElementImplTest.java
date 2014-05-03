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

import com.google.dart.engine.source.Source;
import com.google.dart.server.ElementKind;

import junit.framework.TestCase;

import static org.mockito.Mockito.mock;

public class ElementImplTest extends TestCase {
  private Source source = mock(Source.class);

  public void test_access() throws Exception {
    ElementImpl element = new ElementImpl(
        source,
        ElementKind.METHOD,
        "foo",
        10,
        20,
        "(int i, String s)",
        "Map<String, int>",
        true,
        true,
        true);
    assertSame(source, element.getSource());
    assertSame(ElementKind.METHOD, element.getKind());
    assertEquals("foo", element.getName());
    assertEquals(10, element.getOffset());
    assertEquals(20, element.getLength());
    assertEquals("(int i, String s)", element.getParameters());
    assertEquals("Map<String, int>", element.getReturnType());
    assertTrue(element.isAbstract());
    assertTrue(element.isPrivate());
    assertTrue(element.isStatic());
    // toString
    assertEquals("[name=foo, kind=METHOD, offset=10, length=20, parameters=(int i, String s), "
        + "return=Map<String, int>]", element.toString());
  }

  public void test_equals() throws Exception {
    ElementImpl elementA = new ElementImpl(
        source,
        ElementKind.METHOD,
        "aaa",
        1,
        2,
        "()",
        "",
        false,
        false,
        false);
    ElementImpl elementA2 = new ElementImpl(
        source,
        ElementKind.METHOD,
        "aaa",
        10,
        2,
        "()",
        "",
        false,
        false,
        false);
    ElementImpl elementB = new ElementImpl(
        source,
        ElementKind.METHOD,
        "bbb",
        10,
        20,
        "()",
        "",
        false,
        false,
        false);
    assertTrue(elementA.equals(elementA));
    assertTrue(elementA.equals(elementA2));
    assertFalse(elementA.equals(this));
    assertFalse(elementA.equals(elementB));
  }

  public void test_hashCode() throws Exception {
    ElementImpl element = new ElementImpl(
        source,
        ElementKind.METHOD,
        "foo",
        10,
        20,
        "(int i, String s)",
        "Map<String, int>",
        true,
        true,
        true);
    element.hashCode();
  }

  public void test_hashCode_nullName() throws Exception {
    ElementImpl element = new ElementImpl(
        source,
        ElementKind.LIBRARY,
        null,
        10,
        20,
        null,
        null,
        false,
        false,
        false);
    element.hashCode();
  }
}
