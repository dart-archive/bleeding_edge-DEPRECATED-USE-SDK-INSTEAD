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
package com.google.dart.engine.search;

import com.google.common.collect.ImmutableList;
import com.google.dart.engine.EngineTestCase;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.internal.search.scope.LibrarySearchScope;
import com.google.dart.engine.internal.search.scope.UniverseSearchScope;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class SearchScopeFactoryTest extends EngineTestCase {
  public void test_createLibraryScope_array() throws Exception {
    LibraryElement libraryA = mock(LibraryElement.class);
    LibraryElement libraryB = mock(LibraryElement.class);
    SearchScope scope = SearchScopeFactory.createLibraryScope(libraryA, libraryB);
    assertThat(scope).isInstanceOf(LibrarySearchScope.class);
  }

  public void test_createLibraryScope_collection() throws Exception {
    LibraryElement libraryA = mock(LibraryElement.class);
    LibraryElement libraryB = mock(LibraryElement.class);
    SearchScope scope = SearchScopeFactory.createLibraryScope(ImmutableList.of(libraryA, libraryB));
    assertThat(scope).isInstanceOf(LibrarySearchScope.class);
  }

  public void test_createLibraryScope_single() throws Exception {
    LibraryElement library = mock(LibraryElement.class);
    SearchScope scope = SearchScopeFactory.createLibraryScope(library);
    assertThat(scope).isInstanceOf(LibrarySearchScope.class);
  }

  public void test_createUniverseScope() throws Exception {
    SearchScope scope = SearchScopeFactory.createUniverseScope();
    assertThat(scope).isInstanceOf(UniverseSearchScope.class);
  }
}
