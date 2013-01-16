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
package com.google.dart.engine.internal.search.scope;

import com.google.common.collect.ImmutableSet;
import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LibrarySearchScopeTest extends EngineTestCase {
  private final LibraryElement libraryA = mock(LibraryElement.class);
  private final LibraryElement libraryB = mock(LibraryElement.class);
  private final Element element = mock(Element.class);

  public void test_arrayConstructor_inA_false() throws Exception {
    when(element.getAncestor(LibraryElement.class)).thenReturn(libraryB);
    LibrarySearchScope scope = new LibrarySearchScope(libraryA);
    assertThat(scope.getLibraries()).containsOnly(libraryA);
    assertFalse(scope.encloses(element));
  }

  public void test_arrayConstructor_inA_true() throws Exception {
    when(element.getAncestor(LibraryElement.class)).thenReturn(libraryA);
    LibrarySearchScope scope = new LibrarySearchScope(libraryA, libraryB);
    assertThat(scope.getLibraries()).containsOnly(libraryA, libraryB);
    assertTrue(scope.encloses(element));
  }

  public void test_collectionConstructor_inB() throws Exception {
    when(element.getAncestor(LibraryElement.class)).thenReturn(libraryB);
    LibrarySearchScope scope = new LibrarySearchScope(ImmutableSet.of(libraryA, libraryB));
    assertThat(scope.getLibraries()).containsOnly(libraryA, libraryB);
    assertTrue(scope.encloses(element));
  }
}
