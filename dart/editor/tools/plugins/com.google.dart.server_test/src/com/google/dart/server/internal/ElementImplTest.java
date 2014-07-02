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

import junit.framework.TestCase;

public class ElementImplTest extends TestCase {
//  private String contextId = "my-context-id";
//  private String id = "my-id";
//  private Source source = mock(Source.class);
//
//  public void test_access() throws Exception {
//    ElementImpl element = new ElementImpl(
//        contextId,
//        id,
//        source,
//        ElementKind.METHOD,
//        "foo",
//        10,
//        20,
//        "(int i, String s)",
//        "Map<String, int>",
//        true,
//        true,
//        true);
//    assertEquals(contextId, element.getContextId());
//    assertEquals(id, element.getId());
//    assertSame(source, element.getSource());
//    assertSame(ElementKind.METHOD, element.getKind());
//    assertEquals("foo", element.getName());
//    assertEquals(10, element.getOffset());
//    assertEquals(20, element.getLength());
//    assertEquals("(int i, String s)", element.getParameters());
//    assertEquals("Map<String, int>", element.getReturnType());
//    assertTrue(element.isAbstract());
//    assertTrue(element.isPrivate());
//    assertTrue(element.isStatic());
//    // toString
//    assertEquals("[name=foo, kind=METHOD, offset=10, length=20, parameters=(int i, String s), "
//        + "return=Map<String, int>]", element.toString());
//  }
//
  public void test_create_ClassElement() throws Exception {
//    ElementLocation location = mock(ElementLocation.class);
//    when(location.getEncoding()).thenReturn("my-id");
//    // mock ClassElement
//    ClassElement engineElement = mock(ClassElement.class);
//    when(engineElement.getLocation()).thenReturn(location);
//    when(engineElement.getKind()).thenReturn(com.google.dart.engine.element.ElementKind.CLASS);
//    when(engineElement.getDisplayName()).thenReturn("MyClass");
//    when(engineElement.getNameOffset()).thenReturn(42);
//    // create server Element
//    ElementImpl element = ElementImpl.create(contextId, engineElement);
//    assertEquals("my-id", element.getId());
//    assertSame(ElementKind.CLASS, element.getKind());
//    assertEquals("MyClass", element.getName());
//    assertEquals(42, element.getOffset());
//    assertEquals("MyClass".length(), element.getLength());
//    assertEquals(false, element.isAbstract());
//    assertEquals(false, element.isPrivate());
//    assertEquals(false, element.isStatic());
  }
//
//  public void test_create_CompilationUnit() throws Exception {
//    ElementLocation location = mock(ElementLocation.class);
//    when(location.getEncoding()).thenReturn("my-id");
//    // mock CompilationUnitElement
//    CompilationUnitElement engineElement = mock(CompilationUnitElement.class);
//    when(engineElement.getLocation()).thenReturn(location);
//    when(engineElement.getKind()).thenReturn(
//        com.google.dart.engine.element.ElementKind.COMPILATION_UNIT);
//    when(engineElement.getDisplayName()).thenReturn("test.dart");
//    when(engineElement.getNameOffset()).thenReturn(42);
//    // create server Element
//    ElementImpl element = ElementImpl.create(contextId, engineElement);
//    assertEquals("my-id", element.getId());
//    assertSame(ElementKind.COMPILATION_UNIT, element.getKind());
//    assertEquals("test.dart", element.getName());
//    assertEquals(-1, element.getOffset());
//    assertEquals(0, element.getLength());
//  }
//
//  public void test_create_null() throws Exception {
//    ElementImpl element = ElementImpl.create(contextId, null);
//    assertSame(null, element);
//  }
//
//  public void test_createId() throws Exception {
//    ElementLocation location = mock(ElementLocation.class);
//    Element element = mock(Element.class);
//    when(element.getLocation()).thenReturn(location);
//    when(location.getEncoding()).thenReturn("my-id");
//    assertEquals("my-id", ElementImpl.createId(element));
//  }
//
//  public void test_createId_null() throws Exception {
//    assertEquals(null, ElementImpl.createId(null));
//  }
//
//  public void test_equals() throws Exception {
//    ElementImpl elementA = new ElementImpl(
//        contextId,
//        id,
//        source,
//        ElementKind.METHOD,
//        "aaa",
//        1,
//        2,
//        "()",
//        "",
//        false,
//        false,
//        false);
//    ElementImpl elementA2 = new ElementImpl(
//        contextId,
//        id,
//        source,
//        ElementKind.METHOD,
//        "aaa",
//        10,
//        2,
//        "()",
//        "",
//        false,
//        false,
//        false);
//    ElementImpl elementB = new ElementImpl(
//        contextId,
//        id,
//        source,
//        ElementKind.METHOD,
//        "bbb",
//        10,
//        20,
//        "()",
//        "",
//        false,
//        false,
//        false);
//    assertTrue(elementA.equals(elementA));
//    assertTrue(elementA.equals(elementA2));
//    assertFalse(elementA.equals(this));
//    assertFalse(elementA.equals(elementB));
//  }
//
//  public void test_hashCode() throws Exception {
//    ElementImpl element = new ElementImpl(
//        contextId,
//        id,
//        source,
//        ElementKind.METHOD,
//        "foo",
//        10,
//        20,
//        "(int i, String s)",
//        "Map<String, int>",
//        true,
//        true,
//        true);
//    element.hashCode();
//  }
//
//  public void test_hashCode_nullName() throws Exception {
//    ElementImpl element = new ElementImpl(
//        contextId,
//        id,
//        source,
//        ElementKind.LIBRARY,
//        null,
//        10,
//        20,
//        null,
//        null,
//        false,
//        false,
//        false);
//    element.hashCode();
//  }
}
