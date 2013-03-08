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

import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.internal.search.scope.LibrarySearchScope;
import com.google.dart.engine.internal.search.scope.UniverseSearchScope;

import java.util.Collection;

/**
 * The class <code>SearchScopeFactory</code> defines utility methods that can be used to create
 * search scopes.
 * 
 * @coverage dart.engine.search
 */
public final class SearchScopeFactory {
  /**
   * A search scope that encompasses everything in the "universe". Because it does not hold any
   * state there is no reason not to share a single instance.
   */
  private static final SearchScope UNIVERSE_SCOPE = new UniverseSearchScope();

  /**
   * Create a search scope that encompasses everything in the given library.
   * 
   * @param library the library defining which elements are included in the scope
   * @return the search scope that was created
   */
  public static SearchScope createLibraryScope(Collection<LibraryElement> libraries) {
    return new LibrarySearchScope(libraries);
  }

  /**
   * Create a search scope that encompasses everything in the given libraries.
   * 
   * @param libraries the libraries defining which elements are included in the scope
   * @return the search scope that was created
   */
  public static SearchScope createLibraryScope(LibraryElement... libraries) {
    return new LibrarySearchScope(libraries);
  }

  /**
   * Create a search scope that encompasses everything in the given library.
   * 
   * @param library the library defining which elements are included in the scope
   * @return the search scope that was created
   */
  public static SearchScope createLibraryScope(LibraryElement library) {
    return new LibrarySearchScope(library);
  }

  /**
   * Create a search scope that encompasses everything in the universe.
   * 
   * @return the search scope that was created
   */
  public static SearchScope createUniverseScope() {
    return UNIVERSE_SCOPE;
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private SearchScopeFactory() {
    super();
  }

}
