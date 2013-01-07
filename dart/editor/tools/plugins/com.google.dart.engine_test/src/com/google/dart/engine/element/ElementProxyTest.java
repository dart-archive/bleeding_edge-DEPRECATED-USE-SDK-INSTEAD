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
package com.google.dart.engine.element;

import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.context.AnalysisContext;
import com.google.dart.engine.source.Source;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ElementProxyTest extends EngineTestCase {
  public void test_getSource_ClassElement() throws Exception {
    Source source = mock(Source.class);
    CompilationUnitElement unitElement = mock(CompilationUnitElement.class);
    when(unitElement.getSource()).thenReturn(source);
    ClassElement classElement = mock(ClassElement.class);
    when(classElement.getEnclosingElement()).thenReturn(unitElement);
    // verify Source
    ElementProxy proxy = new ElementProxy(classElement);
    assertSame(source, proxy.getSource());
  }

  public void test_getSource_CompilationUnitElement() throws Exception {
    Source source = mock(Source.class);
    CompilationUnitElement element = mock(CompilationUnitElement.class);
    when(element.getSource()).thenReturn(source);
    // verify Source
    ElementProxy proxy = new ElementProxy(element);
    assertSame(source, proxy.getSource());
  }

  public void test_getSource_LibraryElement() throws Exception {
    Source source = mock(Source.class);
    CompilationUnitElement unitElement = mock(CompilationUnitElement.class);
    when(unitElement.getSource()).thenReturn(source);
    LibraryElement libraryElement = mock(LibraryElement.class);
    when(libraryElement.getDefiningCompilationUnit()).thenReturn(unitElement);
    // verify Source
    ElementProxy proxy = new ElementProxy(libraryElement);
    assertSame(source, proxy.getSource());
  }

  public void test_hashCode_equals() throws Exception {
    AnalysisContext context = mock(AnalysisContext.class);
    ElementLocation location = mock(ElementLocation.class);
    // mock Element
    Element element = mock(Element.class);
    when(element.getContext()).thenReturn(context);
    when(element.getLocation()).thenReturn(location);
    // create proxy
    ElementProxy proxy = new ElementProxy(element);
    ElementProxy proxy2 = new ElementProxy(element);
    ElementProxy proxy3 = new ElementProxy(mock(Element.class));
    // hashCode
    assertThat(proxy.hashCode()).isEqualTo(proxy2.hashCode());
    assertThat(proxy.hashCode()).isNotEqualTo(proxy3.hashCode());
    // equals
    assertTrue(proxy.equals(proxy));
    assertTrue(proxy.equals(proxy2));
    assertFalse(proxy.equals(null));
    assertFalse(proxy.equals(proxy3));
  }

  public void test_new_fromElement() throws Exception {
    // prepare mocks
    AnalysisContext context = mock(AnalysisContext.class);
    ElementLocation location = mock(ElementLocation.class);
    ElementKind kind = ElementKind.CLASS;
    String name = "MyClass";
    int nameOffset = 42;
    // mock Element
    Element element = mock(Element.class);
    when(element.getContext()).thenReturn(context);
    when(element.getLocation()).thenReturn(location);
    when(element.getKind()).thenReturn(kind);
    when(element.getName()).thenReturn(name);
    when(element.getNameOffset()).thenReturn(nameOffset);
    // verify proxy
    ElementProxy proxy = new ElementProxy(element);
    assertSame(context, proxy.getContext());
    assertEquals(location, proxy.getLocation());
    assertSame(kind, proxy.getKind());
    assertEquals(name, proxy.getName());
    assertEquals(nameOffset, proxy.getNameOffset());
  }

  public void test_new_fromElementProperties() throws Exception {
    // prepare mocks
    AnalysisContext context = mock(AnalysisContext.class);
    Source source = mock(Source.class);
    ElementLocation location = mock(ElementLocation.class);
    ElementKind kind = ElementKind.CLASS;
    String name = "MyClass";
    int nameOffset = 42;
    // verify proxy
    ElementProxy proxy = new ElementProxy(context, source, location, kind, name, nameOffset);
    assertSame(context, proxy.getContext());
    assertEquals(source, proxy.getSource());
    assertEquals(location, proxy.getLocation());
    assertSame(kind, proxy.getKind());
    assertEquals(name, proxy.getName());
    assertEquals(nameOffset, proxy.getNameOffset());
  }

  public void test_requestElement() throws Exception {
    AnalysisContext context = mock(AnalysisContext.class);
    ElementLocation location = mock(ElementLocation.class);
    // mock Element
    Element element = mock(Element.class);
    when(element.getContext()).thenReturn(context);
    when(element.getLocation()).thenReturn(location);
    // mock AnalysisContext.getElement()
    Element element2 = mock(Element.class);
    when(context.getElement(location)).thenReturn(element2);
    // request Element
    ElementProxy proxy = new ElementProxy(element);
    assertSame(element2, proxy.requestElement());
  }
}
